/*
 * Copyright (C) 2021 Lucas Nishimura <lucas.nishimura@gmail.com>
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
package com.osstelecom.db.inventory.topology.impact;

import com.osstelecom.db.inventory.topology.connection.INetworkConnection;
import com.osstelecom.db.inventory.topology.node.INetworkNode;
import com.osstelecom.db.inventory.topology.node.SourceTargetWrapper;
import com.osstelecom.db.inventory.topology.ITopology;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.slf4j.LoggerFactory;

/**
 * Está uma bagunça, mas tem casos bem simples de varredura de rede
 *
 * @author Nishisan
 */
public class DefaultImpactManagerImpl extends ImpactManagerAbs {

    private Boolean working = false;

    private LinkedBlockingQueue<SourceTargetWrapper> weakQueue = new LinkedBlockingQueue<>();
    private ArrayList<Thread> threadPool = new ArrayList<>();
    private org.slf4j.Logger logger = LoggerFactory.getLogger(DefaultImpactManagerImpl.class);
    private Boolean debug = true;
    private Integer runningThreads = 0;
    private AtomicLong weakDone = new AtomicLong(0L);
    private ConcurrentHashMap<String, CalculaTionWeakThread> workingThreads = new ConcurrentHashMap<>();
    private String joininngThread = "N/A";

    public DefaultImpactManagerImpl(ITopology topology) {
        super(topology);
    }

    public DefaultImpactManagerImpl() {
        super();
    }
    
    @Override
    public ArrayList<INetworkConnection> getUnreachableConnections() {
        Long start = System.currentTimeMillis();
        ArrayList<INetworkConnection> unreacheableConnections = new ArrayList<>();
        for (INetworkNode n : getUnreacheableNodes()) {
            for (INetworkConnection c : n.getConnections()) {
                if (c.getActive()) {
                    if (!unreacheableConnections.contains(c)) {
                        unreacheableConnections.add(c);
                    }
                }
            }
        }
        Long end = System.currentTimeMillis();
        Long took = end - start;
        return unreacheableConnections;
    }

    /**
     * Find nodes that does not reach the endpoint or endpoints
     *
     * @return
     */
    @Override
    public ArrayList<INetworkNode> getUnreacheableNodes() {
        Long start = System.currentTimeMillis();
        ArrayList<INetworkNode> active = new ArrayList<>();
        ArrayList<INetworkNode> allNodes = new ArrayList<>(this.getTopology().getNodes());
        for (INetworkNode endPoint : this.getTopology().getEndPoints()) {
            if (endPoint.getActive()) {
                active.addAll(walkNodes(endPoint));
            }
        }
        Long end = System.currentTimeMillis();
        Long took = end - start;
//        System.out.println(":::getUnreacheableNodes() Took:" + took + " ms");
        allNodes.removeAll(active);

        return allNodes;
    }

    /**
     * Retorna a lista de nodes que alcançam as saídas.
     *
     * @param node
     * @return
     */
    private ArrayList<INetworkNode> walkNodes(INetworkNode node) {
        ArrayList<INetworkNode> nodes = new ArrayList<>();
        walkNodes(node, null, nodes);
        return nodes;
    }

    /**
     * Experimental
     *
     * @param startNode
     * @param walked
     * @deprecated
     */
    private void propagate(INetworkNode startNode, ConcurrentHashMap<String, INetworkConnection> walked) {
        if (walked == null) {
            walked = new ConcurrentHashMap<>();
        }

        for (INetworkConnection connection : startNode.getConnections()) {
            INetworkNode next;
            if (connection.getTarget().equals(startNode)) {
                next = connection.getSource();
                if (!walked.containsKey(connection.getUuid())) {
                    walked.put(connection.getUuid(), connection);
                    next.addEndPointConnection(connection);
                    System.out.println(":::::From: " + startNode.getName() + " To " + next.getName() + " C:" + next.getEndpointConnectionsCount());
                    propagate(next, walked);

                } else {
                    System.out.println("Aborting:::" + connection.getName());
                }
            } else {
                next = connection.getTarget();
            }

        }
    }

