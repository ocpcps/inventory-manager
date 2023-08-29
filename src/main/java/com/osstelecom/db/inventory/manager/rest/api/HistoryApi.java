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
import com.osstelecom.db.inventory.manager.exception.InvalidRequestException;
import com.osstelecom.db.inventory.manager.exception.ResourceNotFoundException;
import com.osstelecom.db.inventory.manager.request.FindHistoryCircuitRequest;
import com.osstelecom.db.inventory.manager.request.FindHistoryConnectionRequest;
import com.osstelecom.db.inventory.manager.request.FindHistoryResourceRequest;
import com.osstelecom.db.inventory.manager.request.FindHistoryServiceRequest;
import com.osstelecom.db.inventory.manager.response.GetHistoryResponse;
import com.osstelecom.db.inventory.manager.security.model.AuthenticatedCall;
import com.osstelecom.db.inventory.manager.session.HistorySession;

import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @changelog -------------------------------------------------- 25-11-2022:
 *            Lucas Nishimura <lucas.nishimura at telefonica.com> Revisado so
 *            métodos de
 *            insert update delete
 * @author Lucas Nishimura
 * @created 08.08.2022
 */
@RestController
@RequestMapping("inventory/v1")
public class HistoryApi extends BaseApi {

    @Autowired
    private HistorySession historySession;

    @AuthenticatedCall(role = { "user" })
    @GetMapping(path = "/{domain}/history/resource/{id}", produces = "application/json")
    public GetHistoryResponse getHistoryResourceById(@PathVariable("domain") String domain,
            @PathVariable("id") String id, HttpServletRequest httpRequest)
            throws DomainNotFoundException, ArangoDaoException, ResourceNotFoundException, InvalidRequestException {
        FindHistoryResourceRequest request = new FindHistoryResourceRequest(id, domain);
        this.setUserDetails(request);
        httpRequest.setAttribute("request", request);
        return historySession.getHistoryResourceById(request);
    }

    @AuthenticatedCall(role = { "user" })
    @GetMapping(path = "/{domain}/history/connection/{id}", produces = "application/json")
    public GetHistoryResponse getHistoryConnectionById(@PathVariable("domain") String domain,
            @PathVariable("id") String id, HttpServletRequest httpRequest)
            throws DomainNotFoundException, ArangoDaoException, ResourceNotFoundException, InvalidRequestException {
        FindHistoryConnectionRequest request = new FindHistoryConnectionRequest(id, domain);
        this.setUserDetails(request);
        httpRequest.setAttribute("request", request);
        return historySession.getHistoryConnectionById(request);
    }

    @AuthenticatedCall(role = { "user" })
    @GetMapping(path = "/{domain}/history/circuit/{id}", produces = "application/json")
    public GetHistoryResponse getHistoryCircuitById(@PathVariable("domain") String domain,
            @PathVariable("id") String id, HttpServletRequest httpRequest)
            throws DomainNotFoundException, ArangoDaoException, ResourceNotFoundException, InvalidRequestException {
        FindHistoryCircuitRequest request = new FindHistoryCircuitRequest(id, domain);
        this.setUserDetails(request);
        httpRequest.setAttribute("request", request);
        return historySession.getHistoryCircuitById(request);
    }

    @AuthenticatedCall(role = { "user" })
    @GetMapping(path = "/{domain}/history/service/{id}", produces = "application/json")
    public GetHistoryResponse getHistoryServicenById(@PathVariable("domain") String domain,
            @PathVariable("id") String id, HttpServletRequest httpRequest)
            throws DomainNotFoundException, ArangoDaoException, ResourceNotFoundException, InvalidRequestException {
        FindHistoryServiceRequest request = new FindHistoryServiceRequest(id, domain);
        this.setUserDetails(request);
        httpRequest.setAttribute("request", request);
        return historySession.getHistoryServiceById(request);
    }

}
