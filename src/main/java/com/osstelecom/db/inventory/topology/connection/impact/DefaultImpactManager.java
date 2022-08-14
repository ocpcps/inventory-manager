
/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.osstelecom.db.inventory.topology.connection.impact;

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
public class DefaultImpactManager extends ImpactManager {

    private Boolean working = false;

    private LinkedBlockingQueue<SourceTargetWrapper> weakQueue = new LinkedBlockingQueue<>();
    private ArrayList<Thread> threadPool = new ArrayList<>();
    private org.slf4j.Logger logger = LoggerFactory.getLogger(DefaultImpactManager.class);
    private Boolean debug = false;
    private Integer runningThreads = 0;
    private AtomicLong weakDone = new AtomicLong(0L);
    private ConcurrentHashMap<String, CalculaTionWeakThread> workingThreads = new ConcurrentHashMap<>();
    private String joininngThread = "N/A";

    public DefaultImpactManager(ITopology topology) {
        super(topology);
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
//        System.out.println(":::getUnreachableConnections() Took:" + took + " ms");
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
        if (threadCount > this.getTopology().getNodes().size()) {
            threadCount = this.getTopology().getNodes().size() - 1;
        }

        this.working = true;
        Long start = System.currentTimeMillis();

        this.getTopology().resetDynamicValues();

        //
        // Esses nunca vão chegar lá....
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
        ArrayList<Thread> threadPool = new ArrayList<>();
        for (int x = 0; x < threadCount; x++) {
            String threadName = "WEAK-" + x;
            CalculaTionWeakThread thread = new CalculaTionWeakThread(useCache, threadName);
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
                Logger.getLogger(DefaultImpactManager.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        this.working = false;
        stats.interrupt();
        List<INetworkNode> lowConnectedDevices = null;
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

        logger.debug("WEAK Found " + lowConnectedDevices.size() + " Nodes, processing impact");
        if (all) {
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

            for (INetworkNode node : lowConnectedDevices) {
                node.disable();
                for (INetworkConnection connection : node.getConnections()) {
                    connection.disable();
                    this.getTopology().getConnectionByName(connection.getName()).disable();

                }

                ArrayList<INetworkNode> impacted = this.getUnreacheableNodes();

//                node.addImpactList(node);
                if (!impacted.isEmpty()) {
                    for (INetworkNode impactedBy : impacted) {
                        if (!alreadyDown.contains(impactedBy)) {
                            if (!node.equals(impactedBy)) {
                                node.addImpactList(impactedBy);
                            }
                        }
                    }
                }

                node.enable();
                for (INetworkConnection connection : node.getConnections()) {
                    connection.enable();
                    this.getTopology().getConnectionByName(connection.getName()).enable();
                }
            }

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
                        } else {
                            step = 1;
                            this.mayIStop = false;
                            walkAllPaths(workload.getSource(), workload.getTarget(), workload.getLimit());
                        }
                        Long end = System.currentTimeMillis();
                        Long took = end - start;
                        logger.debug("Took " + took + " ms to process: " + workload.getSource().getName() + "(" + workload.getSource().getEndpointConnectionsCount() + ") Queue is:" + weakQueue.size() + " [" + String.format("%.2f", getCurrentWorkPerc()) + "] % Stopped:[" + this.mayIStop + "]");
                        logger.debug("--------------------------------------------------------------------------");
                    }
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
         * @param s
         * @param d
         */
        public void walkAllPaths(INetworkNode s, INetworkNode d, Integer limit) {
            ArrayList<INetworkNode> pathList = new ArrayList<>();
            pathList.add(s);
            step = 2;
            if (useCache) {
                while (s.getUnprobedConnections().size() > 0) {
                    step = 2;
                    if (!mayIStop) {
                        computeAllPossiblePathsToTarget(s, d, pathList, limit, 0);
                        step = 3;
                    } else {
                        break;
                    }
                }
            } else {
                if (!mayIStop) {
                    step = 2;
                    computeAllPossiblePathsToTarget(s, d, pathList, limit, 0);
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
            step = 4;
            counter.incrementAndGet();

            if (source.equals(target)) {
                markSegmentAsReacheable(pathList, level, false);
                return;
            }

            source.setVisited();
            level++;
            if (debug) {
                logger.debug(getSpaces(level) + "Processing Node:" + source.getName() + " Connections to Endpoint: [" + source.getEndpointConnectionsCount() + "]");
            }
            for (INetworkConnection connection : source.getConnections()) {
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
                        if (!other.isVisited()) {
                            pathList.add(other);

                            if (mayIStop) {
                                markSegmentAsReacheable(pathList, level, false);
                                return;
                            }

                            if (debug) {
                                logger.debug(getSpaces(level) + " Connection From:" + source.getName() + " To :" + other.getName() + "  to Endpoint: [" + other.getEndpointConnectionsCount(source) + "] Unvisted:" + source.getUnVisitedConnections().size());
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
                                logger.debug(getSpaces(level) + " --> Skipping:" + other.getName() + " Already Visited");
                            }
                        }
                    }

                } else {
                    if (debug) {
                        logger.debug(getSpaces(level) + "Inactive Connection Found!");
                    }
                }

            }

            source.setUnvisited();
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
            Integer targetSleep = 5000;
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
                        }
                        if (!v.getEnded()) {
                            runningThreads.incrementAndGet();
                            logger.debug("   [" + s + "] Working On: [" + work.getSource() + "](" + work.getSource().getEndpointConnectionsCount() + ") TO: [" + work.getTarget() + "] Can Stop:[" + v.canIStop() + "] Position: [" + lastCounter + "/" + v.getActual() + "] E:" + v.getEnded());
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
