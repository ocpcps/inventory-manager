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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.osstelecom.db.inventory.manager.exception.ArangoDaoException;
import com.osstelecom.db.inventory.manager.exception.DomainNotFoundException;
import com.osstelecom.db.inventory.manager.exception.GenericException;
import com.osstelecom.db.inventory.manager.exception.InvalidRequestException;
import com.osstelecom.db.inventory.manager.exception.ResourceNotFoundException;
import com.osstelecom.db.inventory.manager.exception.SchemaNotFoundException;
import com.osstelecom.db.inventory.manager.exception.ScriptRuleException;
import com.osstelecom.db.inventory.manager.request.CreateIconRequest;
import com.osstelecom.db.inventory.manager.request.CreateServiceRequest;
import com.osstelecom.db.inventory.manager.request.DeleteIconRequest;
import com.osstelecom.db.inventory.manager.request.DeleteServiceRequest;
import com.osstelecom.db.inventory.manager.request.FilterRequest;
import com.osstelecom.db.inventory.manager.request.GetIconRequest;
import com.osstelecom.db.inventory.manager.request.GetServiceRequest;
import com.osstelecom.db.inventory.manager.request.PatchIconRequest;
import com.osstelecom.db.inventory.manager.request.PatchServiceRequest;
import com.osstelecom.db.inventory.manager.resources.model.IconModel;
import com.osstelecom.db.inventory.manager.resources.exception.AttributeConstraintViolationException;
import com.osstelecom.db.inventory.manager.response.CreateIconResponse;
import com.osstelecom.db.inventory.manager.response.CreateServiceResponse;
import com.osstelecom.db.inventory.manager.response.DeleteIconResponse;
import com.osstelecom.db.inventory.manager.response.DeleteServiceResponse;
import com.osstelecom.db.inventory.manager.response.FilterResponse;
import com.osstelecom.db.inventory.manager.response.GetIconResponse;
import com.osstelecom.db.inventory.manager.response.GetServiceResponse;
import com.osstelecom.db.inventory.manager.response.PatchIconResponse;
import com.osstelecom.db.inventory.manager.response.PatchServiceResponse;
import com.osstelecom.db.inventory.manager.security.model.AuthenticatedCall;
import com.osstelecom.db.inventory.manager.session.IconSession;
import javax.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.PostMapping;

/**
 *
 * @author Lucas Nishimura <lucas.nishimura@gmail.com>
 * @created 18.08.2022
 */
@RestController
@RequestMapping("inventory/v1")
public class IconApi extends BaseApi {

    @Autowired
    private IconSession iconSession;

    @AuthenticatedCall(role = {"user"})
    @GetMapping(path = "/icon/{schemaName}", produces = "application/json")
    public GetIconResponse getIconById(@PathVariable("schemaName") String schemaName, HttpServletRequest httpRequest) throws InvalidRequestException, ResourceNotFoundException, DomainNotFoundException, ArangoDaoException {
        GetIconRequest request = new GetIconRequest();
        this.setUserDetails(request);
        request.setPayLoad(new IconModel(schemaName)); //ServiceResource

        httpRequest.setAttribute("request", request);
        return iconSession.getIconById(request);
    }

    @AuthenticatedCall(role = {"user"})
    @DeleteMapping(path = "/icon/{schemaName}", produces = "application/json")
    public DeleteIconResponse deleteIcon(@PathVariable("schemaName") String schemaName, HttpServletRequest httpRequest) throws DomainNotFoundException, ArangoDaoException {
        DeleteIconRequest request = new DeleteIconRequest(schemaName);
        this.setUserDetails(request);
        httpRequest.setAttribute("request", request);
        return iconSession.deleteIcon(request);
    }

    @AuthenticatedCall(role = {"user"})
    @PutMapping(path = "/icon", produces = "application/json", consumes = "application/json")
    public CreateIconResponse createIcon(@RequestBody CreateIconRequest request, HttpServletRequest httpRequest) throws InvalidRequestException, DomainNotFoundException, ResourceNotFoundException, ArangoDaoException {
        this.setUserDetails(request);
        httpRequest.setAttribute("request", request);
        return iconSession.createIcon(request);
    }

    @AuthenticatedCall(role = {"user"})
    @PatchMapping(path = "/service/{schemaName}", produces = "application/json", consumes = "application/json")
    public PatchIconResponse patchManagedResource(@RequestBody PatchIconRequest request, @PathVariable("schemaName") String schemaName, HttpServletRequest httpRequest) throws InvalidRequestException, DomainNotFoundException, ResourceNotFoundException, ArangoDaoException, SchemaNotFoundException, GenericException, AttributeConstraintViolationException, ScriptRuleException {
        this.setUserDetails(request);
        request.setPayLoad(new IconModel(schemaName));
        httpRequest.setAttribute("request", request);
        return iconSession.updateIcon(request);
    }

    // @AuthenticatedCall(role = {"user"})
    // @PatchMapping(path = "/{domainName}/service", produces = "application/json", consumes = "application/json")
    // public PatchServiceResponse patchManagedResource(@RequestBody PatchServiceRequest request, @PathVariable("domainName") String domainName, HttpServletRequest httpRequest) throws InvalidRequestException, DomainNotFoundException, ResourceNotFoundException, ArangoDaoException, SchemaNotFoundException, GenericException, AttributeConstraintViolationException, ScriptRuleException {
    //     this.setUserDetails(request);
    //     request.setRequestDomain(domainName);
    //     httpRequest.setAttribute("request", request);
    //     return iconSession.updateService(request);
    // }

    // @AuthenticatedCall(role = {"user"})
    // @PostMapping(path = "/{domain}/service/filter", produces = "application/json", consumes = "application/json")
    // public FilterResponse findCircuitsByFilter(@RequestBody FilterRequest filter, @PathVariable("domain") String domain, HttpServletRequest httpRequest) throws ArangoDaoException, ResourceNotFoundException, DomainNotFoundException, InvalidRequestException {
    //     this.setUserDetails(filter);
    //     filter.setRequestDomain(domain);
    //     httpRequest.setAttribute("request", filter);
    //     return iconSession.findServiceByFilter(filter);
    // }
}
