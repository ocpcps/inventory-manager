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

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.arangodb.entity.DocumentCreateEntity;
import com.arangodb.entity.DocumentUpdateEntity;
import com.google.common.eventbus.Subscribe;
import com.osstelecom.db.inventory.manager.dao.ManagedResourceDao;
import com.osstelecom.db.inventory.manager.dto.FilterDTO;
import com.osstelecom.db.inventory.manager.events.ManagedResourceCreatedEvent;
import com.osstelecom.db.inventory.manager.events.ManagedResourceUpdatedEvent;
import com.osstelecom.db.inventory.manager.events.ResourceSchemaUpdatedEvent;
import com.osstelecom.db.inventory.manager.events.ServiceStateTransionedEvent;
import com.osstelecom.db.inventory.manager.exception.ArangoDaoException;
import com.osstelecom.db.inventory.manager.exception.DomainNotFoundException;
import com.osstelecom.db.inventory.manager.exception.GenericException;
import com.osstelecom.db.inventory.manager.exception.InvalidRequestException;
import com.osstelecom.db.inventory.manager.exception.ResourceNotFoundException;
import com.osstelecom.db.inventory.manager.exception.SchemaNotFoundException;
import com.osstelecom.db.inventory.manager.exception.ScriptRuleException;
import com.osstelecom.db.inventory.manager.listeners.EventManagerListener;
import com.osstelecom.db.inventory.manager.resources.Domain;
import com.osstelecom.db.inventory.manager.resources.GraphList;
import com.osstelecom.db.inventory.manager.resources.ManagedResource;
import com.osstelecom.db.inventory.manager.resources.ServiceResource;
import com.osstelecom.db.inventory.manager.resources.exception.AttributeConstraintViolationException;
import com.osstelecom.db.inventory.manager.resources.model.ResourceSchemaModel;
import com.osstelecom.db.inventory.manager.session.DynamicRuleSession;
import com.osstelecom.db.inventory.manager.session.SchemaSession;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

