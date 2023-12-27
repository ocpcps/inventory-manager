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
import com.osstelecom.db.inventory.topology.ITopology;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Nishisan
 */
public interface ImpactManagerIf {

    public ArrayList<INetworkNode> getUnreacheableNodes();

    /**
     * 
     * @param connLimit min connection in nodes
     * @param all , bring all impacted and weak nodes.
     * @param threadCount , threadCount for calculations
     * @param useCache = use Cache
     * @return 
     */
    public List<INetworkNode> getWeakNodes(Integer connLimit, Boolean all, Integer threadCount, Boolean useCache);

    public List<INetworkNode> getWeakNodes(Integer connLimit, ArrayList<INetworkNode> nodes);

    public ITopology getTopology();

    public void setTopology(ITopology topology);

    public ArrayList<INetworkConnection> getUnreachableConnections();

}
