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
package com.osstelecom.db.inventory.visualization.session;

import com.osstelecom.db.inventory.manager.dto.FilterDTO;
import com.osstelecom.db.inventory.manager.exception.ArangoDaoException;
import com.osstelecom.db.inventory.manager.exception.DomainNotFoundException;
import com.osstelecom.db.inventory.manager.exception.InvalidRequestException;
import com.osstelecom.db.inventory.manager.exception.ResourceNotFoundException;
import com.osstelecom.db.inventory.manager.operation.DomainManager;
import com.osstelecom.db.inventory.manager.request.FilterRequest;
import com.osstelecom.db.inventory.manager.resources.Domain;
import com.osstelecom.db.inventory.manager.resources.GraphList;
import com.osstelecom.db.inventory.manager.resources.ManagedResource;
import com.osstelecom.db.inventory.manager.resources.ResourceConnection;
import com.osstelecom.db.inventory.manager.response.FilterResponse;
import com.osstelecom.db.inventory.manager.session.GraphSession;
import com.osstelecom.db.inventory.manager.session.ResourceSession;
import com.osstelecom.db.inventory.manager.session.UtilSession;
import com.osstelecom.db.inventory.visualization.dto.ThreeJSLinkDTO;
import com.osstelecom.db.inventory.visualization.dto.ThreeJSViewDTO;
import com.osstelecom.db.inventory.visualization.dto.ThreeJsNodeDTO;
import com.osstelecom.db.inventory.visualization.exception.InvalidGraphException;
import com.osstelecom.db.inventory.visualization.request.ExpandNodeRequest;
import com.osstelecom.db.inventory.visualization.request.GetDomainTopologyRequest;
import com.osstelecom.db.inventory.visualization.request.GetStructureDependencyRequest;
import com.osstelecom.db.inventory.visualization.response.ThreeJsViewResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 *
 * @author Lucas Nishimura <lucas.nishimura@gmail.com>
 * @created 12.01.2023
 */
@Service
public class FilterViewSession {

    @Autowired
    private ResourceSession resourceSession;

    @Autowired
    private GraphSession graphSession;

    @Autowired
    private UtilSession utils;

    @Autowired
    private DomainManager domainManager;

    private org.slf4j.Logger logger = LoggerFactory.getLogger(FilterViewSession.class);

    public ThreeJsViewResponse getSampleResult(Long limitSize) throws DomainNotFoundException, ArangoDaoException, InvalidRequestException, ResourceNotFoundException {
        Domain domain = domainManager.getDomain("co");
        FilterRequest request = new FilterRequest(new FilterDTO());
        request.getPayLoad().setAqlFilter(" doc.nodeAddress != 'xyz' ");
        request.getPayLoad().setLimit(limitSize);
        request.getPayLoad().setOffSet(0L);
        request.setRequestDomain(domain.getDomainName());
        request.getPayLoad().getObjects().add("connections");
        FilterResponse filterResponse = resourceSession.findManagedResourceByFilter(request);
        ThreeJsViewResponse response = new ThreeJsViewResponse(new ThreeJSViewDTO(filterResponse));
        return response;
    }

