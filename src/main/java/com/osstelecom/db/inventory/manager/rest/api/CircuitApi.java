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
package com.osstelecom.db.inventory.manager.rest.api;

import com.osstelecom.db.inventory.manager.exception.ArangoDaoException;
import com.osstelecom.db.inventory.manager.exception.DomainNotFoundException;
import com.osstelecom.db.inventory.manager.exception.GenericException;
import com.osstelecom.db.inventory.manager.exception.InvalidRequestException;
import com.osstelecom.db.inventory.manager.exception.ResourceNotFoundException;
import com.osstelecom.db.inventory.manager.exception.SchemaNotFoundException;
import com.osstelecom.db.inventory.manager.exception.ScriptRuleException;
import com.osstelecom.db.inventory.manager.request.CreateCircuitPathRequest;
import com.osstelecom.db.inventory.manager.request.CreateCircuitRequest;
import com.osstelecom.db.inventory.manager.request.DeleteCircuitRequest;
import com.osstelecom.db.inventory.manager.request.FilterRequest;
import com.osstelecom.db.inventory.manager.request.GetCircuitPathRequest;
import com.osstelecom.db.inventory.manager.request.PatchCircuitResourceRequest;
import com.osstelecom.db.inventory.manager.resources.CircuitResource;
import com.osstelecom.db.inventory.manager.resources.exception.AttributeConstraintViolationException;
import com.osstelecom.db.inventory.manager.response.CreateCircuitPathResponse;
import com.osstelecom.db.inventory.manager.response.CreateCircuitResponse;
import com.osstelecom.db.inventory.manager.response.DeleteCircuitResponse;
import com.osstelecom.db.inventory.manager.response.FilterResponse;
import com.osstelecom.db.inventory.manager.response.GetCircuitPathResponse;
import com.osstelecom.db.inventory.manager.response.GetCircuitResponse;
import com.osstelecom.db.inventory.manager.response.PatchCircuitResourceResponse;
import com.osstelecom.db.inventory.manager.security.model.AuthenticatedCall;
import com.osstelecom.db.inventory.manager.session.CircuitSession;
import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @changelog -------------------------------------------------- 25-11-2022:
 * Lucas Nishimura <lucas.nishimura at telefonica.com> Revisado so métodos de
 * insert update delete
 * @author Lucas Nishimura
 * @created 08.08.2022
 */
@RestController
@RequestMapping("inventory/v1")
public class CircuitApi extends BaseApi {

    @Autowired
    private CircuitSession circuitSession;

    /**
     * Cria um cirtuito....
     *
     * @todo melhorar o tratamento de exception
     * @param request
     * @param domain
     * @return
     * @throws ResourceNotFoundException
     * @throws GenericException
     * @throws SchemaNotFoundException
     * @throws AttributeConstraintViolationException
     * @throws ScriptRuleException
     * @throws AttributeConstraintViolationException
     */
    @AuthenticatedCall(role = {"user"})
    @PutMapping(path = "/{domain}/circuit", produces = "application/json", consumes = "application/json")
    public CreateCircuitResponse createCircuit(@RequestBody CreateCircuitRequest request,
            @PathVariable("domain") String domain, HttpServletRequest httpRequest) throws ArangoDaoException, ResourceNotFoundException, GenericException, SchemaNotFoundException, AttributeConstraintViolationException, ScriptRuleException, AttributeConstraintViolationException, DomainNotFoundException, InvalidRequestException {
        request.setRequestDomain(domain);
        this.setUserDetails(request);
        httpRequest.setAttribute("request", request);
        return circuitSession.createCircuit(request);
    }

    /**
     * Atualiza os dados do circuito
     *
     * @param request
     * @param domain
     * @return
     * @throws DomainNotFoundException
     * @throws ResourceNotFoundException
     * @throws ArangoDaoException
     * @throws IOException
     * @throws InvalidRequestException
     */
    @AuthenticatedCall(role = {"user"})
    @PatchMapping(path = "/{domain}/circuit", produces = "application/json", consumes = "application/json")
    public PatchCircuitResourceResponse patchCircuit(@RequestBody PatchCircuitResourceRequest request,
            @PathVariable("domain") String domain, HttpServletRequest httpRequest) throws DomainNotFoundException, ResourceNotFoundException, ArangoDaoException, IOException, InvalidRequestException, SchemaNotFoundException, GenericException, AttributeConstraintViolationException, ScriptRuleException {
        request.setRequestDomain(domain);
        this.setUserDetails(request);
        httpRequest.setAttribute("request", request);
        return circuitSession.patchCircuitResource(request);
    }

    @AuthenticatedCall(role = {"user"})
    @PatchMapping(path = "/{domain}/circuit/{circuitId}", produces = "application/json", consumes = "application/json")
    public PatchCircuitResourceResponse patchCircuit(@RequestBody PatchCircuitResourceRequest request,
            @PathVariable("domain") String domain, @PathVariable("circuitId") String circuitId, HttpServletRequest httpRequest) throws DomainNotFoundException, ResourceNotFoundException, ArangoDaoException, IOException, InvalidRequestException, SchemaNotFoundException, GenericException, AttributeConstraintViolationException, ScriptRuleException {
        request.setRequestDomain(domain);
        request.getPayLoad().setKey(circuitId);
        this.setUserDetails(request);
        httpRequest.setAttribute("request", request);
        return circuitSession.patchCircuitResource(request);
    }

