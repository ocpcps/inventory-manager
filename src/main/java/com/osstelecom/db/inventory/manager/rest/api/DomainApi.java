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

import com.osstelecom.db.inventory.manager.exception.DomainAlreadyExistsException;
import com.osstelecom.db.inventory.manager.exception.DomainNotFoundException;
import com.osstelecom.db.inventory.manager.exception.GenericException;
import com.osstelecom.db.inventory.manager.exception.InvalidRequestException;
import com.osstelecom.db.inventory.manager.request.CreateDomainRequest;
import com.osstelecom.db.inventory.manager.response.CreateDomainResponse;
import com.osstelecom.db.inventory.manager.security.model.AuthenticatedCall;
import com.osstelecom.db.inventory.manager.session.DomainSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @author Lucas Nishimura <lucas.nishimura@gmail.com>
 * @created 21.07.2022
 */
@RestController
@RequestMapping("inventory/v1/domain")
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
    public String getDomain(@PathVariable("domain") String domainName) throws DomainNotFoundException, InvalidRequestException {
        return gson.toJson(domainSession.getDomain(domainName));
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
    public CreateDomainResponse createDomain(@RequestBody CreateDomainRequest request) throws DomainAlreadyExistsException, InvalidRequestException, GenericException {
        logger.info("Request For Creating a new Domain named: [" + request + "] Received");
        if (request != null) {
            if (request.getPayLoad() != null) {
                CreateDomainResponse response = domainSession.createDomain(request);
                return response;
            } else {
                throw new InvalidRequestException("Request Body is null");
            }
        } else {
            throw new InvalidRequestException("Request Body is null");
        }
    }
}
