/*
 * Copyright (C) 2021 Lucas Nishimura <lucas.nishimura@gmail.com>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package com.osstelecom.db.inventory.manager.session;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import com.osstelecom.db.inventory.manager.configuration.ConfigurationManager;
import com.osstelecom.db.inventory.manager.events.ResourceSchemaUpdatedEvent;
import com.osstelecom.db.inventory.manager.exception.GenericException;
import com.osstelecom.db.inventory.manager.exception.InvalidRequestException;
import com.osstelecom.db.inventory.manager.exception.SchemaNotFoundException;
import com.osstelecom.db.inventory.manager.listeners.EventManagerListener;
import com.osstelecom.db.inventory.manager.resources.BasicResource;
import com.osstelecom.db.inventory.manager.resources.exception.AttributeConstraintViolationException;
import com.osstelecom.db.inventory.manager.resources.model.ResourceAttributeModel;
import com.osstelecom.db.inventory.manager.resources.model.ResourceSchemaModel;
import com.osstelecom.db.inventory.manager.response.CreateResourceSchemaModelResponse;
import com.osstelecom.db.inventory.manager.response.GetSchemasResponse;
import com.osstelecom.db.inventory.manager.response.ListSchemasResponse;
import com.osstelecom.db.inventory.manager.response.PatchResourceSchemaModelResponse;
import com.osstelecom.db.inventory.manager.response.ResourceSchemaResponse;
import java.util.Date;
import org.apache.tools.ant.DirectoryScanner;

/**
 * Classe que lida com os atributos do schema
 *
 * @Todo:Avaliar a refatoração desta classe para remover a complexidade da
 * mesma.
 * @author Lucas Nishimura <lucas.nishimura@gmail.com>
 * @created 14.12.2021
 */
@Service
public class SchemaSession implements RemovalListener<String, ResourceSchemaModel> {

    private String schemaDir;

    private Logger logger = LoggerFactory.getLogger(SchemaSession.class);

    @Autowired
    private UtilSession utilSession;

    @Autowired
    private ConfigurationManager configurationManager;

    @Autowired
    private EventManagerListener eventManager;

    /**
     * Local Cache keeps the Schema for 1 Minute in memory, most of 5k Records
     */
    private Cache<String, ResourceSchemaModel> schemaCache;

    /**
     * Cuida pro processo de incialização e cria um cache com capacidade de
     * armazenamento de 5mil registros, a configuração do cache é ajustavel e
     * parametrizada no arquivo de configuração
     */
    @EventListener(ApplicationReadyEvent.class)
    private void initSchemaSession() {
        this.schemaCache = CacheBuilder
                .newBuilder()
                .maximumSize(5000)
                .removalListener(this)
                .expireAfterWrite(this.configurationManager
                        .loadConfiguration()
                        .getSchemaCacheTTL(), TimeUnit.SECONDS)
                .build();

        try {
            /**
             * Vamos carregar todos os schemas
             */
            this.loadSchemas();
        } catch (SchemaNotFoundException | GenericException ex) {
            logger.error("Failed to Load schemas", ex);
        }
    }

    /**
     * Este método cuida de realizar uma reconciliação Geral na base do
     * netcompass, o problema é que e pode levar alguns dias. Dependendo do
     * tamanho. Não recomendo a execução frequente deste, mas depois que eu vi o
     * Roger atualizando o DB do Arango direto isso pode ser necessário.
     *
     * @throws SchemaNotFoundException
     * @throws GenericException
     */
    public void reconcileSchemas() throws SchemaNotFoundException, GenericException {

        /**
         * Pegamos só o raiz de cada arvore.
         */
        ResourceSchemaModel defaultResource = this.loadSchema("resource.default");
        this.notifyUpdateEvent(defaultResource);

        /**
         * Pegamos agora as connections
         */
        ResourceSchemaModel defaultConnection = this.loadSchema("connection.default");
        this.notifyUpdateEvent(defaultConnection);

        /**
         * Agora os circuitos
         */
        ResourceSchemaModel defaultCircuit = this.loadSchema("circuit.default");
        this.notifyUpdateEvent(defaultCircuit);

        /**
         * Agora os serviços
         */
        ResourceSchemaModel defaultService = this.loadSchema("service.default");
        this.notifyUpdateEvent(defaultService);

    }

    /**
     * Carrega um schema
     *
     * @param schemaName nome do schema a ser carregado
     * @param cached - se true, usará o cache, se false irá ignorar o cache
     * @return
     * @throws SchemaNotFoundException
     * @throws GenericException
     */
    public ResourceSchemaModel loadSchema(String schemaName, Boolean cached)
            throws SchemaNotFoundException, GenericException {
        if (!cached) {
            this.clearSchemaCache();
        }
        return this.loadSchema(schemaName);
    }

    /**
     * Loads the Schema by Name
     *
     * @param schemaName
     * @return
     * @throws SchemaNotFoundException
     * @throws GenericException
     */
    public ResourceSchemaModel loadSchema(String schemaName) throws SchemaNotFoundException, GenericException {
        ResourceSchemaModel schema = this.schemaCache.getIfPresent(schemaName);
        if (schema != null) {
            logger.debug("Hit Cache on Schema: [" + schemaName + "]");
            return schema;
        } else {
            this.schemaDir = configurationManager.loadConfiguration().getSchemaDir();
            schema = this.loadSchemaFromDisk(schemaName, null);
            this.schemaCache.put(schemaName, schema);
            return schema;
        }
    }

