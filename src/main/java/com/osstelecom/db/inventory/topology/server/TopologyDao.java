/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.osstelecom.db.inventory.topology.server;

import com.osstelecom.db.inventory.topology.ITopology;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * @author Lucas
 */
public class TopologyDao {

    private ConcurrentHashMap<String, ITopology> topologyDB = new ConcurrentHashMap<>();

    public ITopology addTopology(ITopology topology) {
        if (topologyDB.containsKey(topology.getUuid())) {
            topologyDB.remove(topology.getUuid());

        }
        topologyDB.put(topology.getUuid(), topology);
        return topology;
    }

    public ITopology removeTopology(ITopology topology) {
        if (topologyDB.containsKey(topology.getUuid())) {
            return topologyDB.remove(topology.getUuid());
        }
        return null;
    }

    public ITopology getTopologyByUuId(String uuid) {
        if (topologyDB.containsKey(uuid)) {
            return topologyDB.get(uuid);
        } else {
            return null;
        }
    }
}
