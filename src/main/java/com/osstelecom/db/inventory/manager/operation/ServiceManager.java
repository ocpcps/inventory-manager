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
import com.osstelecom.db.inventory.manager.dto.FilterDTO;
import com.osstelecom.db.inventory.manager.events.CircuitResourceUpdatedEvent;
import com.osstelecom.db.inventory.manager.events.ManagedResourceCreatedEvent;
import com.osstelecom.db.inventory.manager.events.ManagedResourceUpdatedEvent;
import com.osstelecom.db.inventory.manager.events.ProcessServiceIntegrityEvent;
import com.osstelecom.db.inventory.manager.events.ResourceConnectionCreatedEvent;
import com.osstelecom.db.inventory.manager.events.ResourceConnectionUpdatedEvent;
import com.osstelecom.db.inventory.manager.events.ServiceResourceCreatedEvent;
import com.osstelecom.db.inventory.manager.events.ServiceResourceUpdatedEvent;
import com.osstelecom.db.inventory.manager.events.ServiceStateTransionedEvent;
import com.osstelecom.db.inventory.manager.exception.ArangoDaoException;
import com.osstelecom.db.inventory.manager.exception.DomainNotFoundException;
import com.osstelecom.db.inventory.manager.exception.GenericException;
import com.osstelecom.db.inventory.manager.exception.InvalidRequestException;
import com.osstelecom.db.inventory.manager.exception.ResourceNotFoundException;
import com.osstelecom.db.inventory.manager.exception.SchemaNotFoundException;
import com.osstelecom.db.inventory.manager.exception.ScriptRuleException;
import com.osstelecom.db.inventory.manager.listeners.EventManagerListener;
import com.osstelecom.db.inventory.manager.resources.CircuitResource;
import com.osstelecom.db.inventory.manager.resources.Domain;
import com.osstelecom.db.inventory.manager.resources.GraphList;
import com.osstelecom.db.inventory.manager.resources.ManagedResource;
import com.osstelecom.db.inventory.manager.resources.ResourceConnection;
import com.osstelecom.db.inventory.manager.resources.ServiceResource;
import com.osstelecom.db.inventory.manager.resources.exception.AttributeConstraintViolationException;
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
    public ServiceResource getService(ServiceResource service) throws ResourceNotFoundException, ArangoDaoException, DomainNotFoundException {
        if (service.getDomain() == null && service.getId() != null) {
            String domainName = this.domainManager.getDomainNameFromId(service.getId());
            Domain domain = this.domainManager.getDomain(domainName);
            service.setDomain(domain);
            
        }
        return this.serviceDao.findResource(service);
    }
    
    public ServiceResource getServiceById(ServiceResource service)
            throws ResourceNotFoundException, ArangoDaoException, DomainNotFoundException {
        String timerId = startTimer("getServiceById");
        try {
            lockManager.lock();
            //
            // Deveria estar na session ? Ainda tenho dúvidas..
            //
            if (!service.getId().contains("/")) {
                if (service.getDomain() != null) {
                    service.setId(service.getDomain().getServices() + "/" + service.getId());
                } else if (service.getDomainName() != null) {
                    service.setDomain(this.domainManager.getDomain(service.getDomainName()));
                    service.setId(service.getDomain().getServices() + "/" + service.getId());
                }
                
            } else {
                
                String domainName = this.domainManager.getDomainNameFromId(service.getId());
                service.setDomainName(domainName);
                service.setDomain(this.domainManager.getDomain(domainName));
            }
            Map<String, Object> binds = new HashMap<>();
            binds.put("id", service.getId());
            service = this.serviceDao.findResourceByFilter(new FilterDTO("doc._id == @id", binds), service.getDomain()).getOne();
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
            
            service.setRelatedServices(null);
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
                service.setOperationalStatus("Up");
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
    private void resolveCircuitServiceLinks(ServiceResource newService, ServiceResource oldService) throws ArangoDaoException, ResourceNotFoundException, InvalidRequestException, SchemaNotFoundException, GenericException, AttributeConstraintViolationException, ScriptRuleException {
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
    
    public GraphList<ServiceResource> findServiceByFilter(FilterDTO filter, Domain domain) throws ArangoDaoException, ResourceNotFoundException {
        return this.serviceDao.findResourceByFilter(filter, domain);
    }
    
    public ServiceResource updateService(ServiceResource service) throws ArangoDaoException, ResourceNotFoundException, InvalidRequestException, SchemaNotFoundException, GenericException, AttributeConstraintViolationException, ScriptRuleException {
        String timerId = startTimer("updateServiceResource");
        try {
            this.lockManager.lock();
            service.setLastModifiedDate(new Date());
            
            ResourceSchemaModel schemaModel = schemaSession.loadSchema(service.getAttributeSchemaName());
            service.setSchemaModel(schemaModel);
            schemaSession.validateResourceSchema(service);
            dynamicRuleSession.evalResource(service, "u", this); // <--- Pode não ser verdade , se a chave for  duplicada..

            //
            // Está salvando null de nested resources... ver o que fazer..
            // 
            this.computeServiceIntegrity(service);
            DocumentUpdateEntity<ServiceResource> result = this.serviceDao.updateResource(service);
            ServiceResource newService = result.getNew();
            ServiceResource oldService = result.getOld();
            this.resolveCircuitServiceLinks(newService, oldService);
            
            this.evaluateServiceStateTransition(result);
            
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
    public ServiceResource resolveCircuitsAndServices(ServiceResource service)
            throws ResourceNotFoundException, ArangoDaoException, DomainNotFoundException, InvalidRequestException {
        
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
                    try {
                        CircuitResource resolved = this.circuitResourceManager.findCircuitResource(circuit);
                        resolvedCircuits.add(resolved);
                    } catch (ResourceNotFoundException ex) {
                        logger.warn("Dirty Service Found Pointing to not existing circuit:[{}]", circuit.getKey());
                    }
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

    /**
     * Quando uma conexão é criada
     *
     * @param createdEvent
     */
    private void updateServiceResourceConnectionReferenceCreateEvent(ResourceConnectionCreatedEvent createdEvent) throws ResourceNotFoundException, ArangoDaoException, DomainNotFoundException {
        ResourceConnection connection = createdEvent.getNewResource();
        if (connection.getDependentService() != null) {
            //
            // Temos referencia a um serviço
            //
            ServiceResource service = connection.getDependentService();
            //
            // Recupera a referencia do DB
            //
            service = this.getService(service);
            if (service.getRelatedResourceConnections() == null) {
                service.setRelatedResourceConnections(new ArrayList<>());
            }
            
            if (!service.getRelatedResourceConnections().contains(connection.getId())) {
                service.getRelatedResourceConnections().add(connection.getId());
                this.evaluateServiceStateTransition(this.serviceDao.updateResource(service));
            }
        }
    }

    /**
     * Quando um recurso é criado. Este método cuida de atualizar as relações
     * entre o Serviço e os supostos recursos dos demais dominios. Atenção é
     * quando o recurso é criado!
     *
     * @param createdEvent
     * @throws ResourceNotFoundException
     * @throws ArangoDaoException
     * @throws DomainNotFoundException
     */
    private void updateServiceManagedResourceReferenceCreateEvent(ManagedResourceCreatedEvent createdEvent) throws ResourceNotFoundException, ArangoDaoException, DomainNotFoundException {
        ManagedResource resource = createdEvent.getNewResource();
        if (resource.getDependentService() != null) {
            ServiceResource service = resource.getDependentService();
            //
            // Recupera a referencia do DB
            //
            service = this.getService(service);
            if (service.getRelatedManagedResources() == null) {
                service.setRelatedManagedResources(new ArrayList<>());
            }
            
            if (!service.getRelatedManagedResources().contains(resource.getId())) {
                service.getRelatedManagedResources().add(resource.getId());
                this.evaluateServiceStateTransition(this.serviceDao.updateResource(service));
            }
        }
    }

    /**
     * Quando um recurso é atualizado. Este método cuida de atualizar as
     * relações entre o Serviço e os supostos recursos dos demais dominios.
     * Atenção é quando o recurso é atualizado! Durante a atualização é mais
     * complicado, pois o recurso pode ter sido movido de um serviço para o
     * outro, e precisamos manter as referencias atualizadas
     *
     * @param createdEvent
     * @throws ResourceNotFoundException
     * @throws ArangoDaoException
     * @throws DomainNotFoundException
     */
    private void updateServiceManagedResourceReferenceUpdateEvent(ManagedResourceUpdatedEvent updateEvent) throws ResourceNotFoundException, ArangoDaoException, DomainNotFoundException, InvalidRequestException, SchemaNotFoundException, GenericException, AttributeConstraintViolationException, ScriptRuleException {
        if (updateEvent.getOldResource() == null && updateEvent.getNewResource() != null) {
            ManagedResource resource = updateEvent.getNewResource();
            if (resource.getDependentService() != null) {
                ServiceResource service = resource.getDependentService();
                //
                // Recupera a referencia do DB
                //
                service = this.getService(service);
                if (service.getRelatedManagedResources() == null) {
                    service.setRelatedManagedResources(new ArrayList<>());
                }
                
                if (!service.getRelatedManagedResources().contains(resource.getId())) {
                    service.getRelatedManagedResources().add(resource.getId());
                    this.evaluateServiceStateTransition(this.serviceDao.updateResource(service));
                }
            } else {
                logger.debug("No Resource to Update");
            }
        } else if (updateEvent.getOldResource() != null && updateEvent.getNewResource() != null) {
            
            if (updateEvent.getOldResource().getDependentService() == null && updateEvent.getNewResource().getDependentService() != null) {
                logger.debug("Linking Service from Dependency to:[{}]", updateEvent.getNewResource().getDependentService().getId());

                //
                // Neste cenário não tinha depedencia e agora tem então é só salvar a referencia.
                //
                ServiceResource service = updateEvent.getNewResource().getDependentService();
                ManagedResource resource = updateEvent.getNewResource();
                
                service = this.getService(service);
                if (service.getRelatedManagedResources() == null) {
                    service.setRelatedManagedResources(new ArrayList<>());
                }
                
                if (!service.getRelatedManagedResources().contains(resource.getId())) {
                    service.getRelatedManagedResources().add(resource.getId());
                    this.evaluateServiceStateTransition(this.serviceDao.updateResource(service));
                }
            } else if (updateEvent.getOldResource().getDependentService() != null && updateEvent.getNewResource().getDependentService() != null) {
                ManagedResource resource = updateEvent.getNewResource();
                logger.debug("Changing Service from Dependency from:[{}] to:[{}]", updateEvent.getOldResource().getDependentService().getId(), updateEvent.getNewResource().getDependentService().getId());
                //
                // Tinha serviço nos 2
                //
                if (!updateEvent.getOldResource().getDependentService().getId().equals(updateEvent.getNewResource().getDependentService().getId())) {
                    //
                    // Trocou o serviço, agora a gente precisa remover a referencia do antigo e atualizar no novo
                    //
                    ServiceResource newService = updateEvent.getNewResource().getDependentService();
                    ServiceResource oldService = updateEvent.getOldResource().getDependentService();
                    //
                    // Obtem as referencias
                    //
                    oldService = this.getService(oldService);
                    newService = this.getService(newService);

                    //
                    // No Old Service vamos remover a referencia.
                    //
                    if (oldService.getRelatedManagedResources() != null && !oldService.getRelatedManagedResources().isEmpty()) {
                        if (oldService.getRelatedManagedResources().remove(resource.getId())) {
                            this.updateService(oldService);
                        } else {
                            logger.debug("Resource: [{}] Not Found on Old Service:[{}]", resource.getId(), oldService.getId());
                        }
                    }
                    if (newService.getRelatedManagedResources() == null) {
                        newService.setRelatedManagedResources(new ArrayList<>());
                    }
                    
                    if (!newService.getRelatedManagedResources().contains(resource.getId())) {
                        newService.getRelatedManagedResources().add(resource.getId());
                        this.updateService(newService);
                    }
                    
                } else {
                    logger.debug("Services Are Equal Not Updating...");
                }
                
            }
        }
    }

    /**
     * Quando uma conexão é atualizada. Este método cuida de atualizar as
     * relações entre o Serviço e os supostos recursos dos demais dominios.
     * Atenção é quando o recurso é atualizado! Durante a atualização é mais
     * complicado, pois o recurso pode ter sido movido de um serviço para o
     * outro, e precisamos manter as referencias atualizadas
     *
     * @param createdEvent
     * @throws ResourceNotFoundException
     * @throws ArangoDaoException
     * @throws DomainNotFoundException
     */
    private void updateServiceResourceConnectionReferenceUpdateEvent(ResourceConnectionUpdatedEvent updateEvent) throws ResourceNotFoundException, ArangoDaoException, DomainNotFoundException, InvalidRequestException, SchemaNotFoundException, GenericException, AttributeConstraintViolationException, ScriptRuleException {
        if (updateEvent.getOldResource() == null && updateEvent.getNewResource() != null) {
            ResourceConnection resource = updateEvent.getNewResource();
            if (resource.getDependentService() != null) {
                ServiceResource service = resource.getDependentService();
                //
                // Recupera a referencia do DB
                //
                service = this.getService(service);
                if (service.getRelatedResourceConnections() == null) {
                    service.setRelatedResourceConnections(new ArrayList<>());
                }
                
                if (!service.getRelatedResourceConnections().contains(resource.getId())) {
                    service.getRelatedResourceConnections().add(resource.getId());
                    this.evaluateServiceStateTransition(this.serviceDao.updateResource(service));
                }
            }
        } else if (updateEvent.getOldResource() != null && updateEvent.getNewResource() != null) {
            
            if (updateEvent.getOldResource().getDependentService() == null && updateEvent.getNewResource().getDependentService() != null) {
                logger.debug("Linking Service from Dependency to:[{}]", updateEvent.getNewResource().getDependentService().getId());

                //
                // Neste cenário não tinha depedencia e agora tem então é só salvar a referencia.
                //
                ServiceResource service = updateEvent.getNewResource().getDependentService();
                ResourceConnection resource = updateEvent.getNewResource();
                
                service = this.getService(service);
                if (service.getRelatedResourceConnections() == null) {
                    service.setRelatedResourceConnections(new ArrayList<>());
                }
                
                if (!service.getRelatedResourceConnections().contains(resource.getId())) {
                    service.getRelatedResourceConnections().add(resource.getId());
                    this.evaluateServiceStateTransition(this.serviceDao.updateResource(service));
                }
            } else if (updateEvent.getOldResource().getDependentService() != null && updateEvent.getNewResource().getDependentService() != null) {
                ResourceConnection resource = updateEvent.getNewResource();
                logger.debug("Changing Service from Dependency from:[{}] to:[{}]", updateEvent.getOldResource().getDependentService().getId(), updateEvent.getNewResource().getDependentService().getId());
                //
                // Tinha serviço nos 2
                //
                if (!updateEvent.getOldResource().getDependentService().getId().equals(updateEvent.getNewResource().getDependentService().getId())) {
                    //
                    // Trocou o serviço, agora a gente precisa remover a referencia do antigo e atualizar no novo
                    //
                    ServiceResource newService = updateEvent.getNewResource().getDependentService();
                    ServiceResource oldService = updateEvent.getOldResource().getDependentService();
                    //
                    // Obtem as referencias
                    //
                    oldService = this.getService(oldService);
                    newService = this.getService(newService);

                    //
                    // No Old Service vamos remover a referencia.
                    //
                    if (oldService.getRelatedResourceConnections() != null && !oldService.getRelatedResourceConnections().isEmpty()) {
                        if (oldService.getRelatedResourceConnections().remove(resource.getId())) {
                            this.updateService(oldService);
                        } else {
                            logger.debug("Resource: [{}] Not Found on Old Service:[{}]", resource.getId(), oldService.getId());
                        }
                    }
                    if (newService.getRelatedResourceConnections() == null) {
                        newService.setRelatedResourceConnections(new ArrayList<>());
                    }
                    
                    if (!newService.getRelatedResourceConnections().contains(resource.getId())) {
                        newService.getRelatedResourceConnections().add(resource.getId());
                        this.updateService(newService);
                    }
                    
                } else {
                    logger.debug("Services Are Equal Not Updating...");
                }
                
            }
        }
    }

    /**
     * Avalia se houve transição de estado para notificação do evento de
     * transição. Este cara que deve avisar o ResrouceManager sobre as
     * alterações
     *
     * @param update
     */
    private void evaluateServiceStateTransition(DocumentUpdateEntity<ServiceResource> update) {
        //
        // Verifica se houve transição de estado.
        //

        if (update.getNew() != null && update.getOld() != null) {
            if (!update.getNew().getOperationalStatus().equals(update.getOld().getOperationalStatus())) {
                //
                // Houve Transição de estado
                //
                ServiceStateTransionedEvent serviceTrasitionEvent = new ServiceStateTransionedEvent(update);
                this.eventManager.notifyResourceEvent(serviceTrasitionEvent);
                logger.debug("Service: [{}] Transitioned from:[{}] to:[{}]", update.getNew().getId(), update.getOld().getOperationalStatus(), update.getNew().getOperationalStatus());
            }
        }
    }

    /**
     * Atualiza a referencia de um circuito com serviço.
     *
     * @param service
     * @throws ArangoDaoException
     */
    private void updateServiceCircuitReference(ServiceResource service) throws ArangoDaoException, ResourceNotFoundException, IOException, DomainNotFoundException {

        //
        // Actually Update on DB
        //  
        //this.evaluateServiceStateTransition(this.serviceDao.updateResource(service));

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
//                        DocumentUpdateEntity<ServiceResource> updateResult = this.serviceDao.updateResource(dependentService);
                        this.evaluateServiceStateTransition(this.serviceDao.updateResource(dependentService));
                        //
                        // Salva no Banco de dados
                        //
                        this.updateServiceCircuitReference(dependentService);
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
                            service.setOperationalStatus("Down");
                        }
                    } else {

                        /**
                         * Se não tem nenum circuito quebrado avalia se ainda
                         * continua quebrado...
                         */
                        if (brokenCircuits.isEmpty()) {
                            /**
                             * Tem algum circuito funcionando, se estava
                             * quebrado, normaliza, também avalia se tem algum
                             * circuito fora, e se tiver marca como degradado
                             */
                            if (service.getBroken()) {
                                service.setBroken(false);
                                service.setOperationalStatus("Up");
                                if (!brokenCircuits.isEmpty()) {
                                    service.setDegrated(true);
                                } else {
                                    service.setDegrated(false);
                                    
                                }
                            }
                        } else {
                            if (!workingCircuits.isEmpty()) {
                                /**
                                 * Tem um circuito quebrado, então vai ser
                                 * degradada, porque tem circuito funcionando e
                                 * tem circuito quebrada.
                                 */
                                service.setDegrated(true);
                                service.setBroken(false);
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
                        service.setOperationalStatus("Down");
                    }
                    
                }

                /**
                 * Por fim avalia um cenário de normalização
                 */
                if (brokenCircuits.isEmpty() && !workingCircuits.isEmpty()) {
                    service.setBroken(false);
                    service.setDegrated(false);
                }
            }
            
            if (service.getBroken()) {
                service.setDegrated(true);
                service.setOperationalStatus("Down");
            } else {
                service.setOperationalStatus("Up");
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
                service.setOperationalStatus(service.getDependencies().get(0).getOperationalStatus());
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
                            service.setOperationalStatus("Down");
                        }
                    } else {
                        
                        if (service.getBroken()) {
                            service.setBroken(false);
                            service.setOperationalStatus("Up");
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
                        service.setOperationalStatus("Down");
                    }
                    
                }
                
            }
        }
        
    }
    
    @Subscribe
    public void onProcessServiceIntegrityEvent(ProcessServiceIntegrityEvent processEvent)
            throws ArangoDaoException, ResourceNotFoundException {
        
    }
    
    @Subscribe
    public void onServiceResourceUpdatedEvent(ServiceResourceUpdatedEvent processEvent)
            throws ArangoDaoException, IllegalStateException, IOException, ResourceNotFoundException, DomainNotFoundException {
        this.updateServiceCircuitReference(processEvent.getNewResource());
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
                    
                }
                //
                // Atualiza na origem a depedencia
                //
                for (ServiceResource service : servicesToUpdate) {
                    this.updateServiceCircuitReference(service);
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
                    logger.debug("Found Service ID:[{}] to Update Related ManagedResources", service.getId());
                    
                    if (service.getRelatedManagedResources() != null) {
                        logger.debug("\t Service ID:[{}] Has: [{}] Managed Resource Links", service.getId(), service.getRelatedManagedResources().size());
                    }
                    
                    if (service.getRelatedResourceConnections() != null) {
                        logger.debug("\t Service ID:[{}] Has: [{}] Resource Connections Links", service.getId(), service.getRelatedResourceConnections().size());
                    }

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
                     * referencias. do Circuito e seu Status. Como foi chamado
                     * após o calculo ele também atualiza
                     */
                    this.updateServiceCircuitReference(service);
                    
                }
                
            }
        }
        
    }

    /**
     * Recebe as atualizações de Managed Resource
     *
     * @param updateEvent
     */
    @Subscribe
    public void onManagedResourceUpdatedEvent(ManagedResourceUpdatedEvent updateEvent) throws ResourceNotFoundException, ArangoDaoException, DomainNotFoundException, InvalidRequestException, SchemaNotFoundException, GenericException, AttributeConstraintViolationException, ScriptRuleException {
        logger.debug("Managed Resource [{}] Updated: ", updateEvent.getOldResource().getId());
        //
        // Um Recurso foi Atualizado,  tem algum serviço que ele depende aqui ? 
        //
        this.updateServiceManagedResourceReferenceUpdateEvent(updateEvent);
    }

    /**
     * Called when a Managed Resource is created
     *
     * @param resource
     */
    @Subscribe
    public void onManagedResourceCreatedEvent(ManagedResourceCreatedEvent resource) throws ArangoDaoException, DomainNotFoundException, ResourceNotFoundException {
        if (resource.getNewResource().getDependentService() != null) {
            //
            // Temos um Service, vamos notificar ele que um recurso uso ele.
            //
            logger.debug("New Link Between Managed Resource:[{}] and Service found:[{}]", resource.getNewResource().getId(), resource.getNewResource().getDependentService().getId());
            this.updateServiceManagedResourceReferenceCreateEvent(resource);
            
        }
    }

    /**
     * Recebe as notificações de conexões criadas
     *
     * @param resource
     */
    @Subscribe
    public void onResourceConnectionCreatedEvent(ResourceConnectionCreatedEvent resource) throws ResourceNotFoundException, ArangoDaoException, DomainNotFoundException {
        if (resource.getNewResource().getDependentService() != null) {
            //
            // Ok Temos um serviço para atualizar.
            //
            this.updateServiceResourceConnectionReferenceCreateEvent(resource);
        }
    }

    /**
     * Recebe as notificações de conexões atualizadas Não está fazendo nada...
     *
     * @param resource
     */
    @Subscribe
    public void onResourceConnectionUpdatedEvent(ResourceConnectionUpdatedEvent updateEvent) throws ResourceNotFoundException, ArangoDaoException, DomainNotFoundException, InvalidRequestException, SchemaNotFoundException, GenericException, AttributeConstraintViolationException, ScriptRuleException {
        
        this.updateServiceResourceConnectionReferenceUpdateEvent(updateEvent);

        //
        // Avaliar se preciso disso agora...
        //
        if (updateEvent.getOldResource().getDependentService() == null && updateEvent.getNewResource().getDependentService() != null) {
            //
            // Neste cenário não tinha depedencia e agora tem então é só salvar a referencia.
            //

        } else if (updateEvent.getOldResource().getDependentService() != null && updateEvent.getNewResource().getDependentService() != null) {
            //
            // Tinha serviço nos 2
            //

            if (!updateEvent.getOldResource().getDependentService().equals(updateEvent.getNewResource().getDependentService())) {
                //
                // Trocou o serviço, agora a gente precisa remover a referencia do antigo e atualizar no novo
                //
            }
            
        }
    }

    /**
     * Find a Service Instance
     *
     * @param service
     * @return
     * @throws ResourceNotFoundException
     * @throws ArangoDaoException
     */
    public ServiceResource findServiceResource(ServiceResource service)
            throws ResourceNotFoundException, ArangoDaoException, InvalidRequestException {
        String timerId = startTimer("findServiceResource");
        try {
            lockManager.lock();
            if (service.getId() != null) {
                if (!service.getId().contains("/")) {
                    service.setId(service.getDomain().getServices() + "/" + service.getId());
                }
            }
            
            return this.serviceDao.findResource(service);
        } finally {
            if (lockManager.isLocked()) {
                lockManager.unlock();
            }
            endTimer(timerId);
        }
    }
}