    /**
     * Retorna a lista de nodes que alcançam as saídas.
     *
     * @param node
     * @return
     */
    private void walkNodes(INetworkNode node, ConcurrentHashMap<String, ConcurrentHashMap<String, INetworkConnection>> walkedNodes, ArrayList<INetworkNode> nodes) {
        if (walkedNodes == null) {
            walkedNodes = new ConcurrentHashMap<>();
        }

        if (!walkedNodes.containsKey(node.getUuid())) {
            walkedNodes.put(node.getUuid(), new ConcurrentHashMap<>());
            if (node.getConnections().size() > 0) {
                //
                // forConnectionStart
                //
//                node.getConnections().parallelStream().forEachOrdered((c) -> {
//                });
                for (INetworkConnection c : node.getConnections()) {
                    if (!walkedNodes.get(node.getUuid()).containsKey(c.getUuid())) {
                        walkedNodes.get(node.getUuid()).put(c.getUuid(), c);
                        if (c.getActive()) {
                            if (node.getActive()) {
                                if (!nodes.contains(node)) {
                                    nodes.add(node);
                                }

                                if (node != c.getTarget()) {
                                    //
                                    // Conexao de saída do node
                                    //
                                    walkNodes(c.getTarget(), walkedNodes, nodes);
                                } else {
                                    //
                                    // Conexão de entrada do node
                                    //
                                    walkNodes(c.getSource(), walkedNodes, nodes);
                                }
                            }
                        }
                    }
                }

            } else {
                //
                // Node orfão
                //
                nodes.add(node);
            }
        }
    }

    private INetworkNode getConnectionEndPoint(INetworkConnection connection) {
        if (connection.getTarget().endPoint()) {
            return connection.getTarget();
        }
        if (connection.getSource().endPoint()) {
            return connection.getSource();
        } else {
            return null;
        }
    }

    /**
     * @deprecated @param currentNode
     * @param startedNode
     * @param fromNode
     * @param sourceConnection
     * @param mainMapWalk
     * @param level
     * @param path
     */
    private void processNodeEndPoints(INetworkNode currentNode,
            INetworkNode startedNode,
            INetworkNode fromNode,
            INetworkConnection sourceConnection,
            ConcurrentHashMap<String, ConcurrentHashMap<String, INetworkConnection>> mainMapWalk,
            Integer level, String path) {
        level++;
        if (startedNode == null) {
            startedNode = currentNode;
        }

        if (mainMapWalk == null) {
            mainMapWalk = new ConcurrentHashMap<>();
        }

        if (!mainMapWalk.containsKey(currentNode.getUuid())) {
            mainMapWalk.put(currentNode.getUuid(), new ConcurrentHashMap<>());
        }

        for (INetworkConnection connection : currentNode.getConnections()) {
            sourceConnection = connection;
            if (!mainMapWalk.get(currentNode.getUuid()).containsKey(connection.getUuid())) {
                mainMapWalk.get(currentNode.getUuid()).put(connection.getUuid(), connection);
                INetworkNode next = currentNode.getOtherSide(connection);
                if (startedNode.getConnections().contains(sourceConnection)) {
                    System.out.print("M: OI " + next.getName() + " De:" + currentNode.getName() + " ORIGINATED:" + startedNode.getName() + "/" + sourceConnection.getUuid() + " ");
                    if (getConnectionEndPoint(connection) != null) {
                        startedNode.addEndPointConnection(sourceConnection);
                        System.out.println(" (x)");
                    } else {
                        System.out.println(" ( )");
                    }
                }
                if (fromNode == null) {
                    processNodeEndPoints(next, startedNode, currentNode, sourceConnection, mainMapWalk, level, path);
                } else {
                    processNodeEndPoints(next, startedNode, currentNode, sourceConnection, mainMapWalk, level, path);
                }

            } else {
                INetworkNode end = getConnectionEndPoint(sourceConnection);
                if (end != null) {
                    System.out.print("E: OI " + end.getName() + " De:" + currentNode.getName() + " ORIGINATED:" + startedNode.getName() + "/" + sourceConnection.getUuid() + " ");
                    System.out.println(" (x)");
                    startedNode.addEndPointConnection(sourceConnection);
                }
            }
        }

    }

