/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.osstelecom.db.inventory.manager.objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 *
 * @author Lucas Nishimura <lucas.nishimura@gmail.com>
 * @created 17.08.2022
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Domain {

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

    public Domain() {
    }

    public Domain(String domainName, String connections, String nodes, String serviceConnections, String services, String connectionLayer, String serviceLayer, String circuits, String circuitsLayer, Long atomicId) {
        this.domainName = domainName;
        this.connections = connections;
        this.nodes = nodes;
        this.serviceConnections = serviceConnections;
        this.services = services;
        this.connectionLayer = connectionLayer;
        this.serviceLayer = serviceLayer;
        this.circuits = circuits;
        this.circuitsLayer = circuitsLayer;
        this.atomicId = atomicId;
    }

    public String getDomainName() {
        return domainName;
    }

    public void setDomainName(String domainName) {
        this.domainName = domainName;
    }

    public String getConnections() {
        return connections;
    }

    public void setConnections(String connections) {
        this.connections = connections;
    }

    public String getNodes() {
        return nodes;
    }

    public void setNodes(String nodes) {
        this.nodes = nodes;
    }

    public String getServiceConnections() {
        return serviceConnections;
    }

    public void setServiceConnections(String serviceConnections) {
        this.serviceConnections = serviceConnections;
    }

    public String getServices() {
        return services;
    }

    public void setServices(String services) {
        this.services = services;
    }

    public String getConnectionLayer() {
        return connectionLayer;
    }

    public void setConnectionLayer(String connectionLayer) {
        this.connectionLayer = connectionLayer;
    }

    public String getServiceLayer() {
        return serviceLayer;
    }

    public void setServiceLayer(String serviceLayer) {
        this.serviceLayer = serviceLayer;
    }

    public String getCircuits() {
        return circuits;
    }

    public void setCircuits(String circuits) {
        this.circuits = circuits;
    }

    public String getCircuitsLayer() {
        return circuitsLayer;
    }

    public void setCircuitsLayer(String circuitsLayer) {
        this.circuitsLayer = circuitsLayer;
    }

    public Long getAtomicId() {
        return atomicId;
    }

    public void setAtomicId(Long atomicId) {
        this.atomicId = atomicId;
    }

}
