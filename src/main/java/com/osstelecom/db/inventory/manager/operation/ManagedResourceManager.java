/*
 * Copyright (C) 2022 Lucas Nishimura <lucas.nishimura@gmail.com>
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
package com.osstelecom.db.inventory.manager.operation;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import com.arangodb.entity.DocumentCreateEntity;
import com.arangodb.entity.DocumentDeleteEntity;
import com.arangodb.entity.DocumentUpdateEntity;
import com.google.common.eventbus.Subscribe;
import com.osstelecom.db.inventory.manager.dao.ManagedResourceDao;
import com.osstelecom.db.inventory.manager.dto.FilterDTO;
import com.osstelecom.db.inventory.manager.events.ManagedResourceCreatedEvent;
import com.osstelecom.db.inventory.manager.events.ManagedResourceDeletedEvent;
import com.osstelecom.db.inventory.manager.events.ManagedResourceUpdatedEvent;
import com.osstelecom.db.inventory.manager.events.ResourceConnectionCreatedEvent;
import com.osstelecom.db.inventory.manager.events.ResourceConnectionDeletedEvent;
import com.osstelecom.db.inventory.manager.events.ResourceConnectionUpdatedEvent;
import com.osstelecom.db.inventory.manager.events.ResourceSchemaUpdatedEvent;
import com.osstelecom.db.inventory.manager.events.ServiceStateTransionedEvent;
import com.osstelecom.db.inventory.manager.exception.ArangoDaoException;
import com.osstelecom.db.inventory.manager.exception.AttributeNotFoundException;
import com.osstelecom.db.inventory.manager.exception.DomainNotFoundException;
import com.osstelecom.db.inventory.manager.exception.GenericException;
import com.osstelecom.db.inventory.manager.exception.InvalidRequestException;
import com.osstelecom.db.inventory.manager.exception.ResourceNotFoundException;
import com.osstelecom.db.inventory.manager.exception.SchemaNotFoundException;
import com.osstelecom.db.inventory.manager.exception.ScriptRuleException;
import com.osstelecom.db.inventory.manager.listeners.EventManagerListener;
import com.osstelecom.db.inventory.manager.resources.BasicResource;
import com.osstelecom.db.inventory.manager.resources.Domain;
import com.osstelecom.db.inventory.manager.resources.GraphList;
import com.osstelecom.db.inventory.manager.resources.ManagedResource;
import com.osstelecom.db.inventory.manager.resources.ResourceConnection;
import com.osstelecom.db.inventory.manager.resources.ServiceResource;
import com.osstelecom.db.inventory.manager.resources.exception.AttributeConstraintViolationException;
import com.osstelecom.db.inventory.manager.resources.model.ResourceAttributeModel;
import com.osstelecom.db.inventory.manager.resources.model.ResourceSchemaModel;
import com.osstelecom.db.inventory.manager.session.DynamicRuleSession;
import com.osstelecom.db.inventory.manager.session.SchemaSession;

/**
 *
 * @author Lucas Nishimura
 * @created 30.08.2022
 */
@Service
public class ManagedResourceManager extends Manager {

    @Autowired
    private EventManagerListener eventManager;

    @Autowired
    private LockManager lockManager;

    @Autowired
    private DynamicRuleSession dynamicRuleSession;

    @Autowired
    private SchemaSession schemaSession;

    @Autowired
    private ManagedResourceDao managedResourceDao;

    @Autowired
    private ServiceManager serviceManager;

    @Autowired
    private DomainManager domainManager;

    private Logger logger = LoggerFactory.getLogger(ManagedResourceManager.class);

    /**
     * Gets a Managed Resource
     *
     * @param resource
     * @return
     */
    public ManagedResource get(ManagedResource resource)
            throws ResourceNotFoundException, ArangoDaoException, InvalidRequestException {
        return this.findManagedResource(resource);
    }

    /**
     * Delete a Managed Resource
     *
     * @param resource
     * @return
     */
    public ManagedResource delete(ManagedResource resource) throws ArangoDaoException, ResourceNotFoundException {
        //
        // Cuidar dessa lógica é triste...
        //
        // throw new UnsupportedOperationException("Not supported yet.");
        DocumentDeleteEntity<ManagedResource> deletedEntity = this.managedResourceDao.deleteResource(resource);
        if (deletedEntity.getOld() != null) {
            ManagedResource deletedResource = deletedEntity.getOld();
            ManagedResourceDeletedEvent deletedEvent = new ManagedResourceDeletedEvent(deletedResource, null);
            this.eventManager.notifyResourceEvent(deletedEvent);
            return deletedResource;
        } else {
            throw new ResourceNotFoundException("Delete Request, didnt find the resource.");
        }

    }