    private List<ResourceSchemaModel> loadSchemaFromDisk() throws SchemaNotFoundException, GenericException {
        List<ResourceSchemaModel> result = new ArrayList<>();
        this.schemaDir = configurationManager.loadConfiguration().getSchemaDir();

        String schemaDirectory = configurationManager.loadConfiguration().getSchemaDir();
        schemaDirectory = schemaDirectory.replace("\\.", "/");
        DirectoryScanner scanner = new DirectoryScanner();
        String[] filter = new String[1];
        filter[0] = "**/*.json";
        scanner.setIncludes(filter);
        scanner.setBasedir(schemaDirectory);
        scanner.setCaseSensitive(true);
        scanner.scan();

        for (String schemaFile : scanner.getIncludedFiles()) {
            if (schemaFile.startsWith("resource")
                    || schemaFile.startsWith("location")
                    || schemaFile.startsWith("connection")
                    || schemaFile.startsWith("service")
                    || schemaFile.startsWith("circuit")) {

                schemaFile = schemaFile.replaceAll("\\/", ".").replaceAll("\\.json", "");
                ResourceSchemaModel schema = this.loadSchemaFromDisk(schemaFile, null);
                result.add(schema);
            }

        }
        return result;
    }

    public ListSchemasResponse listSchemas(String filter) throws SchemaNotFoundException, GenericException {
        List<ResourceSchemaModel> result = this.loadSchemaFromDisk();
        if (filter.equals("*")) {
            return new ListSchemasResponse(result);
        } else {
            result.removeIf(a -> !a.getSchemaName().contains(filter));
            return new ListSchemasResponse(result);
        }
    }

    /**
     * Loads all schemas
     *
     * @return
     * @throws
     * com.osstelecom.db.inventory.manager.exception.SchemaNotFoundException
     * @throws com.osstelecom.db.inventory.manager.exception.GenericException
     */
    public GetSchemasResponse loadSchemas() throws SchemaNotFoundException, GenericException {
        List<String> result = new ArrayList<>();
        this.loadSchemaFromDisk().forEach(s -> {
            result.add(s.getSchemaName());
        });

        return new GetSchemasResponse(result);
    }

    /**
     * Loads the Schema by Name
     *
     * @param schemaName
     * @return
     * @throws SchemaNotFoundException
     * @throws GenericException
     */
    public ResourceSchemaResponse loadSchemaByName(String schemaName) throws SchemaNotFoundException, GenericException {
        return new ResourceSchemaResponse(this.loadSchema(schemaName));
    }

    /**
     *
     * @param filter
     * @return
     * @throws SchemaNotFoundException
     * @throws GenericException
     */
    public GetSchemasResponse getSchemaByFilter(String filter) throws SchemaNotFoundException, GenericException {
        GetSchemasResponse r = this.loadSchemas();
        r.getPayLoad().removeIf(a -> !a.startsWith(filter));
        return r;
    }

    /**
     * Merge a schema name, the result model is the output
     *
     * @param result
     * @param resourceModel
     */
    private void mergeSchemaModelSession(ResourceSchemaModel result, ResourceSchemaModel resourceModel) {
        for (Map.Entry<String, ResourceAttributeModel> entry : resourceModel.getAttributes().entrySet()) {
            String key = entry.getKey();
            entry.getValue().setItemHash(utilSession.getMd5(resourceModel.getSchemaName() + "." + key));
            ResourceAttributeModel model = entry.getValue();
            if (!result.getAttributes().containsKey(key)) {
                model.setId(resourceModel.getSchemaName() + "." + key);
                result.getAttributes().put(key, model);
            }
        }

    }

    /**
     * Smartly reads the schema definition from disk
     *
     * @param schemaName
     * @param result
     * @param loadInherited
     * @return
     * @throws SchemaNotFoundException
     * @throws GenericException
     */
    private ResourceSchemaModel loadSchemaFromDisk(String schemaName, ResourceSchemaModel result)
            throws SchemaNotFoundException, GenericException {
        schemaName = schemaName.replaceAll("\\.", "/");
        logger.debug("Trying to load Schema from: " + schemaName);
        File f = new File(this.schemaDir + "/" + schemaName + ".json");

        if (f.exists()) {
            try {
                //
                // Arquivo existe vamos ler os modelos
                //
                FileReader jsonReader = new FileReader(f);
                ResourceSchemaModel resourceModel = utilSession.getGson().fromJson(jsonReader,
                        ResourceSchemaModel.class);
                jsonReader.close();

                if (result == null) {
                    result = resourceModel;
                } else {
                    //
                    // Eu tenho um filho
                    //
                    if (resourceModel.getChildrenSchemas() != null) {
                        if (!resourceModel.getChildrenSchemas().contains(result.getSchemaName())) {
                            resourceModel.getChildrenSchemas().add(result.getSchemaName());
                            try {
                                this.writeModelToDisk(resourceModel, true);
                            } catch (InvalidRequestException ex) {
                                logger.error("Failed to Syncronize Data with DISK");
                            }
                        }
                    }
                }
                logger.debug("Loaded  SchemaName: [" + resourceModel.getSchemaName() + "]");

                this.mergeSchemaModelSession(result, resourceModel);

                if (!resourceModel.getFromSchema().equals(".")) {
                    result = this.loadSchemaFromDisk(resourceModel.getFromSchema(), result);
                }
                //
                // Salva só no final
                //
                return result;
            } catch (FileNotFoundException ex) {
                throw new GenericException(ex.getMessage(), ex);
            } catch (IOException ex) {
                throw new GenericException(ex.getMessage(), ex);
            } catch (JsonSyntaxException | JsonIOException ex) {
                logger.error("Invalid Json File:[{}]", f, ex);
                throw new GenericException(ex.getMessage(), ex);
            }
        } else {
            throw new SchemaNotFoundException("Schema With Name:[" + schemaName + "] was not found File: ["
                    + this.schemaDir + "/" + schemaName + ".json" + "]");
        }

    }

