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
package com.osstelecom.db.inventory.manager.rest.api;

import com.arangodb.ArangoDBException;
import com.osstelecom.db.inventory.manager.exception.ArangoDaoException;
import com.osstelecom.db.inventory.manager.exception.DomainNotFoundException;
import com.osstelecom.db.inventory.manager.exception.GenericException;
import com.osstelecom.db.inventory.manager.exception.InvalidRequestException;
import com.osstelecom.db.inventory.manager.exception.ResourceNotFoundException;
import com.osstelecom.db.inventory.manager.exception.SchemaNotFoundException;
import com.osstelecom.db.inventory.manager.exception.ScriptRuleException;
import com.osstelecom.db.inventory.manager.request.CreateConnectionRequest;
import com.osstelecom.db.inventory.manager.request.CreateResourceLocationRequest;
import com.osstelecom.db.inventory.manager.request.DeleteResourceConnectionRequest;
import com.osstelecom.db.inventory.manager.request.FindResourceConnectionRequest;
import com.osstelecom.db.inventory.manager.request.ListResourceConnectionRequest;
import com.osstelecom.db.inventory.manager.request.PatchResourceConnectionRequest;
import com.osstelecom.db.inventory.manager.resources.exception.AttributeConstraintViolationException;
import com.osstelecom.db.inventory.manager.resources.exception.ConnectionAlreadyExistsException;
import com.osstelecom.db.inventory.manager.resources.exception.MetricConstraintException;
import com.osstelecom.db.inventory.manager.resources.exception.NoResourcesAvailableException;
import com.osstelecom.db.inventory.manager.response.CreateLocationConnectionResponse;
import com.osstelecom.db.inventory.manager.response.CreateResourceConnectionResponse;
import com.osstelecom.db.inventory.manager.response.CreateResourceLocationResponse;
import com.osstelecom.db.inventory.manager.response.DeleteResourceConnectionResponse;
import com.osstelecom.db.inventory.manager.response.FindResourceConnectionResponse;
import com.osstelecom.db.inventory.manager.response.PatchResourceConnectionResponse;
import com.osstelecom.db.inventory.manager.response.TypedListResponse;
import com.osstelecom.db.inventory.manager.session.ResourceSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.osstelecom.db.inventory.manager.security.model.AuthenticatedCall;
import com.osstelecom.db.inventory.manager.session.ResourceLocationSession;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;

/**
 * Classe que representa os elementos do Inventário Teste
 *
 * @author Lucas Nishimura <lucas.nishimura@gmail.com>
 * @created 14.12.2021
 */
@RestController
@RequestMapping("inventory/v1")
public class InventoryApi extends BaseApi {

    @Autowired
    private ResourceSession resourceSession;

    @Autowired
    private ResourceLocationSession resourceLocationSession;

    /**
     * Cria um novo Location
     *
     * @param request
     * @param domain
     * @return
     * @throws GenericException
     */
    @AuthenticatedCall(role = {"user"})
    @PutMapping(path = "/{domain}/location", produces = "application/json", consumes = "application/json")
    public CreateResourceLocationResponse createLocation(@RequestBody CreateResourceLocationRequest request, @PathVariable("domain") String domain, HttpServletRequest httpRequest) throws GenericException, SchemaNotFoundException, AttributeConstraintViolationException, ScriptRuleException, InvalidRequestException, DomainNotFoundException, ArangoDaoException {
        try {
            //
            // Prevalesce o domain da URL.... será que deixo assim ?
            //
            request.setRequestDomain(domain);
            this.setUserDetails(request);
            httpRequest.setAttribute("request", request);
            return resourceLocationSession.createResourceLocation(request);
        } catch (ArangoDBException ex) {
            GenericException exa = new GenericException(ex.getMessage());
            exa.setStatusCode(ex.getResponseCode());
            throw exa;
        }
    }

