/*
 * Copyright (C) 2022 Lucas Nishimura <lucas.nishimura@gmail.com>
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
package com.osstelecom.db.inventory.topology.algorithm;

import com.osstelecom.db.inventory.topology.connection.INetworkConnection;
import com.osstelecom.db.inventory.topology.node.INetworkNode;
import com.osstelecom.db.inventory.topology.node.SourceTargetWrapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Lucas Nishimura
 * @created 24.10.2022
 */
public class WeakNodesAlgorithm implements ITopologyAlgorithm {

    private AtomicLong counter = new AtomicLong(0L);
    private AtomicLong interationCounter = new AtomicLong(0L);
    private Queue<SourceTargetWrapper> queue;
    private Map<String, Object> options;
    private Logger logger = LoggerFactory.getLogger(WeakNodesAlgorithm.class);
    private int threadSize;

    @Override
    public void calculate(Queue<SourceTargetWrapper> queue) {
        this.queue = queue;
        this.threadSize = 1;
        this.start();
    }

    @Override
    public void calculate(Queue<SourceTargetWrapper> queue, Map<String, Object> options) {
        this.options = options;
        this.threadSize = 1;
        this.start();
    }

    @Override
    public void calculate(Queue<SourceTargetWrapper> queue, int threadCount) {
        this.queue = queue;
        this.threadSize = threadCount;
        this.start();

    }

    @Override
    public void calculate(Queue<SourceTargetWrapper> queue, Map<String, Object> options, int threadCount) {
        this.queue = queue;
        this.options = options;
        this.threadSize = threadCount;
        this.start();
    }

    /**
     * This is the actually representation where the algorith processes
     */
    public void start() {
        Long start = System.currentTimeMillis();
        if (this.queue != null && !this.queue.isEmpty()) {

            List<Thread> threads = new ArrayList<>();

            for (int x = 0; x < this.threadSize; x++) {
                Thread worker = new Thread(new DfsMultiPathThread());
                threads.add(worker);
            }
            for (Thread t : threads) {
                t.start();
            }

            try {
                for (Thread t : threads) {
                    t.join();
                }

            } catch (InterruptedException ex) {
                java.util.logging.Logger.getLogger(WeakNodesAlgorithm.class.getName()).log(Level.SEVERE, null, ex);
            }

        }
        Long end = System.currentTimeMillis();
        Long took = end - start;
        logger.debug("Job Done: Processed: [{}] Tasks With: [{}] Threads, Iteration Counter:[{}] In :[{}] ms", counter.get(), this.threadSize, this.interationCounter.get(), took);
    }

    private class DfsMultiPathThread implements Runnable {

        @Override
        public void run() {
            while (!queue.isEmpty()) {
                SourceTargetWrapper job = queue.poll();
                if (job != null) {
                    //
                    // Temos  algo para trabalhar
                    //
                    counter.incrementAndGet();
                    this.computeDfs(job);
                }
            }
//            System.out.println(":Processed:" + counter.get());
        }

        private void computeDfs(SourceTargetWrapper job) {
            INetworkNode source = job.getSource();
            INetworkNode target = job.getTarget();
            List<String> pathList = new ArrayList<>();
            Integer level = 0;
            pathList.add(source.getUuid());
            //
            // Generate an unique UID for this Interaction
            //
            String uid = source.getUuid();

            if (job.getUseCache()) {
                this.computeDfsWithCache(source, target, pathList, uid, level);
            } else {
                this.computeDfsWithoutCache(source, target, pathList, uid, level);
            }

//            System.out.println("Tested: " + source.getName() + " TO: " + target.getName() + " Result:" + source.getEndpointConnectionsCount());
        }

        private Boolean computeDfsWithoutCache(INetworkNode source, INetworkNode target, List<String> pathList, String uid, Integer level) {
            //
            // Current Iteration: UID
            //
            interationCounter.incrementAndGet();
            level++;
            Boolean result = false;
            if (source.equals(target)) {
                result = true;
                level--;
                return result;
            }
            source.setVisited(uid);
            for (INetworkConnection connection : source.getUnVisitedConnections(uid)) {
                connection.setVisited(uid);
                INetworkNode other = source.getOtherSide(connection);

                if (!other.isVisited(uid)) {
                    pathList.add(other.getUuid());
                    if (other.equals(target)) {
                        //
                        // Já chegou na saída
                        //
                        source.addEndPointConnection(connection);
                        result = true;
                    } else if (computeDfsWithoutCache(other, target, pathList, uid, level)) {
                        source.addEndPointConnection(connection);
                        result = true;
                    }
                }
                pathList.remove(other.getUuid());

            }
            source.setUnvisited(uid);
            level--;
            return result;
        }

        /**
         * Este método precisa ser testado melhor
         *
         * @param source
         * @param target
         * @param pathList
         * @param uid
         * @param level
         * @return
         */
        private Boolean computeDfsWithCache(INetworkNode source, INetworkNode target, List<String> pathList, String uid, Integer level) {
            //
            // Current Iteration: UID
            //
            interationCounter.incrementAndGet();
            level++;
            Boolean result = false;
            if (source.equals(target)) {
                result = true;
                level--;
                return result;
            }
            source.setVisited(uid);
            for (INetworkConnection connection : source.getUnVisitedConnections(uid)) {
                INetworkNode other = source.getOtherSide(connection);

                if (!other.isVisited(uid)) {
                    pathList.add(other.getUuid());
                    if (other.equals(target)) {
                        //
                        // Já chegou na saída
                        //
                        source.addEndPointConnection(connection);
                        result = true;
                    } else if (other.getEndpointConnectionsCount(source) > 0) {
                        //
                        // Mark as ok, já que tem uma saída por este nó.
                        //
                        source.addEndPointConnection(connection);
                        result = true;
                    } else if (computeDfsWithoutCache(other, target, pathList, uid, level)) {
                        //
                        // Se for para andar tudo, não vai poder usar o cache., mas depois que descobre pode.
                        //
                        source.addEndPointConnection(connection);
                        result = true;
                    }
                }
                pathList.remove(other.getUuid());

            }
            source.setUnvisited(uid);
            level--;
            return result;
        }
    }
}
