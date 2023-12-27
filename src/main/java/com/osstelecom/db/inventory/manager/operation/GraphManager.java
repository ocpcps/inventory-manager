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

import com.osstelecom.db.inventory.manager.dao.GraphDao;
import com.osstelecom.db.inventory.manager.exception.InvalidRequestException;
import com.osstelecom.db.inventory.manager.exception.ResourceNotFoundException;
import com.osstelecom.db.inventory.manager.resources.GraphList;
import com.osstelecom.db.inventory.manager.resources.ManagedResource;
import com.osstelecom.db.inventory.manager.resources.ResourceConnection;
import com.osstelecom.db.inventory.topology.DefaultTopology;
import com.osstelecom.db.inventory.topology.ITopology;
import com.osstelecom.db.inventory.topology.impact.WeakNodesImpactManager;
import com.osstelecom.db.inventory.topology.node.DefaultNode;
import com.osstelecom.db.inventory.topology.node.INetworkNode;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 *
 * @author Lucas Nishimura
 * @created 02.02.2023
 */
@Service
public class GraphManager {

    @Autowired
    private GraphDao graphDao;

    private Logger logger = LoggerFactory.getLogger(GraphManager.class);

    public GraphList<ResourceConnection> expandNode(ManagedResource resource, String direction, Integer depth) throws ResourceNotFoundException, InvalidRequestException {
        return this.graphDao.expandNode(resource, direction, depth);
    }

    /**
     * Computes if the the graph topology is fully connected, will return
     * isolated nodes,
     *
     * @todo: Refactor remove from here,
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

    /**
     * Remove from here!
     *
     * @param name
     * @param id
     * @param topology
     * @return
     */
    private INetworkNode createNode(String name, Long id, ITopology topology) {
        INetworkNode node = new DefaultNode(name, id.intValue(), topology);
        return node;
    }
}