    /**
     * Cria um path, utilizei o GSON pois parece melhor que o nativo do spring..
     *
     * @param strReq
     * @param domain
     * @return
     * @throws ResourceNotFoundException
     * @throws GenericException
     * @throws SchemaNotFoundException
     * @throws AttributeConstraintViolationException
     * @throws ScriptRuleException
     * @throws AttributeConstraintViolationException
     */
    @AuthenticatedCall(role = {"user"})
    @PutMapping(path = "/{domain}/circuit/path", produces = "application/json", consumes = "application/json")
    public CreateCircuitPathResponse createCircuitPath(@RequestBody CreateCircuitPathRequest request,
            @PathVariable("domain") String domain, HttpServletRequest httpRequest) throws ArangoDaoException, ResourceNotFoundException, GenericException, SchemaNotFoundException, AttributeConstraintViolationException, ScriptRuleException, AttributeConstraintViolationException, DomainNotFoundException, InvalidRequestException {
        request.setRequestDomain(domain);
        this.setUserDetails(request);
        httpRequest.setAttribute("request", request);
        return circuitSession.createCircuitPath(request);
    }

    /**
     * Recupera um circuito
     *
     * @param strReq
     * @param domain
     * @return
     * @throws ResourceNotFoundException
     * @throws GenericException
     * @throws SchemaNotFoundException
     * @throws AttributeConstraintViolationException
     * @throws ScriptRuleException
     * @throws AttributeConstraintViolationException
     * @throws DomainNotFoundException
     */
    @AuthenticatedCall(role = {"user"})
    @PostMapping(path = "/{domain}/circuit/path", produces = "application/json", consumes = "application/json")
    public GetCircuitPathResponse getCircuitPath(@RequestBody GetCircuitPathRequest request,
            @PathVariable("domain") String domain, HttpServletRequest httpRequest) throws ArangoDaoException, ResourceNotFoundException, GenericException, SchemaNotFoundException, AttributeConstraintViolationException, ScriptRuleException, AttributeConstraintViolationException, DomainNotFoundException, InvalidRequestException {
        request.setRequestDomain(domain);
        this.setUserDetails(request);
        httpRequest.setAttribute("request", request);
        return circuitSession.findCircuitPath(request);
    }

    @AuthenticatedCall(role = {"user"})
    @PostMapping(path = "/{domain}/circuit/filter", produces = "application/json", consumes = "application/json")
    public FilterResponse findCircuitsByFilter(@RequestBody FilterRequest filter,
            @PathVariable("domain") String domain, HttpServletRequest httpRequest) throws ArangoDaoException, ResourceNotFoundException, DomainNotFoundException, InvalidRequestException {
        this.setUserDetails(filter);
        filter.setRequestDomain(domain);
        httpRequest.setAttribute("request", filter);
        return circuitSession.findCircuitByFilter(filter);
    }

    /**
     * Recupera os paths de um circuito pelo id, solicitado pelo @Wellyngton
     *
     * @param domain
     * @param id
     * @param httpRequest
     * @return
     * @throws ArangoDaoException
     * @throws ResourceNotFoundException
     * @throws DomainNotFoundException
     * @throws InvalidRequestException
     */
    @AuthenticatedCall(role = {"user"})
    @GetMapping(path = "/{domain}/circuit/{id}/path", produces = "application/json")
    public GetCircuitPathResponse getCircuitPathById(
            @PathVariable("domain") String domain, @PathVariable("id") String id, HttpServletRequest httpRequest) throws ArangoDaoException, ResourceNotFoundException, DomainNotFoundException, InvalidRequestException {
        GetCircuitPathRequest req = new GetCircuitPathRequest(id, domain);
        this.setUserDetails(req);
        req.setRequestDomain(domain);
        httpRequest.setAttribute("request", req);
        return circuitSession.findCircuitPathById(req);
    }

    @AuthenticatedCall(role = {"user"})
    @GetMapping(path = "/{domain}/circuit/{id}", produces = "application/json")
    public GetCircuitResponse getCircuitById(@PathVariable("domain") String domain,
            @PathVariable("id") String circuitId, HttpServletRequest httpRequest) throws DomainNotFoundException, ArangoDaoException, ResourceNotFoundException, InvalidRequestException {
        GetCircuitPathRequest request = new GetCircuitPathRequest(circuitId, domain);
        this.setUserDetails(request);
        httpRequest.setAttribute("request", request);
        return this.circuitSession.findCircuitById(request);
    }

    @AuthenticatedCall(role = {"user"})
    @DeleteMapping(path = "/{domain}/circuit/{id}", produces = "application/json")
    public DeleteCircuitResponse deleteCircuitById(@PathVariable("domain") String domain,
            @PathVariable("id") String circuitId, HttpServletRequest httpRequest) throws DomainNotFoundException, ArangoDaoException, InvalidRequestException {
        DeleteCircuitRequest request = new DeleteCircuitRequest();
        request.setPayLoad(new CircuitResource());
        request.getPayLoad().setKey(circuitId);
        request.setRequestDomain(domain);
        this.setUserDetails(request);
        httpRequest.setAttribute("request", request);
        return this.circuitSession.deleteCircuitById(request);

    }
}
