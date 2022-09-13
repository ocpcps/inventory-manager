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
import com.osstelecom.db.inventory.manager.events.CircuitStateTransionedEvent;
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
import java.util.stream.Collectors;

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
    private LockManager lockManager;

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
            if (service.getOperationalStatus() == null || service.getOperationalStatus().isEmpty()) {
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

            this.resolveCircuitServiceLinks(service, null);
            //
            // Aqui criou o managed resource
            //
            ServiceResourceCreatedEvent event = new ServiceResourceCreatedEvent(service);
            this.eventManager.notifyResourceEvent(event);
            return service;
        } catch (Exception e) {
            ArangoDaoException ex = new ArangoDaoException("Error while creating ServiceResource", e);
//            e.printStackTrace();s
            throw ex;
        } finally {
            if (lockManager.isLocked()) {
                lockManager.unlock();
            }
            endTimer(timerId);
        }

    }

    private void resolveCircuitServiceLinks(ServiceResource newService, ServiceResource oldService) throws ArangoDaoException {
        //
        // Aqui temos certeza que o serviço foi criado, então vamos pegar e atualizar as dependencias do circuito
        //
        if (newService.getCircuits() != null) {
            if (!newService.getCircuits().isEmpty()) {
                //
                // Como temos serviços associados vamos garantir que o circuito seja notificado
                //

                for (CircuitResource circuit : newService.getCircuits()) {
                    //
                    // 
                    //
                    if (!circuit.getServices().contains(newService.getId())) {
                        //
                        // Adiciona o ID do serviço no circuito
                        //
                        circuit.getServices().add(newService.getId());
                        circuitResourceManager.updateCircuitResource(circuit);
                    }

                }

            } else {
                //
                // Se a lista de circuitos for vazia precisamos ver se antes não era.
                //
                if (!oldService.getCircuits().isEmpty()) {
                    //
                    // Não era vazia, ou seja ficou vazia vamos precisar remover a referencia.
                    //
                    for (CircuitResource circuit : oldService.getCircuits()) {
                        if (circuit.getServices().contains(oldService.getId())) {
                            circuit.getServices().remove(circuit.getId());
                            circuitResourceManager.updateCircuitResource(circuit);
                        }
                    }
                }

            }
        }
        //
        // Agora vamos comparar com o OLD, para saber se temos circuitos para remover..
        //
        if (oldService != null) {
            if (!oldService.getCircuits().isEmpty()) {
                for (CircuitResource circuit : oldService.getCircuits()) {
                    if (!newService.getCircuits().contains(circuit)) {
                        //
                        // Marca o Circuito para remoção
                        //
                        circuit.getServices().remove(circuit.getId());
                        circuitResourceManager.updateCircuitResource(circuit);
                    }
                }
            }
        }

    }

    public ServiceResource updateService(ServiceResource service) throws ArangoDaoException {
        String timerId = startTimer("updateServiceResource");
        try {
            lockManager.lock();
            service.setLastModifiedDate(new Date());
            //
            // Está salvando null de nested resources... ver o que fazer..
            // 
            DocumentUpdateEntity<ServiceResource> result = serviceDao.updateResource(service);
            ServiceResource newService = result.getNew();
            ServiceResource oldService = result.getOld();

            this.resolveCircuitServiceLinks(newService, oldService);
            ServiceResourceUpdatedEvent event = new ServiceResourceUpdatedEvent(oldService, newService);
            this.eventManager.notifyResourceEvent(event);
            return newService;
        } finally {
            if (lockManager.isLocked()) {
                lockManager.unlock();
            }
            endTimer(timerId);
        }
    }

    /**
     * Resolves Services and Circuits
     *
     * @param service
     * @return
     * @throws ResourceNotFoundException
     * @throws ArangoDaoException
     */
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
                if (circuit.getRevisionId() == null) {
                    CircuitResource resolved = this.circuitResourceManager.findCircuitResource(circuit);
                    resolvedCircuits.add(resolved);
                } else {
                    //
                    // Se tem revision já foi resolvido.
                    //
                    resolvedCircuits.add(circuit);
                }
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
        //
        // Na atualização do Serviço, precisamo 
        //
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

    /**
     * Recebe as transições de estado dos circuitos
     *
     * @param event
     */
    @Subscribe
    public void onCircuitStateTransionedEvent(CircuitStateTransionedEvent event) throws ResourceNotFoundException, ArangoDaoException {
        if (event.getNewResource().getServices() != null) {
            if (!event.getNewResource().getServices().isEmpty()) {
                logger.debug("Circuit:[{}] State Changed, Impacted Services Count:[{}]",
                        event.getNewResource().getId(),
                        event.getNewResource().getServices().size());
                //
                // Trata o Status Aqui
                //
                List<ServiceResource> servicesToUpdate = new ArrayList<>();
                for (String serviceId : event.getNewResource().getServices()) {
                    //
                    // o Circuito só pode impactar serviços do mesmo dominio.
                    //
                    ServiceResource service = new ServiceResource(serviceId);
                    service.setDomain(event.getNewResource().getDomain());
                    service = this.getServiceById(service);
                    if (service.getDegrated() != event.getNewResource().getDegrated()) {
                        service.setDegrated(event.getNewResource().getDegrated());
                        //
                        // Atualiza as referencias do Circuito
                        //
                        service.getCircuits().remove(event.getNewResource());
                        service.getCircuits().add(event.getNewResource());
                        servicesToUpdate.add(service);

                    }

                }

                for (ServiceResource service : servicesToUpdate) {
                    //
                    // Verifica o estado final do serviço
                    //

                    List<CircuitResource> workingCircuits = service.getCircuits()
                            .stream()
                            .filter(c -> !c.getBroken()).collect(Collectors.toList());

                    if (workingCircuits != null) {
                        if (workingCircuits.isEmpty()) {
                            if (!service.getBroken()) {
                                service.setBroken(true);
                            }
                        } else {
                            if (service.getBroken()) {
                                service.setBroken(false);
                            }
                        }
                    } else {
                        if (!service.getBroken()) {
                            service.setBroken(true);
                        }

                    }

                    if (service.getBroken()) {
                        service.setDegrated(true);
                        service.setOperationalStatus("DOWN");
                    } else {
                        service.setOperationalStatus("UP");
                    }

                    this.updateService(service);
                }

            }
        }
    }

    /**
     * Identifica se houve uma transição do estado do serviço, se houve vai
     * procurar as depedencias e atualizar o estado do servico
     *
     * @param newService
     * @param oldService
     * @throws ArangoDaoException
     * @throws IllegalStateException
     * @throws IOException
     */
    private void computeServiceResourceUpdated(ServiceResource newService, ServiceResource oldService)
            throws ArangoDaoException, IllegalStateException, IOException {

        if (newService.getBroken() != oldService.getBroken()
                || newService.getDegrated() != oldService.getDegrated()
                || newService.getOperationalStatus().equalsIgnoreCase(oldService.getOperationalStatus())) {
            try {
                this.serviceDao.findUpperResources(newService).forEach(s -> {
                    //
                    // Propaga para cima o evento impactando os Serviços que 
                    // dependem deste serviço
                    //
                    ProcessServiceIntegrityEvent event = new ProcessServiceIntegrityEvent(s);
                    this.eventManager.notifyResourceEvent(event);
                });

            } catch (ResourceNotFoundException ex) {
                //
                // Esta ex, é lançada quando não tem Upper Resource....
                // Então é seguro ignorar ela ou simplesmente omitir...
                // 
                //
            }

        }

    }

}
