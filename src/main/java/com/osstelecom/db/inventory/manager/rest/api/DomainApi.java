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
import com.osstelecom.db.inventory.manager.exception.DomainAlreadyExistsException;
import com.osstelecom.db.inventory.manager.exception.DomainNotFoundException;
import com.osstelecom.db.inventory.manager.exception.GenericException;
import com.osstelecom.db.inventory.manager.exception.InvalidRequestException;
import com.osstelecom.db.inventory.manager.exception.ResourceNotFoundException;
import com.osstelecom.db.inventory.manager.request.CreateDomainRequest;
import com.osstelecom.db.inventory.manager.request.DeleteDomainRequest;
import com.osstelecom.db.inventory.manager.request.UpdateDomainRequest;
import com.osstelecom.db.inventory.manager.response.CreateDomainResponse;
import com.osstelecom.db.inventory.manager.response.DeleteDomainResponse;
import com.osstelecom.db.inventory.manager.response.DomainResponse;
import com.osstelecom.db.inventory.manager.response.GetDomainsResponse;
import com.osstelecom.db.inventory.manager.response.UpdateDomainResponse;
import com.osstelecom.db.inventory.manager.security.model.AuthenticatedCall;
import com.osstelecom.db.inventory.manager.session.DomainSession;
import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @author Lucas Nishimura
 * @created 21.07.2022
 */
@RestController
@RequestMapping("inventory/v1/domain")
//@Api(tags = "Domain API")
public class DomainApi extends BaseApi {

    @Autowired
    private DomainSession domainSession;

    /**
     * Obtem o dominio
     *
     * @param domainName
     * @return
     * @throws DomainNotFoundException
     * @throws InvalidRequestException
     */
    @AuthenticatedCall(role = {"user", "operator"})
    @GetMapping(path = "/{domain}", produces = "application/json")
    public DomainResponse getDomain(@PathVariable("domain") String domainName, HttpServletRequest httpRequest) throws DomainNotFoundException, InvalidRequestException, ArangoDaoException, ResourceNotFoundException, IOException {
        return domainSession.getDomain(domainName);
    }

    /**
     * Deleta um dominio
     *
     * @param domainName
     * @return
     * @throws DomainNotFoundException
     */
    @AuthenticatedCall(role = {"user", "operator"})
    @DeleteMapping(path = "/{domainName}", produces = "application/json")
    public DeleteDomainResponse deleteDomain(@PathVariable("domainName") String domainName, HttpServletRequest httpRequest) throws DomainNotFoundException, ResourceNotFoundException, ArangoDaoException, IOException {
        DeleteDomainRequest request = new DeleteDomainRequest(domainName);
        httpRequest.setAttribute("request", request);
        return domainSession.deleteDomain(request);
    }

    /**
     * List all Domains created in this system
     *
     * @return
     */
    @AuthenticatedCall(role = {"user", "operator"})
    @GetMapping(path = "/", produces = "application/json")
    public GetDomainsResponse getAllDomains() {
        return domainSession.getAllDomains();
    }

    /**
     * Cria um novo Domain xD
     *
     * @param request
     * @return
     * @throws DomainAlreadyExistsException
     * @throws InvalidRequestException
     * @throws GenericException
     */
    @AuthenticatedCall(role = {"user"})
    @PutMapping(path = "/", produces = "application/json", consumes = "application/json")
    public CreateDomainResponse createDomain(@RequestBody CreateDomainRequest request, HttpServletRequest httpRequest) throws DomainAlreadyExistsException, InvalidRequestException, GenericException {
        if (request != null) {
            logger.info("Request For Creating a new Domain named: [" + request + "] Received");
            if (request.getPayLoad() != null) {
                httpRequest.setAttribute("request", request);
                CreateDomainResponse response = domainSession.createDomain(request);
                return response;
            } else {
                throw new InvalidRequestException("Request Body is null");
            }
        } else {
            throw new InvalidRequestException("Request Body is null");
        }
    }

    /**
     * Atualiza o description do domain
     *
     * @param request
     * @param domainName
     * @param httpRequest
     * @return
     * @throws DomainAlreadyExistsException
     * @throws InvalidRequestException
     * @throws GenericException
     * @throws DomainNotFoundException
     * @throws ArangoDaoException
     */
    @AuthenticatedCall(role = {"user"})
    @PatchMapping(path = "/{domainName}", produces = "application/json", consumes = "application/json")
    public UpdateDomainResponse updateDomain(@RequestBody UpdateDomainRequest request, @PathVariable("domainName") String domainName, HttpServletRequest httpRequest) throws DomainAlreadyExistsException, InvalidRequestException, GenericException, DomainNotFoundException, ArangoDaoException {
        if (request != null) {
            request.setRequestDomain(domainName);
            logger.info("Request For Update a Domain named: [" + request.getRequestDomain() + "] Received");
            if (request.getPayLoad() != null) {
                httpRequest.setAttribute("request", request);
                UpdateDomainResponse response = domainSession.updateDomain(request);
                return response;
            } else {
                throw new InvalidRequestException("Request Body is null");
            }
        } else {
            throw new InvalidRequestException("Request Body is null");
        }
    }
}