    /**
     * Obtem as estruturas relacionadas a este recurso
     *
     * @param request
     * @return
     * @throws DomainNotFoundException
     * @throws ArangoDaoException
     * @throws ResourceNotFoundException
     * @throws InvalidRequestException
     */
    public ThreeJsViewResponse getResourceStrucureDependency(GetStructureDependencyRequest request) throws DomainNotFoundException, ArangoDaoException, ResourceNotFoundException, InvalidRequestException {
        Domain domain = domainManager.getDomain(request.getRequestDomain());
        ThreeJSViewDTO viewData = new ThreeJSViewDTO();
        /**
         * Vamos arrumar o Recurso que veio
         */
        ManagedResource resource = (ManagedResource) request.getPayLoad();

        resource.setDomain(domain);
        logger.debug("Searching Resource: [{}/{}]", resource.getKey(), resource.getDomainName());
        resource = this.resourceSession.findManagedResource(resource);

        /**
         * Encontrei o Resource. Se e ele for parent procuro os filhos se ele
         * for filho, procuro o parent e os filhos
         */
        String parentKey = resource.getKey();
        if (resource.getStructureId() != null && !resource.getStructureId().trim().equals("")) {
            logger.debug("Searching for Parent Element:[{}/{}]", resource.getStructureId(), resource.getDomainName());
            //
            // É um Filho, vamos encontrar seu pai e consequentemente seus irmãos.
            //

            ManagedResource parent = new ManagedResource(domain, resource.getStructureId());
            parent = this.resourceSession.findManagedResource(parent);
            //
            // Encontrou o Pai agora vamos procurar os filhos
            //
            logger.debug("Parent Found, searching for children");
            viewData.getNodes().add(new ThreeJsNodeDTO(parent));

            parentKey = parent.getKey();

        } else {
            viewData.getNodes().add(new ThreeJsNodeDTO(resource));
        }

        String childNodeAqlFilter = " doc.structureId == @structureId";

        Map<String, Object> bindings = new HashMap<>();
        bindings.put("structureId", parentKey);
        FilterDTO filterChildResource = new FilterDTO(childNodeAqlFilter, bindings, domain.getDomainName());
        filterChildResource.getObjects().add("nodes");
        GraphList<ManagedResource> nodes = this.resourceSession.findManagedResourceByFilter(filterChildResource);

        //
        // Ok Encontramos nodes, agora precisamos procurar as conexões...
        //
        List<String> nodeIds = new ArrayList<>();
        try {
            nodes.forEach(node -> {
                nodeIds.add(node.getKey());
                viewData.getNodes().add(new ThreeJsNodeDTO(node));
            });
        } catch (IOException | IllegalStateException ex) {
            logger.error("Failed to Fetch Nodes from Stream", ex);
        }

        //
        // Agora vamos recuperar as conexões dos elementos
        //
        String childConnectionsAqlFilter = " doc.fromResource._key "
                + " in @nodeIds "
                + "or  doc.toResource._key  in @nodeIds ";

        bindings.clear();
        bindings.put("nodeIds", nodeIds);
        FilterDTO filterChildConnections = new FilterDTO(childConnectionsAqlFilter, bindings, domain.getDomainName());
        filterChildConnections.getObjects().add("connections");
        GraphList<ResourceConnection> connections = this.resourceSession.findResourceConnectionByFilter(filterChildConnections);
        try {
            connections.forEach(connection -> {
                ThreeJSLinkDTO link = new ThreeJSLinkDTO(connection);
                if (!viewData.getLinks().contains(link)) {
                    viewData.getLinks().add(link);
                }
            });
        } catch (IOException | IllegalStateException ex) {
            logger.error("Failed to Fetch Connections from Stream", ex);
        }

        ThreeJsViewResponse response = new ThreeJsViewResponse(viewData);
        return response;
    }

    /**
     * Obtem a topologia do dominio com base em um filtro
     *
     * @param request
     */
    public ThreeJsViewResponse getDomainTopologyByFilter(GetDomainTopologyRequest request) throws InvalidRequestException,
            ArangoDaoException, DomainNotFoundException, ResourceNotFoundException, InvalidGraphException {

        logger.debug("Received:");
        logger.debug(utils.toJson(request));
        //
        // Vamos fazer uma persquisa de NODES!, ele vai ser o ponto inicial
        //
        this.domainManager.getDomain(request.getRequestDomain());
        request.getPayLoad().setDomainName(request.getRequestDomain());
        GraphList<ManagedResource> nodes = this.resourceSession.findManagedResourceByFilter(request.getPayLoad());

        ThreeJSViewDTO view = new ThreeJSViewDTO();

        view.setNodesByGraph(nodes);

        FilterDTO connectionsFilter = new FilterDTO();
        connectionsFilter.setDomainName(request.getRequestDomain());
        String connectionsAQLFilter = " doc.fromResource._key "
                + " in @nodeIds "
                + "or  doc.toResource._key  in @nodeIds ";

        connectionsFilter.addObject("connection");
        connectionsFilter.setAqlFilter(connectionsAQLFilter);
        connectionsFilter.addBinding("nodeIds", view.getNodeIds());

        GraphList<ResourceConnection> connections = this.resourceSession.findResourceConnectionByFilter(connectionsFilter);

        view.setLinksByGraph(connections);

        //
        // Check if the Graph is consistent
        //
        view.validate();

        return new ThreeJsViewResponse(view);
    }

    public ThreeJsViewResponse expandNodeById(ExpandNodeRequest request) throws DomainNotFoundException, ArangoDaoException, ResourceNotFoundException, InvalidRequestException, InvalidGraphException {
        Domain domain = this.domainManager.getDomain(request.getRequestDomain());
        ManagedResource resource = new ManagedResource(domain, request.getPayLoad().getNodeId());
        //
        // Recupera a referencia do resource do DB
        //
        resource = this.resourceSession.findManagedResource(resource);

        GraphList<ResourceConnection> result = this.graphSession.expandNode(resource, request.getPayLoad().getDirection(), request.getPayLoad().getDepth());
        
        return new ThreeJsViewResponse(new ThreeJSViewDTO(result).validate());
    }

}
