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

import com.osstelecom.db.inventory.manager.resources.GraphList;
import com.osstelecom.db.inventory.manager.resources.ResourceConnection;
import com.osstelecom.db.inventory.manager.response.FilterResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Payload de Grafo do Netcompass, a principio o payload original dos recursos
 * são muito . Então criamos este wrapper para encapsular os dados relevantes
 * para o grafo.
 *
 * @author Lucas Nishimura <lucas.nishimura@gmail.com>
 * @created 12.01.2023
 */
public class ThreeJSViewDTO {

    private List<ThreeJsNodeDTO> nodes = new ArrayList<>();
    private List<ThreeJSLinkDTO> links = new ArrayList<>();

    public ThreeJSViewDTO() {
    }

    public ThreeJSViewDTO(FilterResponse filter) {

        if (filter.getPayLoad().getNodes() != null) {
//            filter.getPayLoad().getNodes().forEach(node -> {
//                this.nodes.add(new ThreeJsNodeDTO(node.getKey(), node.getName(), "", node.getDomainName()));
//            });
        }

        if (filter.getPayLoad().getConnections() != null) {
            filter.getPayLoad().getConnections().forEach(connection -> {

                nodes.add(new ThreeJsNodeDTO(connection.getFromResource().getKey(),
                        connection.getFromResource().getName(),
                        connection.getFromResource().getAttributeSchemaName(),
                        connection.getFromResource().getDomainName(), connection.getFromResource().getOperationalStatus()));

                nodes.add(new ThreeJsNodeDTO(connection.getToResource().getKey(),
                        connection.getToResource().getName(),
                        connection.getToResource().getAttributeSchemaName(),
                        connection.getToResource().getDomainName(), connection.getToResource().getOperationalStatus()));

                links.add(new ThreeJSLinkDTO(connection.getFromResource().getKey(), connection.getToResource().getKey()));
            });
        }
    }

    public ThreeJSViewDTO(GraphList<ResourceConnection> connections) {
        //
        // Popula os nós
        //

        if (!connections.isEmpty()) {
            try {
                connections.forEach(connection -> {
                    nodes.add(new ThreeJsNodeDTO(connection.getFromResource().getKey(),
                            connection.getFromResource().getName(),
                            connection.getFromResource().getAttributeSchemaName(),
                            connection.getFromResource().getDomainName(), connection.getFromResource().getOperationalStatus()));

                    nodes.add(new ThreeJsNodeDTO(connection.getToResource().getKey(),
                            connection.getToResource().getName(),
                            connection.getToResource().getAttributeSchemaName(),
                            connection.getToResource().getDomainName(), connection.getToResource().getOperationalStatus()));

                    links.add(new ThreeJSLinkDTO(connection.getFromResource().getKey(), connection.getToResource().getKey()));
                });
            } catch (IOException ex) {
            }
        }
    }

    public List<ThreeJsNodeDTO> getNodes() {
        return nodes;
    }

    public void setNodes(List<ThreeJsNodeDTO> nodes) {
        this.nodes = nodes;
    }

    public List<ThreeJSLinkDTO> getLinks() {
        return links;
    }

    public void setLinks(List<ThreeJSLinkDTO> links) {
        this.links = links;
    }

}