    @EventListener(ApplicationReadyEvent.class)
    private void onStartUp() {
        this.eventManager.registerListener(this);
    }

    /**
     * Create a managed Resource
     *
     * @param resource
     * @return
     * @throws SchemaNotFoundException
     * @throws AttributeConstraintViolationException
     * @throws GenericException
     * @throws AttributeNotFoundException
     */
    public ManagedResource create(ManagedResource resource) throws SchemaNotFoundException,
            AttributeConstraintViolationException, GenericException, ScriptRuleException, ArangoDaoException,
            InvalidRequestException, ResourceNotFoundException, DomainNotFoundException, AttributeNotFoundException {
        String timerId = startTimer("createManagedResource");
        Boolean useUpsert = false;
        try {
            lockManager.lock();

            //
            // Mover isso para session...
            //
            if (!resource.getOperationalStatus().equals("Up")
                    && !resource.getOperationalStatus().equals("Down")) {
                throw new InvalidRequestException(
                        "Invalid OperationalStatus:[" + resource.getOperationalStatus() + "]");
            }

            //
            // START - Subir as validações para session
            //
            if (resource.getKey() == null) {
                resource.setKey(getUUID());
            } else {
                //
                // Teve um ID declarado pelo usuário ou solicitante, podemos converter isso para
                // um upsert
                //
                useUpsert = true;
            }

            if (resource.getDependentService() != null) {
                //
                // Valida se o serviço existe
                //
                ServiceResource service = this.serviceManager.getService(resource.getDependentService());

                //
                // Atualiza para referencia do DB
                //
                resource.setDependentService(service);

                //
                // Agora vamos ver se o serviço é de um dominio diferente do recurso... não
                // podem ser do mesmo
                //
                if (service.getDomain().getDomainName().equals(resource.getDomain().getDomainName())) {
                    throw new InvalidRequestException("Resource and Parent Service cannot be in the same domain.");
                }
            }

            resource.setAtomId(resource.getDomain().addAndGetId());
            ResourceSchemaModel schemaModel = schemaSession.loadSchema(resource.getAttributeSchemaName());
            resource.setSchemaModel(schemaModel);
            //
            // chama validate mas ele já seta os defaults
            //
            schemaSession.validateResourceSchema(resource);       //  
            dynamicRuleSession.evalResource(resource, "I", this); // <--- Pode não ser verdade , se a chave for duplicada..
            //
            // END - Subir as validações para session
            //

            DocumentCreateEntity<ManagedResource> result;
            if (useUpsert) {
                result = this.managedResourceDao.upsertResource(resource);
            } else {
                result = this.managedResourceDao.insertResource(resource);
            }
            resource.setId(result.getId());
            resource.setKey(result.getKey());
            resource.setRevisionId(result.getRev());
            //
            // Aqui criou o managed resource
            //
            ManagedResourceCreatedEvent event = new ManagedResourceCreatedEvent(result);
            this.eventManager.notifyResourceEvent(event);
            return resource;
        } finally {
            if (lockManager.isLocked()) {
                lockManager.unlock();
            }
            endTimer(timerId);
        }

    }

    /**
     * Updates a Managed Resource
     *
     * @param resource
     * @return
     * @throws InvalidRequestException
     * @throws ArangoDaoException
     * @throws AttributeConstraintViolationException
     * @throws AttributeNotFoundException
     * @throws ResourceNotFoundException
     * @throws GenericException
     * @throws SchemaNotFoundException
     */
    public ManagedResource update(ManagedResource resource) throws InvalidRequestException, ArangoDaoException,
            AttributeConstraintViolationException, ScriptRuleException, SchemaNotFoundException, GenericException,
            ResourceNotFoundException, AttributeNotFoundException {
        return this.updateManagedResource(resource, false);
    }

    /**
     * Search for a Managed Resource
     *
     * @param resource
     * @return
     * @throws ResourceNotFoundException
     * @throws ArangoDaoException
     */
    public ManagedResource findManagedResource(ManagedResource resource)
            throws ResourceNotFoundException, ArangoDaoException, InvalidRequestException {
        if (resource.getId() != null) {
            if (!resource.getId().contains("/")) {
                resource.setId(resource.getDomain().getNodes() + "/" + resource.getId());
            }
        }

        return this.managedResourceDao.findResource(resource);
    }

