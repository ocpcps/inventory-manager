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
package com.osstelecom.db.inventory.manager.dao;

import com.arangodb.ArangoCursor;
import com.arangodb.ArangoDatabase;
import com.arangodb.model.AqlQueryOptions;
import com.osstelecom.db.inventory.manager.exception.InvalidRequestException;
import com.osstelecom.db.inventory.manager.exception.ResourceNotFoundException;
import com.osstelecom.db.inventory.manager.resources.CircuitResource;
import com.osstelecom.db.inventory.manager.resources.GraphList;
import com.osstelecom.db.inventory.manager.resources.ManagedResource;
import com.osstelecom.db.inventory.manager.resources.ResourceConnection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Realiza as operações de travesia de grafo no arangoDB
 *
 * @author Lucas Nishimura
 * @created 02.02.2023
 */
@Component
public class GraphDao {

    @Autowired
    private ArangoDatabase arangoDatabase;

    protected Logger logger = LoggerFactory.getLogger(GraphDao.class);

    /**
     * Obtem os vertices relacionados ao circuito Muito especifica para
     * generalizar
     *
     * @param circuit
     * @return
     */
    public GraphList<ResourceConnection> findCircuitPaths(CircuitResource circuit) {
        // String aql = "FOR v, e, p IN 1..@dLimit ANY @aPoint " +
        // circuit.getDomain().getConnections() + "\n"
        // + " FILTER v._id == @zPoint "
        // + " AND @circuitId in e.circuits[*] "
        // + " AND e.operationalStatus ==@operStatus "
        // + " for a in p.edges[*] return distinct a";
        String aql = "FOR path\n"
                + "  IN 1..@dLimit ANY k_paths\n"
                + "  @aPoint TO @zPoint\n"
                + "  GRAPH '" + circuit.getDomain().getConnectionLayer() + "'\n"
                + "    for v in  path.edges\n"
                + "      filter @circuitId  in v.circuits[*] \n"
                + "        \n"
                + "       return v";

        HashMap<String, Object> bindVars = new HashMap<>();
        bindVars.put("dLimit", circuit.getCircuitPath().size() + 1);
        bindVars.put("aPoint", circuit.getaPoint().getId());
        bindVars.put("zPoint", circuit.getzPoint().getId());
        bindVars.put("circuitId", circuit.getId());
        logger.info("(query) RUNNING: AQL:[{}]", aql);
        bindVars.forEach((k, v) -> {
            logger.info("\t  [@{}]=[{}]", k, v);

        });
        ArangoCursor<ResourceConnection> cursor = this.arangoDatabase.query(aql, bindVars,
                new AqlQueryOptions().fullCount(true).count(true).batchSize(5000), ResourceConnection.class);
        return new GraphList<>(cursor);
    }

    /**
     * Expand um Nó, avaliar se aqui é o melhor lugar
     *
     * @param resource
     * @param direction
     * @param depth
     * @return
     * @throws ResourceNotFoundException
     */
    public GraphList<ResourceConnection> expandNode(ManagedResource resource, String direction, Integer depth) throws ResourceNotFoundException, InvalidRequestException {

        direction = direction.toUpperCase();
        if (direction.equals("OUTBOUND") || direction.equals("INBOUND") || direction.equals("ANY")) {

            String aql = "for v,e,p in 1..@depth \n"
                    + " " + direction + " @resourceId graph @graphName"
                    + "\n return distinct e";
            logger.debug("Running GRAPH AQL:[{}]", aql);
            Map<String, Object> bindVars = new ConcurrentHashMap<>();
            bindVars.put("depth", depth);
            bindVars.put("resourceId", resource.getId());
            bindVars.put("graphName", resource.getDomain().getConnectionLayer());
            Long start = System.currentTimeMillis();
            ArangoCursor<ResourceConnection> cursor = this.arangoDatabase.query(aql, bindVars, new AqlQueryOptions().fullCount(true).count(true).batchSize(5000), ResourceConnection.class);

            GraphList<ResourceConnection> result = new GraphList<>(cursor, start);

            if (result.isEmpty()) {
                throw new ResourceNotFoundException("No Resources found for expand")
                        .addDetails("resource", resource)
                        .addDetails("direction", direction)
                        .addDetails("depth", depth)
                        .addDetails("graphName", resource.getDomain().getConnectionLayer())
                        .addDetails("aql", aql)
                        .addDetails("bindVars", bindVars);

            }
            Long end = System.currentTimeMillis();
            Long took = end - start;
            logger.debug("Query Took:[{}] ms To Get :[{}] Results", took, result.size());
            return result;
        } else {
            throw new InvalidRequestException("Direction must be : [OUTBOUND|INBOUND|ANY] Received: [" + direction + "]");
        }
    }
}
