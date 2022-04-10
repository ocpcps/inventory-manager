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

import com.arangodb.ArangoCursor;
import com.arangodb.entity.DocumentCreateEntity;
import com.arangodb.entity.DocumentUpdateEntity;
import com.osstelecom.db.inventory.manager.configuration.ConfigurationManager;
import com.osstelecom.db.inventory.manager.dao.ArangoDao;
import com.osstelecom.db.inventory.manager.dto.DomainDTO;
import com.osstelecom.db.inventory.manager.dto.FilterDTO;
import com.osstelecom.db.inventory.manager.dto.TimerDto;
import com.osstelecom.db.inventory.manager.events.CircuitResourceCreatedEvent;
import com.osstelecom.db.inventory.manager.events.CircuitResourceUpdatedEvent;
import com.osstelecom.db.inventory.manager.events.ManagedResourceConnectionCreatedEvent;
import com.osstelecom.db.inventory.manager.events.ManagedResourceCreatedEvent;
import com.osstelecom.db.inventory.manager.events.ResourceLocationCreatedEvent;
import com.osstelecom.db.inventory.manager.exception.DomainAlreadyExistsException;
import com.osstelecom.db.inventory.manager.exception.DomainNotFoundException;
import com.osstelecom.db.inventory.manager.exception.GenericException;
import com.osstelecom.db.inventory.manager.exception.ResourceNotFoundException;
import com.osstelecom.db.inventory.manager.exception.SchemaNotFoundException;
import com.osstelecom.db.inventory.manager.exception.ScriptRuleException;
import com.osstelecom.db.inventory.manager.resources.BasicResource;
import com.osstelecom.db.inventory.manager.resources.CircuitResource;
import com.osstelecom.db.inventory.manager.resources.ConsumableMetric;
import com.osstelecom.db.inventory.manager.resources.ManagedResource;
import com.osstelecom.db.inventory.manager.resources.ResourceConnection;
import com.osstelecom.db.inventory.manager.resources.ResourceLocation;
import com.osstelecom.db.inventory.manager.resources.exception.AttributeConstraintViolationException;
import com.osstelecom.db.inventory.manager.resources.exception.ConnectionAlreadyExistsException;
import com.osstelecom.db.inventory.manager.resources.exception.MetricConstraintException;
import com.osstelecom.db.inventory.manager.resources.exception.NoResourcesAvailableException;
import com.osstelecom.db.inventory.manager.resources.model.ResourceSchemaModel;
import com.osstelecom.db.inventory.manager.session.DynamicRuleSession;
import com.osstelecom.db.inventory.manager.session.EventManagerSession;
import com.osstelecom.db.inventory.manager.session.SchemaSession;
import com.osstelecom.db.inventory.topology.DefaultTopology;
import com.osstelecom.db.inventory.topology.ITopology;
import com.osstelecom.db.inventory.topology.connection.INetworkConnection;
import com.osstelecom.db.inventory.topology.node.DefaultNode;
import com.osstelecom.db.inventory.topology.node.INetworkNode;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
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

/**
 * Gerência a alocação de recursos
 *
 * @author Lucas Nishimura <lucas.nishimura@gmail.com>
 */
@Service
public class DomainManager {

    private ReentrantLock lockManager = new ReentrantLock();
//    private AtomicLong atomId = new AtomicLong(0);
    private ConcurrentHashMap<String, DomainDTO> domains = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, TimerDto> timers = new ConcurrentHashMap<>();

    @Autowired
    private DynamicRuleSession dynamicRuleSession;

    @Autowired
    private ArangoDao arangoDao;

    @Autowired
    private SchemaSession schemaSession;

    @Autowired
    private ConfigurationManager configuration;

    @Autowired
    private EventManagerSession eventManager;