    /**
     * <p>
     * Find a managed resource by id, in arangodb, the ID is a combination of
     * the collection plus id something like collection/uuid.
     * <p>
     * It the given resource just contains the UUID, the method will fix it to
     * the pattern as collection/uuid based on the resource domain
     *
     * @param resource
     * @return ManagedResource - The resource
     * @throws ResourceNotFoundException
     * @throws ArangoDaoException
     * @throws InvalidRequestException
     */
    public ManagedResource findManagedResourceById(ManagedResource resource)
            throws ResourceNotFoundException, ArangoDaoException, InvalidRequestException {
        String timerId = startTimer("findManagedResourceById");
        try {
            lockManager.lock();

            if (!resource.getId().contains("/")) {
                resource.setId(resource.getDomain().getNodes() + "/" + resource.getId());
            }
            resource = this.managedResourceDao.findResource(resource);
            return resource;
        } finally {
            if (lockManager.isLocked()) {
                lockManager.unlock();
            }
            endTimer(timerId);
        }
    }

    public String findManagedResource(FilterDTO filter) {
        return this.managedResourceDao.runNativeQuery(filter);
    }

    /**
     * Update a Resource,
     *
     * @param resource
     * @param fromEvent
     * @return
     * @throws InvalidRequestException
     * @throws ArangoDaoException
     * @throws AttributeConstraintViolationException
     * @throws ScriptRuleException
     * @throws SchemaNotFoundException
     * @throws GenericException
     * @throws ResourceNotFoundException
     * @throws AttributeNotFoundException
     */
    public ManagedResource updateManagedResource(ManagedResource resource, Boolean fromEvent)
            throws InvalidRequestException,
            ArangoDaoException, AttributeConstraintViolationException, ScriptRuleException, SchemaNotFoundException,
            GenericException, ResourceNotFoundException, AttributeNotFoundException {
        String timerId = startTimer("updateManagedResource");
        try {
            lockManager.lock();
            //
            // Mover isso para session...
            //
            if (!resource.getOperationalStatus().equals("Up")
                    && !resource.getOperationalStatus().equals("Down")) {
                throw new InvalidRequestException(
                        "Invalid OperationalStatus:[" + resource.getOperationalStatus() + "]");
            }

            ResourceSchemaModel schemaModel = schemaSession.loadSchema(resource.getAttributeSchemaName());
            resource.setSchemaModel(schemaModel);

            this.schemaSession.validateResourceSchema(resource);
            dynamicRuleSession.evalResource(resource, "U", this); // <--- Pode não ser verdade , se a chave for
            // duplicada..

            resource.setAttributes(calculateDefaultValues(schemaModel, resource, fromEvent));

            resource.setLastModifiedDate(new Date());
            //
            // Demora ?
            //
            String timerId2 = startTimer("updateManagedResource.1");

            List<String> eventSourceIds = resource.getEventSourceIds();
            resource.setEventSourceIds(null);

            DocumentUpdateEntity<ManagedResource> updatedEntity = this.managedResourceDao.updateResource(resource);
            endTimer(timerId2);

            ManagedResource updatedResource = updatedEntity.getNew();
            updatedResource.setEventSourceIds(eventSourceIds);

            eventManager.notifyResourceEvent(
                    new ManagedResourceUpdatedEvent(updatedEntity.getOld(), updatedEntity.getNew()));
            return updatedResource;
        } finally {
            if (lockManager.isLocked()) {
                lockManager.unlock();
            }
            endTimer(timerId);
        }
    }

