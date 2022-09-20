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
import com.osstelecom.db.inventory.manager.events.CircuitResourceUpdatedEvent;
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
import java.util.HashMap;
import java.util.Map;
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
    private DomainManager domainManager;

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
            Map<String, Object> binds = new HashMap<>();
            binds.put("id", service.getId());
            service = this.serviceDao.findResourceByFilter("doc._id == @id", binds, service.getDomain()).getOne();
            return service;
        } finally {
            if (lockManager.isLocked()) {
                lockManager.unlock();
            }
            endTimer(timerId);
        }
    }

    /**
     * Deleta um serviço
     *
     * @param service
     * @return
     * @throws ArangoDaoException
     */
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

    /**
     * Cria um novo serviço
     *
     * @param service
     * @return
     * @throws ArangoDaoException
     */
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
            //
            // Simula o ID a ser criado
            //
            String toPersistId = service.getDomain().getServices() + "/" + service.getKey();
            if (service.getOperationalStatus() == null || service.getOperationalStatus().isEmpty()) {
                service.setOperationalStatus("UP");
            }

            service.setAtomId(service.getDomain().addAndGetId());

            ResourceSchemaModel schemaModel = schemaSession.loadSchema(service.getAttributeSchemaName());
            service.setSchemaModel(schemaModel);
            schemaSession.validateResourceSchema(service);
            dynamicRuleSession.evalResource(service, "I", this); // <--- Pode não ser verdade , se a chave for  duplicada..

            //
            // Trata a chave do circuito
            //
            if (service.getCircuits() != null) {
                if (!service.getCircuits().isEmpty()) {
                    service.getCircuits().forEach(circuit -> {
                        /**
                         * The ID
                         */
                        if (!circuit.getServices().contains(toPersistId)) {
                            circuit.getServices().add(toPersistId);
                        }
                    });
                }
            }
            //
            // Já computa o stado final da solução
            //
            this.computeServiceIntegrity(service);
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
            e.printStackTrace();
            throw ex;
        } finally {
            if (lockManager.isLocked()) {
                lockManager.unlock();
            }
            endTimer(timerId);
        }

    }

    /**
     *
     * @param newService
     * @param oldService
     * @throws ArangoDaoException
     */
    private void resolveCircuitServiceLinks(ServiceResource newService, ServiceResource oldService) throws ArangoDaoException, ResourceNotFoundException {
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

                    CircuitResource fromDbCircuit = circuitResourceManager.findCircuitResource(circuit);

                    if (!fromDbCircuit.getServices().contains(newService.getId())) {
                        //
                        // Adiciona o ID do serviço no circuito
                        //
                        logger.debug("Adding New Service to the Circuit:[{}] Current Service Size is: [{}]", newService.getId(), fromDbCircuit.getServices().size());
                        fromDbCircuit.getServices().add(newService.getId());
                        circuitResourceManager.updateCircuitResource(fromDbCircuit);
                    }

                }

            } else {
                //
                // Se a lista de circuitos for vazia precisamos ver se antes não era.
                //
                if (oldService != null) {
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

    public ServiceResource updateService(ServiceResource service) throws ArangoDaoException, ResourceNotFoundException {
        String timerId = startTimer("updateServiceResource");
        try {
            lockManager.lock();
            service.setLastModifiedDate(new Date());
            //
            // Está salvando null de nested resources... ver o que fazer..
            // 
            this.computeServiceIntegrity(service);
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
            throws ResourceNotFoundException, ArangoDaoException, DomainNotFoundException {

        List<ServiceResource> resolvedServices = new ArrayList<>();
        if (service.getDependencies() != null && !service.getDependencies().isEmpty()) {
            for (ServiceResource item : service.getDependencies()) {
                if (item.getDomain() == null) {
                    if (item.getDomainName() != null) {
                        //
                        // Trata o Inter domain
                        //
                        item.setDomain(this.domainManager.getDomain(item.getDomainName()));
                    } else {
                        item.setDomain(service.getDomain());
                    }
                }

                /**
                 * Garante que não vamos levar em consideração o status
                 * operacional,pois pode ter atualizado... isso é ruim,acontece
                 * porque na atualização do serviço ele não atualizou as
                 * referencias. Vou tentar resolver isso
                 */
//                item.setOperationalStatus(null);
                ServiceResource resolved = this.getService(item);
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

    /**
     * Registra o Listener no EventBUS
     */
    @EventListener(ApplicationReadyEvent.class)
    private void onStartUp() {
        eventManager.registerListener(this);
    }

    @Subscribe
    public void onProcessServiceIntegrityEvent(ProcessServiceIntegrityEvent processEvent)
            throws ArangoDaoException, ResourceNotFoundException {

    }

    @Subscribe
    public void onProcessServiceResourceUpdatedEvent(ServiceResourceUpdatedEvent processEvent)
            throws ArangoDaoException, IllegalStateException, IOException, ResourceNotFoundException, DomainNotFoundException {
        this.updateServiceReferences(processEvent.getNewResource());
    }

    @Subscribe
    public void onServiceResourceCreatedEvent(ServiceResourceCreatedEvent createdEvent) throws ArangoDaoException, ResourceNotFoundException, IOException, DomainNotFoundException {
        if (createdEvent.getNewResource().getDependencies() != null) {
            if (!createdEvent.getNewResource().getDependencies().isEmpty()) {
                //
                // Vamos olhar as dependencias do Serviço criado
                //
                List<ServiceResource> servicesToUpdate = new ArrayList<>();
                for (ServiceResource dependency : createdEvent.getNewResource().getDependencies()) {
                    //
                    // Valida se a dependencia é do mesmo dominio.
                    //

                    //
                    // Sincroniza com o DB
                    //
                    dependency = this.serviceDao.findResource(dependency);
                    if (dependency.getRelatedServices() != null) {
                        if (!dependency.getRelatedServices().contains(createdEvent.getNewResource().getId())) {
                            dependency.getRelatedServices().add(createdEvent.getNewResource().getId());
                            servicesToUpdate.add(dependency);
                        }
                    } else {
                        dependency.setRelatedServices(new ArrayList<>());
                        dependency.getRelatedServices().add(createdEvent.getNewResource().getId());
                        servicesToUpdate.add(dependency);
                    }

//                    if (dependency.getDomainName().equals(createdEvent.getNewResource().getDomainName())) {
//
//                    } else {
//                        //
//                        // Domain Jump
//                        //
//                    }
                }
                //
                // Atualiza na origem a depedencia
                //
                for (ServiceResource service : servicesToUpdate) {
                    this.updateServiceReferences(service);
                }

            }
        }

    }

    /**
     * Recebe as notificações de udpate dos serviços serviço
     *
     * @param event
     */
    @Subscribe
    public void onCircuitResourceUpdatedEvent(CircuitResourceUpdatedEvent event) throws ResourceNotFoundException, ArangoDaoException, IOException, DomainNotFoundException {
        //
        // Um Circuito Sofre alteração, mas ele foi alterado de estado ? 
        //

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
                    //
                    // Recupera o serviço do Banco
                    //
                    service = this.getServiceById(service);
                    //
                    // Atualiza as referencias do Circuito
                    //
                    service.getCircuits().removeIf(c -> c.getId().equals(event.getNewResource().getId()));
                    service.getCircuits().add(event.getNewResource());

                    //
                    // Avalia o status dos Circuitos
                    //
                    servicesToUpdate.add(service);

                }

                for (ServiceResource service : servicesToUpdate) {
                    //
                    // Computa o status Final do serviço
                    //
                    this.computeServiceIntegrity(service);
                    /**
                     * Manda para um método especifico só para atualizar as
                     * referencias. do Circuito e seu Status
                     */
                    this.updateServiceReferences(service);

                }

            }
        }

    }

    /**
     * Atualiza a referencia de um circuito com serviço.
     *
     * @param service
     * @throws ArangoDaoException
     */
    private void updateServiceReferences(ServiceResource service) throws ArangoDaoException, ResourceNotFoundException, IOException, DomainNotFoundException {

        /**
         * Verifica se este serviço é necessário para algum outro, ou seja, do
         * pai, procura os filhos. Note que este método só encontra serviços do
         * mesmo dominio.
         */
        try {
            //
            // Vamos procurar os filhos de outro jeito agora..
            //
            if (service.getRelatedServices() != null && !service.getRelatedServices().isEmpty()) {
                //
                // Tem Filhos 
                //

                for (String relatedServiceId : service.getRelatedServices()) {
                    String domainName = this.domainManager.getDomainNameFromId(relatedServiceId);

                    ServiceResource dependentService = new ServiceResource(relatedServiceId);
                    dependentService.setDomainName(domainName);
                    dependentService.setDomain(this.domainManager.getDomain(domainName));
                    dependentService = this.getServiceById(dependentService);
                    dependentService.getDependencies().removeIf(d -> d.getId().equals(service.getId()));
                    dependentService.getDependencies().add(service);
                    try {
                        //
                        // Computa primeiro para saber o estado
                        //
                        this.computeServiceIntegrity(dependentService);
                        DocumentUpdateEntity<ServiceResource> updateResult = this.serviceDao.updateResource(dependentService);
                        //
                        // Salva no Banco de dados
                        //
                        this.updateServiceReferences(dependentService);
                    } catch (ArangoDaoException | ResourceNotFoundException | DomainNotFoundException | IOException ex) {
                        logger.error("Failed to Update Service Dependecies", ex);
                    }
                }

            }

            /**
             * Desligado, pois somente consulta dados do mesmo domininio.
             */
//            this.serviceDao.findUpperResources(service).forEach(dependentService -> {
//                //
//                // Aqui vamos ter a lista dos serviços que dependem do serviço que acabou de ser atualizado.
//                //
//                if (dependentService.getDependencies() != null) {
//                    dependentService.getDependencies().removeIf(d -> d.getId().equals(service.getId()));
//                    dependentService.getDependencies().add(service);
//                    try {
//                        this.computeServiceIntegrity(dependentService);
//                        this.updateServiceReferences(dependentService);
//                    } catch (ArangoDaoException | ResourceNotFoundException | DomainNotFoundException | IOException ex) {
//                        logger.error("Failed to Update Service Dependecies", ex);
//                    }
//                }
//
//            });
        } catch (ResourceNotFoundException ex) {
            //
            // Isto é esperado visto que podemos não ter dependencias.
            // 
        }
    }

    /**
     * Computa o status final do serviço
     *
     * @param service
     */
    private void computeServiceIntegrity(ServiceResource service) {
        //
        // Verifica o estado final do serviço
        //
        if (service.getCircuits() != null && !service.getCircuits().isEmpty()) {
            if (service.getCircuits().size() == 1) {
                //
                // Se tem apenas um circuito, o status do serviço reflete o status do circuito
                //
                service.setDegrated(service.getCircuits().get(0).getDegrated());
                service.setBroken(service.getCircuits().get(0).getBroken());
            } else {

                List<CircuitResource> workingCircuits = service.getCircuits()
                        .stream()
                        .filter(c -> !c.getBroken()).collect(Collectors.toList());

                List<CircuitResource> brokenCircuits = service.getCircuits()
                        .stream()
                        .filter(c -> c.getBroken()).collect(Collectors.toList());

                if (workingCircuits != null) {
                    if (workingCircuits.isEmpty()) {
                        if (!service.getBroken()) {
                            service.setBroken(true);
                        }
                    } else {
                        /**
                         * Tem algum circuito funcionando, se estava quebrado,
                         * normaliza, também avalia se tem algum circuito fora,
                         * e se tiver marca como degradado
                         */
                        if (service.getBroken()) {
                            service.setBroken(false);
                            if (!brokenCircuits.isEmpty()) {
                                service.setDegrated(true);
                            } else {
                                service.setDegrated(false);

                            }
                        }

                    }
                } else {
                    /**
                     * Nenhum circuito funcionando, marca o serviço como
                     * quebrado
                     */
                    if (!service.getBroken()) {
                        service.setBroken(true);
                        service.setDegrated(true);
                    }

                }
            }

            if (service.getBroken()) {
                service.setDegrated(true);
                service.setOperationalStatus("DOWN");
            } else {
                service.setOperationalStatus("UP");
            }
        } else if (service.getDependencies() != null && !service.getDependencies().isEmpty()) {
            //
            // Trabalha com a depedencia de serviço
            //
            if (service.getDependencies().size() == 1) {
                //
                // se só tiver um serviço reflete o status
                //

                service.setDegrated(service.getDependencies().get(0).getDegrated());
                service.setBroken(service.getDependencies().get(0).getBroken());
            } else {
                //
                // Temos multiplos serviços.
                //

                List<ServiceResource> workingServices = service.getDependencies()
                        .stream()
                        .filter(c -> !c.getBroken()).collect(Collectors.toList());

                List<ServiceResource> brokenServices = service.getDependencies()
                        .stream()
                        .filter(c -> c.getBroken()).collect(Collectors.toList());

                if (workingServices != null) {
                    if (workingServices.isEmpty()) {
                        if (!service.getBroken()) {
                            service.setBroken(true);
                        }
                    } else {

                        if (service.getBroken()) {
                            service.setBroken(false);
                            if (!brokenServices.isEmpty()) {
                                service.setDegrated(true);
                            } else {
                                service.setDegrated(false);

                            }
                        }

                    }
                } else {
                    /**
                     * Nenhum seriço funcionando, marca o serviço como quebrado
                     */
                    if (!service.getBroken()) {
                        service.setBroken(true);
                        service.setDegrated(true);
                    }

                }

            }
        }

    }

}
