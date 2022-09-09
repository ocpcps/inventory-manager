/*
 * Copyright (C) 2021 Lucas Nishimura <lucas.nishimura@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.osstelecom.db.inventory.manager.operation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import javax.management.ServiceNotFoundException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import com.arangodb.entity.DocumentCreateEntity;
import com.arangodb.entity.DocumentUpdateEntity;
import com.google.common.eventbus.Subscribe;
import com.osstelecom.db.inventory.manager.dao.ServiceResourceDao;
import com.osstelecom.db.inventory.manager.events.ProcessServiceIntegrityEvent;
import com.osstelecom.db.inventory.manager.events.ServiceResourceCreatedEvent;
import com.osstelecom.db.inventory.manager.events.ServiceResourceUpdatedEvent;
import com.osstelecom.db.inventory.manager.exception.ArangoDaoException;
import com.osstelecom.db.inventory.manager.exception.DomainNotFoundException;
import com.osstelecom.db.inventory.manager.exception.ResourceNotFoundException;
import com.osstelecom.db.inventory.manager.listeners.EventManagerListener;
import com.osstelecom.db.inventory.manager.resources.CircuitResource;
import com.osstelecom.db.inventory.manager.resources.ServiceResource;
import com.osstelecom.db.inventory.manager.resources.model.ResourceSchemaModel;
import com.osstelecom.db.inventory.manager.session.DynamicRuleSession;
import com.osstelecom.db.inventory.manager.session.SchemaSession;

@Service
public class ServiceManager extends Manager {

    @Autowired
    private DynamicRuleSession dynamicRuleSession;

    @Autowired
    private SchemaSession schemaSession;

    @Autowired
    private ServiceResourceDao serviceDao;

    @Autowired
    private EventManagerListener eventManager;

    @Autowired
    private CircuitResourceManager circuitResourceManager;

    @Autowired
    private ReentrantLock lockManager;

    private Logger logger = LoggerFactory.getLogger(ServiceManager.class);

    /**
     * Retrieves a domain by name
     *
     * @param domainName
     * @return
     * @throws DomainNotFoundException
     * @throws ArangoDaoException
     * @throws ServiceNotFoundException
     */
    public ServiceResource getService(ServiceResource service) throws ResourceNotFoundException, ArangoDaoException {
        return this.serviceDao.findResource(service);
    }

    public ServiceResource getServiceById(ServiceResource service)
            throws ResourceNotFoundException, ArangoDaoException {
        String timerId = startTimer("getServiceById");
        try {
            lockManager.lock();

            if (!service.getId().contains("/")) {
                service.setId(service.getDomain().getServices() + "/" + service.getId());
            }

            service = this.serviceDao.findResource(service);
            return service;
        } finally {
            if (lockManager.isLocked()) {
                lockManager.unlock();
            }
            endTimer(timerId);
        }
    }

    public ServiceResource deleteService(ServiceResource service) throws ArangoDaoException {
        try {
            lockManager.lock();
            this.serviceDao.deleteResource(service);
            return service;
        } finally {
            if (lockManager.isLocked()) {
                lockManager.unlock();
            }
        }
    }

    public ServiceResource createService(ServiceResource service) throws ArangoDaoException {
        String timerId = startTimer("createServiceResource");

        try {
            lockManager.lock();
            //
            // Garante que o Merge Funcione
            //
            if (service.getKey() == null) {
                service.setKey(this.getUUID());
            }
            if(service.getOperationalStatus() == null || service.getOperationalStatus().isEmpty()){
                service.setOperationalStatus("UP");
            }

            service.setAtomId(service.getDomain().addAndGetId());
            ResourceSchemaModel schemaModel = schemaSession.loadSchema(service.getAttributeSchemaName());
            service.setSchemaModel(schemaModel);
            schemaSession.validateResourceSchema(service);
            dynamicRuleSession.evalResource(service, "I", this); // <--- Pode não ser verdade , se a chave for
                                                                 // duplicada..

            DocumentCreateEntity<ServiceResource> result = serviceDao.insertResource(service);
            service.setKey(result.getId());
            service.setRevisionId(result.getRev());
            //
            // Aqui criou o managed resource
            //
            ServiceResourceCreatedEvent event = new ServiceResourceCreatedEvent(service);
            this.eventManager.notifyEvent(event);
            return service;
        } catch (Exception e) {
            throw new ArangoDaoException("Error while creating ServiceResource", e);
        } finally {
            if (lockManager.isLocked()) {
                lockManager.unlock();
            }
            endTimer(timerId);
        }

    }

    public ServiceResource updateService(ServiceResource service) throws ArangoDaoException {
        String timerId = startTimer("updateServiceResource");
        try {
            lockManager.lock();
            service.setLastModifiedDate(new Date());
            DocumentUpdateEntity<ServiceResource> result = serviceDao.updateResource(service);
            ServiceResource newService = result.getNew();
            ServiceResource oldService = result.getOld();
            ServiceResourceUpdatedEvent event = new ServiceResourceUpdatedEvent(oldService, newService);
            this.eventManager.notifyEvent(event);
            return newService;
        } finally {
            if (lockManager.isLocked()) {
                lockManager.unlock();
            }
            endTimer(timerId);
        }
    }

    public ServiceResource resolveService(ServiceResource service)
            throws ResourceNotFoundException, ArangoDaoException {

        List<ServiceResource> resolvedServices = new ArrayList<>();
        if (service.getDependencies() != null && !service.getDependencies().isEmpty()) {
            for (ServiceResource item : service.getDependencies()) {
                if (item.getDomain() == null) {
                    item.setDomain(service.getDomain());
                }
                ServiceResource resolved = this.getService(service);
                resolvedServices.add(resolved);
            }
        }
        service.setDependencies(resolvedServices);

        List<CircuitResource> resolvedCircuits = new ArrayList<>();
        if (service.getCircuits() != null && !service.getCircuits().isEmpty()) {
            for (CircuitResource circuit : service.getCircuits()) {
                if (circuit.getDomain() == null) {
                    circuit.setDomain(service.getDomain());
                }
                CircuitResource resolved = this.circuitResourceManager.findCircuitResource(circuit);
                resolvedCircuits.add(resolved);
            }
        }
        service.setCircuits(resolvedCircuits);

        return service;
    }

    @EventListener(ApplicationReadyEvent.class)
    private void onStartUp() {
        eventManager.registerListener(this);
    }

    @Subscribe
    public void onProcessServiceIntegrityEvent(ProcessServiceIntegrityEvent processEvent)
            throws ArangoDaoException, ResourceNotFoundException {
        computeServiceIntegrity(processEvent.getNewResource());
    }

    @Subscribe
    public void onProcessServiceResourceUpdatedEvent(ServiceResourceUpdatedEvent processEvent)
            throws ArangoDaoException, IllegalStateException, IOException {
        computeServiceResourceUpdated(processEvent.getNewResource(), processEvent.getOldResource());
    }

    private void computeServiceIntegrity(ServiceResource service) throws ArangoDaoException, ResourceNotFoundException {
        //
        // Forks with the logic of checking integrity
        //
        Long start = System.currentTimeMillis();
        List<String> brokenDependencies = new ArrayList<>();
        int totalDependencies = 0;

        service = this.getServiceById(service);
        service = this.resolveService(service);

        if (service.getDependencies() != null) {
            for (ServiceResource item : service.getDependencies()) {
                if (!item.getOperationalStatus().equalsIgnoreCase("UP") && !item.getDegrated()) {
                    brokenDependencies.add(item.getId());
                }
            }
            totalDependencies += service.getDependencies().size();
        }

        if (service.getCircuits() != null) {
            for (CircuitResource item : service.getCircuits()) {
                //
                // get current node status
                //
                if (!item.getOperationalStatus().equalsIgnoreCase("UP") && !item.getDegrated()) {
                    brokenDependencies.add(item.getId());
                }
            }
            totalDependencies += service.getCircuits().size();
        }
        service.setBrokenResources(brokenDependencies);

        if (brokenDependencies.isEmpty()) {
            service.setOperationalStatus("UP");
            service.setBroken(false);
            service.setDegrated(false);
        } else if (brokenDependencies.size() < totalDependencies) {
            service.setOperationalStatus("UP");
            service.setBroken(false);
            service.setDegrated(true);
        } else {
            service.setOperationalStatus("DOWN");
            service.setBroken(true);
            service.setDegrated(false);
        }

        Long end = System.currentTimeMillis();
        Long took = end - start;
        logger.debug("Check Service Integrity for [{}] Took: {} ms Broken Count:[{}]",
                service.getId(), took, service.getBrokenResources().size());
        this.updateService(service);
    }

    private void computeServiceResourceUpdated(ServiceResource newService, ServiceResource oldService)
            throws ArangoDaoException, IllegalStateException, IOException {

        if (newService.getBroken() != oldService.getBroken()
                || newService.getDegrated() != oldService.getDegrated()
                || newService.getOperationalStatus().equalsIgnoreCase(oldService.getOperationalStatus())) {

            this.serviceDao.findUpperResources(newService).forEach(s -> {
                ProcessServiceIntegrityEvent event = new ProcessServiceIntegrityEvent(s);
                this.eventManager.notifyEvent(event);
            });

        }

    }

}