    /**
     * Search a list of Managed Resources by filter
     *
     * @param filter
     * @param domainName
     * @return
     * @throws DomainNotFoundException
     * @throws ResourceNotFoundException
     * @throws ArangoDaoException
     * @throws InvalidRequestException
     */
    public GraphList<ManagedResource> getNodesByFilter(FilterDTO filter, String domainName)
            throws DomainNotFoundException, ResourceNotFoundException, ArangoDaoException, InvalidRequestException {
        Domain domain = domainManager.getDomain(domainName);
        if (filter.getLimit() != null) {
            if (filter.getLimit() > 1000) {
                throw new InvalidRequestException(
                        "Result Set Limit cannot be over 1000, please descrease limit value to a range between 0 and 1000");
            } else {
                if (filter.getLimit() < 0L) {
                    filter.setLimit(1000L);
                }
            }
        } else {
            filter.setLimit(1000L);
        }
        if (filter.getObjects().contains("nodes")) {

            if (filter.getClasses() != null && !filter.getClasses().isEmpty()) {
                filter.getBindings().put("classes", filter.getClasses());
            }

            if (filter.getBindings() != null && !filter.getBindings().isEmpty()) {
                filter.getBindings().putAll(filter.getBindings());
            }
            return this.managedResourceDao.findResourceByFilter(filter, domain);
        }
        throw new InvalidRequestException("getNodesByFilter() can only retrieve nodes objects");
    }

    /**
     * Retrieve a Stream list ( GraphList) from a SchemaModelName
     *
     * @param model
     * @param domain
     * @return
     * @throws ResourceNotFoundException
     * @throws ArangoDaoException
     * @throws InvalidRequestException
     */
    public GraphList<ManagedResource> findManagedResourcesBySchemaName(ResourceSchemaModel model, Domain domain)
            throws ResourceNotFoundException, ArangoDaoException, InvalidRequestException {
        return this.managedResourceDao.findResourcesBySchemaName(model.getSchemaName(), domain);
    }

//    /**
//     * Process the schema update Event, this is very heavy for the system, avoid
//     * this use case.
//     * <p>
//     * Once a schema is updates, all referenced objects must be updated and all
//     * rules has to be rechecked.
//     *
//     * @deprecated
//     * @param update
//     */
//    public void processSchemaUpdatedEvent(ResourceSchemaUpdatedEvent update) {
//
//    }
    /**
     * Called when a Managed Resource is created
     *
     * @param resource
     */
    @Subscribe
    public void onManagedResourceCreatedEvent(ManagedResourceCreatedEvent resource) {
    }

    /**
     * An resource Schema update just Happened...we neeed to update and check
     * all resources...
     *
     * @param update
     */
    @Subscribe
    public void onResourceSchemaUpdatedEvent(ResourceSchemaUpdatedEvent update) {
        //
        // Notify the schema session that a schema has changed
        // Now, it will search for:
        // Nodes to be updates -> Connections that relies on those nodes
        //
//        this.processSchemaUpdatedEvent(update);
    }

    /**
     * Recebe as atualizações de Managed Resource
     *
     * @param updateEvent
     * @throws GenericException
     * @throws SchemaNotFoundException
     * @throws AttributeNotFoundException
     * @throws ScriptRuleException
     * @throws AttributeConstraintViolationException
     * @throws InvalidRequestException
     * @throws ResourceNotFoundException
     * @throws ArangoDaoException
     */
    @Subscribe
    public void onManagedResourceUpdatedEvent(ManagedResourceUpdatedEvent updateEvent) throws SchemaNotFoundException,
            GenericException, ArangoDaoException, ResourceNotFoundException, InvalidRequestException,
            AttributeConstraintViolationException, ScriptRuleException, AttributeNotFoundException {
        logger.debug("Managed Resource [{}] Updated: ", updateEvent.getOldResource().getId());

        ManagedResource resource = updateEvent.getNewResource();
        ResourceSchemaModel schemaModel = schemaSession.loadSchema(resource.getAttributeSchemaName());
        updateRelatedChildResources(resource.getId(), resource.getDomainName(), schemaModel.getRelatedSchemas());
    }

    /**
     * Recebe a notificação de que uma conexão foi criada,
     *
     * @param connectionCreatedEvent
     * @throws InvalidRequestException
     * @throws ResourceNotFoundException
     * @throws ArangoDaoException
     * @throws AttributeNotFoundException
     * @throws GenericException
     * @throws SchemaNotFoundException
     * @throws ScriptRuleException
     * @throws AttributeConstraintViolationException
     */
    @Subscribe
    public void onResourceConnectionCreatedEvent(ResourceConnectionCreatedEvent connectionCreatedEvent)
            throws ArangoDaoException, ResourceNotFoundException, InvalidRequestException,
            AttributeConstraintViolationException, ScriptRuleException, SchemaNotFoundException, GenericException,
            AttributeNotFoundException {

        ResourceConnection resourceConnection = connectionCreatedEvent.getNewResource();
        if (!CollectionUtils.isEmpty(resourceConnection.getEventSourceIds())) {
            if (resourceConnection.getEventSourceIds().contains(resourceConnection.getId())) {
                return;
            }
        }

        ManagedResource resource = connectionCreatedEvent.getNewResource().getToResource();

        List<String> sourceIds = new ArrayList<>(resourceConnection.getEventSourceIds());
        sourceIds.add(resourceConnection.getId());
        resource.setEventSourceIds(sourceIds);

        this.updateManagedResource(resource, true);
    }

