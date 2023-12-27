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
package com.osstelecom.db.inventory.manager.resources;

import com.arangodb.entity.DocumentField;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import java.util.Date;

/**
 *
 * @author Lucas Nishimura
 * @created 15.12.2021
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Domain implements Serializable{

    @DocumentField(DocumentField.Type.KEY)
    @Schema(example = "network")
    private String domainName;
    @Schema(example = "network_connections")
    private String connections;
    @Schema(example = "network_nodes")
    private String nodes;
    private String serviceConnections;
    @Schema(example = "network_services")
    private String services;
    private String connectionLayer;
    private String serviceLayer;
    private String circuits;
    private String circuitsLayer;
    private String domainDescription;
    private String metrics;
    @Schema(example = "0")
    private Long resourceCount;
    @Schema(example = "0")
    private Long connectionCount;
    @Schema(example = "0")
    private Long circuitCount;
    @Schema(example = "0")
    private Long serviceCount;
    @Schema(example = "0")
    private Long atomicId;
    @Schema(example = "2023-04-18T23:20:00.000Z")
    private Date lastStatsCalc;
    @Schema(example = "2023-04-18T23:20:00.000Z")
    private Date lastUpdate;

    private Boolean valid;

    /**
     * @return the domainName
     */
    public String getDomainName() {
        return domainName;
    }

    /**
     * @param domainName the domainName to set
     */
    public void setDomainName(String domainName) {
        this.domainName = domainName;
    }

    /**
     * @return the connections
     */
    public String getConnections() {
        return connections;
    }

    /**
     * @param connections the connections to set
     */
    public void setConnections(String connections) {
        this.connections = connections;
    }

    /**
     * @return the metrics
     */
    public String getMetrics() {
        return metrics;
    }

    /**
     * @param metrics the metrics to set
     */
    public void setMetrics(String metrics) {
        this.metrics = metrics;
    }

    /**
     * @return the nodes
     */
    public String getNodes() {
        return nodes;
    }

    /**
     * @param nodes the nodes to set
     */
    public void setNodes(String nodes) {
        this.nodes = nodes;
    }

    /**
     * @return the connectionLayer
     */
    public String getConnectionLayer() {
        return connectionLayer;
    }

    /**
     * @param connectionLayer the connectionLayer to set
     */
    public void setConnectionLayer(String connectionLayer) {
        this.connectionLayer = connectionLayer;
    }

    /**
     * @return the serviceLayer
     */
    public String getServiceLayer() {
        return serviceLayer;
    }

    /**
     * @param serviceLayer the serviceLayer to set
     */
    public void setServiceLayer(String serviceLayer) {
        this.serviceLayer = serviceLayer;
    }

    /**
     * @return the serviceConnections
     */
    public String getServiceConnections() {
        return serviceConnections;
    }

    /**
     * @param serviceConnections the serviceConnections to set
     */
    public void setServiceConnections(String serviceConnections) {
        this.serviceConnections = serviceConnections;
    }

    /**
     * @return the services
     */
    public String getServices() {
        return services;
    }

    /**
     * @param services the services to set
     */
    public void setServices(String services) {
        this.services = services;
    }

    /**
     * @return the atomicId
     */
    public Long getAtomicId() {
        return atomicId;
    }

    /**
     * @param atomicId the atomicId to set
     */
    public void setAtomicId(Long atomicId) {
        this.atomicId = atomicId;
    }

    /**
     * @return the circuits
     */
    public String getCircuits() {
        return circuits;
    }

    /**
     * @param circuits the circuits to set
     */
    public void setCircuits(String circuits) {
        this.circuits = circuits;
    }

    /**
     * @return the circuitsLayer
     */
    public String getCircuitsLayer() {
        return circuitsLayer;
    }

    /**
     * @param circuitsLayer the circuitsLayer to set
     */
    public void setCircuitsLayer(String circuitsLayer) {
        this.circuitsLayer = circuitsLayer;
    }

    public Long addAndGetId() {
        if (this.atomicId == null) {
            this.atomicId = 0L;
        }
        return this.atomicId++;
    }

    /**
     * @return the resourceCount
     */
    public Long getResourceCount() {
        return resourceCount;
    }

    /**
     * @param resourceCount the resourceCount to set
     */
    public void setResourceCount(Long resourceCount) {
        this.resourceCount = resourceCount;
    }

    /**
     * @return the connectionCount
     */
    public Long getConnectionCount() {
        return connectionCount;
    }

    /**
     * @param connectionCount the connectionCount to set
     */
    public void setConnectionCount(Long connectionCount) {
        this.connectionCount = connectionCount;
    }

    /**
     * @return the circuitCount
     */
    public Long getCircuitCount() {
        return circuitCount;
    }

    /**
     * @param circuitCount the circuitCount to set
     */
    public void setCircuitCount(Long circuitCount) {
        this.circuitCount = circuitCount;
    }

    /**
     * @return the serviceCount
     */
    public Long getServiceCount() {
        return serviceCount;
    }

    /**
     * @param serviceCount the serviceCount to set
     */
    public void setServiceCount(Long serviceCount) {
        this.serviceCount = serviceCount;
    }

    /**
     * @return the lastStatsCalc
     */
    public Date getLastStatsCalc() {
        return lastStatsCalc;
    }

    /**
     * @param lastStatsCalc the lastStatsCalc to set
     */
    public void setLastStatsCalc(Date lastStatsCalc) {
        this.lastStatsCalc = lastStatsCalc;
    }

    public String getDomainDescription() {
        return domainDescription;
    }

    /**
     * @param domainDescription the domainDescription to set
     */
    public void setDomainDescription(String domainDescription) {
        this.domainDescription = domainDescription;
    }

    /**
     * @return the lastUpdate
     */
    public Date getLastUpdate() {
        return lastUpdate;
    }

    /**
     * @param lastUpdate the lastUpdate to set
     */
    public void setLastUpdate(Date lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

    /**
     * @return the valid
     */
    public Boolean getValid() {
        return valid;
    }

    /**
     * @param valid the valid to set
     */
    public void setValid(Boolean valid) {
        this.valid = valid;
    }

}
