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
package com.osstelecom.db.inventory.topology.session;

import com.osstelecom.db.inventory.manager.dto.TransientTopologyDTO;
import com.osstelecom.db.inventory.manager.request.ComputeTransientTopologyRequest;
import com.osstelecom.db.inventory.manager.response.ComputeTransientTopologyResponse;
import com.osstelecom.db.inventory.topology.DefaultTopology;
import com.osstelecom.db.inventory.topology.connection.INetworkConnection;
import com.osstelecom.db.inventory.topology.impact.WeakNodesImpactManager;
import com.osstelecom.db.inventory.topology.node.DefaultNode;
import com.osstelecom.db.inventory.topology.node.INetworkNode;
import guru.nidi.graphviz.model.MutableGraph;
import guru.nidi.graphviz.parse.Parser;
import java.io.IOException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 *
 * @author Lucas Nishimura
 * @created 08.11.2022
 */
@Service
public class TransientTopologySession {

    private Logger logger = LoggerFactory.getLogger(TransientTopologySession.class);

    public ComputeTransientTopologyResponse computeTransientTopologyRequest(ComputeTransientTopologyRequest request) throws IOException {

        logger.debug("Computing Transient Topology Data");
        //
        // First Create the Transient Graph:
        //
        TransientTopologyDTO transientData = request.getPayLoad();
        MutableGraph g = new Parser().read(transientData.getDotTopology());
        DefaultTopology topology = new DefaultTopology(new WeakNodesImpactManager());
        //
        // Transfere o conhecimento do Graph para nosso modelo
        //
        g.nodes().forEach(n -> {
            //
            // Adiciona os Nodes
            //
            INetworkNode node = new DefaultNode(n.name().value(), topology);

            if (transientData.getEndPoints().contains(node.getName())) {
                node.setEndPoint(true);
            }

            if (transientData.getDisabledObjects().contains(node.getName())) {
                node.disable();
            }

        });

        g.edges().forEach(c -> {
            INetworkNode from = topology.getNodeByName(c.from().name().value());
            INetworkNode to = topology.getNodeByName(c.to().name().value());
            INetworkConnection connection = topology.addConnection(from, to, from.getName() + "." + to.getName());

            if (transientData.getDisabledObjects().contains(connection.getName())) {
                connection.disable();
            }

        });
        logger.debug("Topology Data, Nodes:" + topology.getNodes().size() + " Connections: " + topology.getConnections().size() + " Compute Weak Nodes:[" + transientData.getComputeWeakNodes() + "]");

//        System.out.println("Topology Data, Nodes:" + topology.getNodes().size() + " Connections: " + topology.getConnections().size() + " Compute Weak Nodes:[" + transientData.getComputeWeakNodes() + "]");
        //
        // Topologia está montada..
        //
        if (transientData.getComputeWeakNodes()) {
            logger.debug("Weak Nodes Calculation");
            List<INetworkNode> unreacheable = topology.getImpactManager().getUnreacheableNodes();
            if (unreacheable != null) {
                unreacheable.forEach(u -> {
                    transientData.getUnreacheableNodes().add(u.getName());
                });
            }

//            List<INetworkNode> weakNodes = topology.getImpactManager().getWeakNodes(transientData.getMinConnections(), false, transientData.getThreadCount(), transientData.getDfsCache());
            //
            // Desliguei o cache forçado, pois com cache ele trás resultados não deterministicos.
            // @todo verificar o bug do cache
            //
            List<INetworkNode> weakNodes = topology.getImpactManager().getWeakNodes(transientData.getMinConnections(), false, transientData.getThreadCount(), false);
            if (weakNodes != null) {
                weakNodes.forEach(w -> {
                    transientData.getWeakNodes().add(w.getName());
                });
            }
        }

        return new ComputeTransientTopologyResponse(transientData);
    }
}