    @Subscribe
    public void onResourceConnectionUpdatedEvent(ResourceConnectionUpdatedEvent connectionUpdatedEvent)
            throws ArangoDaoException, ResourceNotFoundException, InvalidRequestException,
            AttributeConstraintViolationException, ScriptRuleException, SchemaNotFoundException, GenericException,
            AttributeNotFoundException {

        ManagedResource resource = connectionUpdatedEvent.getNewResource().getToResource();

        ResourceConnection resourceConnection = connectionUpdatedEvent.getNewResource();
        if (!CollectionUtils.isEmpty(resourceConnection.getEventSourceIds())) {
            if (resourceConnection.getEventSourceIds().contains(resource.getId())) {
                return;
            }
        }

        List<String> sourceIds = new ArrayList<>(resourceConnection.getEventSourceIds());
        sourceIds.add(resourceConnection.getId());
        resource.setEventSourceIds(sourceIds);

        this.updateManagedResource(resource, true);
    }

    @Subscribe
    public void onResourceConnectionDeletedEvent(ResourceConnectionDeletedEvent connectionDeletedEvent)
            throws ArangoDaoException, ResourceNotFoundException, InvalidRequestException,
            AttributeConstraintViolationException, ScriptRuleException, SchemaNotFoundException, GenericException,
            AttributeNotFoundException {

        ResourceConnection resourceConnection = connectionDeletedEvent.getOldResource();
        if (resourceConnection != null) {
            if (!CollectionUtils.isEmpty(resourceConnection.getEventSourceIds())) {
                if (resourceConnection.getEventSourceIds().contains(resourceConnection.getId())) {
                    return;
                }
            }
        }

        ManagedResource resource = connectionDeletedEvent.getOldResource().getToResource();
        if (resource != null) {
            List<String> sourceIds = new ArrayList<>(resourceConnection.getEventSourceIds());
            sourceIds.add(resourceConnection.getId());
            resource.setEventSourceIds(sourceIds);
            this.updateManagedResource(resource, true);
        }
    }

    /**
     * Recebe as notificações de Serviços
     *
     * @param serviceUpdateEvent
     * @throws AttributeNotFoundException
     * @throws GenericException
     * @throws SchemaNotFoundException
     */
    @Subscribe
    public void onServiceStateTransionedEvent(ServiceStateTransionedEvent serviceStateTransitionedEvent)
            throws DomainNotFoundException, ResourceNotFoundException, ArangoDaoException, InvalidRequestException,
            AttributeConstraintViolationException, ScriptRuleException, SchemaNotFoundException, GenericException,
            AttributeNotFoundException {
        if (serviceStateTransitionedEvent.getNewResource().getRelatedManagedResources() != null
                && !serviceStateTransitionedEvent.getNewResource().getRelatedManagedResources().isEmpty()) {

            for (String managedResourceId : serviceStateTransitionedEvent.getNewResource()
                    .getRelatedManagedResources()) {
                String domainName = this.domainManager.getDomainNameFromId(managedResourceId);
                if (!domainName.equals(serviceStateTransitionedEvent.getNewResource().getDomainName())) {
                    //
                    // Só propaga se o serviço e o recurso estiverem em dominios diferentes!
                    // Isto é para "tentar" evitar uma referencia circular, é para tentar, pois não
                    // garante.
                    //

                    //
                    // Então sabemos que o Serviço em questão afeta recursos de outros dominios!
                    //
                    Domain domain = this.domainManager.getDomain(domainName);
                    ManagedResource resource = new ManagedResource(domain, managedResourceId);

                    //
                    // Obtem a referencia do DB
                    //
                    resource = this.get(resource);

                    //
                    // Replica o estado do Serviço no Recurso.
                    //
                    resource.setOperationalStatus(
                            serviceStateTransitionedEvent.getNewResource().getOperationalStatus());
                    resource.setDependentService(serviceStateTransitionedEvent.getNewResource());
                    //
                    // Atualiza tudo, retrigando todo ciclo novamente
                    //
                    this.updateManagedResource(resource, true);
                }
            }
        } else {
            logger.debug("Service Transaction Ignored");
        }
    }

