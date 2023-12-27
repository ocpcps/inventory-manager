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
