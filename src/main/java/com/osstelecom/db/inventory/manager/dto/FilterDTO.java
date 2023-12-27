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

import com.osstelecom.db.inventory.manager.resources.CircuitResource;
import com.osstelecom.db.inventory.manager.resources.ConsumableMetric;
import com.osstelecom.db.inventory.manager.resources.Domain;
import com.osstelecom.db.inventory.manager.resources.ManagedResource;
import com.osstelecom.db.inventory.manager.resources.ResourceConnection;
import com.osstelecom.db.inventory.manager.resources.ServiceResource;
import com.osstelecom.db.inventory.manager.resources.model.IconModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * @author Lucas Nishimura
 * @created 26.01.2022
 */
public class FilterDTO {

    private List<String> classes;
    private List<String> objects = new ArrayList<>();
    private List<String> fields = new ArrayList<>();
    private String aqlFilter;
    private Map<String, Object> bindings = new ConcurrentHashMap<>();
    private String targetRegex;
    private boolean computeWeakLinks = false;
    private Integer computeThreads = 8;
    private Integer minCuts = 1;
    private List<ManagedResource> nodes;
    private List<ResourceConnection> connections;
    private List<CircuitResource> circuits;
    private List<ServiceResource> services;
    private List<IconModel> icons;
    private List<ConsumableMetric> metrics;
    private Long nodeCount = 0L;
    private Long connectionsCount = 0L;
    private Long circuitCount = 0L;
    private Long serviceCount = 0L;
    private Long iconCount = 0L;
    private Long metricCount = 0L;
    private String sortCondition = "";
    private Long offSet = -1L;
    private Long limit = -1L;
    private String domainName;
    private Boolean paginated = false;

    public void addBinding(String name, Object value) {
        this.bindings.put(name, value);
    }

    public void addObject(String object) {
        this.objects.add(object);
    }

    public FilterDTO() {
    }

    public static FilterDTO findAllCircuits(Domain domain) {
        String aql = " for doc in   `" + domain.getCircuits() + "`";
        FilterDTO filter = new FilterDTO(aql);
        return filter;
    }

    public static FilterDTO findAllLocationConnections(Domain domain) {
        String aql = " for doc in   `" + domain.getConnections() + "`";
        FilterDTO filter = new FilterDTO(aql);
        return filter;
    }

    public static FilterDTO findAllManagedResource(Domain domain) {
        String aql = " for doc in   `" + domain.getNodes() + "`";
        FilterDTO filter = new FilterDTO(aql);
        return filter;
    }

    public static FilterDTO findAllResourceConnection(Domain domain) {
        String aql = " for doc in   `" + domain.getConnections() + "`";
        FilterDTO filter = new FilterDTO(aql);
        return filter;
    }

    public static FilterDTO findAllResourceLocation(Domain domain) {
        String aql = " for doc in   `" + domain.getNodes() + "`";
        aql += " filter doc.attributeSchemaName like 'location.%'";
        FilterDTO filter = new FilterDTO(aql);
        return filter;
    }

    public static FilterDTO findAllServices(Domain domain) {
        String aql = " for doc in   `" + domain.getServices() + "`";
        FilterDTO filter = new FilterDTO(aql);
        return filter;
    }

   

    public FilterDTO(String aqlFilter) {
        this.aqlFilter = aqlFilter;
    }

    public FilterDTO(String aqlFilter, String sortCondition) {
        this.aqlFilter = aqlFilter;
        this.sortCondition = sortCondition;
    }

    public FilterDTO(String aqlFilter, String sortCondition, Map<String, Object> bindings) {
        this.aqlFilter = aqlFilter;
        this.sortCondition = sortCondition;
        this.bindings = bindings;
    }

    public FilterDTO(String aqlFilter, Map<String, Object> bindings) {
        this.aqlFilter = aqlFilter;
        this.bindings = bindings;
    }

    public FilterDTO(String aqlFilter, Map<String, Object> bindings, String domainName) {
        this.aqlFilter = aqlFilter;
        this.bindings = bindings;
        this.domainName = domainName;
    }

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
        // if (this.connections != null) {
        // this.setConnectionsCount(this.connections.size());
        // }
    }

    public Long getNodeCount() {
        return nodeCount;
    }

    public void setNodeCount(Long nodeCount) {
        this.nodeCount = nodeCount;
    }

    public Long getConnectionsCount() {
        return connectionsCount;
    }

    public void setConnectionsCount(Long connectionsCount) {
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

    /**
     * @return the sortCondition
     */
    public String getSortCondition() {
        return sortCondition;
    }

    /**
     * @param sortCondition the sortCondition to set
     */
    public void setSortCondition(String sortCondition) {
        this.sortCondition = sortCondition;
    }

    /**
     * @return the circuits
     */
    public List<CircuitResource> getCircuits() {
        return circuits;
    }

    /**
     * @param circuits the circuits to set
     */
    public void setCircuits(List<CircuitResource> circuits) {
        this.circuits = circuits;
    }

    /**
     * @return the services
     */
    public List<ServiceResource> getServices() {
        return services;
    }

    /**
     * @param services the services to set
     */
    public void setServices(List<ServiceResource> services) {
        this.services = services;
    }

    /**
     * @return the icons
     */
    public List<IconModel> getIcons() {
        return icons;
    }

    /**
     * @param icons the icons to set
     */
    public void setIcons(List<IconModel> icons) {
        this.icons = icons;
    }

    /**
     * @return the metrics
     */
    public List<ConsumableMetric> getMetrics() {
        return metrics;
    }

    /**
     * @param metrics the metrics to set
     */
    public void setMetrics(List<ConsumableMetric> metrics) {
        this.metrics = metrics;
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
     * @return the iconCount
     */
    public Long getIconCount() {
        return iconCount;
    }

    /**
     * @param iconCount the iconCount to set
     */
    public void setIconCount(Long iconCount) {
        this.iconCount = iconCount;
    }

    /**
     * @return the metricCount
     */
    public Long getMetricCount() {
        return metricCount;
    }

    /**
     * @param metricCount the metricCount to set
     */
    public void setMetricCount(Long metricCount) {
        this.metricCount = metricCount;
    }

    /**
     * @return the offSet
     */
    public Long getOffSet() {
        return offSet;
    }

    /**
     * @param offSet the offSet to set
     */
    public void setOffSet(Long offSet) {
        this.offSet = offSet;
    }

    /**
     * @return the limit
     */
    public Long getLimit() {
        return limit;
    }

    /**
     * @param limit the limit to set
     */
    public void setLimit(Long limit) {
        this.limit = limit;
    }

    /**
     * @return the paginated
     */
    public Boolean getPaginated() {
        return paginated;
    }

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

    public List<String> getFields() {
        return this.fields;
    }

    public void setFields(List<String> fields) {
        this.fields = fields;
    }

    public void addField(String field) {
        this.fields.add(field);
    }
}
