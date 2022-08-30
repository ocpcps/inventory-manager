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
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import com.arangodb.entity.DocumentCreateEntity;
import com.arangodb.entity.DocumentUpdateEntity;
import com.osstelecom.db.inventory.graph.arango.GraphList;
import com.osstelecom.db.inventory.manager.configuration.ConfigurationManager;
import com.osstelecom.db.inventory.manager.dao.ArangoDao;
import com.osstelecom.db.inventory.manager.dao.ManagedResourceDao;
import com.osstelecom.db.inventory.manager.dto.DomainDTO;
import com.osstelecom.db.inventory.manager.dto.FilterDTO;
import com.osstelecom.db.inventory.manager.dto.TimerDto;
import com.osstelecom.db.inventory.manager.events.CircuitResourceCreatedEvent;
import com.osstelecom.db.inventory.manager.events.CircuitResourceUpdatedEvent;
import com.osstelecom.db.inventory.manager.events.ConsumableMetricCreatedEvent;
import com.osstelecom.db.inventory.manager.events.DomainCreatedEvent;
import com.osstelecom.db.inventory.manager.events.ManagedResourceCreatedEvent;
import com.osstelecom.db.inventory.manager.events.ManagedResourceUpdatedEvent;
import com.osstelecom.db.inventory.manager.events.ProcessCircuityIntegrityEvent;
import com.osstelecom.db.inventory.manager.events.ResourceConnectionCreatedEvent;
import com.osstelecom.db.inventory.manager.events.ResourceLocationCreatedEvent;
import com.osstelecom.db.inventory.manager.events.ResourceSchemaUpdatedEvent;
import com.osstelecom.db.inventory.manager.events.ServiceResourceCreatedEvent;
import com.osstelecom.db.inventory.manager.events.ServiceResourceUpdatedEvent;
import com.osstelecom.db.inventory.manager.exception.ArangoDaoException;
import com.osstelecom.db.inventory.manager.exception.DomainAlreadyExistsException;
import com.osstelecom.db.inventory.manager.exception.DomainNotFoundException;
import com.osstelecom.db.inventory.manager.exception.GenericException;
import com.osstelecom.db.inventory.manager.exception.InvalidRequestException;
import com.osstelecom.db.inventory.manager.exception.ResourceNotFoundException;
import com.osstelecom.db.inventory.manager.exception.SchemaNotFoundException;
import com.osstelecom.db.inventory.manager.exception.ScriptRuleException;
import com.osstelecom.db.inventory.manager.exception.ServiceNotFoundException;
import com.osstelecom.db.inventory.manager.listeners.EventManagerListener;
import com.osstelecom.db.inventory.manager.resources.BasicResource;
import com.osstelecom.db.inventory.manager.resources.CircuitResource;
import com.osstelecom.db.inventory.manager.resources.ConsumableMetric;
import com.osstelecom.db.inventory.manager.resources.ManagedResource;
import com.osstelecom.db.inventory.manager.resources.ResourceConnection;
import com.osstelecom.db.inventory.manager.resources.ResourceLocation;
import com.osstelecom.db.inventory.manager.resources.ServiceResource;

import com.osstelecom.db.inventory.manager.resources.exception.AttributeConstraintViolationException;
import com.osstelecom.db.inventory.manager.resources.exception.ConnectionAlreadyExistsException;
import com.osstelecom.db.inventory.manager.resources.exception.MetricConstraintException;
import com.osstelecom.db.inventory.manager.resources.exception.NoResourcesAvailableException;
import com.osstelecom.db.inventory.manager.resources.model.ResourceSchemaModel;
import com.osstelecom.db.inventory.manager.session.DynamicRuleSession;
import com.osstelecom.db.inventory.manager.session.SchemaSession;
import com.osstelecom.db.inventory.topology.DefaultTopology;
import com.osstelecom.db.inventory.topology.ITopology;
import com.osstelecom.db.inventory.topology.node.DefaultNode;
import com.osstelecom.db.inventory.topology.node.INetworkNode;

/**
 * This class is the main Domain Manager it will handle all operations related
 * to persistence and topology, acting as a adapter betweeen, the persistence
 * layer,topology manager e businnes rules.
 * <p>
 * This class has a 'lockManager' instance of Reentrant lock. It is used to
 * control de atomicity of all operations, so instead of relying the concurrency
 * control to the persistence layer, we try to figure out it here.
 *
 * @author Lucas Nishimura <lucas.nishimura@gmail.com>
 */
@Service
public class DomainManager {

    private ConcurrentHashMap<String, DomainDTO> domains = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, TimerDto> timers = new ConcurrentHashMap<>();

    @Autowired
    private ReentrantLock lockManager;

    @Autowired
    private DynamicRuleSession dynamicRuleSession;

    @Autowired
    private ArangoDao arangoDao;

