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

import com.osstelecom.db.inventory.topology.node.INetworkNode;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Lucas Nishimura
 * @created 08.11.2022
 */
public class TransientTopologyDTO {

    private String dotTopology;
    private Boolean computeWeakNodes = false;
    private Boolean dfsCache = false;
    private Integer minConnections = 1;
    private Integer threadCount = 4;
    private List<String> endPoints = new ArrayList<>();
    private List<String> disabledObjects = new ArrayList<>();
    private List<String> weakNodes = new ArrayList<>();
    private List<String> unreacheableNodes = new ArrayList<>();
    

    /**
     * @return the endPoints
     */
    public List<String> getEndPoints() {
        return endPoints;
    }

    /**
     * @param endPoints the endPoints to set
     */
    public void setEndPoints(List<String> endPoints) {
        this.endPoints = endPoints;
    }

    /**
     * @return the weakNodes
     */
    public List<String> getWeakNodes() {
        return weakNodes;
    }

    /**
     * @param weakNodes the weakNodes to set
     */
    public void setWeakNodes(List<String> weakNodes) {
        this.weakNodes = weakNodes;
    }

    /**
     * @return the unreacheableNodes
     */
    public List<String> getUnreacheableNodes() {
        return unreacheableNodes;
    }

    /**
     * @param unreacheableNodes the unreacheableNodes to set
     */
    public void setUnreacheableNodes(List<String> unreacheableNodes) {
        this.unreacheableNodes = unreacheableNodes;
    }

    /**
     * @return the dotTopology
     */
    public String getDotTopology() {
        return dotTopology;
    }

    /**
     * @param dotTopology the dotTopology to set
     */
    public void setDotTopology(String dotTopology) {
        this.dotTopology = dotTopology;
    }

    /**
     * @return the computeWeakNodes
     */
    public Boolean getComputeWeakNodes() {
        return computeWeakNodes;
    }

    /**
     * @param computeWeakNodes the computeWeakNodes to set
     */
    public void setComputeWeakNodes(Boolean computeWeakNodes) {
        this.computeWeakNodes = computeWeakNodes;
    }

    /**
     * @return the minConnections
     */
    public Integer getMinConnections() {
        return minConnections;
    }

    /**
     * @param minConnections the minConnections to set
     */
    public void setMinConnections(Integer minConnections) {
        this.minConnections = minConnections;
    }

    /**
     * @return the threadCount
     */
    public Integer getThreadCount() {
        return threadCount;
    }

    /**
     * @param threadCount the threadCount to set
     */
    public void setThreadCount(Integer threadCount) {
        this.threadCount = threadCount;
    }

    /**
     * @return the disabledObjects
     */
    public List<String> getDisabledObjects() {
        return disabledObjects;
    }

    /**
     * @param disabledObjects the disabledObjects to set
     */
    public void setDisabledObjects(List<String> disabledObjects) {
        this.disabledObjects = disabledObjects;
    }

    /**
     * @return the dfsCache
     */
    public Boolean getDfsCache() {
        return dfsCache;
    }

    /**
     * @param dfsCache the dfsCache to set
     */
    public void setDfsCache(Boolean dfsCache) {
        this.dfsCache = dfsCache;
    }

}
