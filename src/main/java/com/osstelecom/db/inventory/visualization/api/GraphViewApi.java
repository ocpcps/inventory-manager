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
package com.osstelecom.db.inventory.visualization.api;

import com.osstelecom.db.inventory.manager.exception.ArangoDaoException;
import com.osstelecom.db.inventory.manager.exception.DomainNotFoundException;
import com.osstelecom.db.inventory.manager.exception.InvalidRequestException;
import com.osstelecom.db.inventory.manager.exception.ResourceNotFoundException;
import com.osstelecom.db.inventory.manager.resources.CircuitResource;
import com.osstelecom.db.inventory.manager.resources.ManagedResource;
import com.osstelecom.db.inventory.manager.resources.ResourceConnection;
import com.osstelecom.db.inventory.manager.rest.api.BaseApi;
import com.osstelecom.db.inventory.visualization.dto.ExpandNodeDTO;
import com.osstelecom.db.inventory.visualization.exception.InvalidGraphException;
import com.osstelecom.db.inventory.visualization.request.ExpandNodeRequest;
import com.osstelecom.db.inventory.visualization.request.GetCircuitByConnectionTopologyRequest;
import com.osstelecom.db.inventory.visualization.request.GetConnectionsByCircuitRequest;
import com.osstelecom.db.inventory.visualization.request.GetDomainTopologyRequest;
import com.osstelecom.db.inventory.visualization.request.GetStructureTopologyDependencyRequest;
import com.osstelecom.db.inventory.visualization.response.ThreeJsViewResponse;
import com.osstelecom.db.inventory.visualization.session.FilterViewSession;
import javax.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @author Lucas Nishimura <lucas.nishimura@gmail.com>
 * @created 12.01.2023
 */
@RestController
@RequestMapping("inventory/v1/graph-view")
public class GraphViewApi extends BaseApi {

    @Autowired
    private FilterViewSession viewSession;

    private Logger logger = LoggerFactory.getLogger(GraphViewApi.class);

    /**
     * Obtem um sample dataset
     *
     * @param limit
     * @param httpRequest
     * @return
     * @throws DomainNotFoundException
     * @throws ArangoDaoException
     * @throws InvalidRequestException
     * @throws ResourceNotFoundException
     */
    @GetMapping(path = "/sample/{limit}", produces = "application/json")
    public ThreeJsViewResponse getSampleView(@PathVariable("limit") Long limit, HttpServletRequest httpRequest) throws DomainNotFoundException, ArangoDaoException, InvalidRequestException, ResourceNotFoundException {
        return viewSession.getSampleResult(limit);
    }

    /**
     * Obtem a topologia da estrutura
     *
     * @param domain
     * @param resourceKey
     * @param httpRequest
     * @return
     * @throws DomainNotFoundException
     * @throws ArangoDaoException
     * @throws ResourceNotFoundException
     * @throws InvalidRequestException
     */
    @GetMapping(path = "{domain}/resource/{resourceKey}", produces = "application/json")
    public ThreeJsViewResponse getResourceStrucureDependency(@PathVariable("domain") String domain, @PathVariable("resourceKey") String resourceKey, HttpServletRequest httpRequest) throws DomainNotFoundException, ArangoDaoException, ResourceNotFoundException, InvalidRequestException, InvalidGraphException {
        GetStructureTopologyDependencyRequest request = new GetStructureTopologyDependencyRequest();
        request.setPayLoad(new ManagedResource());
        request.getPayLoad().setKey(resourceKey);
        request.setRequestDomain(domain);
        this.setUserDetails(request);
        httpRequest.setAttribute("request", request);
        return this.viewSession.getResourceStrucureDependency(request);
    }

    

    @GetMapping(path = "{domain}/resource/{resourceKey}/services", produces = "application/json")
    public ThreeJsViewResponse getServicesByResourceId(@PathVariable("domain") String domain,
            @PathVariable("resourceKey") String resourceKey) throws DomainNotFoundException, ArangoDaoException, ResourceNotFoundException, InvalidRequestException, InvalidGraphException {
        
        return null;
    }