    @Autowired
    private SchemaSession schemaSession;

    @Autowired
    private ConfigurationManager configuration;

    @Autowired
    private EventManagerListener eventManager;

    @Autowired
    private ManagedResourceDao managedResourceDao;

    private Logger logger = LoggerFactory.getLogger(DomainManager.class);

    /**
     * Creates a Domain
     *
     * @param domainDto
     * @return
     * @throws DomainAlreadyExistsException
     */
    public DomainDTO createDomain(DomainDTO domainDto) throws DomainAlreadyExistsException {
        String timerId = startTimer("createDomain");
        try {

            lockManager.lock();
            domainDto = arangoDao.createDomain(domainDto);
            if (domainDto.getAtomicId() == null) {
                domainDto.setAtomicId(0L);
            }
            domains.put(domainDto.getDomainName(), domainDto);
        } finally {
            if (lockManager.isLocked()) {
                lockManager.unlock();
            }
            endTimer(timerId);
        }
        DomainCreatedEvent domainCreatedEvent = new DomainCreatedEvent(domainDto);
        eventManager.notifyEvent(domainCreatedEvent);
        return domainDto;
    }

    public DomainDTO deleteDomain(DomainDTO domain) throws DomainNotFoundException {
        try {
            lockManager.lock();
            domain = this.getDomain(domain.getDomainName());
            return this.arangoDao.deleteDomain(domain);
        } finally {
            if (lockManager.isLocked()) {
                lockManager.unlock();
            }
        }
    }

    /**
     * Retrieves a domain by name
     *
     * @param domainName
     * @return
     * @throws DomainNotFoundException
     */
    public DomainDTO getDomain(String domainName) throws DomainNotFoundException {
        if (!domains.containsKey(domainName)) {
            throw new DomainNotFoundException("Domain :[" + domainName + "] not found");
        }
        return domains.get(domainName);
    }

    /**
     * Get the list of all domains.
     *
     * @return
     */
    public ArrayList<DomainDTO> getAllDomains() {
        ArrayList<DomainDTO> result = new ArrayList<>();

        this.domains.forEach((name, domain) -> {
            result.add(domain);
        });
        return result;
    }

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

