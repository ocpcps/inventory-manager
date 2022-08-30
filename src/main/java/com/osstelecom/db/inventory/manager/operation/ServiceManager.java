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

import java.util.Date;
import java.util.concurrent.locks.ReentrantLock;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.arangodb.entity.DocumentCreateEntity;
import com.arangodb.entity.DocumentUpdateEntity;
import com.osstelecom.db.inventory.manager.dao.ArangoDao;
import com.osstelecom.db.inventory.manager.events.ServiceResourceCreatedEvent;
import com.osstelecom.db.inventory.manager.events.ServiceResourceUpdatedEvent;
import com.osstelecom.db.inventory.manager.exception.ArangoDaoException;
import com.osstelecom.db.inventory.manager.exception.DomainNotFoundException;
import com.osstelecom.db.inventory.manager.exception.ServiceNotFoundException;
import com.osstelecom.db.inventory.manager.listeners.EventManagerListener;
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
    private ArangoDao arangoDao;

    @Autowired
    private EventManagerListener eventManager;

    @Autowired
    private ReentrantLock lockManager;

    /**
     * Retrieves a domain by name
     *
     * @param domainName
     * @return
     * @throws DomainNotFoundException
     * @throws ArangoDaoException
     * @throws ServiceNotFoundException
     */
    public ServiceResource getService(ServiceResource service) throws ServiceNotFoundException, ArangoDaoException {
        return this.arangoDao.findServiceById(service);
    }

    public ServiceResource deleteService(ServiceResource service) {
        try {
            lockManager.lock();
            return this.arangoDao.deleteService(service);
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
            if (service.getUid() == null) {
                service.setUid(this.getUUID());
            }
            service.setAtomId(service.getDomain().addAndGetId());
            ResourceSchemaModel schemaModel = schemaSession.loadSchema(service.getAttributeSchemaName());
            service.setSchemaModel(schemaModel);
            schemaSession.validateResourceSchema(service);
            dynamicRuleSession.evalResource(service, "I", this); // <--- Pode não ser verdade , se a chave for duplicada..

            DocumentCreateEntity<ServiceResource> result = arangoDao.createService(service);
            service.setUid(result.getId());
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

    public ServiceResource updateService(ServiceResource resource) {
        String timerId = startTimer("updateServiceResource");
        try {
            lockManager.lock();
            resource.setLastModifiedDate(new Date());
            DocumentUpdateEntity<ServiceResource> result = arangoDao.updateService(resource);
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

}