    public List<INetworkNode> getWeakNodes(Integer connLimit, ArrayList<INetworkNode> nodes) {
        return this.getWeakNodes(connLimit, false, 1, false, nodes);
    }

    @Override
    public List<INetworkNode> getWeakNodes(Integer connLimit, Boolean all, Integer threadCount, Boolean useCache) {
        return this.getWeakNodes(connLimit, all, threadCount, useCache, null);
    }

    /**
     * If Working get the Process Complete %
     *
     * @return
     */
    public Float getCurrentWorkPerc() {
        weakDone.incrementAndGet();
        Long total = weakQueue.size() + weakDone.get() + workingThreads.size();
        Float percDone = weakDone.get() / total.floatValue();
        percDone = percDone * 100;
        return percDone;
    }

    /**
     *
     * @param connLimit
     * @param all
     * @param threadCount
     * @param useCache
     * @param nodes
     * @return
     */
    public List<INetworkNode> getWeakNodes(Integer connLimit, Boolean all, Integer threadCount, Boolean useCache, ArrayList<INetworkNode> nodes) {

        weakDone.set(0L);
        //
        // Trata a quantidade de threads, não pode ser maior que a quantidade
        //  de nós a serem testadas
        //
        if (threadCount > this.getTopology().getNodes().size()) {
            threadCount = this.getTopology().getNodes().size() - 1;
        }

        this.working = true;
        Long start = System.currentTimeMillis();

        this.getTopology().resetDynamicValues();

        //
        // Esses nunca vão chegar lá....
        // Já elege pelo DFS os que não tocam nenhuma saída...
        //
        ArrayList<INetworkNode> alreadyDown = this.getUnreacheableNodes();
        logger.debug("Removing: " + alreadyDown.size() + " Because Already Unreacheable");
        //
        // Cria uma queue, mas a gente já nem calcula aqueles que não chegam a nenhum endpoint..
        //
        if (nodes == null) {
            for (INetworkNode node : this.getTopology().getNodes()) {
                for (INetworkNode target : this.getTopology().getEndPoints()) {
                    if (!alreadyDown.contains(node)) {
                        if (!node.endPoint()) {
                            SourceTargetWrapper w = new SourceTargetWrapper();
                            w.setSource(node);
                            w.setTarget(target);
                            w.setLimit(connLimit);
                            w.setUseCache(useCache);
                            weakQueue.add(w);
                        }
                    }
                }
            }
        } else {
            for (INetworkNode node : this.getTopology().getNodes()) {
                if (nodes.contains(node)) {
                    for (INetworkNode target : this.getTopology().getEndPoints()) {
                        if (!node.endPoint()) {
                            SourceTargetWrapper w = new SourceTargetWrapper();
                            w.setSource(node);
                            w.setTarget(target);
                            w.setLimit(connLimit);
                            w.setUseCache(useCache);
                            weakQueue.add(w);
                        }
                    }
                }
            }
        }
        //
        // Note that this is a local thread list, not really a pool
        //

        weakQueue.forEach(o -> {
            logger.debug("Queue From:[" + o.getSource().getName() + "] To: [" + o.getTarget().getName() + "] Created");
        });

        ArrayList<Thread> threadPool = new ArrayList<>();
        for (int x = 0; x < threadCount; x++) {
            String threadName = "WEAK-" + x;
            CalculaTionWeakThread thread = new CalculaTionWeakThread(useCache, threadName);
            logger.debug("Thread: [{}] Created", threadName);
            threadPool.add(new Thread(thread, threadName));
            workingThreads.put(threadName, thread);
        }
        Thread stats = new Thread(new StatsThread());

        //
        // Inicia uma Thread de Processamento
        //
        for (Thread t : threadPool) {
            t.start();
        }

        stats.start();

        for (Thread t : threadPool) {
            try {
                joininngThread = t.getName();
                t.join();
                logger.debug("Thread:" + t.getName() + " Joined");
                workingThreads.remove(t.getName());

                if (workingThreads.isEmpty()) {
                    break;
                }
            } catch (InterruptedException ex) {
                Logger.getLogger(DefaultImpactManagerImpl.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        this.working = false;
        logger.debug("Working All DONE, stopping stats Threads");
        stats.interrupt();
        List<INetworkNode> lowConnectedDevices = null;

        logger.debug("Working All DONE, Collecting Network Data");
        if (nodes == null) {
            lowConnectedDevices
                    = this.getTopology()
                            .getNodes()
                            .parallelStream()
                            .filter(n -> n.getEndpointConnectionsCount() <= connLimit && !n.endPoint())
                            .collect(Collectors.toList());
        } else {
            lowConnectedDevices = nodes.parallelStream().filter(n -> n.getEndpointConnectionsCount() <= connLimit && !n.endPoint())
                    .collect(Collectors.toList());
        }

        if (all) {
            logger.debug("WEAK Found " + lowConnectedDevices.size() + " Nodes, processing impact");
            //
            // Second Stage: identificar elementos impactados
            //
            ArrayList<INetworkNode> allImpactedNodes = new ArrayList<>();
            for (INetworkNode node : lowConnectedDevices) {
                node.disable();
                for (INetworkConnection connection : node.getConnections()) {

                    connection.disable();

                    this.getTopology().getConnectionByName(connection.getName()).disable();

                }

                ArrayList<INetworkNode> impacted = this.getUnreacheableNodes();
                for (INetworkNode impactedBy : impacted) {
                    if (nodes != null) {
                        if (nodes.contains(impactedBy)) {
                            if (!allImpactedNodes.contains(impactedBy)) {
                                allImpactedNodes.add(impactedBy);
                            }
                        }
                    } else {
                        if (!allImpactedNodes.contains(impactedBy)) {
                            allImpactedNodes.add(impactedBy);
                        }
                    }
                }

                node.enable();
                for (INetworkConnection connection : node.getConnections()) {
                    connection.enable();
                    this.getTopology().getConnectionByName(connection.getName()).enable();
                }
            }

            return allImpactedNodes;
        } else {
//            ArrayList<INetworkNode> alreadyDown = new ArrayList<>();
//            alreadyDown = this.getUnreacheableNodes();
            logger.debug("WEAK Found " + lowConnectedDevices.size() + " Nodes, processing impact");
//            for (INetworkNode node : lowConnectedDevices) {
//                node.disable();
//                for (INetworkConnection connection : node.getConnections()) {
//                    connection.disable();
//                    this.getTopology().getConnectionByName(connection.getName()).disable();
//
//                }
//
//                ArrayList<INetworkNode> impacted = this.getUnreacheableNodes();
//
////                node.addImpactList(node);
//                if (!impacted.isEmpty()) {
//                    for (INetworkNode impactedBy : impacted) {
//                        if (!alreadyDown.contains(impactedBy)) {
//                            if (!node.equals(impactedBy)) {
//                                node.addImpactList(impactedBy);
//                            }
//                        }
//                    }
//                }
//
//                node.enable();
//                for (INetworkConnection connection : node.getConnections()) {
//                    connection.enable();
//                    this.getTopology().getConnectionByName(connection.getName()).enable();
//                }
//            }

        }

        Long end = System.currentTimeMillis();
        Long took = end - start;

        return lowConnectedDevices;
    }

    private synchronized SourceTargetWrapper getWork() {
        if (!weakQueue.isEmpty()) {
            return weakQueue.poll();
        } else {
            return null;
        }
    }

    /**
     * Uma tentativa não elegante, a classe precisa ser refeita deixando ela
     * "thread safe"
     */
    private class CalculaTionWeakThread implements Runnable {

        private final String myName;

        private Boolean printPaths = false;
        private final Boolean useCache;
        private AtomicLong counter = new AtomicLong(0L);
        private AtomicLong last = new AtomicLong(0L);
        private SourceTargetWrapper workload;
        private Long start;
        private Boolean mayIStop = false;
        private Integer step;
        private Boolean ended = false;

        public Long getCounter() {
            Long actual = counter.get();
            Long lastCounter = last.get();
            Long result = actual - lastCounter;
            last.set(actual);

            return result;
        }

        public Long getActual() {
            return counter.get();
        }

        public CalculaTionWeakThread(Boolean useCache, String myName) {
            this.useCache = useCache;
            this.myName = myName;
        }

        public SourceTargetWrapper getCurrentWork() {
            return this.workload;
        }

        private String getSpaces(int level) {
            String s = "";
            for (int x = 0; x < level; x++) {
                s += "  ";
            }
            return s;
        }

        public Long getStartTime() {
            return this.start;
        }

        public void youMayStop() {
            if (!this.mayIStop) {
                this.mayIStop = true;
            }
        }

        public Boolean canIStop() {
            return this.mayIStop;
        }

        @Override
        public void run() {

            Long totalWorked = 0L;
            logger.debug("Weak Calculation Thread Started...Queue Size is:" + weakQueue.size());
            runningThreads++;
            step = -1;
            while (!weakQueue.isEmpty()) {
                step = 0;
                this.workload = getWork();
                totalWorked++;
                if (workload != null) {
                    start = System.currentTimeMillis();
                    if (workload.getSource().getEndpointConnectionsCount() <= workload.getLimit()) {

                        logger.debug("Starting to process: [" + workload.getSource().getName() + "](" + workload.getSource().getEndpointConnectionsCount() + ")->[" + workload.getTarget().getName() + "] With :" + workload.getSource().getConnections().size() + " Connections and " + workload.getSource().getUnprobedConnections().size() + " Unprobed Connections");

                        if (workload.getSource().getConnectionCount() <= workload.getLimit()) {
                            //
                            // Já é fraco por natureza...
                            //
//                          step = 1;
//                          walkAllPaths(workload.getSource(), workload.getTarget(), workload.getLimit());
                        } else {
                            step = 1;
                            this.mayIStop = false;
                            walkAllPaths(workload.getSource(), workload.getTarget(), workload.getLimit());
                        }
                        Long end = System.currentTimeMillis();
                        Long took = end - start;
                        logger.debug("Took " + took + " ms to process: " + workload.getSource().getName() + "(" + workload.getSource().getEndpointConnectionsCount() + ") Queue is:" + weakQueue.size() + " [" + String.format("%.2f", getCurrentWorkPerc()) + "] % Stopped:[" + this.mayIStop + "]");
                        logger.debug("--------------------------------------------------------------------------");
                    } else {
                        logger.debug("Skipped to process: [" + workload.getSource().getName() + "](" + workload.getSource().getEndpointConnectionsCount() + ")->[" + workload.getTarget().getName() + "] With :" + workload.getSource().getConnections().size() + " Connections and " + workload.getSource().getUnprobedConnections().size() + " Unprobed Connections");

                    }
//                    workload.getSource().setUnvisited();
                }

            }

            //
            // Make sure we get all unprobed Nodes
            //
            runningThreads--;
            ended = true;
            logger.debug("--------------------------------------------------------------------------");
            logger.debug("[" + this.myName + "] Weak Thread is Done xD  Workded on:" + totalWorked + " Tested:[" + counter.get() + "] Interations");
            logger.debug("--------------------------------------------------------------------------");
            return;
        }

        public Boolean getEnded() {
            return ended;
        }

        /**
         * Caminha pot todos os possiveis caminhos de ponto A para ponto B
         *
         * @param sourceNode
         * @param targetNode
         */
        public void walkAllPaths(INetworkNode sourceNode, INetworkNode targetNode, Integer limit) {
            ArrayList<INetworkNode> pathList = new ArrayList<>();
            pathList.add(sourceNode);
            step = 2;
            if (useCache) {
                while (sourceNode.getUnprobedConnections().size() > 0) {
                    step = 2;
                    if (!mayIStop) {
                        computeAllPossiblePathsToTarget(sourceNode, targetNode, pathList, limit, 0);
                        step = 3;
                    } else {
//                        logger.debug("STOP FOR:" + sourceNode.getName());
                        break;
                    }
                }
            } else {
                if (!mayIStop) {
                    step = 2;
                    computeAllPossiblePathsToTarget(sourceNode, targetNode, pathList, limit, 0);
                    step = 3;
                }
            }

            //
            // Ajuda a salvar memória xD, comentado em debug!
            //
            pathList.clear();
        }

        /**
         * Calcula todos os possiveis caminhos salvando em uma lista os caminhos
         * utilizados xD
         *
         * @param source
         * @param target
         * @param pathList
         */
        public void computeAllPossiblePathsToTarget(INetworkNode source, INetworkNode target, ArrayList<INetworkNode> pathList, Integer limit, Integer level) {

//            if (mayIStop) {
//                return;
//            }
            step = 4;
            counter.incrementAndGet();

            if (source.equals(target)) {
                markSegmentAsReacheable(pathList, level, false);
                return;
            }

            source.setVisited(Thread.currentThread().getName());
            level++;
            if (debug) {
                logger.debug(getSpaces(level) + "Processing Node:" + source.getName() + " Connections to Endpoint: [" + source.getEndpointConnectionsCount() + "/" + source.getConnections().size() + "]");
            }
            for (INetworkConnection connection : source.getConnections()) {
                if (debug) {
                    logger.debug(getSpaces(level) + " Processing Node:" + source.getName() + " Connection: " + connection.getName());
                }
                step = 5;
                //
                // Só leva em considerações nós ativos
                //
                if (connection.getActive()) {
                    //
                    // Cheguei em uma conexão...e um nó oposto...
                    //
                    INetworkNode other = source.getOtherSide(connection);
                    //
                    // Vamos ver se este nó possui conexões que chegam na saida...
                    //

                    if (other != null) {
                        if (!other.isVisited(Thread.currentThread().getName())) {
                            pathList.add(other);

                            if (mayIStop) {
                                if (debug) {
                                    logger.debug(getSpaces(level) + " mayIStop IS TRUE FOR:" + source.getName() + " To :" + other.getName() + "  to Endpoint: [" + other.getEndpointConnectionsCount(source) + "] Unvisted:" + source.getUnVisitedConnections(Thread.currentThread().getName()).size());
                                }
                                markSegmentAsReacheable(pathList, level, false);
                                return;
                            }

                            if (debug) {
                                logger.debug(getSpaces(level) + " Connection From:" + source.getName() + " To :" + other.getName() + "  to Endpoint: [" + other.getEndpointConnectionsCount(source) + "] Unvisted:" + source.getUnVisitedConnections(Thread.currentThread().getName()).size());
                            }

                            if (!useCache) {
//                                if (source.getEndpointConnectionsCount() < limit) {
                                computeAllPossiblePathsToTarget(other, target, pathList, limit, level);
                                source.markConnectionAsProbed(connection);

                            } else {
                                //
                                // Esta é a parte que transfere e reaproveita o conhecimento, parece que está quase ok no monothread :)
                                // 
//                                Long outputSolutions = other.getEndpointConnectionsCount(source);
//                                if (outputSolutions >= limit) {
//
//                                    
//
//                                } else {
//                                    computeAllPossiblePathsToTarget(other, target, pathList, limit, level);
//                                }

                                if (!other.getSolutionsExcept(source).isEmpty()) {
                                    if (debug) {
                                        logger.debug(getSpaces(level) + " Connection From:" + source.getName() + " To :" + other.getName() + "  Leads to Endpoint By Cache");
                                    }
                                    markSegmentAsReacheable(pathList, level, true);
                                } else {
                                    computeAllPossiblePathsToTarget(other, target, pathList, limit, level);
                                }

                                source.markConnectionAsProbed(connection);
                            }
                            pathList.remove(other);
                        } else {
                            if (debug) {
                                logger.debug(getSpaces(level) + "  --> Skipping:" + other.getName() + "(" + other.getEndpointConnectionsCount(source) + "/" + other.getConnectionCount() + ") From:[" + source.getName() + "] Already Visited");
                            }
                            //
                            // Já Visitei. Este segmento, tem conexões com o EndPoint ? 
                            //
                            if (other.getEndpointConnectionsCount(other) > 0) {
                                pathList.add(other);
                                markSegmentAsReacheable(pathList, level, false);
                                pathList.remove(other);
                                if (debug) {
                                    logger.debug(getSpaces(level) + "  Connection:" + connection.getName() + " OK TO ENDPOINT!!!");
                                }
                            } else {
                                if (debug) {
                                    logger.debug(getSpaces(level) + "  Connection:" + connection.getName() + " NOOOOT OK TO ENDPOINT!!!");
                                }

//                                if (other.getConnectionCount() > other.getProbedConnectionsCount()) {
//                                    computeAllPossiblePathsToTarget(other, target, pathList, limit, level);
//                                    source.markConnectionAsProbed(connection);
//                                }
                            }
//                            return;
                        }
                    } else {
                        if (debug) {
                            logger.debug(getSpaces(level) + " Other Side is null from:[" + source.getName() + "]");
                        }
                    }

                } else {
                    if (debug) {
                        logger.debug(getSpaces(level) + "Inactive Connection Found! IN:[" + source.getName() + "]");
                    }
                }

            }

            source.setUnvisited(Thread.currentThread().getName());
            if (debug) {
                logger.debug(getSpaces(level) + "Done:" + source.getName() + " Has:" + source.getEndpointConnectionsCount());
            }

        }

        private synchronized void markSegmentAsReacheable(ArrayList<INetworkNode> pathList, Integer level, Boolean cached) {
            String pathStr = "";

            for (Integer x = 0; x < pathList.size(); x++) {
                INetworkNode sourceNode = pathList.get(x);
                pathStr += sourceNode.getName() + ".";
                if (x + 1 < pathList.size()) {
                    INetworkNode targetNode = pathList.get(x + 1);

                    ArrayList<INetworkConnection> sourceTargetConnection = sourceNode.getConnectionRelated(targetNode);
                    if (sourceTargetConnection.size() == 1) {
                        //
                        // Adiciona no nó a conexão que leva a saída... ou seja já foi processada...
                        //
                        for (INetworkConnection endPointConnection : sourceTargetConnection) {
                            //
                            // End point Connection
                            //
                            if (useCache) {
                                if (sourceTargetConnection.size() > 1) {
                                    if (targetNode.getEndpointConnectionsCount(sourceNode) > 0L) {
                                        sourceNode.addEndPointConnection(endPointConnection);
                                        if (debug) {
                                            logger.debug(getSpaces(level) + " Segment:" + sourceNode.getName() + " (" + sourceNode.getEndpointConnectionsCount() + ") TO:" + targetNode.getName() + " (" + targetNode.getEndpointConnectionsCount() + ")");
                                        }
                                    } else {
//                                    sourceNode.addEndPointConnection(endPointConnection);
//                                    logger.warn(getSpaces(level) + " Segment:" + sourceNode.getName() + " (" + sourceNode.getEndpointConnectionsCount() + ") TO:" + targetNode.getName() + " (" + targetNode.getEndpointConnectionsCount(sourceNode)+ ") Was Skipped");

                                        if (debug) {
                                            logger.warn(getSpaces(level) + " Segment:" + sourceNode.getName() + " (" + sourceNode.getEndpointConnectionsCount() + ") TO:" + targetNode.getName() + " (" + targetNode.getEndpointConnectionsCount() + ") Was Skipped");
                                        }
                                    }
                                } else {
                                    sourceNode.addEndPointConnection(endPointConnection);
                                }
                            } else {
                                sourceNode.addEndPointConnection(endPointConnection);
                                if (debug) {
                                    logger.debug(getSpaces(level) + " Segment:" + sourceNode.getName() + " (" + sourceNode.getEndpointConnectionsCount() + ") TO:" + targetNode.getName() + " (" + targetNode.getEndpointConnectionsCount() + ")");
                                }
                            }
                        }

                    }
                }

            }

            if (!cached) {
                //
                // Avaliar impacto em memória.
                //
                pathList.get(0).addSolution(pathList);
            }

            if (debug) {
                logger.debug("---------------------------------------------------------------------------------------------");
                logger.debug(getSpaces(level) + "Found Path:[" + pathStr + "] From: " + pathList.get(0).getName() + " To:" + pathList.get(1).getName());
                logger.debug("----------------------------------------------------------------------------------------------");
            }
        }

        private String getNodeAddress(ArrayList<INetworkNode> nodes) {
            String result = "";
            for (INetworkNode n : nodes) {
                result += n.getName() + ".";
            }
            return result;
        }

        private Integer getStep() {
            return this.step;
        }
    }

    private class StatsThread implements Runnable {

        private DecimalFormat f = new DecimalFormat("##.00");

        @Override
        public void run() {
            Long last = 0L;
            Long dif = 0L;
            AtomicLong actual = new AtomicLong(0L);
            Double result;
            Integer targetSleep = 1000;
            AtomicLong runningThreads = new AtomicLong(0);
            while (working) {
                Long start = System.currentTimeMillis();
                try {
                    Thread.sleep(targetSleep);
                } catch (InterruptedException ex) {
                    logger.debug(workingThreads.size() + " Threads Running ::: Done a Total of " + workingThreads.size() + " Interations");
                }

                runningThreads.set(0L);
                workingThreads.forEach((s, v) -> {
                    Long lastCounter = v.getCounter();
                    actual.addAndGet(lastCounter);
                    SourceTargetWrapper work = v.getCurrentWork();
                    if (work != null) {
                        if (work.getSource().getEndpointConnectionsCount() > work.getLimit()) {
                            v.youMayStop();
                        } else if (work.getSource().getUnprobedConnections().size() == 0 && work.getSource().getProbedConnectionsCount() > 0 && work.getSource().getActiveConnnectionsCount() > 0) {
                            logger.debug("   [" + s + "] Working On: [" + work.getSource() + "] May Stop because: " + work.getSource().getUnprobedConnections().size());
                            v.youMayStop();
                        }
                        if (!v.getEnded()) {
                            runningThreads.incrementAndGet();
                            logger.debug("   [" + s + "] Working On: [" + work.getSource() + "](" + work.getSource().getEndpointConnectionsCount() + ") TO: [" + work.getTarget() + "] Can Stop:[" + v.canIStop() + "] Unprobed Connections:[" + work.getSource().getUnprobedConnections().size() + "] Position: [" + lastCounter + "/" + v.getActual() + "] E:" + v.getEnded());
                        }
                    }
                });

                dif = actual.get() - last;

                Long end = System.currentTimeMillis();
                Long took = end - start;

                result = dif / took.doubleValue();
                result = result * 1000;
                Long totalSize = weakQueue.size() + weakDone.get() + workingThreads.size();
                logger.debug("--------------------------------------------------------------------------------------");
                logger.debug("[" + runningThreads.get() + "] Threads Running - Total Processed  Paths  :::" + actual.get() + " Paths Performance is  " + f.format(result) + "/s Queue:" + weakQueue.size() + "/" + totalSize + " (" + f.format(getCurrentWorkPerc()) + "%) took:" + took + " ms JT:[" + joininngThread + "]");
            }

//            System.out.println("FIM:::" + counter.get());
        }
    }
};
