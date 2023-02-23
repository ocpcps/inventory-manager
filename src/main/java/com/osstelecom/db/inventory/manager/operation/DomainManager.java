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

import com.arangodb.entity.DocumentUpdateEntity;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import com.google.common.eventbus.Subscribe;
import com.osstelecom.db.inventory.manager.dao.CircuitResourceDao;
import com.osstelecom.db.inventory.manager.dao.DomainDao;
import com.osstelecom.db.inventory.manager.dao.ManagedResourceDao;
import com.osstelecom.db.inventory.manager.dao.ResourceConnectionDao;
import com.osstelecom.db.inventory.manager.dao.ServiceResourceDao;
import com.osstelecom.db.inventory.manager.dto.FilterDTO;
import com.osstelecom.db.inventory.manager.events.ConsumableMetricCreatedEvent;
import com.osstelecom.db.inventory.manager.events.DomainCreatedEvent;
import com.osstelecom.db.inventory.manager.exception.ArangoDaoException;
import com.osstelecom.db.inventory.manager.exception.DomainAlreadyExistsException;
import com.osstelecom.db.inventory.manager.exception.DomainNotFoundException;
import com.osstelecom.db.inventory.manager.exception.InvalidRequestException;
import com.osstelecom.db.inventory.manager.exception.ResourceNotFoundException;
import com.osstelecom.db.inventory.manager.listeners.EventManagerListener;
import com.osstelecom.db.inventory.manager.resources.BasicResource;
import com.osstelecom.db.inventory.manager.resources.ConsumableMetric;
import com.osstelecom.db.inventory.manager.resources.Domain;
import com.osstelecom.db.inventory.manager.resources.ManagedResource;
import com.osstelecom.db.inventory.manager.resources.ResourceConnection;
import com.osstelecom.db.inventory.topology.DefaultTopology;
import com.osstelecom.db.inventory.topology.ITopology;
import com.osstelecom.db.inventory.topology.impact.WeakNodesImpactManager;
import com.osstelecom.db.inventory.topology.node.DefaultNode;
import com.osstelecom.db.inventory.topology.node.INetworkNode;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;

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
public class DomainManager extends Manager {

    private Map<String, Domain> domains = new ConcurrentHashMap<>();

    @Autowired
    private LockManager lockManager;

    @Autowired
    private EventManagerListener eventManager;

    @Autowired
    private DomainDao domainDao;

    @Autowired
    private ManagedResourceDao managedResourceDao;

    @Autowired
    private ResourceConnectionDao resourceConnectionDao;

    @Autowired
    private ServiceResourceDao serviceResourceDao;

    @Autowired
    private CircuitResourceDao circuitResourceDao;

    private Map<String, Domain> updatingDomains = new ConcurrentHashMap<>();

    private Logger logger = LoggerFactory.getLogger(DomainManager.class);

    /**
     * Starts and fetch all available domains
     */
    @EventListener(ApplicationReadyEvent.class)
    private void onStartUp() throws ArangoDaoException {
        eventManager.registerListener(this);
        this.loadDomainsFromDb();

    }

    private void loadDomainsFromDb() throws ArangoDaoException {
        this.domainDao.getDomains().forEach(d -> {
            logger.debug("\tFound Domain: [{}] Atomic ID:[{}]", d.getDomainName(), d.getAtomicId());
            if (!this.domains.containsKey(d.getDomainName())) {
                this.domains.put(d.getDomainName(), d);
            }
        });
    }

    /**
     * Update the persistence layer with the current atomic id in the domain
     */
    @PreDestroy
    private void onShutDown() {
        this.domains.forEach((domainName, domain) -> {
            this.domainDao.updateDomain(domain);
            logger.debug("Domain: [{}] Updated", domainName);
        });
    }