    /**
     * Creates the Schema on the schema directory
     *
     * @param model
     */
    public CreateResourceSchemaModelResponse createResourceSchemaModel(ResourceSchemaModel model)
            throws GenericException, SchemaNotFoundException, InvalidRequestException {
        //
        // Sanitization
        //
        if (model == null) {
            throw new InvalidRequestException("Request Cannot Be Null");
        }

        //
        // Sanitização do Nome
        //
        if (model.getSchemaName() != null) {
            if (utilSession.isValidStringValue(model.getSchemaName())) {
                if (model.getSchemaName().startsWith("resource")
                        || model.getSchemaName().startsWith("circuit")
                        || model.getSchemaName().startsWith("location")
                        || model.getSchemaName().startsWith("connection")
                        || model.getSchemaName().startsWith("service")) {

                } else {
                    throw new InvalidRequestException(
                            "Schema Name Must Start with [resource,circuit,location,service,connection]");
                }
            } else {
                throw new InvalidRequestException("Schema Name Must Contains Only Letters, Numbers or [.,-]");
            }

            //
            // Valida se o nome do schema e do from schema são diferentes.
            //
            if (model.getSchemaName().equals(model.getFromSchema())) {
                throw new InvalidRequestException("Schema Name Must not be equal from Schema Name");
            }
        }

        //
        // Check if Model Exists
        //
        if (this.loadSchemas().getPayLoad().contains(model.getSchemaName())) {
            //
            // Já existe
            //
            throw new InvalidRequestException("Model Named:[" + model.getSchemaName() + "] Already Exists");
        }

        if (model.getSchemaName().contains(" ")) {
            throw new InvalidRequestException("Schema Name Cannot Have Spaces");
        }

        //
        // If the From Schema is null, set to the default
        //
        if (model.getFromSchema() == null) {
            model.setFromSchema("default");
        } else {
            //
            // Check if the from Schema Exists...
            // We try to load it to check if it exists.
            //
            this.loadSchema(model.getFromSchema());
        }

        if (model.getFromSchema().contains("json")) {
            throw new InvalidRequestException("FromSchema  Cannot Have .json");
        }

        if (model.getSchemaName().contains("json")) {
            throw new InvalidRequestException("Schema Name Cannot Have .json");
        }

        List<String> removeAttributes = new ArrayList<>();

        List<String> invalidAttributes = new ArrayList<>();

        //
        // Sanitização dos Atributos
        //
        model.getAttributes().forEach((k, v) -> {

            //
            // Valida se o atributo começa ou termina com espaço
            // Se começar ou terminar com espaço será marcado como inválido
            //
            if (k.startsWith(" ") || k.endsWith(" ")) {
                invalidAttributes.add(k);
            } else if (!utilSession.isValidStringValue(k)) {
                invalidAttributes.add(k);
            } else {
                if (v.getId() != null && !v.getId().equals("")) {
                    removeAttributes.add(k);
                }

                if (v.getVariableType() == null) {
                    //
                    // Seta o default para String
                    //
                    v.setVariableType("String");
                } else if (v.getVariableType().trim().equals("")) {
                    v.setVariableType("String");
                }
            }
        });

        model.setCreationDate(new Date());
        model.setLastUpdate(new Date());

        if (!invalidAttributes.isEmpty()) {
            throw new InvalidRequestException("Invalid Attribute Names: [" + String.join(",", invalidAttributes) + "]");
        }

        removeAttributes.forEach(attribute -> {
            model.getAttributes().remove(attribute);
        });

        resolveRelatedParentSchemas(model, null);

        this.writeModelToDisk(model, false);
        this.clearSchemaCache();
        return new CreateResourceSchemaModelResponse(this.loadSchema(model.getSchemaName()));

    }

    /**
     * Clear all cached Entries.. .
     */
    public void clearSchemaCache() {
        logger.debug("Clearing all Cached Schemas..." + this.schemaCache.size());
        this.schemaCache.invalidateAll();
    }

    /**
     * Retrieve a list of Cached Entries.
     *
     * @return
     */
    public Map<String, ResourceSchemaModel> getCachedSchemas() {
        return this.schemaCache.asMap();
    }

