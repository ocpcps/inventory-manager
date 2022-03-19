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
package com.osstelecom.db.inventory.manager.dto;

import com.arangodb.entity.DocumentField;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.concurrent.atomic.AtomicLong;

/**
 *
 * @author Lucas Nishimura <lucas.nishimura@gmail.com>
 * @created 15.12.2021
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DomainDTO {

    @DocumentField(DocumentField.Type.KEY)
    private String domainName;
    private String connections;
    private String nodes;
    private String serviceConnections;
    private String services;
    private String connectionLayer;
    private String serviceLayer;
    private String circuits;
    private String circuitsLayer;
    private Long atomicId;

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
        if (this.atomicId==null){
            this.atomicId = 0L;
        }
        return this.atomicId++;
    }

}
