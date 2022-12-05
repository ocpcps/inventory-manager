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

import com.arangodb.ArangoDBException;
import com.osstelecom.db.inventory.manager.exception.ArangoDaoException;
import com.osstelecom.db.inventory.manager.exception.DomainNotFoundException;
import com.osstelecom.db.inventory.manager.exception.GenericException;
import com.osstelecom.db.inventory.manager.exception.InvalidRequestException;
import com.osstelecom.db.inventory.manager.exception.ResourceNotFoundException;
import com.osstelecom.db.inventory.manager.exception.SchemaNotFoundException;
import com.osstelecom.db.inventory.manager.exception.ScriptRuleException;
import com.osstelecom.db.inventory.manager.request.CreateManagedResourceRequest;
import com.osstelecom.db.inventory.manager.request.DeleteManagedResourceRequest;
import com.osstelecom.db.inventory.manager.request.FilterRequest;
import com.osstelecom.db.inventory.manager.request.FindManagedResourceRequest;
import com.osstelecom.db.inventory.manager.request.ListManagedResourceRequest;
import com.osstelecom.db.inventory.manager.request.PatchManagedResourceRequest;
import com.osstelecom.db.inventory.manager.resources.exception.AttributeConstraintViolationException;
import com.osstelecom.db.inventory.manager.response.CreateManagedResourceResponse;
import com.osstelecom.db.inventory.manager.response.DeleteManagedResourceResponse;
import com.osstelecom.db.inventory.manager.response.FilterResponse;
import com.osstelecom.db.inventory.manager.response.FindManagedResourceResponse;
import com.osstelecom.db.inventory.manager.response.PatchManagedResourceResponse;
import com.osstelecom.db.inventory.manager.response.TypedListResponse;
import com.osstelecom.db.inventory.manager.security.model.AuthenticatedCall;
import com.osstelecom.db.inventory.manager.session.ResourceSession;
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
 * Deals with resource API
 *
 * @author Lucas Nishimura <lucas.nishimura@gmail.com>
 * @created 25.11.2022
 */
@RestController
@RequestMapping("inventory/v1")
public class ResourceApi extends BaseApi {

    @Autowired
    private ResourceSession resourceSession;

    /**
     * Cria um Managed Resource
     *
     * @param requestBody
     * @param domain
     * @return
     * @throws GenericException
     * @throws SchemaNotFoundException
     * @throws AttributeConstraintViolationException
     * @throws ScriptRuleException
     * @throws InvalidRequestException
     * @throws DomainNotFoundException
     */
    @AuthenticatedCall(role = {"user"})
    @PutMapping(path = "/{domain}/resource", produces = "application/json", consumes = "application/json")
    public CreateManagedResourceResponse createManagedResource(@RequestBody CreateManagedResourceRequest request, @PathVariable("domain") String domain) throws GenericException, SchemaNotFoundException, AttributeConstraintViolationException, ScriptRuleException, InvalidRequestException, DomainNotFoundException, ArangoDaoException, ResourceNotFoundException {
        try {
            this.setUserDetails(request);
            request.setRequestDomain(domain);
            return resourceSession.createManagedResource(request);
        } catch (ArangoDBException ex) {
            GenericException exa = new GenericException(ex.getMessage());
            exa.setStatusCode(ex.getResponseCode());
            throw exa;
        }
    }

    /**
     * Find Managed Resource By ID
     *
     * @param domain
     * @param resourceId
     * @return
     * @throws InvalidRequestException
     * @throws DomainNotFoundException
     * @throws ResourceNotFoundException
     * @throws ArangoDaoException
     */
    @AuthenticatedCall(role = {"user"})
    @GetMapping(path = "/{domain}/resource/{resourceId}", produces = "application/json")
    public FindManagedResourceResponse findManagedResourceById(@PathVariable("domain") String domain, @PathVariable("resourceId") String resourceId) throws InvalidRequestException, DomainNotFoundException, ResourceNotFoundException, ArangoDaoException {
        FindManagedResourceRequest findRequest = new FindManagedResourceRequest(resourceId, domain);
        this.setUserDetails(findRequest);
        return resourceSession.findManagedResourceById(findRequest);
    }

