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

import com.google.common.eventbus.Subscribe;
import com.osstelecom.db.inventory.manager.dao.AbstractArangoDao;
import com.osstelecom.db.inventory.manager.dao.CircuitResourceDao;
import com.osstelecom.db.inventory.manager.dao.ManagedResourceDao;
import com.osstelecom.db.inventory.manager.dao.ResourceConnectionDao;
import com.osstelecom.db.inventory.manager.dao.ServiceResourceDao;
import com.osstelecom.db.inventory.manager.events.ResourceSchemaUpdatedEvent;
import com.osstelecom.db.inventory.manager.exception.ArangoDaoException;
import com.osstelecom.db.inventory.manager.exception.GenericException;
import com.osstelecom.db.inventory.manager.exception.InvalidRequestException;
import com.osstelecom.db.inventory.manager.exception.ResourceNotFoundException;
import com.osstelecom.db.inventory.manager.exception.SchemaNotFoundException;
import com.osstelecom.db.inventory.manager.jobs.DbJobStage;
import com.osstelecom.db.inventory.manager.listeners.EventManagerListener;
import com.osstelecom.db.inventory.manager.resources.BasicResource;
import com.osstelecom.db.inventory.manager.resources.CircuitResource;
import com.osstelecom.db.inventory.manager.resources.Domain;
import com.osstelecom.db.inventory.manager.resources.GraphList;
import com.osstelecom.db.inventory.manager.resources.ManagedResource;
import com.osstelecom.db.inventory.manager.resources.ResourceConnection;
import com.osstelecom.db.inventory.manager.resources.ServiceResource;
import com.osstelecom.db.inventory.manager.resources.exception.AttributeConstraintViolationException;
import com.osstelecom.db.inventory.manager.resources.model.ResourceSchemaModel;
import com.osstelecom.db.inventory.manager.session.SchemaSession;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

/**
 * Praticamente criei para lidar com os updates de Schemas nos dominios e nos
 * recursos
 *
 * @author Lucas Nishimura <lucas.nishimura@gmail.com>
 * @created 13.12.2022
 */
@Service
public class SchemaManager extends Manager {

    @Autowired
    private SchemaSession schemaSession;
    @Autowired
    private DomainManager domainManager;

    @Autowired
    private ManagedResourceDao managedResourceDao;

    @Autowired
    private ResourceConnectionDao resourceConnectionDao;

    @Autowired
    private CircuitResourceDao circuitResourceDao;

    @Autowired
    private ServiceResourceDao serviceResourceDao;

    @Autowired
    private EventManagerListener eventManager;

    private Logger logger = LoggerFactory.getLogger(SchemaManager.class);