    /**
     * Cria uma nova conexão
     *
     * @param request
     * @param domain
     * @return
     * @throws GenericException
     * @throws DomainNotFoundException
     */
    @AuthenticatedCall(role = {"user"})
    @PutMapping(path = "/{domain}/location/connection", produces = "application/json", consumes = "application/json")
    public CreateLocationConnectionResponse createResourceLocationConnection(@RequestBody CreateConnectionRequest request, @PathVariable("domain") String domain, HttpServletRequest httpRequest) throws ArangoDaoException, GenericException, SchemaNotFoundException, AttributeConstraintViolationException, ScriptRuleException, ResourceNotFoundException, DomainNotFoundException, InvalidRequestException {
        try {
            this.setUserDetails(request);
            request.setRequestDomain(domain);
            httpRequest.setAttribute("request", request);
            return resourceLocationSession.createResourceLocationConnection(request);
        } catch (ArangoDBException ex) {
            ex.printStackTrace();
            GenericException exa = new GenericException(ex.getMessage());
            exa.setStatusCode(ex.getResponseCode());
            throw exa;
        }
    }

    /**
     * @deprecated @param domain
     * @return
     * @throws InvalidRequestException
     * @throws DomainNotFoundException
     * @throws ResourceNotFoundException
     * @throws ArangoDaoException
     */
    @AuthenticatedCall(role = {"user"})
    @GetMapping(path = "/{domain}/connection", produces = "application/json")
    public TypedListResponse listResourceConnections(@PathVariable("domain") String domain, HttpServletRequest httpRequest) throws InvalidRequestException, DomainNotFoundException, ResourceNotFoundException, ArangoDaoException {
        //
        // This is a Find ALL Query
        //
        ListResourceConnectionRequest listRequest = new ListResourceConnectionRequest(domain);
        this.setUserDetails(listRequest);

        httpRequest.setAttribute("request", listRequest);
        return resourceSession.listResourceConnection(listRequest);
    }

    @AuthenticatedCall(role = {"user"})
    @GetMapping(path = "/{domain}/resource/category", produces = "application/json")
    public TypedListResponse getResourceCategories(@PathVariable("domain") String domainName, HttpServletRequest httpRequest) throws DomainNotFoundException, ResourceNotFoundException, ArangoDaoException, InvalidRequestException, AttributeConstraintViolationException {
        List<String> mockList = new ArrayList<>();
        mockList.add("default");
        TypedListResponse response = new TypedListResponse(mockList);
        return response;
    }

    @AuthenticatedCall(role = {"user"})
    @GetMapping(path = "/{domain}/resource/businessStatus", produces = "application/json")
    public TypedListResponse getBusinessStatus(@PathVariable("domain") String domainName, HttpServletRequest httpRequest) throws DomainNotFoundException, ResourceNotFoundException, ArangoDaoException, InvalidRequestException, AttributeConstraintViolationException {
        List<String> mockList = new ArrayList<>();
        mockList.add("Planned");
        mockList.add("Pending Connection");
        mockList.add("Active");
        mockList.add("Pending Disconnection");
        mockList.add("Inactive");
        TypedListResponse response = new TypedListResponse(mockList);
        return response;
    }

    @AuthenticatedCall(role = {"user"})
    @GetMapping(path = "/{domain}/resource/operStatus", produces = "application/json")
    public TypedListResponse getOperStatus(@PathVariable("domain") String domainName, HttpServletRequest httpRequest) throws DomainNotFoundException, ResourceNotFoundException, ArangoDaoException, InvalidRequestException, AttributeConstraintViolationException {
        List<String> mockList = new ArrayList<>();
        mockList.add("Up");
        mockList.add("Down");
        TypedListResponse response = new TypedListResponse(mockList);
        return response;
    }

    @AuthenticatedCall(role = {"user"})
    @GetMapping(path = "/{domain}/resource/adminStatus", produces = "application/json")
    public TypedListResponse getAdminStatus(@PathVariable("domain") String domainName, HttpServletRequest httpRequest) throws DomainNotFoundException, ResourceNotFoundException, ArangoDaoException, InvalidRequestException, AttributeConstraintViolationException {
        List<String> mockList = new ArrayList<>();
        mockList.add("Up");
        mockList.add("Down");
        TypedListResponse response = new TypedListResponse(mockList);
        return response;
    }

