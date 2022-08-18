/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.osstelecom.db.inventory.manager.objects;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * @author Lucas Nishimura <lucas.nishimura@gmail.com>
 * @created 17.08.2022
 */
public class Filter extends BasicObject {

    private ArrayList<String> classes;
    private ArrayList<String> objects = new ArrayList<>();
    private String aqlFilter;
    private ConcurrentHashMap<String, Object> bindings = new ConcurrentHashMap<>();
    private String targetRegex;
    private Boolean computeWeakLinks = false;
    private Integer computeThreads = 8;
    private Integer minCuts = 1;
    private ArrayList<ManagedResource> nodes;
    private ArrayList<ResourceConnection> connections;
    private Integer nodeCount = 0;
    private Integer connectionsCount = 0;

    public void addObject(String obj) {
        if (!this.objects.contains(obj)) {
            this.objects.add(obj);
        }
    }

    public void addBingind(String key, Object value) {
        this.bindings.put(key, value);
    }

    public ArrayList<String> getClasses() {
        return classes;
    }

    public void setClasses(ArrayList<String> classes) {
        this.classes = classes;
    }

    public String getTargetRegex() {
        return targetRegex;
    }

    public void setTargetRegex(String targetRegex) {
        this.targetRegex = targetRegex;
    }

    public Boolean getComputeWeakLinks() {
        return computeWeakLinks;
    }

    public void setComputeWeakLinks(Boolean computeWeakLinks) {
        this.computeWeakLinks = computeWeakLinks;
    }

    public Integer getComputeThreads() {
        return computeThreads;
    }

    public void setComputeThreads(Integer computeThreads) {
        this.computeThreads = computeThreads;
    }

    public Integer getMinCuts() {
        return minCuts;
    }

    public void setMinCuts(Integer minCuts) {
        this.minCuts = minCuts;
    }

    public ArrayList<ManagedResource> getNodes() {
        return nodes;
    }

    public void setNodes(ArrayList<ManagedResource> nodes) {
        this.nodes = nodes;
    }

    public ArrayList<ResourceConnection> getConnections() {
        return connections;
    }

    public void setConnections(ArrayList<ResourceConnection> connections) {
        this.connections = connections;
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

    public Filter() {
    }

    public ArrayList<String> getObjects() {
        return objects;
    }

    public void setObjects(ArrayList<String> objects) {
        this.objects = objects;
    }

    public String getAqlFilter() {
        return aqlFilter;
    }

    public void setAqlFilter(String aqlFilter) {
        this.aqlFilter = aqlFilter;
    }

    public ConcurrentHashMap<String, Object> getBindings() {
        return bindings;
    }

    public void setBindings(ConcurrentHashMap<String, Object> bindings) {
        this.bindings = bindings;
    }
}
