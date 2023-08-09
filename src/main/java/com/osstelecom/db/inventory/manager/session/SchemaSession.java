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
import com.osstelecom.db.inventory.manager.request.CreateResourceSchemaModelRequest;
import com.osstelecom.db.inventory.manager.request.PatchResourceSchemaModelRequest;
import com.osstelecom.db.inventory.manager.resources.BasicResource;
import com.osstelecom.db.inventory.manager.resources.exception.AttributeConstraintViolationException;
import com.osstelecom.db.inventory.manager.resources.model.ResourceAttributeModel;
import com.osstelecom.db.inventory.manager.resources.model.ResourceSchemaModel;
import com.osstelecom.db.inventory.manager.response.CreateResourceSchemaModelResponse;
import com.osstelecom.db.inventory.manager.response.GetSchemasResponse;
import com.osstelecom.db.inventory.manager.response.ListSchemasResponse;
import com.osstelecom.db.inventory.manager.response.PatchResourceSchemaModelResponse;
import com.osstelecom.db.inventory.manager.response.ResourceSchemaResponse;
import java.util.Comparator;
import java.util.Date;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.beanutils.BeanComparator;
import org.apache.tools.ant.DirectoryScanner;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

/**
 * Classe que lida com os atributos do schema
 *
 * @Todo:Avaliar a refatoração desta classe para remover a complexidade da
 * mesma.
 * @author Lucas Nishimura
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

    private Cache<String, List<ResourceSchemaModel>> allSchemasCache;

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

        this.allSchemasCache = CacheBuilder
                .newBuilder()
                .maximumSize(5000)
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
                /**
                 * Trata data
                 */
                if (schema.getCreationDate() == null) {
                    /**
                     * Seta bem antigo
                     */
                    schema.setCreationDate(new Date(0));
                }

                if (schema.getLastUpdate() == null) {
                    schema.setLastUpdate(new Date(0));
                }
                result.add(schema);
            }

        }
        return result;
    }

    public ListSchemasResponse listSchemas(int page, int size, String sortField, String sortDirection, String filter) throws SchemaNotFoundException, GenericException {
        /**
         * Faz uso do Cache
         */
        List<ResourceSchemaModel> result = this.allSchemasCache.getIfPresent(filter);
        if (result == null) {
            result = this.loadSchemaFromDisk();
            this.allSchemasCache.put(filter, result);
        }

        Pageable pageRequest = PageRequest.of(page, size);

        int start = (int) pageRequest.getOffset();
        int end = Math.min((start + pageRequest.getPageSize()), result.size());

        /**
         * Vamos fazer um sorting :)
         */
        Stream<ResourceSchemaModel> stream = result.stream();
        if (sortField != null) {
            Comparator<ResourceSchemaModel> comparator = new BeanComparator<>(sortField);
            if ("desc".equalsIgnoreCase(sortDirection)) {
                comparator = comparator.reversed();
            }
            stream = stream.sorted(comparator);
        }
        result = stream.collect(Collectors.toList());

        List<ResourceSchemaModel> pageContent = result.subList(start, end);

        if (filter.equals("*")) {
            return new ListSchemasResponse(new PageImpl<>(pageContent, pageRequest, result.size()));
        } else {
            result.removeIf(a -> !a.getSchemaName().contains(filter));
            return new ListSchemasResponse(new PageImpl<>(pageContent, pageRequest, result.size()));
        }
    }

    /**
     * Loads all schemas apenas os nomes
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

        ResourceSchemaModel cachedSchema = this.schemaCache.getIfPresent(schemaName);
        if (cachedSchema != null) {
            return cachedSchema;
        }

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
     * @return
     * @throws com.osstelecom.db.inventory.manager.exception.GenericException
     * @throws
     * com.osstelecom.db.inventory.manager.exception.SchemaNotFoundException
     * @throws
     * com.osstelecom.db.inventory.manager.exception.InvalidRequestException
     */
    public CreateResourceSchemaModelResponse createResourceSchemaModel(CreateResourceSchemaModelRequest req)
            throws GenericException, SchemaNotFoundException, InvalidRequestException {

        ResourceSchemaModel model = req.getPayLoad();
        model.setAuthor(req.getUserLogin());
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
     * Valida o esquema de recurso fornecido com base em seu modelo de atributo
     * de esquema (ResourceSchemaModel).
     *
     * <p>
     * Este método realiza a validação do esquema de recurso fornecido
     * (representado por um objeto BasicResource) de acordo com o modelo de
     * atributo de esquema associado. O processo de validação inclui as
     * seguintes etapas:</p>
     *
     * <ol>
     * <li>Sanitização das chaves:
     * <ul>
     * <li>O método verifica se existem atributos no recurso que não estão
     * presentes no modelo de atributo de esquema associado.</li>
     * <li>Quaisquer atributos ausentes são removidos do recurso antes de
     * prosseguir com a validação.</li>
     * </ul>
     * </li>
     * <li>Verificação de "Allow All":
     * <ul>
     * <li>Se a configuração "Allow All" no modelo de atributo de esquema for
     * definida como "false" ou não estiver presente, o método procederá com a
     * validação dos atributos definidos no modelo.</li>
     * <li>Se "Allow All" for definido como "true", o método logará um aviso
     * indicando que essa configuração está habilitada, mas não executará a
     * validação dos atributos específicos.</li>
     * </ul>
     * </li>
     * <li>Validação dos atributos obrigatórios:
     * <ul>
     * <li>O método verifica se os atributos obrigatórios definidos no modelo de
     * atributo de esquema estão presentes no recurso.</li>
     * <li>Caso um atributo obrigatório esteja ausente e não tenha um valor
     * padrão definido, uma exceção AttributeConstraintViolationException será
     * lançada.</li>
     * <li>Se o valor padrão estiver definido para um atributo obrigatório, o
     * método validará o valor do atributo com base nas restrições
     * especificadas, como expressões regulares de validação, valores permitidos
     * e tipos de variável.</li>
     * </ul>
     * </li>
     * <li>Validação dos atributos de descoberta:
     * <ul>
     * <li>O método valida os atributos de descoberta (discoveryAttributes) do
     * recurso com base em seus modelos de atributo de esquema associados.</li>
     * <li>Somente os atributos de descoberta que são marcados como
     * "isDiscovery" no modelo de atributo de esquema podem ser usados na
     * validação.</li>
     * <li>O método garantirá que os atributos de descoberta estejam associados
     * a seus modelos de atributo de esquema correspondentes e que os valores
     * fornecidos sejam do tipo correto, conforme definido nos modelos.</li>
     * </ul>
     * </li>
     * <li>Marcação do modelo de atributo de esquema como válido ou inválido:
     * <ul>
     * <li>Após concluir a validação dos atributos, o método atualizará o estado
     * de validação (isValid) do modelo de atributo de esquema associado ao
     * recurso com base nos resultados da validação.</li>
     * </ul>
     * </li>
     * </ol>
     *
     * @param resource O objeto BasicResource que representa o recurso a ser
     * validado.
     * @throws AttributeConstraintViolationException Se houver algum atributo
     * ausente, inválido ou em violação das restrições especificadas no modelo
     * de atributo de esquema.
     *
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
     * Obtém o valor de atributo formatado com base no modelo fornecido
     * (ResourceAttributeModel).
     *
     * <p>
     * Este método é responsável por obter o valor do atributo formatado com
     * base nas restrições definidas no modelo de atributo
     * (ResourceAttributeModel) associado. Ele realiza as seguintes etapas de
     * validação:</p>
     *
     * <ol>
     * <li>Verificação do valor nulo:
     * <ul>
     * <li>Se o valor fornecido for nulo, o método registrará um aviso no log
     * indicando o nome do atributo (model.getName()).</li>
     * </ul>
     * </li>
     * <li>Definição do valor de "IsList":
     * <ul>
     * <li>Se o valor de "IsList" no modelo for nulo, ele será definido como
     * "false".</li>
     * </ul>
     * </li>
     * <li>Validação dos valores permitidos (AllowedValues):
     * <ul>
     * <li>Se a lista de "AllowedValues" no modelo não estiver vazia, o método
     * verificará se o valor fornecido está contido na lista.</li>
     * <li>Se "IsList" for verdadeiro, o método iterará sobre cada valor da
     * lista e verificará se ele está na lista de "AllowedValues". Se não
     * estiver, uma exceção AttributeConstraintViolationException será
     * lançada.</li>
     * <li>Se "IsList" for falso, o método verificará se o valor fornecido está
     * na lista de "AllowedValues". Se não estiver, uma exceção
     * AttributeConstraintViolationException será lançada.</li>
     * </ul>
     * </li>
     * <li>Sanitização do tipo de variável (VariableType):
     * <ul>
     * <li>Se o tipo de variável no modelo (VariableType) for nulo, o método
     * registrará um aviso no log indicando o nome do atributo (model.getName())
     * e definirá o tipo de variável como "String" por padrão.</li>
     * </ul>
     * </li>
     * <li>Formatação de valores baseados no tipo de variável:
     * <ul>
     * <li>Se o tipo de variável for "String", o método retornará o valor como
     * uma String. Se "IsList" for verdadeiro, o valor será retornado como uma
     * lista de Strings.</li>
     * <li>Se o tipo de variável for "Number", o método tentará formatar o valor
     * como um número. Se "IsList" for verdadeiro, o valor será retornado como
     * uma lista de Longs.</li>
     * <li>Se o tipo de variável for "Boolean", o método tentará formatar o
     * valor como um booleano. Se "IsList" for verdadeiro, o valor será
     * retornado como uma lista de Booleanos.</li>
     * <li>Se o tipo de variável for "Float", o método tentará formatar o valor
     * como um número de ponto flutuante. Se "IsList" for verdadeiro, o valor
     * será retornado como uma lista de Floats.</li>
     * <li>Se o tipo de variável for "Date" ou "DateTime", o método tentará
     * formatar o valor como uma data ou data e hora, respectivamente, com base
     * no formato especificado na configuração.</li>
     * <li>Se o tipo de variável for "GeoLine", o método retornará o valor como
     * uma lista de Floats.</li>
     * <li>Se o tipo de variável não corresponder a nenhum dos tipos acima, uma
     * exceção AttributeConstraintViolationException será lançada.</li>
     * </ul>
     * </li>
     * </ol>
     *
     * @param model O objeto ResourceAttributeModel que representa o modelo de
     * atributo a ser usado para validação e formatação.
     * @param value O valor do atributo a ser validado e formatado.
     * @return O valor formatado do atributo com base no modelo fornecido.
     * @throws AttributeConstraintViolationException Se o valor não atender às
     * restrições especificadas no modelo de atributo.
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
     * Realiza um patch no modelo de esquema de recurso.
     *
     * <p>
     * Este método é responsável por aplicar um "patch" no modelo de esquema de
     * recurso. Um patch é uma operação que modifica parcialmente o modelo de
     * esquema existente com base nas atualizações fornecidas em um novo modelo
     * (update). O método realiza as seguintes etapas:</p>
     *
     * <ol>
     * <li>Verificação do modelo de atualização:
     * <ul>
     * <li>Verifica se o modelo de atualização (update) não é nulo. Se for nulo,
     * uma exceção InvalidRequestException será lançada com a mensagem
     * "Attribute Schema Model not found".</li>
     * </ul>
     * </li>
     * <li>Carregamento do modelo original:
     * <ul>
     * <li>Carrega o modelo de esquema original com base no nome de esquema
     * fornecido no modelo de atualização (update.getSchemaName()).</li>
     * </ul>
     * </li>
     * <li>Verificação e validação do nome de esquema:
     * <ul>
     * <li>Verifica se o nome de esquema fornecido na atualização não foi
     * alterado em relação ao modelo original. Se o nome de esquema for
     * diferente, uma exceção InvalidRequestException será lançada com a
     * mensagem "Schema Name Cannot Be changed...".</li>
     * </ul>
     * </li>
     * <li>Verificação e atualização do atributo "fromSchema":
     * <ul>
     * <li>Verifica se o atributo "fromSchema" fornecido na atualização é
     * diferente do atributo "fromSchema" do modelo original. Se for diferente,
     * atualiza o valor do atributo "fromSchema" no modelo original e define o
     * atributo "attributesChanged" como verdadeiro.</li>
     * </ul>
     * </li>
     * <li>Verificação e atualização do atributo "allowAll":
     * <ul>
     * <li>Verifica se o atributo "allowAll" fornecido na atualização é
     * diferente do atributo "allowAll" do modelo original. Se for diferente,
     * atualiza o valor do atributo "allowAll" no modelo original.</li>
     * </ul>
     * </li>
     * <li>Verificação e atualização do atributo "graphItemColor":
     * <ul>
     * <li>Verifica se o atributo "graphItemColor" fornecido na atualização é
     * diferente do atributo "graphItemColor" do modelo original. Se for
     * diferente, atualiza o valor do atributo "graphItemColor" no modelo
     * original e define o atributo "attributesChanged" como verdadeiro.</li>
     * </ul>
     * </li>
     * <li>Sanitização dos atributos de atualização:
     * <ul>
     * <li>Itera sobre cada atributo do modelo de atualização (update) e realiza
     * as seguintes ações:
     * <ul>
     * <li>Verifica se o nome do atributo (k) é uma string válida usando o
     * método utilSession.isValidStringValue(k). Se não for uma string válida, o
     * nome do atributo será adicionado à lista "invalidAttributes".</li>
     * <li>Verifica se o atributo possui um ID (v.getId()) não nulo e não vazio.
     * Se possuir, adiciona o nome do atributo à lista "removeAttributes".</li>
     * <li>Define o tipo de variável (v.getVariableType()) para "String" se o
     * atributo não tiver um tipo de variável especificado.</li>
     * </ul>
     * </li>
     * </ul>
     * </li>
     * <li>Verificação de atributos inválidos:
     * <ul>
     * <li>Se a lista "invalidAttributes" não estiver vazia, uma exceção
     * InvalidRequestException será lançada com a mensagem "Invalid Attribute
     * Names: [lista de atributos inválidos separados por vírgula]".</li>
     * </ul>
     * </li>
     * <li>Remoção de atributos:
     * <ul>
     * <li>Remove os atributos da atualização (update) que estão na lista
     * "removeAttributes".</li>
     * </ul>
     * </li>
     * <li>Atualização dos atributos:
     * <ul>
     * <li>Para cada atributo restante no modelo de atualização (update),
     * realiza as seguintes ações:
     * <ul>
     * <li>Se o atributo possui a propriedade "doRemove" definida como
     * verdadeira, remove o atributo correspondente do modelo original e define
     * o atributo "attributesChanged" como verdadeiro.</li>
     * <li>Se o atributo não existe no modelo original, é adicionado ao modelo
     * original como um novo atributo, e o atributo "attributesChanged" é
     * definido como verdadeiro.</li>
     * <li>Se o atributo existe no modelo original, compara os valores do
     * atributo no modelo de atualização com o modelo original e atualiza os
     * valores do modelo original com base nas diferenças encontradas.</li>
     * </ul>
     * </li>
     * </ul>
     * </li>
     * <li>Atualização do registro de última modificação:
     * <ul>
     * <li>Atualiza a data de última modificação (lastUpdate) do modelo original
     * para a data atual.</li>
     * </ul>
     * </li>
     * <li>Resolução de esquemas parentes relacionados:
     * <ul>
     * <li>Chama o método "resolveRelatedParentSchemas" para atualizar esquemas
     * parentes relacionados.</li>
     * </ul>
     * </li>
     * <li>Verificação de alterações nos atributos:
     * <ul>
     * <li>Verifica se houve mudanças nos atributos do modelo original
     * (original.getAttributesChanged()). Se houver mudanças, grava o modelo
     * original em disco, limpa o cache do esquema e notifica o Message Bus
     * sobre a atualização.</li>
     * </ul>
     * </li>
     * <li>Retorno do resultado do patch:
     * <ul>
     * <li>Retorna um objeto PatchResourceSchemaModelResponse contendo o modelo
     * de esquema de recurso após a aplicação do patch.</li>
     * </ul>
     * </li>
     * </ol>
     *
     * @param update O modelo de esquema de recurso contendo as atualizações a
     * serem aplicadas no modelo original.
     * @return Um objeto PatchResourceSchemaModelResponse contendo o modelo de
     * esquema de recurso após a aplicação do patch.
     * @throws InvalidRequestException Se o modelo de atualização (update) for
     * nulo ou se houver nomes de atributos inválidos.
     * @throws GenericException Se ocorrer um erro genérico durante o processo
     * de patch.
     * @throws SchemaNotFoundException Se o esquema original não for encontrado.
     */
    public PatchResourceSchemaModelResponse patchSchemaModel(PatchResourceSchemaModelRequest req)
            throws InvalidRequestException, GenericException, SchemaNotFoundException {
        ResourceSchemaModel update = req.getPayLoad();

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

    /**
     * Resolve os esquemas parentes relacionados ao modelo de esquema de
     * recurso.
     *
     * <p>
     * Este método é responsável por resolver os esquemas parentes relacionados
     * ao modelo de esquema de recurso fornecido. Ele realiza as seguintes
     * etapas:</p>
     *
     * <ol>
     * <li>Extração dos esquemas parentes:
     * <ul>
     * <li>Extrai a lista de esquemas parentes do modelo de esquema de recurso
     * fornecido (newSchemaModel) usando o método
     * extractParentsFromDefaultExpression(newSchemaModel).</li>
     * </ul>
     * </li>
     * <li>Iteração sobre os esquemas parentes:
     * <ul>
     * <li>Para cada esquema parente na lista de esquemas parentes extraídos,
     * realiza as seguintes ações:
     * <ul>
     * <li>Carrega o modelo do esquema parente do disco usando o nome do esquema
     * parente.</li>
     * <li>Obtém a lista de esquemas relacionados do esquema parente.</li>
     * <li>Se a lista de esquemas relacionados for nula, cria uma nova lista
     * contendo o nome do esquema do modelo de esquema fornecido
     * (newSchemaModel) e define essa lista como a lista de esquemas
     * relacionados no esquema parente.</li>
     * <li>Se a lista de esquemas relacionados não for nula, verifica se ela não
     * contém o nome do esquema do modelo de esquema fornecido (newSchemaModel).
     * Se não contiver, adiciona o nome do esquema à lista de esquemas
     * relacionados.</li>
     * <li>Grava o modelo do esquema parente atualizado no disco.</li>
     * </ul>
     * </li>
     * </ul>
     * </li>
     * <li>Verificação do esquema anterior:
     * <ul>
     * <li>Se o modelo de esquema de recurso anterior (oldSchemaModel) não for
     * nulo, executa as seguintes ações:
     * <ul>
     * <li>Extrai a lista de esquemas parentes do modelo de esquema de recurso
     * anterior usando o método
     * extractParentsFromDefaultExpression(oldSchemaModel).</li>
     * <li>Itera sobre cada esquema parente no modelo anterior e verifica se ele
     * não está presente na lista de esquemas parentes extraídos no modelo
     * atual. Se não estiver presente, realiza as seguintes ações:
     * <ul>
     * <li>Carrega o modelo do esquema parente do disco usando o nome do esquema
     * parente.</li>
     * <li>Obtém a lista de esquemas relacionados do esquema parente.</li>
     * <li>Se a lista de esquemas relacionados for nula ou não contiver o nome
     * do esquema do modelo de esquema fornecido (newSchemaModel), não faz
     * nada.</li>
     * <li>Se a lista de esquemas relacionados contiver o nome do esquema do
     * modelo de esquema fornecido (newSchemaModel), remove o nome do esquema da
     * lista de esquemas relacionados.</li>
     * <li>Grava o modelo do esquema parente atualizado no disco.</li>
     * </ul>
     * </li>
     * </ul>
     * </li>
     * </ul>
     * </li>
     * </ol>
     *
     * @param newSchemaModel O modelo de esquema de recurso atualizado para o
     * qual os esquemas parentes relacionados devem ser resolvidos.
     * @param oldSchemaModel O modelo de esquema de recurso anterior, se houver,
     * para verificar e atualizar as referências aos esquemas parentes
     * relacionados.
     * @throws SchemaNotFoundException Se algum dos esquemas parentes não for
     * encontrado.
     * @throws GenericException Se ocorrer um erro genérico durante o processo
     * de resolução dos esquemas parentes relacionados.
     * @throws InvalidRequestException Se houver um problema com a solicitação,
     * como um esquema nulo ou inválido.
     */
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

    /**
     * Extrai os esquemas parentes do modelo de esquema de recurso com base nas
     * expressões de valor padrão definidas nos atributos.
     *
     * <p>
     * Este método é responsável por percorrer os atributos do modelo de esquema
     * de recurso fornecido (schemaModel) e extrair os nomes dos esquemas
     * parentes com base nas expressões de valor padrão definidas em seus
     * atributos. Ele realiza as seguintes etapas:</p>
     *
     * <ol>
     * <li>Cria uma lista vazia para armazenar os nomes dos esquemas
     * parentes.</li>
     * <li>Itera sobre os atributos do modelo de esquema de recurso fornecido
     * (schemaModel):
     * <ul>
     * <li>Obtém o valor padrão (defaultValue) definido no atributo atual.</li>
     * <li>Verifica se o valor padrão corresponde a uma expressão de referência
     * a um esquema parente:
     * <ul>
     * <li>Verifica se o valor padrão não é nulo e corresponde à expressão de
     * referência de esquema parente por meio de uma expressão regular
     * (regex).</li>
     * <li>A expressão regular usada para correspondência é:
     * "^\\$\\([\\w]+[\\w+\\.]+[\\.]+[\\w]+\\)$"</li>
     * </ul>
     * </li>
     * <li>Se a expressão de referência ao esquema parente for encontrada:
     * <ul>
     * <li>Remove os caracteres especiais "$" e "(" da expressão de
     * referência.</li>
     * <li>Obtém o nome do esquema parente a partir do valor extraído após a
     * última ocorrência do caractere ".".</li>
     * <li>Adiciona o nome do esquema parente à lista de esquemas parentes.</li>
     * </ul>
     * </li>
     * </ul>
     * </li>
     * <li>Retorna a lista de esquemas parentes extraídos.</li>
     * </ol>
     *
     * @param schemaModel O modelo de esquema de recurso a partir do qual os
     * esquemas parentes devem ser extraídos.
     * @return Uma lista contendo os nomes dos esquemas parentes extraídos com
     * base nas expressões de valor padrão definidas nos atributos do modelo de
     * esquema de recurso fornecido.
     */
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
     * Grava o modelo de esquema de recurso (model) no disco como um arquivo
     * JSON.
     *
     * <p>
     * Este método é responsável por salvar o modelo de esquema de recurso
     * fornecido (model) como um arquivo JSON no disco. Ele realiza as seguintes
     * etapas:</p>
     *
     * <ol>
     * <li>Obtém o nome do modelo de esquema de recurso (modelName) para ser
     * usado na construção do caminho do arquivo no disco.</li>
     * <li>Converte o nome do modelo para um formato de caminho no disco,
     * substituindo pontos (.) por barras (/), para criar o caminho
     * correto.</li>
     * <li>Cria um objeto de caminho (Path) usando o caminho do diretório
     * configurado para armazenar os esquemas (schemaDir) e o caminho do modelo
     * de esquema (modelPathStr) convertido.</li>
     * <li>Converte o objeto de caminho (Path) para um objeto de arquivo (File)
     * para verificação.</li>
     * <li>Verifica se o arquivo já existe e, se não existir ou se a opção de
     * sobrescrever (overwrite) for definida como true, prossegue com a gravação
     * no disco.</li>
     * <li>Cria o diretório pai para o arquivo, se necessário, chamando mkdirs()
     * no objeto de arquivo (f) para garantir que o caminho de destino esteja
     * presente.</li>
     * <li>Desabilita a flag de mudança de atributos (attributesChanged) no
     * modelo de esquema, pois essa flag não precisa ser salva no disco.</li>
     * <li>Inicializa um FileWriter para o arquivo de destino e usa a biblioteca
     * Gson para converter o modelo de esquema (model) em formato JSON.</li>
     * <li>Fecha o escritor após concluir a gravação do arquivo.</li>
     * <li>Após a gravação, limpa o cache do esquema para garantir que as
     * alterações sejam refletidas imediatamente no carregamento futuro.</li>
     * </ol>
     *
     * @param model O modelo de esquema de recurso a ser gravado no disco como
     * um arquivo JSON.
     * @param overwrite Um booleano indicando se deve ou não sobrescrever um
     * arquivo existente, caso o arquivo do modelo já exista no disco.
     * @throws GenericException Se ocorrer algum erro genérico durante o
     * processo de gravação do arquivo.
     * @throws InvalidRequestException Se o modelo de esquema de recurso já
     * existir no disco e a opção de sobrescrever (overwrite) não estiver
     * definida como true.
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