/**
 *
 * @author Lucas Nishimura <lucas.nishimura@gmail.com>
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
    public ManagedResource get(ManagedResource resource) throws ResourceNotFoundException, ArangoDaoException {
        return this.findManagedResource(resource);
    }

    /**
     * Delete a Managed Resource
     *
     * @param resource
     * @return
     */
    public ManagedResource delete(ManagedResource resource) throws ArangoDaoException {
        //
        // Cuidar dessa lógica é triste...
        //
//        throw new UnsupportedOperationException("Not supported yet.");

        return this.managedResourceDao.deleteResource(resource).getOld();
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
     */
    public ManagedResource create(ManagedResource resource) throws SchemaNotFoundException, AttributeConstraintViolationException, GenericException, ScriptRuleException, ArangoDaoException, InvalidRequestException, ResourceNotFoundException, DomainNotFoundException {
        String timerId = startTimer("createManagedResource");
        Boolean useUpsert = false;
        try {
            lockManager.lock();
            //
            // START - Subir as validações para session
            //
            if (resource.getKey() == null) {
                resource.setKey(getUUID());
            } else {
                //
                // Teve um ID declarado pelo usuário ou solicitante, podemos converter isso para um upsert
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
                // Agora vamos ver se o serviço é de um dominio diferente do recurso... não podem ser do mesmo
                //
                if (service.getDomain().getDomainName().equals(resource.getDomain().getDomainName())) {
                    throw new InvalidRequestException("Resource and Parent Service cannot be in the same domain.");
                }
            }

            resource.setAtomId(resource.getDomain().addAndGetId());
            ResourceSchemaModel schemaModel = schemaSession.loadSchema(resource.getAttributeSchemaName());
            resource.setSchemaModel(schemaModel);
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
     */
    public ManagedResource update(ManagedResource resource) throws InvalidRequestException, ArangoDaoException, AttributeConstraintViolationException {
        return this.updateManagedResource(resource);
    }

    /**
     * Search for a Managed Resource
     *
     * @param resource
     * @return
     * @throws ResourceNotFoundException
     * @throws ArangoDaoException
     */
    public ManagedResource findManagedResource(ManagedResource resource) throws ResourceNotFoundException, ArangoDaoException {
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
     * @throws DomainNotFoundException
     * @throws ResourceNotFoundException
     * @throws ArangoDaoException
     */
    public ManagedResource findManagedResourceById(ManagedResource resource) throws ResourceNotFoundException, ArangoDaoException {
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

    /**
     * Update a Resource,
     *
     * @param resource
     * @return
     * @throws InvalidRequestException
     */
    public ManagedResource updateManagedResource(ManagedResource resource) throws InvalidRequestException, ArangoDaoException, AttributeConstraintViolationException {
        String timerId = startTimer("updateManagedResource");
        try {
            lockManager.lock();
            //
            // Mover isso para session...
            //
            if (!resource.getOperationalStatus().equalsIgnoreCase("UP") && !resource.getOperationalStatus().equalsIgnoreCase("DOWN")) {
                throw new InvalidRequestException("Invalid OperationalStatus:[" + resource.getOperationalStatus() + "]");
            }

            this.schemaSession.validateResourceSchema(resource);

            resource.setLastModifiedDate(new Date());
            //
            // Demora ?
            //
            String timerId2 = startTimer("updateManagedResource.1");
            DocumentUpdateEntity<ManagedResource> updatedEntity = this.managedResourceDao.updateResource(resource);
            endTimer(timerId2);
            ManagedResource updatedResource = updatedEntity.getNew();
            eventManager.notifyResourceEvent(new ManagedResourceUpdatedEvent(updatedEntity.getOld(), updatedEntity.getNew()));
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
    public GraphList<ManagedResource> getNodesByFilter(FilterDTO filter, String domainName) throws DomainNotFoundException, ResourceNotFoundException, ArangoDaoException, InvalidRequestException {
        Domain domain = domainManager.getDomain(domainName);
        if (filter.getObjects().contains("nodes")) {
            Map<String, Object> bindVars = new HashMap<>();

            if (filter.getClasses() != null && !filter.getClasses().isEmpty()) {
                bindVars.put("classes", filter.getClasses());
            }

            if (filter.getBindings() != null && !filter.getBindings().isEmpty()) {
                bindVars.putAll(filter.getBindings());
            }
            return this.managedResourceDao.findResourceByFilter(filter, bindVars, domain);
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
     */
    public GraphList<ManagedResource> findManagedResourcesBySchemaName(ResourceSchemaModel model, Domain domain) throws ResourceNotFoundException, ArangoDaoException {
        return this.managedResourceDao.findResourcesBySchemaName(model.getSchemaName(), domain);
    }

    /**
     * Process the schema update Event, this is very heavy for the system, avoid
     * this use case.
     * <p>
     * Once a schema is updates, all referenced objects must be updated and all
     * rules has to be rechecked.
     *
     * @param update
     */
    public void processSchemaUpdatedEvent(ResourceSchemaUpdatedEvent update) {
        /**
         * Here we need to find all resources that are using the schema, update
         * ip, check validations and save it back to the database. Schemas are
         * shared between all domains so we need to check all Domains..
         */

        for (Domain domain : domainManager.getAllDomains()) {
            //
            // Update the schema on each domain
            //
            logger.debug("Updating Schema[{}] On Domain:[{}]", update.getModel().getSchemaName(), domain.getDomainName());

            try {
                GraphList<ManagedResource> nodesToUpdate = this.findManagedResourcesBySchemaName(update.getModel(), domain);
                logger.debug("Found {} Elements to Update", nodesToUpdate.size());

                ResourceSchemaModel model = this.schemaSession.loadSchema(update.getModel().getSchemaName());
                AtomicLong totalProcessed = new AtomicLong(0L);

                //
                // We need to make this processing multithread.
                //
                nodesToUpdate.forEachParallel(resource -> {

                    try {

                        resource.setSchemaModel(model);
                        schemaSession.validateResourceSchema(resource);

                    } catch (AttributeConstraintViolationException ex) {
                        //
                        // resource is invalid model.
                        //

                        logger.error("Failed to Validate Attributes", ex);
                        //
                        // Mark the resource schema model as invalid
                        //      
                        resource.getSchemaModel().setIsValid(false);
                    }
                    try {
                        this.managedResourceDao.updateResource(resource);
                    } catch (ArangoDaoException ex) {
                        logger.error("Failed to Update resource:[{}]", resource.getKey(), ex);
                    }
                    if (totalProcessed.incrementAndGet() % 1000 == 0) {
                        logger.debug("Updated {} Records", totalProcessed.get());
                    }

                });
            } catch (IOException | IllegalStateException | GenericException | SchemaNotFoundException | ArangoDaoException ex) {
                logger.error("Failed to update Resource Schema Model", ex);
            } catch (ResourceNotFoundException ex) {
                logger.error("Domain Has No Resources on Schema:[{}]", update.getModel(), ex);
            }
            logger.debug("Updating Schema[{}] On Domain:[{}] DONE", update.getModel().getSchemaName(), domain.getDomainName());
        }
    }

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
        this.processSchemaUpdatedEvent(update);
    }

    /**
     * Recebe as atualizações de Managed Resource
     *
     * @param updateEvent
     */
    @Subscribe
    public void onManagedResourceUpdatedEvent(ManagedResourceUpdatedEvent updateEvent) {
        logger.debug("Managed Resource [{}] Updated: ", updateEvent.getOldResource().getId());

    }

    /**
     * Recebe as notificações de Serviços
     *
     * @param serviceUpdateEvent
     */
    @Subscribe
    public void onServiceStateTransionedEvent(ServiceStateTransionedEvent serviceStateTransitionedEvent) throws DomainNotFoundException, ResourceNotFoundException, ArangoDaoException, InvalidRequestException, AttributeConstraintViolationException {
        if (serviceStateTransitionedEvent.getNewResource().getRelatedManagedResources() != null
                && !serviceStateTransitionedEvent.getNewResource().getRelatedManagedResources().isEmpty()) {

            for (String managedResourceId : serviceStateTransitionedEvent.getNewResource().getRelatedManagedResources()) {
                String domainName = this.domainManager.getDomainNameFromId(managedResourceId);
                if (!domainName.equals(serviceStateTransitionedEvent.getNewResource().getDomainName())) {
                    //
                    // Só propaga se o serviço e o recurso estiverem em dominios diferentes!
                    // Isto é para "tentar" evitar uma referencia circular, é para tentar, pois não garante.
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
                    resource.setOperationalStatus(serviceStateTransitionedEvent.getNewResource().getOperationalStatus());
                    resource.setDependentService(serviceStateTransitionedEvent.getNewResource());
                    //
                    // Atualiza tudo, retrigando todo ciclo novamente
                    //
                    this.update(resource);
                }
            }
        }else{
            logger.debug("Service Transaction Ignored");
        }
    }

}