    /**
     * Validate the attribute typecasting setting the right values and types.
     *
     * @param resource
     * @throws AttributeConstraintViolationException
     */
    public void validateResourceSchema(BasicResource resource) throws AttributeConstraintViolationException {

        //
        // As vezes uma chave será exluída e devemos processar isso aqui, faz a
        // sanitização das chaves
        //
        List<String> deletedAttributes = new ArrayList<>();

        resource.getAttributes().forEach((attrName, attrValue) -> {
            if (!resource.getSchemaModel().getAttributes().containsKey(attrName)) {
                deletedAttributes.add(attrName);
            }
        });

        if (!deletedAttributes.isEmpty()) {
            deletedAttributes.forEach((deletedAttrName) -> {
                logger.debug("Deleting Attribute from Resource:[{}]", resource.getKey());
                resource.getAttributes().remove(deletedAttrName);
            });

        }

        if (resource.getSchemaModel().getAllowAll() == null) {
            resource.getSchemaModel().setAllowAll(false);
        }
        if (!resource.getSchemaModel().getAllowAll()) {

            for (String key : resource.getAttributes().keySet()) {
                if (!resource.getSchemaModel().getAttributes().containsKey(key)) {
                    resource.getSchemaModel().setIsValid(false);
                    throw new AttributeConstraintViolationException("Invalid Attribute named:[" + key + "] for model: ["
                            + resource.getSchemaModel().getSchemaName() + "]");

                }
            }

            //
            // Valida os campos obrigatórios...
            //
            try {
                for (ResourceAttributeModel entry : resource.getSchemaModel().getAttributes().values()) {
                    if (entry.getRequired() == null) {
                        entry.setRequired(false);
                    }
                    if (entry.getValidate() == null) {
                        entry.setValidate(false);
                    }
                    if (entry.getRequired()) {
                        if (resource.getAttributes() == null) {
                            throw new AttributeConstraintViolationException(
                                    "Missing Required Attribute Named:[" + entry.getName() + "]");
                        }
                        if (!resource.getAttributes().containsKey(entry.getName())) {
                            if (entry.getDefaultValue() != null) {
                                Object value = this.getAttributeValue(entry, entry.getDefaultValue());

                                if (!entry.getIsList() && entry.getValidationRegex() != null) {
                                    //
                                    // Não é lista e tem uma regex
                                    //
                                    if (!entry.getDefaultValue().matches(entry.getValidationRegex())) {
                                        resource.getSchemaModel().setIsValid(false);
                                        throw new AttributeConstraintViolationException("Value: ["
                                                + entry.getDefaultValue() + "] does not matches validation regex:["
                                                + entry.getValidationRegex() + "]");
                                    }

                                } else {
                                    //
                                    // Valida a lista
                                    //
                                }

                                resource.getAttributes().put(entry.getName(),
                                        value);
                            } else {
                                //
                                // Lança a exception de validação
                                //
                                resource.getSchemaModel().setIsValid(false);

                                throw new AttributeConstraintViolationException(
                                        "Missing Required Attribute Named:[" + entry.getName() + "]");
                            }
                        } else {
                            Object value = this.getAttributeValue(entry, resource.getAttributes().get(entry.getName()));

                            if (!entry.getIsList() && entry.getValidationRegex() != null
                                    && !entry.getValidationRegex().trim().equals("") && entry.getValidate()) {
                                String stringValue = value.toString();
                                if (!stringValue.matches(entry.getValidationRegex())) {
                                    resource.getSchemaModel().setIsValid(false);
                                    throw new AttributeConstraintViolationException(
                                            "Value: [" + stringValue + "] does not matches validation regex:["
                                            + entry.getValidationRegex() + "]");
                                }
                            }

                            resource.getAttributes().put(entry.getName(), value);
                        }
                    } else {
                        if (resource.getAttributes() != null) {
                            if (!resource.getAttributes().containsKey(entry.getName())) {
                                if (entry.getDefaultValue() != null) {
                                    Object value = getAttributeValue(entry, entry.getDefaultValue());
                                    if (value != null) {
                                        resource.getAttributes().put(entry.getName(),
                                                value);
                                    }
                                } else {
                                    //
                                    // Isso pode gerar inconsistencia
                                    //
                                }
                            } else {
                                Object value = this.getAttributeValue(entry,
                                        resource.getAttributes().get(entry.getName()));
                                if (value != null) {
                                    String stringValue = value.toString();
                                    if (entry.getValidationRegex() != null
                                            && !entry.getValidationRegex().trim().equals("") && entry.getValidate()) {
                                        if (!stringValue.matches(entry.getValidationRegex())) {
                                            resource.getSchemaModel().setIsValid(false);
                                            throw new AttributeConstraintViolationException(
                                                    "Value: [" + stringValue + "] does not matches validation regex:["
                                                    + entry.getValidationRegex() + "]");
                                        }
                                    }
                                    resource.getAttributes().put(entry.getName(),
                                            value);
                                }
                            }
                        }
                    }
                }

                //
                // Segunda parte valida os atributos da rede
                //
                if (resource.getDiscoveryAttributes() != null && !resource.getDiscoveryAttributes().isEmpty()) {
                    //
                    // Note que só validamos o typecasting
                    //
                    for (Map.Entry<String, Object> data : resource.getDiscoveryAttributes().entrySet()) {
                        String name = data.getKey();
                        Object value = data.getValue();
                        ResourceAttributeModel model = resource.getSchemaModel().getAttributes().get(name);
                        if (model != null) {
                            if (model.getIsDiscovery()) {
                                //
                                // Podemos usar aqui :)
                                //
                                resource.getDiscoveryAttributes().put(name, this.getAttributeValue(model, value));
                            } else {
                                throw new AttributeConstraintViolationException(
                                        "Attribute named:[" + name + "] is not discovery attribute for model: ["
                                        + resource.getSchemaModel().getSchemaName() + "]");
                            }
                        } else {
                            throw new AttributeConstraintViolationException("Invalid Discovery Attribute named:[" + name
                                    + "] for model: [" + resource.getSchemaModel().getSchemaName() + "]");
                        }
                    }

                }
                //
                // Se bateu aqui tá tudo certinho :)
                //
                resource.getSchemaModel().setIsValid(true);

            } catch (AttributeConstraintViolationException acve) {
                //
                // Erro em alguma validação, marca o schemamodel do recurso como inválido
                //
                resource.getSchemaModel().setIsValid(false);
                throw acve;
            }
            //
            // Se bateu aqui tá tudo certinho :)
            //
            resource.getSchemaModel().setIsValid(true);
        } else {
            logger.warn("Allow ALL Is Enable, please fix me!");
            resource.getSchemaModel().setIsValid(true);
        }
    }