    private Logger logger = LoggerFactory.getLogger(DomainManager.class);

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
        return domainDto;
    }

    public DomainDTO getDomain(String domainName) throws DomainNotFoundException {
        if (!domains.containsKey(domainName)) {
            throw new DomainNotFoundException("Domain :[" + domainName + "] not found");
        }
        return domains.get(domainName);
    }

    public DomainManager() {

    }

    /**
     *
     * @param resource
     * @return
     * @throws SchemaNotFoundException
     * @throws AttributeConstraintViolationException
     * @throws GenericException
     */
    public ManagedResource createManagedResource(ManagedResource resource) throws SchemaNotFoundException, AttributeConstraintViolationException, GenericException, ScriptRuleException {
        String timerId = startTimer("createManagedResource");
        try {
            lockManager.lock();
            resource.setUid(this.getUUID());
            resource.setAtomId(resource.getDomain().addAndGetId());
            ResourceSchemaModel schemaModel = schemaSession.loadSchema(resource.getAttributeSchemaName());
            resource.setSchemaModel(schemaModel);
            schemaSession.validateResourceSchema(resource);
            dynamicRuleSession.evalResource(resource, "I", this);
            DocumentCreateEntity<ManagedResource> result = arangoDao.createManagedResource(resource);
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
//            CircuitResource result = doc.getNew();
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

    public ConsumableMetric createConsumableMetric(String name) {
        String timerId = startTimer("createConsumableMetric");
        try {
            lockManager.lock();
            ConsumableMetric metric = new ConsumableMetric(this);
            metric.setMetricName(name);
            endTimer(timerId);
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
            return this.createResourceConnection(from, to, domain);
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

            DomainDTO domain = this.getDomain(connection.getDomain().getDomainName());
            ResourceSchemaModel schemaModel = schemaSession.loadSchema(connection.getAttributeSchemaName());
            connection.setSchemaModel(schemaModel);
            schemaSession.validateResourceSchema(connection);
            dynamicRuleSession.evalResource(connection, "I", this);
//            arangoDao.createConnection(connection);
            DocumentCreateEntity<ResourceConnection> result = arangoDao.createConnection(connection);
            connection.setUid(result.getId());
            connection.setRevisionId(result.getRev());
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

            ManagedResourceConnectionCreatedEvent event = new ManagedResourceConnectionCreatedEvent(from, to, connection);
            this.eventManager.notifyEvent(event);
            return connection;
        } finally {
            if (lockManager.isLocked()) {
                lockManager.unlock();
                endTimer(timerId);
            }
        }
    }

    public ResourceLocation findResourceLocation(String name, String nodeAdrress, String className, String domainName) throws ResourceNotFoundException, DomainNotFoundException, IOException {
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

    public ManagedResource findManagedResource(BasicResource resource) throws ResourceNotFoundException, DomainNotFoundException, IOException {
        return this.findManagedResource(resource.getName(), resource.getNodeAddress(), resource.getClassName(), resource.getDomain().getDomainName());
    }

    public ManagedResource findManagedResource(String name, String nodeAdrress, String className, String domainName) throws ResourceNotFoundException, DomainNotFoundException, IOException {
        String timerId = startTimer("findResourceLocation");
        try {
            lockManager.lock();
            DomainDTO domain = this.getDomain(domainName);
            ManagedResource resource = arangoDao.findManagedResource(name, nodeAdrress, className, domain);
            return resource;
        } finally {
            if (lockManager.isLocked()) {
                lockManager.unlock();
            }
            endTimer(timerId);
        }
    }

    /**
     * Cria um ID único para o Objeto
     *
     * @return
     */
    private String getUUID() {
        return UUID.randomUUID().toString();
    }

    /**
     * Inicia os dominios conhecidoss
     */
    @EventListener(ApplicationReadyEvent.class)
    private void onStartUp() throws IOException {
        this.arangoDao.getDomains().forEach(d -> {
            this.domains.put(d.getDomainName(), d);
        });

    }

    @PreDestroy
    private void onShutDown() {
        this.domains.forEach((domainName, domain) -> {
            this.arangoDao.updateDomain(domain);
        });

    }

    public void abortTransaction() throws ScriptRuleException {
        this.abortTransaction("Generic Aborted by script... no cause specified");
    }

    public void abortTransaction(String msg) throws ScriptRuleException {
        this.abortTransaction(msg, null);
    }

    /**
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

    public Boolean isLocation(BasicResource resource) {
        return resource.getObjectClass().contains("Location");
    }

    public Boolean isConnection(BasicResource resource) {
        return resource.getObjectClass().contains("Connection");
    }

    public Boolean isManagedResource(BasicResource resource) {
        return resource.getObjectClass().contains("ManagedResource");
    }

    public Boolean isCircuit(BasicResource resource) {
        return resource.getObjectClass().contains("Circuit");
    }

    private String startTimer(String operation) {
        String uid = UUID.randomUUID().toString();
        timers.put(uid, new TimerDto(uid, operation, System.currentTimeMillis()));
        return uid;
    }

    /**
     * Computa os timers...
     *
     * @param uid
     */
    private void endTimer(String uid) {
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

        }
    }

    public CircuitResource findCircuitResource(CircuitResource circuit) throws ResourceNotFoundException, IOException {
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

    
    public ResourceConnection findResourceConnection(ResourceConnection connection) throws ResourceNotFoundException, IOException {
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
     * Atualiza a conexão de um recurso
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
     * Atualiza um circuito
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
            this.eventManager.notifyEvent(event);
            return newResource;
        } finally {
            if (lockManager.isLocked()) {
                lockManager.unlock();
            }
            endTimer(timerId);
        }
    }

    public ArrayList<ResourceConnection> findCircuitPath(CircuitResource circuit) throws IOException {
        return arangoDao.findCircuitPath(circuit);
    }

    public void findWeakLinks(ArrayList<ResourceConnection> connections, FilterDTO filter) {
        //
        // Valida se temos dados de conexões...
        //

        if (!connections.isEmpty()) {
            //
            // Vamos validar se a regex encontra targets
            //

            ArrayList<BasicResource> targets = new ArrayList<>();

            Pattern p = Pattern.compile(filter.getTargetRegex());
            for (ResourceConnection connection : connections) {
                Matcher sourceMatcher = p.matcher(connection.getFrom().getName());
                if (sourceMatcher.matches()) {
                    if (!targets.contains(connection.getFrom())) {
                        targets.add(connection.getFrom());
                    }
                }

                Matcher targetMatched = p.matcher(connection.getTo().getName());
                if (targetMatched.matches()) {
                    if (!targets.contains(connection.getFrom())) {
                        targets.add(connection.getTo());
                    }
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

    public void test(String filter, Integer threads) {
        String aql = "for doc in inventory_connections "
                + " filter doc.from.name like @filter"
                + "    or  doc.to.name like @filter return doc";
        HashMap<String, Object> bindings = new HashMap<>();
        bindings.put("filter", filter);
        ArangoCursor<ResourceConnection> connections = arangoDao.filterConnectionByAQL(aql, bindings);
        DefaultTopology topology = new DefaultTopology();
        AtomicLong id = new AtomicLong(0L);
//        INetworkNode saida = createNode("OUTPUT", id.incrementAndGet(), topology);
//        saida.setEndPoint(true);

        connections.forEachRemaining(connection -> {
            if (connection.getFrom().getName().contains("m-br") && connection.getTo().getName().contains("m-br")) {
                INetworkNode from = topology.getNodeByName(connection.getFrom().getName());
                INetworkNode to = topology.getNodeByName(connection.getTo().getName());

                if (from == null) {
                    from = createNode(connection.getFrom().getName(), id.incrementAndGet(), topology);
                }

                if (to == null) {
                    to = createNode(connection.getTo().getName(), id.incrementAndGet(), topology);
                }

                if (from.getName().contains("gwc")) {
//                topology.addConnection(from, saida);
                    from.setEndPoint(true);
                } else {
                    from.setEndPoint(false);
                    from.addAttribute("erbCount", 0);
                }

                if (to.getName().contains("gwc")) {
                    to.setEndPoint(true);
//                topology.addConnection(to, saida);
                } else {
                    to.setEndPoint(false);
                    to.addAttribute("erbCount", 0);
                }

                if (from.getConnectionRelated(to).isEmpty()) {
                    INetworkConnection topologyConnection = topology.addConnection(from, to);
                }
            } else if (connection.getFrom().getName().contains("m-br")
                    || connection.getTo().getName().contains("m-br")) {
                logger.debug("Connection From: [" + connection.getFrom().getName() + "] TO:[" + connection.getTo().getName() + "]");
                BasicResource resource = null;
                Boolean goAhead = true;
                if (connection.getFrom().getName().contains("m-br")) {
                    resource = connection.getFrom();
//                    if (!connection.getTo().getName().contains("erb")) {
//                        goAhead = false;
//                    }
                } else {
                    resource = connection.getTo();
//                    if (!connection.getFrom().getName().contains("erb")) {
//                        goAhead = false;
//                    }
                }

                if (goAhead) {
                    INetworkNode node = topology.getNodeByName(resource.getName());
                    if (node == null) {
                        node = createNode(resource.getName(), id.incrementAndGet(), topology);
                        node.addAttribute("erbCount", 0);
                    }
                    if (node.getAttribute("erbCount") == null) {
                        node.addAttribute("erbCount", 0);
                    }
                    Integer count = (Integer) node.getAttribute("erbCount");
                    count++;
                    node.addAttribute("erbCount", count);
                }

            }
        });
        logger.debug("-------------------------------------------------------------");
        logger.debug("Topology Loaded! ");
        logger.debug("Topology Size:");
        logger.debug("   Nodes:" + topology.getNodes().size());
        logger.debug("   Connections:" + topology.getConnections().size());
        logger.debug("   EndPoints:" + topology.getEndPoints().size());
        for (INetworkNode node : topology.getEndPoints()) {
            logger.debug("       " + node.getName());
        }

        Long start = System.currentTimeMillis();
        logger.debug("-------------------------------------------------------------");
        logger.debug("Weak Nodes With 1 Connection or LESS:");
        logger.debug("-------------------------------------------------------------");
        List<INetworkNode> weak = null;
        if (threads > 1) {
            weak = topology.getImpactManager().getWeakNodes(1, false, threads, false);
        } else {
            weak = topology.getImpactManager().getWeakNodes(1, false, threads, false);
        }

        logger.debug("Found " + weak.size() + " Weak Nodes");
        for (INetworkNode n : weak) {
            logger.debug("  ::Weak " + n.getName() + " Connections size:" + n.getEndpointConnectionsCount() + " Impact Count:" + n.getImpactedNodes().size() + " ERBS:" + n.getAttribute("erbCount"));
            if (!n.getImpactedNodes().isEmpty()) {
                n.getImpactedNodes().forEach((k, v) -> {
                    logger.debug("      ::Node " + n.getName() + " Impacts:" + v.getName() + " ERBS: " + v.getAttribute("erbCount"));
                });

            }
        }

        Long end = System.currentTimeMillis();
        Long took = end - start;
        logger.debug("Process took: " + took + " ms With:" + threads + " Threads");
        try {
            connections.close();
        } catch (IOException ex) {
            System.out.println("OOOOOOO Deu RUIm!");
        }
        //
        // Uma vez usados todos os recursos, destroy tudo...
        //
        topology.destroyTopology();
    }

    private INetworkNode createNode(String name, Long id, ITopology topology) {
//        logger.debug("Created Node:" + name);
        INetworkNode node = new DefaultNode(name, id.intValue(), topology);
        return node;
    }

    public ArrayList<BasicResource> getNodesByFilter(FilterDTO filter, String domainName) throws DomainNotFoundException, ResourceNotFoundException, IOException {
        DomainDTO domain = getDomain(domainName);
        if (filter.getObjects().contains("nodes")) {
            return arangoDao.getNodesByFilter(filter, domain);
        }
        return null;
    }

    public ArrayList<ResourceConnection> getConnectionsByFilter(FilterDTO filter, String domainName) throws DomainNotFoundException, ResourceNotFoundException, IOException {
        DomainDTO domain = getDomain(domainName);
        if (filter.getObjects().contains("connections")) {
            return arangoDao.getConnectionsByFilter(filter, domain);
        }
        return null;
    }
}
