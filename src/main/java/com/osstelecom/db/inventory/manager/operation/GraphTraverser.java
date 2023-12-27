/*
 * Copyright (C) 2023 Lucas Nishimura <lucas.nishimura@gmail.com>
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
package com.osstelecom.db.inventory.manager.operation;

import com.arangodb.ArangoCursor;
import com.arangodb.ArangoDatabase;
import com.arangodb.model.AqlQueryOptions;
import com.osstelecom.db.inventory.manager.dao.ConsumableMetricDao;
import com.osstelecom.db.inventory.manager.dto.TraversalResult;
import com.osstelecom.db.inventory.manager.elements.PathElement;
import com.osstelecom.db.inventory.manager.resources.BasicResource;
import com.osstelecom.db.inventory.manager.resources.ResourceConnection;
import java.io.IOException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * O Arango Não faz Travessias Multi threads... então a gente vai fazer por
 * ele... 01/09/2023 - Não está pronta ainda essa classe
 *
 * @author Lucas Nishimura <lucas.nishimura@gmail.com>
 * @created 30.08.2023
 */
public class GraphTraverser {
    
    private ExecutorService executorService;
    private final ConcurrentLinkedQueue<List<PathElement>> allPaths;
    private final AtomicLong activeThreads = new AtomicLong(0L);
    private final Semaphore semaphore = new Semaphore(0);
    private final AtomicLong interationCounter = new AtomicLong(0L);
    private final ArangoDatabase arangoDb;
    private Logger logger = LoggerFactory.getLogger(GraphTraverser.class);
    
    public GraphTraverser(ArangoDatabase arangoDb) {
        
        this.allPaths = new ConcurrentLinkedQueue<>();
        this.arangoDb = arangoDb;
    }

    /**
     * Executa uma travesia no grafo em CPU do microserviço
     *
     * @param startNode
     * @param endNode
     * @param maxDepth
     * @param nodeFilter
     * @param direction
     * @return
     * @throws InterruptedException
     */
    public TraversalResult findAllPaths(BasicResource startNode,
            BasicResource endNode,
            int maxDepth,
            Predicate<BasicResource> nodeFilter, String direction) throws InterruptedException {
        TraversalResult result = null;
        Long start = System.currentTimeMillis();
        try {
            this.executorService = Executors.newFixedThreadPool(2);
            this.activeThreads.set(0L);
            this.interationCounter.set(0L);
            List<PathElement> initialPath = new ArrayList<>();
            initialPath.add(new PathElement(startNode, null));
            
            findAllPathsUtil(startNode, endNode, initialPath, 0, maxDepth, nodeFilter, direction);
            
            while (activeThreads.get() > 0 || semaphore.availablePermits() > 0) {
                semaphore.acquire();
            }
            executorService.shutdown();
            
            Long end = System.currentTimeMillis();
            Long took = end - start;
            result = new TraversalResult(new ArrayList<>(allPaths), interationCounter.get(), took);
            logger.debug("Graph Traverser: Took:[{}]ms To Process:[{}] Interation", took, interationCounter);
            return result;
        } finally {
            allPaths.clear();
        }
        
    }
    
    private void findAllPathsUtil(final BasicResource current,
            final BasicResource endNode,
            final List<PathElement> currentPath,
            int currentDepth, int maxDepth,
            Predicate<BasicResource> nodeFilter, String direction) {
        interationCounter.incrementAndGet();
        if (endNode != null) {
            if (current.getId().equals(endNode.getId())) {
                allPaths.add(new ArrayList<>(currentPath));
                return;
            }
        }
        
        if (maxDepth > 0) {
            if (currentDepth >= maxDepth) {
                return;
            }
        }
        List<PathElement> adjacentNodes = getAdjacentNodes(current, direction, nodeFilter);
        for (final PathElement pathElement : adjacentNodes) {
            if (!currentPath.contains(pathElement)) {
                final List<PathElement> newPath = new ArrayList<>(currentPath);
                newPath.add(pathElement);
                activeThreads.incrementAndGet();
                executorService.execute(() -> {
                    try {
                        findAllPathsUtil(pathElement.getNode(), endNode, newPath, currentDepth + 1, maxDepth, nodeFilter, direction);
                    } finally {
                        activeThreads.decrementAndGet();
                        semaphore.release();
                    }
                });
            }
        }
    }

    /**
     *
     * @param currentNode
     * @param direction
     * @param nodeFilter
     * @return
     */
    private List<PathElement> getAdjacentNodes(BasicResource currentNode, String direction, Predicate<BasicResource> nodeFilter) {
        List<PathElement> adjacentNodes = new ArrayList<>();
        //
        // Calcular Direction , testar o precidado se não for nullo
        //

        //
        // pesquisa nas connections
        //
        String aql = "for doc in " + currentNode.getDomain().getConnections();
        if (direction.equalsIgnoreCase("OUTBOUND")) {
            aql += " filter doc.fromResource._id == @resourceId return doc";
        } else {
            aql += " filter doc.toResource._id == @resourceId return doc";
        }
        
        try (ArangoCursor<ResourceConnection> cursor = this.arangoDb.query(aql, Map.of("resourceId", currentNode.getId()),
                new AqlQueryOptions().fullCount(true).count(true), ResourceConnection.class)) {
            cursor.forEach(c -> {
                BasicResource other = c.getOther(currentNode);
                PathElement path = new PathElement(other, c);
                if (nodeFilter != null) {
                    if (nodeFilter.test(other)) {
                        adjacentNodes.add(path);
                    }
                } else {
                    adjacentNodes.add(path);
                }
            });
        } catch (IOException ex) {
        }
        return adjacentNodes;
    }
    
}