    /**
     * Valid types are String, Number, Boolean,Date,DateTime
     *
     * @param model
     * @param value
     * @return
     * @throws ParseException
     */
    public Object getAttributeValue(ResourceAttributeModel model, Object value)
            throws AttributeConstraintViolationException {
        // if (!model.getIsList()) {
        if (value == null) {
            logger.warn("Value is null for:" + model.getName());
        }
        if (model.getIsList() == null) {
            model.setIsList(false);
        }
        try {
            if (model.getAllowedValues() != null) {
                if (!model.getAllowedValues().isEmpty()) {
                    if (model.getIsList()) {
                        //
                        // Model é uma lista, então precisamos ver se todos os valores da Lista estão no
                        // allowed values.
                        //
                        if (value instanceof List) {
                            List list = (List) value;
                            for (Object o : list) {
                                //
                                // Deve ser migrado para o método validateResourceSchema()
                                //
                                if (o != null) {
                                    if (!o.toString().trim().equals("")) {
                                        if (!model.getAllowedValues().contains(o.toString())) {
                                            throw new AttributeConstraintViolationException(
                                                    "Attribute [" + model.getName() + "] of type:"
                                                    + model.getVariableType() + " Value : ["
                                                    + o.toString() + "] is not allowed here Allowed vars are:["
                                                    + String.join(",", model.getAllowedValues()) + "]");
                                        }
                                    }
                                }
                            }
                        } else {
                            throw new AttributeConstraintViolationException(
                                    "Attribute [" + model.getName() + "] of type:" + model.getVariableType()
                                    + " Is a List, please send a list , not a scalar value");

                        }

                    } else {
                        if (value != null) {
                            if (!value.toString().trim().equals("")) {
                                if (!model.getAllowedValues().contains(value.toString())) {
                                    //
                                    // Não Pode Prosseguir
                                    //
                                    throw new AttributeConstraintViolationException(
                                            "Attribute [" + model.getName() + "] of type:" + model.getVariableType()
                                            + " Value : ["
                                            + value + "] is not allowed here Allowed vars are:["
                                            + String.join(",", model.getAllowedValues()) + "]");

                                }
                            }
                        }
                    }
                }
            }

            //
            // Sanitiza o tipo de variavel se subiu lixo
            // @since rc1
            //
            if (model.getVariableType() == null) {
                logger.warn("Please Check:" + model.getName() + " Null Variable Type, assuming string as default");
                model.setVariableType("String");
            }

            if (model.getVariableType().equalsIgnoreCase("String")) {
                //
                // String will get the String representation as it is..
                //
                if (value != null && !value.toString().equals("")) {
                    if (model.getIsList() && value instanceof List) {
                        //
                        // lista de string
                        //
                        List<String> list = (List) value;
                        return list;
                    } else {
                        if (value != null && !value.toString().equals("")) {
                            if (value instanceof String) {
                                if (value.toString().equals("")) {
                                    //
                                    // Trata como null;
                                    //
                                    return null;
                                } else {
                                    return value;
                                }
                            } else {
                                throw new AttributeConstraintViolationException("Attribute [" + model.getName()
                                        + "] of type:" + model.getVariableType() + " Does not accpect value: [" + value
                                        + "] of type:" + value.getClass().getCanonicalName());
                            }
                        } else {
                            return null;
                        }
                    }
                } else {
                    return null;
                }
            } else if (model.getVariableType().equalsIgnoreCase("Number")) {
                if (value != null && !value.toString().equals("")) {
                    if (model.getIsList() && value instanceof List) {
                        //
                        // lista de string
                        //
                        List<Long> list = (List) value;
                        return list;
                    } else {
                        if (value != null) {
                            if (value.toString().equals("")) {
                                return null;
                            }
                            if (value instanceof Long) {
                                return value;
                            }
                            if (value.toString().contains(".")) {
                                Double d = Double.parseDouble(value.toString());
                                return d.longValue();
                            }
                            return Long.parseLong(value.toString());
                        } else {
                            return null;
                        }
                    }
                } else {
                    return null;
                }
            } else if (model.getVariableType().equalsIgnoreCase("Boolean")) {
                if (model.getIsList() && value instanceof List) {
                    //
                    // lista de string
                    //
                    List<Boolean> list = (List) value;
                    return list;
                } else {
                    if (value != null && !value.toString().equals("")) {
                        if (value instanceof Boolean) {
                            return value;
                        }
                        if (value.toString().equalsIgnoreCase("true") || value.toString().equalsIgnoreCase("false")) {
                            return value.toString().equalsIgnoreCase("true");
                        } else if (value.toString().equals("")) {
                            return false;
                        } else {
                            throw new AttributeConstraintViolationException(
                                    "Attribute [" + model.getName() + "] of type:"
                                    + model.getVariableType() + " Does not accpect value: [" + value + "]");
                        }
                    } else {
                        return false;
                    }
                }
            } else if (model.getVariableType().equalsIgnoreCase("Float")) {
                if (value != null && !value.toString().equals("")) {
                    if (model.getIsList() && value instanceof List) {
                        //
                        // lista de string
                        //
                        List<Float> list = (List) value;
                        return list;
                    } else {
                        if (value instanceof Float) {
                            return value;
                        }
                        return Float.parseFloat(value.toString());
                    }
                } else {
                    return null;
                }
            } else if (model.getVariableType().equalsIgnoreCase("Date")) {

                if (value != null && !value.toString().equals("")) {
                    SimpleDateFormat sdf = new SimpleDateFormat(
                            configurationManager.loadConfiguration().getDateFormat());
                    sdf.setLenient(false);
                    if (model.getIsList() && value instanceof List) {
                        //
                        // lista de string
                        //
                        List list = (List) value;
                        return list;
                    } else {
                        try {
                            return sdf.parse(value.toString());
                        } catch (ParseException ex) {
                            throw new AttributeConstraintViolationException(
                                    "Attribute [" + model.getName() + "] of type:"
                                    + model.getVariableType() + " Cannot Parse Date Value : [" + value
                                    + "] With Mask: ["
                                    + configurationManager.loadConfiguration().getDateFormat() + "]",
                                    ex);
                        }
                    }
                } else {
                    return null;
                }
            } else if (model.getVariableType().equalsIgnoreCase("DateTime")) {
                if (value != null && !value.toString().equals("")) {
                    SimpleDateFormat sdf = new SimpleDateFormat(
                            configurationManager.loadConfiguration().getDateTimeFormat());
                    sdf.setLenient(false);
                    if (model.getIsList() && value instanceof List) {
                        //
                        // lista de string
                        //
                        List list = (List) value;
                        return list;
                    } else {
                        try {
                            return sdf.parse(value.toString());
                        } catch (ParseException ex) {
                            throw new AttributeConstraintViolationException(
                                    "Attribute [" + model.getName() + "] of type:"
                                    + model.getVariableType() + " Cannot Parse Date Time Value : [" + value
                                    + "] With Mask: ["
                                    + configurationManager.loadConfiguration().getDateTimeFormat() + "]",
                                    ex);

                        }
                    }
                } else {
                    return null;
                }
            } else if (model.getVariableType().equalsIgnoreCase("GeoLine")) {
                if (value != null && !value.toString().equals("")) {
                    List<Float> data = (List<Float>) value;
                    return data;
                } else {
                    return null;
                }
            } else {
                throw new AttributeConstraintViolationException(
                        "Attribute [" + model.getName() + "] of type:" + model.getVariableType() + " Cannot be parsed");
            }
        } catch (NumberFormatException ex) {
            throw new AttributeConstraintViolationException("Value: [" + value + "] Cannot be parsed do Number", ex);
        }

    }