    public ServiceResource deleteService(ServiceResource service) throws ArangoDaoException {
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
            // key Previsibility
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
            e.printStackTrace();
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

    /**
     * Create a managed Resource
     *
     * @param resource
     * @return
     * @throws SchemaNotFoundException
     * @throws AttributeConstraintViolationException
     * @throws GenericException
     */
    public ManagedResource createManagedResource(ManagedResource resource) throws SchemaNotFoundException, AttributeConstraintViolationException, GenericException, ScriptRuleException, ArangoDaoException {
        String timerId = startTimer("createManagedResource");
        try {
            lockManager.lock();
            if (resource.getUid() == null) {
                resource.setUid(this.getUUID());
            }
            resource.setAtomId(resource.getDomain().addAndGetId());
            ResourceSchemaModel schemaModel = schemaSession.loadSchema(resource.getAttributeSchemaName());
            resource.setSchemaModel(schemaModel);
            schemaSession.validateResourceSchema(resource);       //  
            dynamicRuleSession.evalResource(resource, "I", this); // <--- Pode não ser verdade , se a chave for duplicada..
            // 

//            DocumentCreateEntity<ManagedResource> result = arangoDao.createManagedResource(resource);
            DocumentCreateEntity<ManagedResource> result = this.managedResourceDao.insertResource(resource);
            resource.setUid(result.getId());
            resource.setRevisionId(result.getRev());
            //
            // Aqui criou o managed resource
            //
            ManagedResourceCreatedEvent event = new ManagedResourceCreatedEvent(resource);
            this.eventManager.notifyEvent(event);
            return resource;
        } finally {
            if (lockManager.isLocked()) {
                lockManager.unlock();
            }
            endTimer(timerId);
        }

    }

    /**
     * Created a Resource Location
     *
     * @param resource
     * @return
     * @throws GenericException
     * @throws SchemaNotFoundException
     * @throws AttributeConstraintViolationException
     * @throws ScriptRuleException
     */
    public ResourceLocation createResourceLocation(ResourceLocation resource) throws GenericException, SchemaNotFoundException, AttributeConstraintViolationException, ScriptRuleException {
        String timerId = startTimer("createResourceLocation");
        try {
            lockManager.lock();
            resource.setUid(this.getUUID());
            resource.setAtomId(resource.getDomain().addAndGetId());

            ResourceSchemaModel schemaModel = schemaSession.loadSchema(resource.getAttributeSchemaName());
            resource.setSchemaModel(schemaModel);
            schemaSession.validateResourceSchema(resource);
            dynamicRuleSession.evalResource(resource, "I", this);
            DocumentCreateEntity<ResourceLocation> result = arangoDao.createResourceLocation(resource);
            resource.setUid(result.getId());
            resource.setRevisionId(result.getRev());
            lockManager.unlock();
            //
            // Aqui de Fato Criou o ResourceLocation
            //
            ResourceLocationCreatedEvent event = new ResourceLocationCreatedEvent(resource);
            this.eventManager.notifyEvent(event);
            return resource;
        } finally {
            if (lockManager.isLocked()) {
                lockManager.unlock();
            }
            endTimer(timerId);
        }
    }

    /**
     * Creates a Circuit Resource
     *
     * @param circuit
     * @return
     * @throws GenericException
     * @throws SchemaNotFoundException
     * @throws AttributeConstraintViolationException
     * @throws ScriptRuleException
     */
    public CircuitResource createCircuitResource(CircuitResource circuit) throws GenericException, SchemaNotFoundException, AttributeConstraintViolationException, ScriptRuleException {
        String timerId = startTimer("createCircuitResource");
        try {
            lockManager.lock();
            DomainDTO domain = circuit.getDomain();
            circuit.setUid(this.getUUID());
            circuit.setAtomId(domain.addAndGetId());
            ResourceSchemaModel schemaModel = schemaSession.loadSchema(circuit.getAttributeSchemaName());
            circuit.setSchemaModel(schemaModel);
            schemaSession.validateResourceSchema(circuit);
            dynamicRuleSession.evalResource(circuit, "I", this);
            DocumentCreateEntity<CircuitResource> result = arangoDao.createCircuitResource(circuit);
            //CircuitResource result = doc.getNew();
            circuit.setUid(result.getId());
            circuit.setRevisionId(result.getRev());
            //
            // Aqui criou o circuito
            //
            CircuitResourceCreatedEvent event = new CircuitResourceCreatedEvent(circuit);
            this.eventManager.notifyEvent(event);
            return circuit;
        } finally {
            if (lockManager.isLocked()) {
                lockManager.unlock();
            }
            endTimer(timerId);
        }
    }

    /**
     * Creates a consumable metric
     *
     * @param name
     * @return
     */
    public ConsumableMetric createConsumableMetric(String name) {
        String timerId = startTimer("createConsumableMetric");
        try {
            lockManager.lock();
            ConsumableMetric metric = new ConsumableMetric(this);
            metric.setMetricName(name);
            endTimer(timerId);
            //
            // Notifica o event Manager da Metrica criada
            //
            ConsumableMetricCreatedEvent event = new ConsumableMetricCreatedEvent(metric);
            eventManager.notifyEvent(event);
            return metric;
        } finally {
            if (lockManager.isLocked()) {
                lockManager.unlock();
            }
            endTimer(timerId);
        }

    }

    /**
     * Cria uma nova Conexão entre dois elementos, Note que a ordem é importante
     * para garantir o Dê -> Para
     *
     * @param from
     * @param to
     * @return
     */
    public ResourceConnection createResourceConnection(BasicResource from, BasicResource to, String domainName) throws ConnectionAlreadyExistsException, MetricConstraintException, NoResourcesAvailableException, GenericException, DomainNotFoundException {
        String timerId = startTimer("createResourceConnection");
        try {
            lockManager.lock();
            DomainDTO domain = this.getDomain(domainName);
            ResourceConnection connection = this.createResourceConnection(from, to, domain); // <-- Event Handled Here

            return connection;
        } finally {
            if (lockManager.isLocked()) {
                lockManager.unlock();
            }
            endTimer(timerId);

        }
    }

    public ResourceConnection createResourceConnection(ResourceConnection connection) throws ConnectionAlreadyExistsException, MetricConstraintException, NoResourcesAvailableException, GenericException, SchemaNotFoundException, AttributeConstraintViolationException, ScriptRuleException, DomainNotFoundException {
        String timerId = startTimer("createResourceConnection");
        try {
            lockManager.lock();
            connection.setUid(this.getUUID());

//            DomainDTO domain = this.getDomain(connection.getDomain().getDomainName());
            ResourceSchemaModel schemaModel = schemaSession.loadSchema(connection.getAttributeSchemaName());
            connection.setSchemaModel(schemaModel);
            schemaSession.validateResourceSchema(connection);
            dynamicRuleSession.evalResource(connection, "I", this);
            //
            // Creates the connection on DB
            //
            DocumentCreateEntity<ResourceConnection> result = arangoDao.createConnection(connection);
            connection.setUid(result.getId());
            connection.setRevisionId(result.getRev());
            //
            // Update Edges
            //

            ResourceConnectionCreatedEvent event = new ResourceConnectionCreatedEvent(connection.getFrom(), connection.getTo(), connection);
            eventManager.notifyEvent(event);
            return connection;
        } finally {
            if (lockManager.isLocked()) {
                lockManager.unlock();
            }
            endTimer(timerId);
        }
    }

    /**
     * Cria uma nova Conexão entre dois elementos, Note que a ordem é importante
     * para garantir o Dê -> Para
     *
     * @param from
     * @param to
     * @return
     */
    public ResourceConnection createResourceConnection(BasicResource from, BasicResource to, DomainDTO domain) throws ConnectionAlreadyExistsException, MetricConstraintException, NoResourcesAvailableException, GenericException {

        String timerId = startTimer("createResourceConnection");
        try {
            lockManager.lock();
            ResourceConnection connection = new ResourceConnection(domain);
            connection.setUid(this.getUUID());
            connection.setFrom(from);
            connection.setTo(to);

//            connection.setAtomId(this.getAtomId());
            connection.setAtomId(connection.getDomain().addAndGetId());
            //
            // Notifica o Elemento Origem para Computar o Consumo de recursos se necessário
            //
//        from.notifyConnection(connection);
            DocumentCreateEntity<ResourceConnection> result = arangoDao.createConnection(connection);
            connection.setUid(result.getId());
            connection.setRevisionId(result.getRev());

            ResourceConnectionCreatedEvent event = new ResourceConnectionCreatedEvent(from, to, connection);
            this.eventManager.notifyEvent(event);
            return connection;
        } finally {
            if (lockManager.isLocked()) {
                lockManager.unlock();
                endTimer(timerId);
            }
        }
    }

    public ResourceLocation findResourceLocation(String name, String nodeAdrress, String className, String domainName) throws ResourceNotFoundException, DomainNotFoundException, ArangoDaoException {
        String timerId = startTimer("findResourceLocation");
        try {
            lockManager.lock();
            DomainDTO domain = this.getDomain(domainName);
            ResourceLocation resource = arangoDao.findResourceLocation(name, nodeAdrress, className, domain);
            return resource;
        } finally {
            if (lockManager.isLocked()) {
                lockManager.unlock();
            }
            endTimer(timerId);
        }
    }

    public ManagedResource findManagedResource(ManagedResource resource) throws ResourceNotFoundException, DomainNotFoundException, ArangoDaoException {
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
    public ManagedResource findManagedResourceById(ManagedResource resource) throws DomainNotFoundException, ResourceNotFoundException, ArangoDaoException {
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
     * Generates an UUID
     *
     * @return
     */
    private String getUUID() {
        return UUID.randomUUID().toString();
    }

    /**
     * Starts and fetch all available domains
     */
    @EventListener(ApplicationReadyEvent.class)
    private void onStartUp() throws ArangoDaoException {
        this.arangoDao.getDomains().forEach(d -> {
            logger.debug("\tFound Domain: [" + d.getDomainName() + "] Atomic ID:[" + d.getAtomicId() + "]");
            this.domains.put(d.getDomainName(), d);
        });
        this.eventManager.setDomainManager(this);

    }

    /**
     * Update the persistence layer with the current atomic id in the domain
     */
    @PreDestroy
    private void onShutDown() {
        this.domains.forEach((domainName, domain) -> {
            this.arangoDao.updateDomain(domain);
            logger.debug("Domain: [" + domainName + "] Updated");
        });

    }

    /**
     * Just abort the current transaction on the dynamic context
     *
     * @throws ScriptRuleException
     */
    public void abortTransaction() throws ScriptRuleException {
        this.abortTransaction("Generic Aborted by script... no cause specified");
    }

    /**
     * Usend inside the dynamic context to process exceptions and abort current
     * transaction without details but a message
     *
     * @param msg
     * @throws ScriptRuleException
     */
    public void abortTransaction(String msg) throws ScriptRuleException {
        this.abortTransaction(msg, null);
    }

    /**
     * Usend inside the dynamic context to process exceptions and abort current
     * transaction with details
     *
     * @param msg
     * @param details
     * @throws ScriptRuleException
     */
    public void abortTransaction(String msg, Object details) throws ScriptRuleException {
        ScriptRuleException ex = new ScriptRuleException(msg);
        if (details != null) {
            ex.setDetails(details);
        }
        logger.warn("Aborting Transaction..." + details);
        throw ex;
    }

    /**
     * Check if given object is a location
     *
     * @param resource
     * @return
     */
    public Boolean isLocation(BasicResource resource) {
        return resource.getObjectClass().contains("Location");
    }

    /**
     * Check if given object is a Connection
     *
     * @param resource
     * @return
     */
    public Boolean isConnection(BasicResource resource) {
        return resource.getObjectClass().contains("Connection");
    }

    /**
     * Check if given object is a Managed Resource
     *
     * @param resource
     * @return
     */
    public Boolean isManagedResource(BasicResource resource) {
        return resource.getObjectClass().contains("ManagedResource");
    }

    /**
     * Check if given object is a Circuit
     *
     * @param resource
     * @return
     */
    public Boolean isCircuit(BasicResource resource) {
        return resource.getObjectClass().contains("Circuit");
    }

    /**
     * Timer Util
     *
     * @param operation
     * @return
     */
    private String startTimer(String operation) {
        String uid = UUID.randomUUID().toString();
        timers.put(uid, new TimerDto(uid, operation, System.currentTimeMillis()));
        return uid;
    }

    /**
     * Time Util, compute the time, return -1 if invalid
     *
     * @param uid
     */
    private Long endTimer(String uid) {
        Long endTimer = System.currentTimeMillis();
        if (timers.containsKey(uid)) {
            TimerDto timer = timers.remove(uid);
            Long tookTimer = endTimer - timer.getStartTimer();
            if (configuration.loadConfiguration().getTrackTimers()) {
                logger.debug("Timer: [" + timer.getUid() + "] Operation:[" + timer.getOperation() + "] Took:" + tookTimer + " ms");
            } else {
                if (tookTimer > 100) {
                    //
                    // Slow Operation Detected
                    //
                    logger.warn("Timer: [" + timer.getUid() + "] Operation:[" + timer.getOperation() + "] Took:" + tookTimer + " ms (>100ms)");
                }
            }
            return tookTimer;
        }
        return -1L;
    }

    /**
     * Find a Circuit Instance
     *
     * @param circuit
     * @return
     * @throws ResourceNotFoundException
     * @throws ArangoDaoException
     */
    public CircuitResource findCircuitResource(CircuitResource circuit) throws ResourceNotFoundException, ArangoDaoException {
        String timerId = startTimer("findCircuitResource");
        try {
            lockManager.lock();
            return arangoDao.findCircuitResource(circuit);
        } finally {
            if (lockManager.isLocked()) {
                lockManager.unlock();
            }
            endTimer(timerId);
        }
    }

    /**
     * Search for a resource connection
     *
     * @param connection
     * @return
     * @throws ResourceNotFoundException
     * @throws ArangoDaoException
     */
    public ResourceConnection findResourceConnection(ResourceConnection connection) throws ResourceNotFoundException, ArangoDaoException {
        String timerId = startTimer("findResourceConnection");
        try {
            lockManager.lock();

            return arangoDao.findResourceConnection(connection);
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
    public ManagedResource updateManagedResource(ManagedResource resource) throws InvalidRequestException {
        String timerId = startTimer("updateManagedResource");
        try {
            lockManager.lock();
            //
            // Mover isso para session...
            //
            if (!resource.getOperationalStatus().equals("UP") && !resource.getOperationalStatus().equalsIgnoreCase("DOWN")) {
                throw new InvalidRequestException("Invalid OperationalStatus:[" + resource.getOperationalStatus() + "]");
            }

            resource.setLastModifiedDate(new Date());
            DocumentUpdateEntity<ManagedResource> updatedEntity = arangoDao.updateManagedResource(resource);
            ManagedResource updatedResource = updatedEntity.getNew();
            //
            // Update the related dependencies
            //
            try {
                ArrayList<String> relatedCircuits = new ArrayList<>();
                this.arangoDao.findRelatedConnections(updatedResource).forEach((c) -> {

                    if (c.getFrom().getUid().equals(updatedResource.getUid())) {
                        //
                        // Update from
                        //

                        c.setFrom(updatedResource);
                        //
                        // validando 
                        //

                    } else if (c.getTo().getUid().equals(updatedResource.getUid())) {
                        //
                        // Update to
                        //
                        c.setTo(updatedResource);

                    }

                    //
                    // Avalia o status final da Conexão
                    //
                    Boolean circuitStateChanged = false;
                    if (c.getFrom().getOperationalStatus().equals("UP")
                            && c.getTo().getOperationalStatus().equals("UP")) {
                        if (c.getOperationalStatus().equals("DOWN")) {
                            c.setOperationalStatus("UP");
                            circuitStateChanged = true;
                        }
                    } else {
                        if (c.getOperationalStatus().equals("UP")) {
                            c.setOperationalStatus("DOWN");
                            circuitStateChanged = true;
                        }
                    }
                    if (circuitStateChanged) {
                        this.updateResourceConnection(c); // <- Atualizou a conexão no banco
                        //
                        // Now Update related Circuits..
                        //
                        if (c.getCircuits() != null) {
                            if (!c.getCircuits().isEmpty()) {
                                for (String circuitId : c.getCircuits()) {
                                    if (!relatedCircuits.contains(circuitId)) {
                                        //
                                        // Garante que só incluímos o mesmo circuito uma vez
                                        //
                                        relatedCircuits.add(circuitId);
                                    }
                                }
                            }
                        }
                    }

                });

                if (!relatedCircuits.isEmpty()) {
                    for (String circuitId : relatedCircuits) {
                        try {
                            CircuitResource circuit = this.arangoDao.findCircuitResourceById(circuitId, updatedResource.getDomain());
                            if (circuit.getaPoint().getId().equals(updatedResource.getUid())) {
                                circuit.setaPoint(updatedResource);
                                circuit = this.updateCircuitResource(circuit);
                            } else if (circuit.getzPoint().getId().equals(updatedResource.getUid())) {
                                circuit.setzPoint(updatedResource);
                                circuit = this.updateCircuitResource(circuit);
                            }

                            //
                            // Circuit Integrity must be checked here,
                            // So we fire an event that will later request that
                            //
                            this.eventManager.notifyEvent(new ProcessCircuityIntegrityEvent(circuit));

                        } catch (ResourceNotFoundException ex) {
                            //
                            // This should never happen...but if happen please try to treat the error
                            //
                            logger.error("Inconsistent Database on Domain Please check Related Circuit Resources: ResourceID:[" + updatedResource.getId() + "]", ex);
                        } catch (ArangoDaoException ex) {
                            logger.error("Arango Level Error", ex);
                        }
                    }
                }

            } catch (IOException | IllegalStateException ex) {
                logger.error("Failed to Update Resource Connection Relation", ex);
            }

            eventManager.notifyEvent(new ManagedResourceUpdatedEvent(updatedEntity.getOld(), updatedEntity.getNew()));
            return updatedResource;
        } finally {
            if (lockManager.isLocked()) {
                lockManager.unlock();
            }
            endTimer(timerId);
        }
    }

    /**
     * Update a Resource Connection
     *
     * @param connection
     * @return
     */
    public ResourceConnection updateResourceConnection(ResourceConnection connection) {
        String timerId = startTimer("updateResourceConnection");
        try {
            lockManager.lock();
            connection.setLastModifiedDate(new Date());
            return arangoDao.updateResourceConnection(connection);
        } finally {
            if (lockManager.isLocked()) {
                lockManager.unlock();
            }
            endTimer(timerId);
        }
    }

    /**
     * Update a Resource Connection
     *
     * @param connection
     * @return
     */
    public List<ResourceConnection> updateResourceConnections(List<ResourceConnection> connections) {
        String timerId = startTimer("updateResourceConnection");
        try {
            lockManager.lock();
            connections.forEach(connection -> {
                connection.setLastModifiedDate(new Date());
            });

            return arangoDao.updateResourceConnections(connections);
        } finally {
            if (lockManager.isLocked()) {
                lockManager.unlock();
            }
            endTimer(timerId);
        }
    }

    /**
     * Update a circuit
     *
     * @param resource
     * @return
     */
    public CircuitResource updateCircuitResource(CircuitResource resource) {
        String timerId = startTimer("updateCircuitResource");
        try {
            lockManager.lock();
            resource.setLastModifiedDate(new Date());
            DocumentUpdateEntity<CircuitResource> result = arangoDao.updateCircuitResource(resource);
            CircuitResource newResource = result.getNew();
            CircuitResource oldResource = result.getOld();
            CircuitResourceUpdatedEvent event = new CircuitResourceUpdatedEvent(oldResource, newResource);
            //
            // Emits the transitional event
            //
            this.eventManager.notifyEvent(event);
            return newResource;
        } finally {
            if (lockManager.isLocked()) {
                lockManager.unlock();
            }
            endTimer(timerId);
        }
    }

    /**
     * Return the circuit connections( Paths )
     *
     * @param circuit
     * @return
     * @throws ArangoDaoException
     */
    public GraphList<ResourceConnection> findCircuitPaths(CircuitResource circuit) throws ArangoDaoException {
        return arangoDao.findCircuitPaths(circuit);
    }

    /**
     * Computes if the the graph topology is fully connected, will return
     * isolated nodes
     *
     * @param connections
     * @param aPoint
     * @return
     */
    public List<String> checkBrokenGraph(List<ResourceConnection> connections, ManagedResource aPoint) {
        List<String> result = new ArrayList<>();
        if (!connections.isEmpty()) {
            //
            // @Todo:Testar memória...
            //
            Long startTime = System.currentTimeMillis();
            DefaultTopology topology = new DefaultTopology();
            AtomicLong localId = new AtomicLong(0L);
            INetworkNode target = createNode(aPoint.getId(), localId.incrementAndGet(), topology);
            //
            // this is the A Point from the circuit, will mark as endPoint. meaning all nodes must reach this one
            //
            target.setEndPoint(true);

            connections.forEach(connection -> {
                INetworkNode from = topology.getNodeByName(connection.getFrom().getId());
                INetworkNode to = topology.getNodeByName(connection.getTo().getId());

                if (from == null) {
                    from = createNode(connection.getFrom().getId(), localId.incrementAndGet(), topology);
                }

                if (to == null) {
                    to = createNode(connection.getTo().getId(), localId.incrementAndGet(), topology);
                }

                if (connection.getOperationalStatus().equals("UP")) {
                    topology.addConnection(from, to, "Connection: " + connection.getId());
                }

                //topology.addConnection(to, from, connection.getId() + ".A");
            });

            logger.debug("-------------------------------------------------------------");
            logger.debug("Topology Loaded! ");
            logger.debug("Topology Size:");
            logger.debug("         Nodes:{}", topology.getNodes().size());
            logger.debug("   Connections:{}", topology.getConnections().size());
            logger.debug("     EndPoints:{}", topology.getEndPoints().size());
            for (INetworkNode node : topology.getEndPoints()) {
                logger.debug("       {}", node.getName());
            }

            List<INetworkNode> weak = topology.getImpactManager().getUnreacheableNodes();
            Long endTime = System.currentTimeMillis();
            Long tookTime = endTime - startTime;
            logger.debug("Found {} Unrecheable Nodes IN: {} ms", weak.size(), tookTime);

            if (!weak.isEmpty()) {
                weak.forEach(node -> {
                    if (!result.contains(node.getName())) {
                        result.add(node.getName());
                    }
                });
            }
            //
            // Try to free the memory
            //
            topology.destroyTopology();
        }
        return result;
    }

    public void findWeakLinks(List<ResourceConnection> connections, FilterDTO filter) {
        //
        // Valida se temos dados de conexões...
        //

        if (!connections.isEmpty()) {
            //
            // Vamos validar se a regex encontra targets
            //

            List<BasicResource> targets = new ArrayList<>();

            Pattern p = Pattern.compile(filter.getTargetRegex());
            for (ResourceConnection connection : connections) {
                Matcher sourceMatcher = p.matcher(connection.getFrom().getName());
                if (sourceMatcher.matches() && !targets.contains(connection.getFrom())) {
                    targets.add(connection.getFrom());
                }

                Matcher targetMatched = p.matcher(connection.getTo().getName());
                if (targetMatched.matches() && !targets.contains(connection.getFrom())) {
                    targets.add(connection.getTo());
                }
            }

            if (!targets.isEmpty()) {
                //
                // Ok temos algo para Trabalhar, montemos a topologia...
                //
                DefaultTopology topology = new DefaultTopology();

                targets.forEach(t -> {
                    //
                    // 
                    //

                });

                connections.forEach(connection -> {

                });

            }
        }
    }

//    public void test(String filter, Integer threads) {
//        String aql = "for doc in inventory_connections "
//                + " filter doc.from.name like @filter"
//                + "    or  doc.to.name like @filter return doc";
//        HashMap<String, Object> bindings = new HashMap<>();
//        bindings.put("filter", filter);
//        ArangoCursor<ResourceConnection> connections = arangoDao.filterConnectionByAQL(aql, bindings);
//        DefaultTopology topology = new DefaultTopology();
//        AtomicLong id = new AtomicLong(0L);
////        INetworkNode saida = createNode("OUTPUT", id.incrementAndGet(), topology);
////        saida.setEndPoint(true);
//
//        connections.forEachRemaining(connection -> {
//            if (connection.getFrom().getName().contains("m-br") && connection.getTo().getName().contains("m-br")) {
//                INetworkNode from = topology.getNodeByName(connection.getFrom().getName());
//                INetworkNode to = topology.getNodeByName(connection.getTo().getName());
//
//                if (from == null) {
//                    from = createNode(connection.getFrom().getName(), id.incrementAndGet(), topology);
//                }
//
//                if (to == null) {
//                    to = createNode(connection.getTo().getName(), id.incrementAndGet(), topology);
//                }
//
//                if (from.getName().contains("gwc")) {
////                topology.addConnection(from, saida);
//                    from.setEndPoint(true);
//                } else {
//                    from.setEndPoint(false);
//                    from.addAttribute("erbCount", 0);
//                }
//
//                if (to.getName().contains("gwc")) {
//                    to.setEndPoint(true);
////                topology.addConnection(to, saida);
//                } else {
//                    to.setEndPoint(false);
//                    to.addAttribute("erbCount", 0);
//                }
//
//                if (from.getConnectionRelated(to).isEmpty()) {
//                    INetworkConnection topologyConnection = topology.addConnection(from, to);
//                }
//            } else if (connection.getFrom().getName().contains("m-br")
//                    || connection.getTo().getName().contains("m-br")) {
//                logger.debug("Connection From: [" + connection.getFrom().getName() + "] TO:[" + connection.getTo().getName() + "]");
//                BasicResource resource = null;
//                Boolean goAhead = true;
//                if (connection.getFrom().getName().contains("m-br")) {
//                    resource = connection.getFrom();
////                    if (!connection.getTo().getName().contains("erb")) {
////                        goAhead = false;
////                    }
//                } else {
//                    resource = connection.getTo();
////                    if (!connection.getFrom().getName().contains("erb")) {
////                        goAhead = false;
////                    }
//                }
//
//                if (goAhead) {
//                    INetworkNode node = topology.getNodeByName(resource.getName());
//                    if (node == null) {
//                        node = createNode(resource.getName(), id.incrementAndGet(), topology);
//                        node.addAttribute("erbCount", 0);
//                    }
//                    if (node.getAttribute("erbCount") == null) {
//                        node.addAttribute("erbCount", 0);
//                    }
//                    Integer count = (Integer) node.getAttribute("erbCount");
//                    count++;
//                    node.addAttribute("erbCount", count);
//                }
//
//            }
//        });
//        logger.debug("-------------------------------------------------------------");
//        logger.debug("Topology Loaded! ");
//        logger.debug("Topology Size:");
//        logger.debug("   Nodes:" + topology.getNodes().size());
//        logger.debug("   Connections:" + topology.getConnections().size());
//        logger.debug("   EndPoints:" + topology.getEndPoints().size());
//        for (INetworkNode node : topology.getEndPoints()) {
//            logger.debug("       " + node.getName());
//        }
//
//        Long start = System.currentTimeMillis();
//        logger.debug("-------------------------------------------------------------");
//        logger.debug("Weak Nodes With 1 Connection or LESS:");
//        logger.debug("-------------------------------------------------------------");
//        List<INetworkNode> weak = null;
//        if (threads > 1) {
//            weak = topology.getImpactManager().getWeakNodes(1, false, threads, false);
//        } else {
//            weak = topology.getImpactManager().getWeakNodes(1, false, threads, false);
//        }
//
//        logger.debug("Found " + weak.size() + " Weak Nodes");
//        for (INetworkNode n : weak) {
//            logger.debug("  ::Weak " + n.getName() + " Connections size:" + n.getEndpointConnectionsCount() + " Impact Count:" + n.getImpactedNodes().size() + " ERBS:" + n.getAttribute("erbCount"));
//            if (!n.getImpactedNodes().isEmpty()) {
//                n.getImpactedNodes().forEach((k, v) -> {
//                    logger.debug("      ::Node " + n.getName() + " Impacts:" + v.getName() + " ERBS: " + v.getAttribute("erbCount"));
//                });
//
//            }
//        }
//
//        Long end = System.currentTimeMillis();
//        Long took = end - start;
//        logger.debug("Process took: " + took + " ms With:" + threads + " Threads");
//        try {
//            connections.close();
//        } catch (IOException ex) {
//            System.out.println("OOOOOOO Deu RUIm!");
//        }
//        //
//        // Uma vez usados todos os recursos, destroy tudo...
//        //
//        topology.destroyTopology();
//    }
    private INetworkNode createNode(String name, Long id, ITopology topology) {
        INetworkNode node = new DefaultNode(name, id.intValue(), topology);
        return node;
    }

    public List<ManagedResource> getNodesByFilter(FilterDTO filter, String domainName) throws DomainNotFoundException, ResourceNotFoundException, ArangoDaoException, InvalidRequestException {
        DomainDTO domain = getDomain(domainName);
        if (filter.getObjects().contains("nodes")) {
            return arangoDao.getNodesByFilter(filter, domain);
        }
        throw new InvalidRequestException("getNodesByFilter() can only retrieve nodes objects");
    }

    public GraphList<ResourceConnection> getConnectionsByFilter(FilterDTO filter, String domainName) throws DomainNotFoundException, ResourceNotFoundException, ArangoDaoException, InvalidRequestException {
        DomainDTO domain = getDomain(domainName);
        if (filter.getObjects().contains("connections")) {
            return arangoDao.getConnectionsByFilter(filter, domain);
        }
        throw new InvalidRequestException("getConnectionsByFilter() can only retrieve connections objects");
    }

    public GraphList<ManagedResource> findManagedResourcesBySchemaName(ResourceSchemaModel model, DomainDTO domain) throws ResourceNotFoundException {
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

        for (DomainDTO domain : this.getAllDomains()) {
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

                    arangoDao.updateManagedResource(resource);

                    if (totalProcessed.incrementAndGet() % 1000 == 0) {
                        logger.debug("Updated {} Records", totalProcessed.get());
                    }

                });
            } catch (IOException | IllegalStateException | GenericException | SchemaNotFoundException ex) {
                logger.error("Failed to update Resource Schema Model", ex);
            } catch (ResourceNotFoundException ex) {
                logger.error("Domain Has No Resources on Schema:[{}]", update.getModel(), ex);
            }
            logger.debug("Updating Schema[{}] On Domain:[{}] DONE", update.getModel().getSchemaName(), domain.getDomainName());
        }
    }
}
