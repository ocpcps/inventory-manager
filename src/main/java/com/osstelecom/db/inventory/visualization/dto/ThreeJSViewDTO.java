/*
 * Copyright (C) 2023 Lucas Nishimura <lucas.nishimura@gmail.com>
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
package com.osstelecom.db.inventory.visualization.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.osstelecom.db.inventory.manager.resources.CircuitResource;
import com.osstelecom.db.inventory.manager.resources.GraphList;
import com.osstelecom.db.inventory.manager.resources.ManagedResource;
import com.osstelecom.db.inventory.manager.resources.ResourceConnection;
import com.osstelecom.db.inventory.manager.resources.ServiceResource;
import com.osstelecom.db.inventory.manager.response.FilterResponse;
import com.osstelecom.db.inventory.visualization.exception.InvalidGraphException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Payload de Grafo do Netcompass, a principio o payload original dos recursos
 * são muito . Então criamos este wrapper para encapsular os dados relevantes
 * para o grafo.
 *
 * @author Lucas Nishimura
 * @created 12.01.2023
 */
public class ThreeJSViewDTO {

    /**
     * @return the circuits
     */
    public List<ThreeJSCircuitDTO> getCircuits() {
        return circuits;
    }

    public List<ThreeJSServiceDTO> getServices() {
        return services;
    }

    private List<ThreeJsNodeDTO> nodes = new ArrayList<>();
    private List<ThreeJSLinkDTO> links = new ArrayList<>();
    private List<ThreeJSCircuitDTO> circuits = new ArrayList<>();
    private List<ThreeJSServiceDTO> services = new ArrayList<>();
    @JsonIgnore
    private Map<String, ThreeJsNodeDTO> nodeMap = new ConcurrentHashMap<>();

    private Long nodeCount = 0L;
    private Long linkCount = 0L;
    private Long circuitCount = 0L;

    public void setCircuits(List<CircuitResource> circuits) {
        this.circuits.clear();
        circuits.forEach(c -> {
            this.circuits.add(new ThreeJSCircuitDTO(c));
        });
    }

    public void setCircuitGraph(GraphList<CircuitResource> circuits) {
        this.circuits.clear();
        circuits.forEach(c -> {
            this.circuits.add(new ThreeJSCircuitDTO(c));
        });
    }

    public void setServices(List<ServiceResource> services) {
        this.services.clear();
        services.forEach(c -> {
            this.services.add(new ThreeJSServiceDTO(c));
        });
    }

    /**
     * @return the nodeMap
     */
    public Map<String, ThreeJsNodeDTO> getNodeMap() {
        if (nodeMap.isEmpty()
                && !this.nodes.isEmpty()) {
            this.nodes.forEach(n -> {

                nodeMap.put(n.getId(), n);
            });

        }
        return nodeMap;
    }

    public ThreeJSViewDTO() {
    }

    public ThreeJSViewDTO(FilterResponse filter) {
        if (filter.getPayLoad().getConnections() != null) {
            filter.getPayLoad().getConnections().forEach(connection -> {

                ThreeJsNodeDTO fromNode = new ThreeJsNodeDTO(connection.getFromResource());
                ThreeJsNodeDTO toNode = new ThreeJsNodeDTO(connection.getToResource());

                nodes.add(fromNode);
                this.nodeMap.put(fromNode.getId(), fromNode);
                nodes.add(toNode);
                this.nodeMap.put(toNode.getId(), toNode);
                links.add(new ThreeJSLinkDTO(connection));
            });
        }
    }

    public ThreeJSViewDTO(GraphList<ResourceConnection> connections) {
        //
        // Popula os nós
        //

        if (!connections.isEmpty()) {

            connections.forEach(connection -> {
                ThreeJsNodeDTO fromNode = new ThreeJsNodeDTO(connection.getFromResource());
                ThreeJsNodeDTO toNode = new ThreeJsNodeDTO(connection.getToResource());
                if (!nodes.contains(fromNode)) {
                    nodes.add(fromNode);
                    this.nodeMap.put(fromNode.getId(), fromNode);
                }
                if (!this.nodes.contains(toNode)) {
                    nodes.add(toNode);
                    this.nodeMap.put(toNode.getId(), toNode);
                }
                links.add(new ThreeJSLinkDTO(connection));
            });
            if (connections.isClosed()) {
                /**
                 * Aqui é possível receber as stats
                 */
            }

        }
    }

    public List<ThreeJsNodeDTO> getNodes() {
        return nodes;
    }

    public void setNodes(List<ThreeJsNodeDTO> nodes) {
        this.nodes = nodes;
    }