    /**
     * Handles the logic to update the ResourceSchemaModel
     *
     * @param original
     * @param update
     * @return
     * @throws InvalidRequestException
     * @throws GenericException
     * @throws SchemaNotFoundException
     */
    public PatchResourceSchemaModelResponse patchSchemaModel(ResourceSchemaModel update)
            throws InvalidRequestException, GenericException, SchemaNotFoundException {

        if (update == null) {
            throw new InvalidRequestException("Attribute Schema Model not found");
        }

        ResourceSchemaModel original = this.loadSchema(update.getSchemaName());

        if (update.getSchemaName() != null) {
            if (!update.getSchemaName().equals(original.getSchemaName())) {
                //
                // Schema Name cannot Be Changed
                //
                throw new InvalidRequestException("Schema Name Cannot Be changed...");
            }
        }

        if (update.getFromSchema() != null) {
            if (!update.getFromSchema().equals(original.getFromSchema())) {
                //
                // Changing Parent Class...are we going to allow it?
                //
                original.setFromSchema(update.getFromSchema());
                original.setAttributesChanged(true);
            }
        }

        if (update.getAllowAll() != null) {
            if (!update.getAllowAll().equals(original.getAllowAll())) {
                original.setAllowAll(update.getAllowAll());
            }
        }

        if (update.getGraphItemColor() != null) {
            if (!update.getGraphItemColor().equals(original.getGraphItemColor())) {
                original.setGraphItemColor(update.getGraphItemColor());
                original.setAttributesChanged(true);
            }
        }

        List<String> removeAttributes = new ArrayList<>();

        List<String> invalidAttributes = new ArrayList<>();

        //
        // Sanitização dos Atributos
        //
        update.getAttributes().forEach((k, v) -> {

            if (!utilSession.isValidStringValue(k)) {
                invalidAttributes.add(k);
            } else {

                if (v.getId() != null && !v.getId().equals("")) {
                    removeAttributes.add(k);
                }

                if (v.getVariableType() == null) {
                    //
                    // Seta o default para String
                    //
                    v.setVariableType("String");
                } else if (v.getVariableType().trim().equals("")) {
                    v.setVariableType("String");
                }
            }
        });

        if (!invalidAttributes.isEmpty()) {
            throw new InvalidRequestException("Invalid Attribute Names: [" + String.join(",", invalidAttributes) + "]");
        }

        removeAttributes.forEach(attribute -> {
            update.getAttributes().remove(attribute);
        });

        if (!update.getAttributes().isEmpty()) {
            //
            // The hard part of comparing and merging maps
            //

            //
            // Compare each
            //
            update.getAttributes().forEach((updateAttrName, updateModel) -> {
                //
                // Check if need to remove the Attribute
                //
                if (updateModel.getDoRemove() == null) {
                    updateModel.setDoRemove(false);
                }

                if (updateModel.getDoRemove()) {

                    if (original.getAttributes().containsKey(updateAttrName)) {
                        original.setAttributesChanged(true);
                        original.getAttributes().remove(updateAttrName);
                        logger.debug("Attribute:[{}] Removed from Schema:[{}]", updateAttrName,
                                original.getSchemaName());
                    }
                } else {
                    //
                    // Patching
                    //

                    if (!original.getAttributes().containsKey(updateAttrName)) {
                        //
                        // Easy Part, new attribute
                        //
                        original.getAttributes().put(updateAttrName, updateModel);
                        original.setAttributesChanged(true);
                    } else {
                        //
                        // Hard Part
                        //
                        ResourceAttributeModel originalAttributeModel = original.getAttributes().get(updateAttrName);

                        if (originalAttributeModel != null) {
                            //
                            // Compare one by one..
                            //

                            if (updateModel.getDescription() != null) {
                                if (!updateModel.getDescription().equals(originalAttributeModel.getDescription())) {
                                    originalAttributeModel.setDescription(updateModel.getDescription());
                                    original.setAttributesChanged(true);
                                }
                            }

                            if (updateModel.getRequired() != null) {
                                if (!updateModel.getRequired().equals(originalAttributeModel.getRequired())) {
                                    originalAttributeModel.setRequired(updateModel.getRequired());
                                    original.setAttributesChanged(true);
                                }
                            }

                            if (updateModel.getValidationRegex() != null
                                    && !updateModel.getValidationRegex().trim().equals("")) {
                                if (!updateModel.getValidationRegex()
                                        .equals(originalAttributeModel.getValidationRegex())) {
                                    originalAttributeModel.setValidationRegex(updateModel.getValidationRegex());
                                    original.setAttributesChanged(true);
                                }
                            }

                            if (updateModel.getValidationScript() != null) {
                                if (!updateModel.getValidationScript()
                                        .equals(originalAttributeModel.getValidationScript())) {
                                    originalAttributeModel.setValidationScript(updateModel.getValidationScript());
                                    original.setAttributesChanged(true);
                                }
                            }

                            if (updateModel.getValidate() != null) {
                                if (!updateModel.getValidate().equals(originalAttributeModel.getValidate())) {
                                    originalAttributeModel.setValidate(updateModel.getValidate());
                                    original.setAttributesChanged(true);
                                }
                            }

                            //
                            // Introduced on rc1, needs testing
                            // @since rc1
                            //
                            if (updateModel.getVariableType() != null) {
                                if (!updateModel.getVariableType().equals(originalAttributeModel.getVariableType())) {
                                    originalAttributeModel.setVariableType(updateModel.getVariableType());
                                    original.setAttributesChanged(true);
                                }
                            }

                            if (updateModel.getDefaultValue() != null) {
                                if (!updateModel.getDefaultValue().equals(originalAttributeModel.getDefaultValue())) {
                                    originalAttributeModel.setDefaultValue(updateModel.getDefaultValue());
                                    original.setAttributesChanged(true);
                                }
                            }

                            if (updateModel.getAllowedValues() != null) {
                                // if
                                // (!updateModel.getAllowedValues().equals(originalAttributeModel.getAllowedValues()))
                                // {
                                originalAttributeModel.setAllowedValues(updateModel.getAllowedValues());
                                original.setAttributesChanged(true);
                                // }
                            }

                            if (updateModel.getDisplayName() != null) {
                                if (!updateModel.getDisplayName().equals(originalAttributeModel.getDisplayName())) {
                                    originalAttributeModel.setDisplayName(updateModel.getDisplayName());
                                }
                            }

                            if (updateModel.isDisplayable()) {
                                if (!originalAttributeModel.isDisplayable()) {
                                    originalAttributeModel.setDisplayable(true);
                                }
                            } else {
                                if (originalAttributeModel.isDisplayable()) {
                                    originalAttributeModel.setDisplayable(false);
                                }
                            }

                        }
                        //
                        // Do not need to show this to the user and file..
                        //
                        if (originalAttributeModel.getDoRemove() != null) {
                            originalAttributeModel.setDoRemove(false);
                        }
                    }
                }

            });

        }

        original.setLastUpdate(new Date());
        resolveRelatedParentSchemas(update, original);

        if (original.getAttributesChanged()) {

            this.writeModelToDisk(original, true);
            this.clearSchemaCache();

            //
            // Notifica o Message Bus da Atualização, ele vai varrer a base procurando
            // utilizações...
            //
            this.notifyUpdateEvent(original);
        }
        return new PatchResourceSchemaModelResponse(this.loadSchema(original.getSchemaName()));
    }

