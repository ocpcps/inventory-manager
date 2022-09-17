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

import com.osstelecom.db.inventory.manager.listeners.EventManagerListener;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import com.osstelecom.db.inventory.manager.configuration.ConfigurationManager;
import com.osstelecom.db.inventory.manager.events.ResourceSchemaUpdatedEvent;
import com.osstelecom.db.inventory.manager.exception.GenericException;
import com.osstelecom.db.inventory.manager.exception.InvalidRequestException;
import com.osstelecom.db.inventory.manager.exception.SchemaNotFoundException;
import com.osstelecom.db.inventory.manager.resources.BasicResource;
import com.osstelecom.db.inventory.manager.resources.exception.AttributeConstraintViolationException;
import com.osstelecom.db.inventory.manager.resources.model.ResourceAttributeModel;
import com.osstelecom.db.inventory.manager.resources.model.ResourceSchemaModel;
import com.osstelecom.db.inventory.manager.response.CreateResourceSchemaModelResponse;
import com.osstelecom.db.inventory.manager.response.PatchResourceSchemaModelResponse;
import com.osstelecom.db.inventory.manager.response.ResourceSchemaResponse;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;

import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

/**
 *
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

    @EventListener(ApplicationReadyEvent.class)
    private void initSchemaSession() {
        this.schemaCache = CacheBuilder
                .newBuilder()
                .maximumSize(5000)
                .removalListener(this)
                .expireAfterWrite(this.configurationManager
                        .loadConfiguration()
                        .getSchemaCacheTTL(), TimeUnit.SECONDS).build();
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
    private ResourceSchemaModel loadSchemaFromDisk(String schemaName, ResourceSchemaModel result) throws SchemaNotFoundException, GenericException {
        schemaName = schemaName.replaceAll("\\.", "/");
        File f = new File(this.schemaDir + "/" + schemaName + ".json");
        logger.debug("Trying to load Schema from: " + schemaName);

        if (f.exists()) {
            try {
                //
                // Arquivo existe vamos ler os modelos
                //
                FileReader jsonReader = new FileReader(f);
                ResourceSchemaModel resourceModel = utilSession.getGson().fromJson(jsonReader, ResourceSchemaModel.class);
                if (result == null) {
                    result = resourceModel;
                }
                logger.debug("Loaded  SchemaName: [" + resourceModel.getSchemaName() + "]");
                jsonReader.close();

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
            }
        } else {
            throw new SchemaNotFoundException("Schema With Name:[" + schemaName + "] was not found");
        }

    }

    /**
     * Creates the Schema on the schema directory
     *
     * @param model
     */
    public CreateResourceSchemaModelResponse createResourceSchemaModel(ResourceSchemaModel model) throws GenericException, SchemaNotFoundException, InvalidRequestException {

        if (model == null) {
            throw new GenericException("Request Cannot Be Null");
        }

        // 
        // If the From Schema is null, set to the default
        // 
        if (model.getFromSchema() == null) {
            model.setFromSchema("default.json");
        } else {
            //
            // Check if the from Schema Exists...
            // We try to load it to check if it exists.
            //
            this.loadSchema(model.getFromSchema());
        }

        this.writeModelToDisk(model, false);
        this.clearSchemaCache();
        return new CreateResourceSchemaModelResponse(this.loadSchema(model.getSchemaName()));

    }

    /**
     * Clear all cached Entries...
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
        if (resource.getSchemaModel().getAllowAll() == null) {
            resource.getSchemaModel().setAllowAll(false);
        }
        if (!resource.getSchemaModel().getAllowAll()) {

            for (String key : resource.getAttributes().keySet()) {
                if (!resource.getSchemaModel().getAttributes().containsKey(key)) {
                    resource.getSchemaModel().setIsValid(false);
                    throw new AttributeConstraintViolationException("Invalid Attribute named:[" + key + "] for model: [" + resource.getSchemaModel().getSchemaName() + "]");

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
                    if (entry.getRequired()) {
                        if (!resource.getAttributes().containsKey(entry.getName())) {
                            if (entry.getDefaultValue() != null) {
                                resource.getAttributes().put(entry.getName(), getAttributeValue(entry, entry.getDefaultValue()));
                            } else {
                                //
                                // Lança a exception de validação
                                //
                                resource.getSchemaModel().setIsValid(false);

                                throw new AttributeConstraintViolationException("Missing Required Attribute Named:[" + entry.getName() + "]");
                            }
                        } else {
                            resource.getAttributes().put(entry.getName(), getAttributeValue(entry, resource.getAttributes().get(entry.getName())));
                        }
                    } else {
                        if (!resource.getAttributes().containsKey(entry.getName())) {
                            if (entry.getDefaultValue() != null) {
                                resource.getAttributes().put(entry.getName(), getAttributeValue(entry, entry.getDefaultValue()));
                            } else {
                                //
                                // Isso pode gerar inconsistencia
                                //
                            }
                        } else {
                            resource.getAttributes().put(entry.getName(), getAttributeValue(entry, resource.getAttributes().get(entry.getName())));
                        }
                    }
                }
                resource.getSchemaModel().setIsValid(true);
            } catch (AttributeConstraintViolationException acve) {
                resource.getSchemaModel().setIsValid(false);
                throw acve;
            }
            resource.getSchemaModel().setIsValid(true);
        } else {
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
    public Object getAttributeValue(ResourceAttributeModel model, Object value) throws AttributeConstraintViolationException {
//        if (!model.getIsList()) {

        try {
            if (model.getAllowedValues() != null) {
                if (!model.getAllowedValues().isEmpty()) {
                    if (!model.getAllowedValues().contains(value)) {
                        //
                        // Pode Prosseguir 
                        //
                        throw new AttributeConstraintViolationException("Attribute [" + model.getName() + "] of type:" + model.getVariableType() + " Value : [" + value + "] is not allowed here Allowed vars are:[" + String.join(",", model.getAllowedValues()) + "]");

                    }
                }
            }

            if (model.getVariableType().equalsIgnoreCase("String")) {
                //
                // String will get the String representation as it is..
                //
                return value;
            } else if (model.getVariableType().equalsIgnoreCase("Number")) {
                if (value instanceof Long) {
                    return value;
                }
                if (value.toString().contains(".")) {
                    Double d = Double.parseDouble(value.toString());
                    return d.longValue();
                }
                return Long.parseLong(value.toString());
            } else if (model.getVariableType().equalsIgnoreCase("Boolean")) {
                if (value instanceof Boolean) {
                    return value;
                }
                if (value.toString().equalsIgnoreCase("true") || value.toString().equalsIgnoreCase("false")) {
                    return value.toString().equalsIgnoreCase("true");
                } else {
                    throw new AttributeConstraintViolationException("Attribute [" + model.getName() + "] of type:" + model.getVariableType() + " Does not accpect value: [" + value + "]");
                }
            } else if (model.getVariableType().equalsIgnoreCase("Float")) {
                if (value instanceof Float) {
                    return value;
                }
                return Float.parseFloat(value.toString());
            } else if (model.getVariableType().equalsIgnoreCase("Date")) {

                SimpleDateFormat sdf = new SimpleDateFormat(configurationManager.loadConfiguration().getDateFormat());
                sdf.setLenient(false);
                try {
                    return sdf.parse(value.toString());
                } catch (ParseException ex) {
                    throw new AttributeConstraintViolationException("Attribute [" + model.getName() + "] of type:" + model.getVariableType() + " Cannot Parse Date Value : [" + value + "] With Mask: [" + configurationManager.loadConfiguration().getDateFormat() + "]", ex);
                }

            } else if (model.getVariableType().equalsIgnoreCase("DateTime")) {
                SimpleDateFormat sdf = new SimpleDateFormat(configurationManager.loadConfiguration().getDateTimeFormat());
                sdf.setLenient(false);
                try {
                    return sdf.parse(value.toString());
                } catch (ParseException ex) {
                    throw new AttributeConstraintViolationException("Attribute [" + model.getName() + "] of type:" + model.getVariableType() + " Cannot Parse Date Time Value : [" + value + "] With Mask: [" + configurationManager.loadConfiguration().getDateTimeFormat() + "]", ex);

                }
            } else {
                throw new AttributeConstraintViolationException("Attribute [" + model.getName() + "] of type:" + model.getVariableType() + " Cannot be parsed");
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
    public PatchResourceSchemaModelResponse patchSchemaModel(ResourceSchemaModel update) throws InvalidRequestException, GenericException, SchemaNotFoundException {

        if (update == null) {
            throw new InvalidRequestException("Attribute Schema Model not found");
        }

        ResourceSchemaModel original = this.loadSchema(update.getSchemaName());

        if (update.getSchemaName() != null) {
            if (!update.getSchemaName().equals(original.getSchemaName())) {
                //
                // Schema Name cannot Be Changed
                // 
                throw new InvalidRequestException("Schema Name Cannot Ne changed...");
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

        if (!update.getAttributes().isEmpty()) {
            //
            // The hard part of comparing and merging maps
            //

            //
            // Compare each
            //
            update.getAttributes().forEach((updateAttrName, updateModel) -> {
                //
                //  Check if need to remove the Attribute
                //
                if (updateModel.getDoRemove() == null) {
                    updateModel.setDoRemove(false);
                }

                if (updateModel.getDoRemove()) {
                    if (original.getAttributes().containsKey(updateAttrName)) {
                        original.getAttributes().remove(updateAttrName);
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

                            if (updateModel.getValidationRegex() != null) {
                                if (!updateModel.getValidationRegex().equals(originalAttributeModel.getValidationRegex())) {
                                    originalAttributeModel.setValidationRegex(updateModel.getValidationRegex());
                                    original.setAttributesChanged(true);
                                }
                            }

                            if (updateModel.getValidationScript() != null) {
                                if (!updateModel.getValidationScript().equals(originalAttributeModel.getValidationScript())) {
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

                            if (updateModel.getDefaultValue() != null) {
                                if (!updateModel.getDefaultValue().equals(originalAttributeModel.getDefaultValue())) {
                                    originalAttributeModel.setDefaultValue(updateModel.getDefaultValue());
                                    original.setAttributesChanged(true);
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

        if (original.getAttributesChanged()) {

            this.writeModelToDisk(original, true);
            this.clearSchemaCache();
            //
            // Notifica o Message Bus da Atualização, ele vai varrer a base procurando utilizações...
            // 
            eventManager.notifyGenericEvent(new ResourceSchemaUpdatedEvent(original));
        }
        return new PatchResourceSchemaModelResponse(this.loadSchema(original.getSchemaName()));
    }

    /**
     * Save the JSON Model to the disk
     *
     * @param model
     * @param overwrite
     * @throws GenericException
     * @throws InvalidRequestException
     */
    private void writeModelToDisk(ResourceSchemaModel model, Boolean overwrite) throws GenericException, InvalidRequestException {
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
//        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        logger.debug("Schema: [" + rn.getKey() + "] Removed From Cache...[" + rn.getCause().name() + "]");
    }
}