    public void setNodesByGraph(GraphList<ManagedResource> resources) {
        nodes.clear();
        this.nodeMap.clear();

        try {
            resources.forEach(resource -> {
                ThreeJsNodeDTO node = new ThreeJsNodeDTO(resource);
                this.addNode(node);
            });
        } catch (IllegalStateException ex) {

        }

    }

    public void addNode(ThreeJsNodeDTO node) {
        if (!this.nodes.contains(node)) {
            this.nodeMap.put(node.getId(), node);
            nodes.add(node);
        }
    }

    /**
     * Monta o grafo, com links e nó a partir dos resource connection
     *
     * @param connections
     * @param addNodes - se for true, ele vai adicionar os nós automaticamente
     */
    public void setLinksByGraph(GraphList<ResourceConnection> connections, boolean addNodes) {
        this.links.clear();
        try {
            connections.forEach(connection -> {
                if (addNodes) {
                    if (!this.nodeMap.containsKey(connection.getFromResource().getKey())) {
                        ThreeJsNodeDTO node = new ThreeJsNodeDTO(connection.getFromResource());
                        this.addNode(node);
                    }
                    if (!this.nodeMap.containsKey(connection.getToResource().getKey())) {
                        ThreeJsNodeDTO node = new ThreeJsNodeDTO(connection.getToResource());
                        this.addNode(node);
                    }
                    links.add(new ThreeJSLinkDTO(connection));
                } else {
                    if (this.nodeMap.containsKey(connection.getFromKeyWithoutCollection())
                            && this.nodeMap.containsKey(connection.getToKeyWithoutCollection())) {
                        links.add(new ThreeJSLinkDTO(connection));
                    }
                }

            });
        } catch (IllegalStateException ex) {
            ex.printStackTrace();
        }
    }

    public void setLinksByGraph(GraphList<ResourceConnection> connections) {
        this.setLinksByGraph(connections, false);
    }

    @JsonIgnore
    public List<String> getNodeIds() {
        return this.nodes.stream().map(ThreeJsNodeDTO::getId).collect(Collectors.toList());
    }

    public List<ThreeJSLinkDTO> getLinks() {
        return links;
    }

    public void setLinks(List<ThreeJSLinkDTO> links) {
        this.links = links;
    }

    public ThreeJSViewDTO validate() throws InvalidGraphException {
        return this.validate(false);
    }

    /**
     * Check if Graph is complete
     */
    public ThreeJSViewDTO validate(boolean fixGraph) throws InvalidGraphException {
        List<String> connectionsToRemove = new ArrayList<>();
        if (!this.links.isEmpty()) {
            for (ThreeJSLinkDTO link : this.links) {
                if (this.nodes.stream()
                        .filter(n -> n.getId().equals(link.getSource()) || n.getId().equals(link.getTarget()))
                        .count() > 0) {
                } else {
                    if (this.nodes.stream().filter(n -> n.getId().equals(link.getTarget())).count() > 0) {
                    } else {
                        if (fixGraph) {
                            connectionsToRemove.add(link.getId());
                        } else {
                            throw new InvalidGraphException("Connection Node(target) not found: ConnectionID:["
                                    + link.getId() + "] Node: [" + link.getTarget() + "]").addDetails("nodes", nodes);
                        }
                    }

                    if (this.nodes.stream().filter(n -> n.getId().equals(link.getSource())).count() > 0) {
                    } else {
                        if (fixGraph) {
                            connectionsToRemove.add(link.getId());
                        } else {
                            throw new InvalidGraphException("Connection Node(source) not found: ConnectionID:["
                                    + link.getId() + "] Node: [" + link.getTarget() + "]").addDetails("nodes", nodes);
                        }
                    }
                }

            }

            if (fixGraph) {
                if (!connectionsToRemove.isEmpty()) {
                    connectionsToRemove.forEach(id -> {
                        this.links.remove(id);
                    });
                }
            }
        }

        this.linkCount = Long.valueOf(this.links.size());
        this.nodeCount = Long.valueOf(this.nodes.size());
        this.circuitCount = Long.valueOf(this.circuits.size());
        return this;
    }

    /**
     * @return the nodeCount
     */
    public Long getNodeCount() {
        return nodeCount;
    }

    /**
     * @param nodeCount the nodeCount to set
     */
    public void setNodeCount(Long nodeCount) {
        this.nodeCount = nodeCount;
    }

    /**
     * @return the linkCount
     */
    public Long getLinkCount() {
        return linkCount;
    }

    /**
     * @param linkCount the linkCount to set
     */
    public void setLinkCount(Long linkCount) {
        this.linkCount = linkCount;
    }

}