    /**
     * Notifica Recursivamente o Bus da atualização
     *
     * @param model
     */
    private void notifyUpdateEvent(ResourceSchemaModel model) {
        logger.debug("Notifying Schema Update:[{}] With [{}] Children", model.getSchemaName(),
                model.getChildrenSchemas().size());
        eventManager.notifyGenericEvent(new ResourceSchemaUpdatedEvent(model));
        if (!model.getChildrenSchemas().isEmpty()) {
            model.getChildrenSchemas().forEach(childSchemaName -> {
                logger.debug("Notifying Child Schema Update:[{}]", childSchemaName);
                try {
                    ResourceSchemaModel childModel = this.loadSchema(childSchemaName);
                    this.notifyUpdateEvent(childModel);
                } catch (SchemaNotFoundException | GenericException ex) {
                    logger.error("Error Processing Child Model on Update event", ex);
                }
            });
        }
    }

    /**
     * Check the allowed types of attributes
     *
     * @return
     */
    public List<String> validAttributesType() {
        List<String> validTypes = new ArrayList<>();
        validTypes.add("Number");
        validTypes.add("Boolean");
        validTypes.add("Float");
        validTypes.add("Date");
        validTypes.add("DateTime");
        validTypes.add("GeoLine");
        return validTypes;
    }

