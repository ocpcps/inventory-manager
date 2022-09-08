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
package com.osstelecom.db.inventory.manager.dto;

import com.osstelecom.db.inventory.manager.resources.ManagedResource;
import com.osstelecom.db.inventory.manager.resources.ResourceConnection;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Lucas Nishimura <lucas.nishimura@gmail.com>
 * @created 26.01.2022
 */
public class FilterDTO {

    private List<String> classes;
    private List<String> objects;
    private String aqlFilter;
    private Map<String, Object> bindings;
    private String targetRegex;
    private boolean computeWeakLinks = false;
    private Integer computeThreads = 8;
    private Integer minCuts = 1;
    private List<ManagedResource> nodes;
    private List<ResourceConnection> connections;
    private Integer nodeCount = 0;
    private Integer connectionsCount = 0;

    public List<ManagedResource> getNodes() {
        return nodes;
    }

    public void setNodes(List<ManagedResource> nodes) {
        this.nodes = nodes;
    }

    public List<ResourceConnection> getConnections() {
        return connections;
    }

    public void setConnections(List<ResourceConnection> connections) {
        this.connections = connections;
        if (this.connections != null) {
            this.setConnectionsCount(this.connections.size());
        }
    }

    public Integer getNodeCount() {
        return nodeCount;
    }

    public void setNodeCount(Integer nodeCount) {
        this.nodeCount = nodeCount;
    }

    public Integer getConnectionsCount() {
        return connectionsCount;
    }

    public void setConnectionsCount(Integer connectionsCount) {
        this.connectionsCount = connectionsCount;
    }

    /**
     * @return the classes
     */
    public List<String> getClasses() {
        return classes;
    }

    /**
     * @param classes the classes to set
     */
    public void setClasses(List<String> classes) {
        this.classes = classes;
    }

    /**
     * @return the objects
     */
    public List<String> getObjects() {
        return objects;
    }

    /**
     * @param objects the objects to set
     */
    public void setObjects(List<String> objects) {
        this.objects = objects;
    }

    /**
     * @return the targetRegex
     */
    public String getTargetRegex() {
        return targetRegex;
    }

    /**
     * @param targetRegex the targetRegex to set
     */
    public void setTargetRegex(String targetRegex) {
        this.targetRegex = targetRegex;
    }

    /**
     * @return the computeWeakLinks
     */
    public boolean getComputeWeakLinks() {
        return computeWeakLinks;
    }

    /**
     * @param computeWeakLinks the computeWeakLinks to set
     */
    public void setComputeWeakLinks(boolean computeWeakLinks) {
        this.computeWeakLinks = computeWeakLinks;
    }

    /**
     * @return the computeThreads
     */
    public Integer getComputeThreads() {
        return computeThreads;
    }

    /**
     * @param computeThreads the computeThreads to set
     */
    public void setComputeThreads(Integer computeThreads) {
        this.computeThreads = computeThreads;
    }

    /**
     * @return the minCuts
     */
    public Integer getMinCuts() {
        return minCuts;
    }

    /**
     * @param minCuts the minCuts to set
     */
    public void setMinCuts(Integer minCuts) {
        this.minCuts = minCuts;
    }

    /**
     * @return the aqlFilter
     */
    public String getAqlFilter() {
        return aqlFilter;
    }

    /**
     * @param aqlFilter the aqlFilter to set
     */
    public void setAqlFilter(String aqlFilter) {
        this.aqlFilter = aqlFilter;
    }

    /**
     * @return the bindings
     */
    public Map<String, Object> getBindings() {
        return bindings;
    }

    /**
     * @param bindings the bindings to set
     */
    public void setBindings(Map<String, Object> bindings) {
        this.bindings = bindings;
    }
}