    private Map<String, Object> calculateDefaultValues(ResourceSchemaModel schemaModel, ManagedResource managedResource,
            boolean fromEvent)
            throws ArangoDaoException, ResourceNotFoundException, InvalidRequestException, AttributeNotFoundException {
        Map<String, Object> result = new HashMap<>();
        result.putAll(managedResource.getAttributes());
        for (Map.Entry<String, ResourceAttributeModel> attribute : schemaModel.getAttributes().entrySet()) {
            String defaultValue = attribute.getValue().getDefaultValue();
            if (defaultValue != null) {
                String regex = "^\\$\\([\\w]+[\\w+\\.]+[\\.]+[\\w]+\\)$";
                if (defaultValue.matches(regex)) {
                    if (!fromEvent) {
                        Object resourceValue = managedResource.getAttributes().get(attribute.getKey());
                        if (resourceValue != null && !ObjectUtils.isEmpty(resourceValue)) {
                            throw new InvalidRequestException("O valor do Atributo " + attribute.getKey()
                                    + " não deve ser enviado, pois será calculado pelo expressão do schema");
                        }
                    }
                    // se for uma expressão, sempre recalcula
                    Object value = calculateDefaultAttributeValue(managedResource.getId(),
                            managedResource.getDomain().getDomainName(), attribute.getValue().getDefaultValue());
                    if (value == null) {
                        result.remove(attribute.getKey());
                        if (attribute.getValue().getRequired().booleanValue()) {
                            if (fromEvent) {
                                managedResource.getSchemaModel().setIsValid(false);
                            } else {
                                throw new AttributeNotFoundException(
                                        "Atributo " + attribute.getValue().getName() + " não encontrado em um nó pai");
                            }
                        }
                    } else {
                        result.replace(attribute.getKey(), value);
                    }
                } else {
                    // se não for expressão, só substitui se não tiver valor
                    Object resourceValue = managedResource.getAttributes().get(attribute.getKey());
                    if (resourceValue == null || ObjectUtils.isEmpty(resourceValue)) {
                        result.remove(attribute.getKey());
                        result.put(attribute.getKey(), defaultValue);
                    }
                }
            }
        }
        return result;
    }

    private Object calculateDefaultAttributeValue(String nodeId, String domain, String defaultValue)
            throws ArangoDaoException, ResourceNotFoundException, InvalidRequestException {

        // remover cifrão e parenteses.
        String maskSign = defaultValue.replace("$", "");
        String maskP = maskSign.replace("(", "");
        String value = maskP.replace(")", "");

        Integer pointer = value.lastIndexOf(".");
        String attributeSchemaName = value.substring(0, pointer);
        String attributeName = value.substring(pointer + 1, value.length());

        GraphList<BasicResource> result = managedResourceDao.findParentsByAttributeSchemaName(nodeId, domain,
                attributeSchemaName, attributeName);

        BasicResource parentResource = null;
        try {
            parentResource = result.getOne();
            if (parentResource == null) {
                return null;
            }
        } catch (NoSuchElementException e) {
            return null;
        }

        ManagedResource resource = managedResourceDao
                .findResource(new ManagedResource(parentResource.getDomain(), parentResource.getId()));
        return resource.getAttributes().get(attributeName);
    }

    private void updateRelatedChildResources(String nodeId, String domain, List<String> relatedSchemas)
            throws ArangoDaoException, ResourceNotFoundException, InvalidRequestException,
            AttributeConstraintViolationException, ScriptRuleException, SchemaNotFoundException, GenericException,
            AttributeNotFoundException {
        if (relatedSchemas != null) {
            for (String relatedSchema : relatedSchemas) {
                GraphList<BasicResource> result = managedResourceDao.findChildrenByAttributeSchemaName(nodeId, domain,
                        relatedSchema);
                for (BasicResource basicResource : result.toList()) {
                    logger.debug("Request update Child Resource [{}] Updated: ", basicResource.getId());
                    ManagedResource resource = managedResourceDao
                            .findResource(new ManagedResource(basicResource.getDomain(), basicResource.getId()));
                    this.updateManagedResource(resource, true);
                }
            }
        }

    }

}