    /**
     * Delete managed resource by id
     *
     * @param domain
     * @param resourceId
     * @return
     * @throws InvalidRequestException
     * @throws DomainNotFoundException
     * @throws ResourceNotFoundException
     * @throws ArangoDaoException
     */
    @AuthenticatedCall(role = {"user"})
    @DeleteMapping(path = "/{domain}/resource/{resourceId}", produces = "application/json")
    public DeleteManagedResourceResponse deleteManagedResourceById(@PathVariable("domain") String domain, @PathVariable("resourceId") String resourceId) throws InvalidRequestException, DomainNotFoundException, ResourceNotFoundException, ArangoDaoException {
        DeleteManagedResourceRequest deleteRequest = new DeleteManagedResourceRequest(resourceId, domain);
        this.setUserDetails(deleteRequest);
        return resourceSession.deleteManagedResource(deleteRequest);
    }

    /**
     * Lista os recursos mas como é muito esse método vai ser deprecado e
     * trocado pelo filtro que suporta paginação
     *
     * @deprecated
     * @param domain
     * @return
     * @throws InvalidRequestException
     * @throws DomainNotFoundException
     * @throws ResourceNotFoundException
     * @throws ArangoDaoException
     */
    @AuthenticatedCall(role = {"user"})
    @GetMapping(path = "/{domain}/resource", produces = "application/json")
    public TypedListResponse listManagedResource(@PathVariable("domain") String domain) throws InvalidRequestException, DomainNotFoundException, ResourceNotFoundException, ArangoDaoException {
        //
        // This is a Find ALL Query
        //
        ListManagedResourceRequest listRequest = new ListManagedResourceRequest(domain);
        this.setUserDetails(listRequest);

        return resourceSession.listManagedResources(listRequest);
    }

    /**
     * Aplica um filtro
     *
     * @param filter
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
    @PostMapping(path = {"/{domain}/filter", "/{domain}/resource/filter"}, produces = "application/json", consumes = "application/json")
    public FilterResponse findManagedResourceByFilter(@RequestBody FilterRequest filter, @PathVariable("domain") String domain) throws ArangoDaoException, ResourceNotFoundException, DomainNotFoundException, InvalidRequestException {
        this.setUserDetails(filter);
        filter.setRequestDomain(domain);
        return resourceSession.findManagedResourceByFilter(filter);
    }

    /**
     * Atualiza um managed resource
     *
     * @param strReq
     * @param domainName
     * @param resourceId
     * @return
     * @throws DomainNotFoundException
     * @throws ResourceNotFoundException
     * @throws ArangoDaoException
     * @throws InvalidRequestException
     */
    @AuthenticatedCall(role = {"user"})
    @PatchMapping(path = "/{domain}/resource/{resourceId}", produces = "application/json", consumes = "application/json")
    public PatchManagedResourceResponse patchManagedResource(@RequestBody PatchManagedResourceRequest request, @PathVariable("domain") String domainName, @PathVariable("resourceId") String resourceId) throws DomainNotFoundException, ResourceNotFoundException, ArangoDaoException, InvalidRequestException, AttributeConstraintViolationException, ScriptRuleException {
        this.setUserDetails(request);
        request.setRequestDomain(domainName);
        request.getPayLoad().setId(resourceId);
        return this.resourceSession.patchManagedResource(request);
    }

    @AuthenticatedCall(role = {"user"})
    @PatchMapping(path = "/{domain}/resource", produces = "application/json", consumes = "application/json")
    public PatchManagedResourceResponse patchManagedResource(@RequestBody PatchManagedResourceRequest request, @PathVariable("domain") String domainName) throws DomainNotFoundException, ResourceNotFoundException, ArangoDaoException, InvalidRequestException, AttributeConstraintViolationException, ScriptRuleException {
        this.setUserDetails(request);
        request.setRequestDomain(domainName);
        return this.resourceSession.patchManagedResource(request);
    }

}