    @EventListener(ApplicationReadyEvent.class)
    private void onStartUp() {
        this.eventManager.registerListener(this);
        logger.debug("Schema Manager Registered");
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
        logger.debug("Schema Update Deteceted.Processing...");
        this.processSchemaUpdatedEvent(update);
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
            logger.debug("Updating Schema[{}] On Domain:[{}]", update.getEventData().getSchemaName(), domain.getDomainName());

            try {

                ResourceSchemaModel model = this.schemaSession.loadSchema(update.getEventData().getSchemaName(), false);

                logger.debug("Schema:[{}] New Model Fields:", update.getEventData().getSchemaName());
                model.getAttributes().forEach((k, v) -> {
                    logger.debug("  Field: [{}] Type: [{}]", k, v.getVariableType());
                });

                String schemaName = update.getEventData().getSchemaName();

                //
                // Recursos
                //
                try {

                    //
                    // Vamos criar o Job Stage para indicar o inicio do processamento
                    //
                    DbJobStage currentStage = update.getRelatedJob().createJobStage("Update Resource Schema", "Update Resource Schema Definition on [" + domain.getDomainName() + "]");
                    GraphList<ManagedResource> resourcesToUpdate = this.managedResourceDao.findResourcesBySchemaName(schemaName, domain);
                    currentStage.setTotalRecords(resourcesToUpdate.size());

                    if (!resourcesToUpdate.isEmpty()) {
                        logger.debug("Found {} Resources to Update On Domain:[{}]", resourcesToUpdate.size(), domain.getDomainName());
                        this.processGraphListUpdate(resourcesToUpdate, model, this.managedResourceDao, currentStage);
                    }
                } catch (ResourceNotFoundException ex) {
                    logger.debug("No Resources to Update On Domain:[{}]", domain.getDomainName());
                }

                //
                // Connections
                //
                try {
                    DbJobStage currentStage = update.getRelatedJob().createJobStage("Update Connections Schema", "Update Connections Schema Definition on [" + domain.getDomainName() + "]");
                    GraphList<ResourceConnection> connectionsToUpdate = this.resourceConnectionDao.findResourcesBySchemaName(schemaName, domain);
                    currentStage.setTotalRecords(connectionsToUpdate.size());
                    if (!connectionsToUpdate.isEmpty()) {
                        logger.debug("Found {} Connections to Update On Domain:[{}]", connectionsToUpdate.size(), domain.getDomainName());
                        this.processGraphListUpdate(connectionsToUpdate, model, this.resourceConnectionDao, currentStage);
                    }
                } catch (ResourceNotFoundException ex) {
                    logger.debug("No Connections to Update On Domain:[{}]", domain.getDomainName());
                }

                //
                // Circuits
                //
                try {
                    DbJobStage currentStage = update.getRelatedJob().createJobStage("Update Connections Schema", "Update Connections Schema Definition on [" + domain.getDomainName() + "]");
                    GraphList<CircuitResource> circuitsToUpdate = this.circuitResourceDao.findResourcesBySchemaName(schemaName, domain);
                    currentStage.setTotalRecords(circuitsToUpdate.size());
                    if (!circuitsToUpdate.isEmpty()) {
                        logger.debug("Found {} Circuits to Update On Domain:[{}]", circuitsToUpdate.size(), domain.getDomainName());
                        this.processGraphListUpdate(circuitsToUpdate, model, this.circuitResourceDao, currentStage);
                    }
                } catch (ResourceNotFoundException ex) {
                    logger.debug("No Circuits to Update On Domain:[{}]", domain.getDomainName());
                }

                //
                // Services
                //
                try {
                    DbJobStage currentStage = update.getRelatedJob().createJobStage("Update Connections Schema", "Update Connections Schema Definition on [" + domain.getDomainName() + "]");
                    GraphList<ServiceResource> servicesToUpdate = this.serviceResourceDao.findResourcesBySchemaName(schemaName, domain);
                    currentStage.setTotalRecords(servicesToUpdate.size());

                    if (!servicesToUpdate.isEmpty()) {
                        logger.debug("Found {} Services to Update On Domain:[{}]", servicesToUpdate.size(), domain.getDomainName());
                        this.processGraphListUpdate(servicesToUpdate, model, this.serviceResourceDao, currentStage);
                    }

                } catch (ResourceNotFoundException ex) {
                    logger.debug("No Services to Update On Domain:[{}]", domain.getDomainName());
                }

            } catch (IOException | IllegalStateException | GenericException | SchemaNotFoundException | InvalidRequestException | ArangoDaoException ex) {
                logger.error("Failed to update Resource Schema Model", ex);
            }

            logger.debug("Updating Schema[{}] On Domain:[{}] DONE", update.getEventData().getSchemaName(), domain.getDomainName());
        }
    }

    private void processGraphListUpdate(GraphList<? extends BasicResource> resourcesToUpdate, ResourceSchemaModel newModel, AbstractArangoDao resourceDao, DbJobStage stage) throws IOException {
        //
        // We need to make this processing multithread.
        //
        AtomicLong resourcesProcessed = new AtomicLong(0L);
        resourcesToUpdate.forEachParallel(resource -> {

            try {

                resource.setSchemaModel(newModel);

                //
                //
                //
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
                resourceDao.updateResource(resource);
            } catch (ArangoDaoException | ResourceNotFoundException | InvalidRequestException ex) {
                logger.error("Failed to Update resource:[{}]", resource.getKey(), ex);
            }
            //
            // Atualiza o Processo da JOB
            //
            stage.setDoneRecords(resourcesProcessed.get());

            if (resourcesProcessed.incrementAndGet() % 1000 == 0) {
                logger.debug("Updated Resource {} Records of / {}", resourcesProcessed.get(), resourcesToUpdate.size());
            }

        });
    }

}