    private void resolveRelatedParentSchemas(ResourceSchemaModel newSchemaModel, ResourceSchemaModel oldSchemaModel)
            throws SchemaNotFoundException, GenericException, InvalidRequestException {
        String schemaName = newSchemaModel.getSchemaName();
        List<String> parents = extractParentsFromDefaultExpression(newSchemaModel);
        for (String parent : parents) {
            ResourceSchemaModel parentSchema = loadSchemaFromDisk(parent, null);
            List<String> relatedSchemas = parentSchema.getRelatedSchemas();
            if (relatedSchemas != null) {
                if (!relatedSchemas.contains(schemaName)) {
                    relatedSchemas.add(schemaName);
                }
            } else {
                relatedSchemas = Arrays.asList(schemaName);
                parentSchema.setRelatedSchemas(relatedSchemas);
            }
            writeModelToDisk(parentSchema, true);
        }

        if (oldSchemaModel != null) {
            List<String> oldParents = extractParentsFromDefaultExpression(oldSchemaModel);
            for (String oldParent : oldParents) {
                // não existe mais a referência na lista de atributos default
                if (!parents.contains(oldParent)) {
                    ResourceSchemaModel oldParentSchema = loadSchemaFromDisk(oldParent, null);
                    List<String> relatedSchemas = oldParentSchema.getRelatedSchemas();
                    if (relatedSchemas != null && relatedSchemas.contains(schemaName)) {
                        relatedSchemas.remove(schemaName);
                        writeModelToDisk(oldParentSchema, true);
                    }
                }
            }
        }
    }

    private List<String> extractParentsFromDefaultExpression(ResourceSchemaModel schemaModel) {
        List<String> parents = new ArrayList<>();
        for (ResourceAttributeModel attrModel : schemaModel.getAttributes().values()) {
            String defaultValue = attrModel.getDefaultValue();
            String regex = "^\\$\\([\\w]+[\\w+\\.]+[\\.]+[\\w]+\\)$";
            if (defaultValue != null && defaultValue.matches(regex)) {
                String value = defaultValue.replace("$", "");
                value = value.replace("(", "");
                Integer pointer = value.lastIndexOf(".");
                String attributeSchemaName = value.substring(0, pointer);
                parents.add(attributeSchemaName);
            }
        }

        return parents;
    }

    /**
     * Save the JSON Model to the disk
     *
     * @param model
     * @param overwrite
     * @throws GenericException
     * @throws InvalidRequestException
     */
    private void writeModelToDisk(ResourceSchemaModel model, Boolean overwrite)
            throws GenericException, InvalidRequestException {
        //
        // Ok lets check the target directory:
        //
        String modelName = model.getSchemaName();
        String modelPathStr = modelName.replace(".", "/");
        Path p = Paths.get(this.configurationManager.loadConfiguration().getSchemaDir() + "/" + modelPathStr + ".json");
        File f = p.toFile();

        //
        // Check if File Exists
        //
        if (!f.exists() || overwrite) {
            f.getParentFile().mkdirs();
            //
            //
            //
            try {
                //
                // Dont Save Attribute change flag to disk.
                //
                model.setAttributesChanged(null);

                FileWriter writer = new FileWriter(f);
                this.utilSession.getGson().toJson(model, writer);
                writer.flush();
                writer.close();
            } catch (IOException ex) {
                throw new GenericException(ex.getMessage(), ex);
            }
            //
            // Reloads the schema and its attributes.
            //
            this.clearSchemaCache();
        } else {
            throw new InvalidRequestException("Resource Schema Model [" + model.getSchemaName() + "] Already Exists");
        }
    }

    /**
     * Just for Debugging
     *
     * @param rn
     */
    @Override
    public void onRemoval(RemovalNotification<String, ResourceSchemaModel> rn) {
        // throw new UnsupportedOperationException("Not supported yet."); //To change
        // body of generated methods, choose Tools | Templates.
        logger.debug("Schema: [" + rn.getKey() + "] Removed From Cache...[" + rn.getCause().name() + "]");
    }
}
