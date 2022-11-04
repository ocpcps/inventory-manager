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
 * @author Lucas Nishimura <lucas.nishimura@gmail.com>
 * @created 24.10.2022
 */
public class WeakNodesAlgorithm implements ITopolocyAlgorithm {

    private AtomicLong counter = new AtomicLong(0L);
    private Queue<SourceTargetWrapper> queue;
    private Map<String, Object> options;
    private Logger logger = LoggerFactory.getLogger(WeakNodesAlgorithm.class);

    @Override
    public void calculate(Queue<SourceTargetWrapper> queue) {
        this.queue = queue;
        this.start();
    }

    @Override
    public void calculate(Queue<SourceTargetWrapper> queue, Map<String, Object> options) {
        this.options = options;
        this.start();
    }

//    @Override
    private void start() {
        //
        //
        //
        
        int threadSize = 8;
        
        if (this.queue != null && !this.queue.isEmpty()) {
//            while (!this.queue.isEmpty()) {
//                SourceTargetWrapper job = this.queue.poll();
//                if (job != null) {
//                    counter.incrementAndGet();
//                }
//            }

            List<Thread> threads = new ArrayList<>();

            for (int x = 0; x < threadSize; x++) {
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

        logger.debug("Job Done: Processed: [{}] Tasks", counter.get());
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
        }

        private void computeDfs(SourceTargetWrapper job) {
            INetworkNode source = job.getSource();
            INetworkNode target = job.getTarget();
            List<String> pathList = new ArrayList<>();
//            List<Integer> level = new ArrayList<>();
            Integer level = 0;
            pathList.add(source.getUuid());
            String uid = source.getUuid();

            Boolean result = this.computeDfsUtils(source, target, pathList, uid, level);

            System.out.println("Tested: " + source.getName() + " TO: " + target.getName() + " Result:" + source.getEndpointConnectionsCount());
        }

        private Boolean computeDfsUtils(INetworkNode source, INetworkNode target, List<String> pathList, String uid, Integer level) {
            //
            // Current Iteration: UID
            //
            level++;
//            System.out.println("\t:[" + level + "] Testing from:" + source.getName() + " TO:" + target.getName());
            Boolean result = false;
            if (source.equals(target)) {
//                System.out.println("Solution Found:!" + pathList);
//                System.out.println("\t\tFound AT: [" + level + "]");
                result = true;
                level--;
                return result;
            }

//            System.out.println("Starting from: ["+source.getName()+"]");
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
//                        System.out.println("\t\tFound 2 AT: [" + level + "]");

                    } else if (computeDfsUtils(other, target, pathList, uid, level)) {
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
