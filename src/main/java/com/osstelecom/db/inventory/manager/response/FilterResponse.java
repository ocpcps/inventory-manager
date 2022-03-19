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
package com.osstelecom.db.inventory.manager.response;

import com.osstelecom.db.inventory.manager.dto.FilterDTO;
import com.osstelecom.db.inventory.manager.resources.BasicResource;
import com.osstelecom.db.inventory.manager.resources.ResourceConnection;
import java.util.ArrayList;

/**
 *
 * @author Lucas Nishimura <lucas.nishimura@gmail.com>
 * @created 26.01.2022
 */
public class FilterResponse extends BasicResponse<FilterDTO> {

    private ArrayList<BasicResource> nodes;
    private ArrayList<ResourceConnection> connections;
    private Integer nodeCount = 0;

    public FilterResponse(FilterDTO obj) {
        super(obj);
    }

    /**
     * @return the nodes
     */
    public ArrayList<BasicResource> getNodes() {
        return nodes;
    }

    /**
     * @param nodes the nodes to set
     */
    public void setNodes(ArrayList<BasicResource> nodes) {
        this.nodes = nodes;
    }

    /**
     * @return the nodeCount
     */
    public Integer getNodeCount() {
        return nodeCount;
    }

    /**
     * @param nodeCount the nodeCount to set
     */
    public void setNodeCount(Integer nodeCount) {
        this.nodeCount = nodeCount;
    }

    /**
     * @return the connections
     */
    public ArrayList<ResourceConnection> getConnections() {
        return connections;
    }

    /**
     * @param connections the connections to set
     */
    public void setConnections(ArrayList<ResourceConnection> connections) {
        this.connections = connections;
    }

}