    /**
     * Creates a Domain
     *
     * @param domain
     * @return
     * @throws DomainAlreadyExistsException
     */
    public Domain createDomain(Domain domain) throws DomainAlreadyExistsException {
        String timerId = startTimer("createDomain");
        try {

            lockManager.lock();
            domain = domainDao.createDomain(domain);
            if (domain.getAtomicId() == null) {
                domain.setAtomicId(0L);
            }
            domains.put(domain.getDomainName(), domain);
        } finally {
            if (lockManager.isLocked()) {
                lockManager.unlock();
            }
            endTimer(timerId);
        }
        DomainCreatedEvent domainCreatedEvent = new DomainCreatedEvent(domain);
        eventManager.notifyGenericEvent(domainCreatedEvent);
        return domain;
    }

    public Domain deleteDomain(Domain domain) throws DomainNotFoundException, ArangoDaoException, ResourceNotFoundException, IOException {
        try {
            lockManager.lock();
            domain = this.getDomain(domain.getDomainName());
            this.domains.remove(domain.getDomainName());
            return this.domainDao.deleteDomain(domain);
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
    public Domain getDomain(String domainName) throws DomainNotFoundException, ArangoDaoException {
        if (!domains.containsKey(domainName)) {

            //
            // Pega de novo
            //
            this.loadDomainsFromDb();

            if (!domains.containsKey(domainName)) {
                //
                // Isso é para gerar a lista de domains existentes... achei overkill
                //
                List<String> domainsList = domains.values().stream()
                        .map(Domain::getDomainName)
                        .collect(Collectors.toList());

                throw new DomainNotFoundException(
                        "Domain :[" + domainName + "] not found Available Domains are: [" + String.join(",", domainsList)
                        + "]");
            }
        }

        Domain domain = domains.get(domainName);
        this.calcStats(domain);
        return domain;
    }

    /**
     * Calcula as estatisticas do dominio a cada 5 minutos.
     *
     * @param domain
     */
    private void calcStats(Domain domain) {
        try {
            if (domain.getLastStatsCalc() == null) {
                logger.debug("Fisrt Time Stats for domain: [{}]", domain.getDomainName());
                domain.setResourceCount(managedResourceDao.getCount(domain));
                domain.setConnectionCount(resourceConnectionDao.getCount(domain));
                domain.setCircuitCount(circuitResourceDao.getCount(domain));
                domain.setServiceCount(serviceResourceDao.getCount(domain));
                domain.setLastStatsCalc(new Date());
                
                //
                // Sync DB
                //
                this.domainDao.updateDomain(domain);
            } else {
                Calendar cal = Calendar.getInstance();
                Date now = new Date();
                cal.setTime(now);
                cal.add(Calendar.MINUTE, -5);
                if (domain.getLastStatsCalc().before(cal.getTime())) {
                    logger.debug("TTL Time Stats for domain: [{}] Last Date: [{}]", domain.getDomainName(), domain.getLastStatsCalc());
                    //
                    // Estava levando um tempo e travando o front, vamos deixar multithread e assincrono
                    //
                    if (!this.updatingDomains.containsKey(domain.getDomainName())) {
                        this.updatingDomains.put(domain.getDomainName(), domain);

                        Thread t = new Thread(() -> {
                            Long start = System.currentTimeMillis();

                            try {
                                logger.debug("Starting Stats Calculation for Domain:[{}]", domain.getDomainName());
                                domain.setResourceCount(managedResourceDao.getCount(domain));
                                domain.setConnectionCount(resourceConnectionDao.getCount(domain));
                                domain.setCircuitCount(circuitResourceDao.getCount(domain));
                                domain.setServiceCount(serviceResourceDao.getCount(domain));
                                domain.setLastStatsCalc(new Date());
                                //
                                // Sync DB
                                //
                                domains.replace(domain.getDomainName(), domain);
                                domainDao.updateDomain(domain);

                            } catch (IOException | InvalidRequestException ex) {
                                logger.error("Generic Error Updating Domain Stats", ex);
                            } finally {
                                this.updatingDomains.remove(domain.getDomainName());
                                Long end = System.currentTimeMillis();
                                Long took = end - start;
                                logger.debug("Stats for domain: [{}] Last Date: [{}] Has Just Updated and Took: [{}] ms", domain.getDomainName(), domain.getLastStatsCalc(), took);

                            }
                        });
                        t.setName(domain.getDomainName() + "-stats-thread");
                        t.start();
                    } else {
                        logger.warn("Already Processing Stats for Domain:[{}]", domain.getDomainName());
                    }

                }
            }
        } catch (IOException ex) {
            //
            // Omite o Error
            //
            logger.error("Generic Error Updating Domain Stats", ex);
        } catch (InvalidRequestException ex) {
            logger.error("Failed to Get Count", ex);
        }
    }

    /**
     * Get the list of all domains.
     *
     * @return
     */
    public List<Domain> getAllDomains() {
        List<Domain> result = new ArrayList<>();

        this.domains.forEach((name, domain) -> {
            this.calcStats(domain);
            result.add(domain);
        });
        return result;
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
            eventManager.notifyGenericEvent(event);
            return metric;
        } finally {
            if (lockManager.isLocked()) {
                lockManager.unlock();
            }
            endTimer(timerId);
        }

    }

    public Domain updateDomain(Domain domain) {
        DocumentUpdateEntity<Domain> result = this.domainDao.updateDomain(domain);
        this.domains.replace(domain.getDomainName(), domain);
        return result.getNew();
    }

    /**
     * Called When a New Domain is Created
     *
     * @param domain
     */
    @Subscribe
    public void onDomainCreatedEvent(DomainCreatedEvent domain) {
    }

    @Subscribe
    public void onConsumableMetricCreatedEvent(ConsumableMetricCreatedEvent metric) {
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
            DefaultTopology topology = new DefaultTopology(new WeakNodesImpactManager());
            AtomicLong localId = new AtomicLong(0L);
            INetworkNode target = createNode(aPoint.getId(), localId.incrementAndGet(), topology);
            //
            // this is the A Point from the circuit, will mark as endPoint. meaning all
            // nodes must reach this one
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

                if (connection.getOperationalStatus().equalsIgnoreCase("UP")) {
                    connection.setOperationalStatus("Up");
                    topology.addConnection(from, to, "Connection: " + connection.getId());
                    logger.debug("Connection from:[{}] To:[{}] is Up", connection.getFrom().getNodeAddress(), connection.getTo().getNodeAddress());

                } else if (connection.getOperationalStatus().equalsIgnoreCase("DOWN")) {
                    logger.debug("Connection from:[{}] To:[{}] is Down", connection.getFrom().getNodeAddress(), connection.getTo().getNodeAddress());
                }

                // topology.addConnection(to, from, connection.getId() + ".A");
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
            logger.debug("Found [{}] Unrecheable Nodes IN: {} ms", weak.size(), tookTime);

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
                DefaultTopology topology = new DefaultTopology(new WeakNodesImpactManager());

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

    private INetworkNode createNode(String name, Long id, ITopology topology) {
        INetworkNode node = new DefaultNode(name, id.intValue(), topology);
        return node;
    }

    public String getDomainNameFromId(String id) {
        String[] payLoad = id.split("/");
        String collectionName = payLoad[0];
        payLoad = collectionName.split("_");
        return payLoad[0];
    }

    // public void test(String filter, Integer threads) {
    // String aql = "for doc in inventory_connections "
    // + " filter doc.from.name like @filter"
    // + " or doc.to.name like @filter return doc";
    // HashMap<String, Object> bindings = new HashMap<>();
    // bindings.put("filter", filter);
    // ArangoCursor<ResourceConnection> connections =
    // arangoDao.filterConnectionByAQL(aql, bindings);
    // DefaultTopology topology = new DefaultTopology();
    // AtomicLong id = new AtomicLong(0L);
    //// INetworkNode saida = createNode("OUTPUT", id.incrementAndGet(), topology);
    //// saida.setEndPoint(true);
    //
    // connections.forEachRemaining(connection -> {
    // if (connection.getFrom().getName().contains("m-br") &&
    // connection.getTo().getName().contains("m-br")) {
    // INetworkNode from = topology.getNodeByName(connection.getFrom().getName());
    // INetworkNode to = topology.getNodeByName(connection.getTo().getName());
    //
    // if (from == null) {
    // from = createNode(connection.getFrom().getName(), id.incrementAndGet(),
    // topology);
    // }
    //
    // if (to == null) {
    // to = createNode(connection.getTo().getName(), id.incrementAndGet(),
    // topology);
    // }
    //
    // if (from.getName().contains("gwc")) {
    //// topology.addConnection(from, saida);
    // from.setEndPoint(true);
    // } else {
    // from.setEndPoint(false);
    // from.addAttribute("erbCount", 0);
    // }
    //
    // if (to.getName().contains("gwc")) {
    // to.setEndPoint(true);
    //// topology.addConnection(to, saida);
    // } else {
    // to.setEndPoint(false);
    // to.addAttribute("erbCount", 0);
    // }
    //
    // if (from.getConnectionRelated(to).isEmpty()) {
    // INetworkConnection topologyConnection = topology.addConnection(from, to);
    // }
    // } else if (connection.getFrom().getName().contains("m-br")
    // || connection.getTo().getName().contains("m-br")) {
    // logger.debug("Connection From: [" + connection.getFrom().getName() + "] TO:["
    // + connection.getTo().getName() + "]");
    // BasicResource resource = null;
    // Boolean goAhead = true;
    // if (connection.getFrom().getName().contains("m-br")) {
    // resource = connection.getFrom();
    //// if (!connection.getTo().getName().contains("erb")) {
    //// goAhead = false;
    //// }
    // } else {
    // resource = connection.getTo();
    //// if (!connection.getFrom().getName().contains("erb")) {
    //// goAhead = false;
    //// }
    // }
    //
    // if (goAhead) {
    // INetworkNode node = topology.getNodeByName(resource.getName());
    // if (node == null) {
    // node = createNode(resource.getName(), id.incrementAndGet(), topology);
    // node.addAttribute("erbCount", 0);
    // }
    // if (node.getAttribute("erbCount") == null) {
    // node.addAttribute("erbCount", 0);
    // }
    // Integer count = (Integer) node.getAttribute("erbCount");
    // count++;
    // node.addAttribute("erbCount", count);
    // }
    //
    // }
    // });
    // logger.debug("-------------------------------------------------------------");
    // logger.debug("Topology Loaded! ");
    // logger.debug("Topology Size:");
    // logger.debug(" Nodes:" + topology.getNodes().size());
    // logger.debug(" Connections:" + topology.getConnections().size());
    // logger.debug(" EndPoints:" + topology.getEndPoints().size());
    // for (INetworkNode node : topology.getEndPoints()) {
    // logger.debug(" " + node.getName());
    // }
    //
    // Long start = System.currentTimeMillis();
    // logger.debug("-------------------------------------------------------------");
    // logger.debug("Weak Nodes With 1 Connection or LESS:");
    // logger.debug("-------------------------------------------------------------");
    // List<INetworkNode> weak = null;
    // if (threads > 1) {
    // weak = topology.getImpactManager().getWeakNodes(1, false, threads, false);
    // } else {
    // weak = topology.getImpactManager().getWeakNodes(1, false, threads, false);
    // }
    //
    // logger.debug("Found " + weak.size() + " Weak Nodes");
    // for (INetworkNode n : weak) {
    // logger.debug(" ::Weak " + n.getName() + " Connections size:" +
    // n.getEndpointConnectionsCount() + " Impact Count:" +
    // n.getImpactedNodes().size() + " ERBS:" + n.getAttribute("erbCount"));
    // if (!n.getImpactedNodes().isEmpty()) {
    // n.getImpactedNodes().forEach((k, v) -> {
    // logger.debug(" ::Node " + n.getName() + " Impacts:" + v.getName() + " ERBS: "
    // + v.getAttribute("erbCount"));
    // });
    //
    // }
    // }
    //
    // Long end = System.currentTimeMillis();
    // Long took = end - start;
    // logger.debug("Process took: " + took + " ms With:" + threads + " Threads");
    // try {
    // connections.close();
    // } catch (IOException ex) {
    // System.out.println("OOOOOOO Deu RUIm!");
    // }
    // //
    // // Uma vez usados todos os recursos, destroy tudo...
    // //
    // topology.destroyTopology();
    // }
}