    /**
     * Cria uma nova conexão
     *
     * @param request
     * @param domain
     * @return
     * @throws GenericException
     */
    @AuthenticatedCall(role = {"user"})
    @PutMapping(path = "/{domain}/resource/connection", produces = "application/json", consumes = "application/json")
    public CreateResourceConnectionResponse createResourceConnection(@RequestBody CreateConnectionRequest request, @PathVariable("domain") String domain, HttpServletRequest httpRequest) throws GenericException, SchemaNotFoundException, AttributeConstraintViolationException, ScriptRuleException, InvalidRequestException, ResourceNotFoundException, ConnectionAlreadyExistsException, MetricConstraintException, NoResourcesAvailableException, DomainNotFoundException, ArangoDaoException {
        try {
            this.setUserDetails(request);
            request.setRequestDomain(domain);
            httpRequest.setAttribute("request", request);
            return resourceSession.createResourceConnection(request);
        } catch (ArangoDBException ex) {
            ex.printStackTrace();
            GenericException exa = new GenericException(ex.getMessage());
            exa.setStatusCode(ex.getResponseCode());
            throw exa;
        }
    }

    @AuthenticatedCall(role = {"user"})
    @PatchMapping(path = "/{domain}/resource/connection", produces = "application/json", consumes = "application/json")
    public PatchResourceConnectionResponse patchResourceConnection(@RequestBody PatchResourceConnectionRequest request, @PathVariable("domain") String domainName, HttpServletRequest httpRequest) throws DomainNotFoundException, ResourceNotFoundException, ArangoDaoException, InvalidRequestException, AttributeConstraintViolationException {
        this.setUserDetails(request);
        request.setRequestDomain(domainName);
        httpRequest.setAttribute("request", request);
        return this.resourceSession.patchResourceConnection(request);
    }

    @AuthenticatedCall(role = {"user"})
    @PatchMapping(path = "/{domain}/resource/connection/{resourceId}", produces = "application/json", consumes = "application/json")
    public PatchResourceConnectionResponse patchResourceConnection(@RequestBody PatchResourceConnectionRequest request, @PathVariable("domain") String domainName, @PathVariable("resourceId") String resourceId, HttpServletRequest httpRequest) throws DomainNotFoundException, ResourceNotFoundException, ArangoDaoException, InvalidRequestException, AttributeConstraintViolationException {
        this.setUserDetails(request);
        request.setRequestDomain(domainName);
        request.getPayLoad().setId(resourceId);
        httpRequest.setAttribute("request", request);
        return this.resourceSession.patchResourceConnection(request);
    }

    @AuthenticatedCall(role = {"user"})
    @DeleteMapping(path = "/{domain}/resource/connection/{resourceId}", produces = "application/json")
    public DeleteResourceConnectionResponse deleteResourceConnectionById(@PathVariable("domain") String domain, @PathVariable("resourceId") String resourceId, HttpServletRequest httpRequest) throws InvalidRequestException, DomainNotFoundException, ResourceNotFoundException, ArangoDaoException {
        DeleteResourceConnectionRequest deleteRequest = new DeleteResourceConnectionRequest(resourceId, domain);
        this.setUserDetails(deleteRequest);
        httpRequest.setAttribute("request", deleteRequest);
        return resourceSession.deleteResourceConnection(deleteRequest);
    }

    @AuthenticatedCall(role = {"user"})
    @GetMapping(path = "/{domain}/resource/connection/{resourceId}", produces = "application/json")
    public FindResourceConnectionResponse findResourceConnectionById(@PathVariable("domain") String domain, @PathVariable("resourceId") String resourceId, HttpServletRequest httpRequest) throws InvalidRequestException, DomainNotFoundException, ResourceNotFoundException, ArangoDaoException {
        FindResourceConnectionRequest findRequest = new FindResourceConnectionRequest(resourceId, domain);
        this.setUserDetails(findRequest);
        httpRequest.setAttribute("request", findRequest);
        return resourceSession.findResourceConnectionById(findRequest);
    }

//    /**
//     * @todo: to be removed, método de teste
//     * @param strReq
//     * @param threads
//     * @return
//     */
//    @PostMapping(path = "test/{threads}", produces = "application/json", consumes = "application/json")
//    public GetCircuitPathResponse test(@RequestBody String strReq, @PathVariable("threads") Integer threads) {
//        GetCircuitPathRequest request = gson.fromJson(strReq, GetCircuitPathRequest.class);
//        GetCircuitPathResponse resp = new GetCircuitPathResponse(request.getPayLoad());
//        resourceSession.test(request.getPayLoad().getCircuit().getNodeAddress(), threads);
//        return resp;
//    }
}