    /**
     * Expand um nó em uma direção especifica
     *
     * @param domain
     * @param resourceKey
     * @param direction
     * @param depth
     * @param httpRequest
     * @return
     * @throws DomainNotFoundException
     * @throws ArangoDaoException
     * @throws ResourceNotFoundException
     * @throws InvalidRequestException
     * @throws InvalidGraphException
     */
    @GetMapping(path = "{domain}/resource/{resourceKey}/expand/{direction}/{depth}", produces = "application/json")
    public ThreeJsViewResponse expandNodeById(@PathVariable("domain") String domain,
            @PathVariable("resourceKey") String resourceKey,
            @PathVariable("direction") String direction, @PathVariable("depth") Integer depth,
            HttpServletRequest httpRequest) throws DomainNotFoundException, ArangoDaoException, ResourceNotFoundException, InvalidRequestException, InvalidGraphException {
        ExpandNodeRequest request = new ExpandNodeRequest();
        request.setPayLoad(new ExpandNodeDTO(resourceKey, domain, direction, depth));
        request.setRequestDomain(domain);
        this.setUserDetails(request);
        httpRequest.setAttribute("request", request);
        return this.viewSession.expandNodeById(request);
    }

    @GetMapping(path = "{domain}/circuit/{circuitKey}/expand", produces = "application/json")
    public ThreeJsViewResponse expandCircuitById(@PathVariable("domain") String domain,
            @PathVariable("circuitKey") String circuitKey,
            HttpServletRequest httpRequest) throws DomainNotFoundException, ArangoDaoException, ResourceNotFoundException, InvalidRequestException, InvalidGraphException {
        GetConnectionsByCircuitRequest request = new GetConnectionsByCircuitRequest();
        CircuitResource circuit = new CircuitResource();
        circuit.setKey(circuitKey);
        circuit.setDomainName(domain);
        request.setPayLoad(circuit);
        request.setRequestDomain(domain);
        this.setUserDetails(request);
        httpRequest.setAttribute("request", request);
        return this.viewSession.getConnectionsByCircuit(request);
    }

    /**
     * Dado uma conexão, ela diz quais circuitos passam por ali
     *
     * @param domain
     * @param connectionKey
     * @return
     * @throws DomainNotFoundException
     * @throws ArangoDaoException
     * @throws ResourceNotFoundException
     * @throws InvalidRequestException
     * @throws InvalidGraphException
     */
    @GetMapping(path = "{domain}/connection/{connectionKey}/circuits", produces = "application/json")
    public ThreeJsViewResponse getCircuitsByConnectionId(@PathVariable("domain") String domain,
            @PathVariable("connectionKey") String connectionKey) throws DomainNotFoundException, ArangoDaoException, ResourceNotFoundException, InvalidRequestException, InvalidGraphException {
        GetCircuitByConnectionTopologyRequest request = new GetCircuitByConnectionTopologyRequest();
        request.setPayLoad(new ResourceConnection());
        request.getPayLoad().setKey(connectionKey);
        request.setRequestDomain(domain);
        return this.viewSession.getCircuitsByConnectionId(request);
    }

    @GetMapping(path = "{domain}/connection/{connectionKey}/services", produces = "application/json")
    public ThreeJsViewResponse getServicesByConnectionId(@PathVariable("domain") String domain,
            @PathVariable("connectionKey") String connectionKey) throws DomainNotFoundException, ArangoDaoException, ResourceNotFoundException, InvalidRequestException, InvalidGraphException {
        
        return null;
    }

    /**
     * Obtém através de um filtro dados da topologia
     *
     * @param domain
     * @param request
     * @param httpRequest
     * @return
     * @throws DomainNotFoundException
     * @throws ArangoDaoException
     * @throws ResourceNotFoundException
     * @throws InvalidRequestException
     * @throws InvalidGraphException
     */
    @PostMapping(path = "{domain}/topology/filter", produces = "application/json", consumes = "application/json")
    public ThreeJsViewResponse getDomainTopologyByFilter(@PathVariable("domain") String domain,
            @RequestBody GetDomainTopologyRequest request, HttpServletRequest httpRequest)
            throws DomainNotFoundException, ArangoDaoException, ResourceNotFoundException,
            InvalidRequestException, InvalidGraphException {
        request.setRequestDomain(domain);
        this.setUserDetails(request);
        httpRequest.setAttribute("request", request);
        return this.viewSession.getDomainTopologyByFilter(request);
    }

}
