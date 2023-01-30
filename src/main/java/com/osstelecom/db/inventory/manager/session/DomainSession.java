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
package com.osstelecom.db.inventory.manager.session;

import com.osstelecom.db.inventory.manager.exception.ArangoDaoException;
import com.osstelecom.db.inventory.manager.exception.DomainAlreadyExistsException;
import com.osstelecom.db.inventory.manager.exception.DomainNotFoundException;
import com.osstelecom.db.inventory.manager.exception.GenericException;
import com.osstelecom.db.inventory.manager.exception.InvalidRequestException;
import com.osstelecom.db.inventory.manager.exception.ResourceNotFoundException;
import com.osstelecom.db.inventory.manager.operation.DomainManager;

import com.osstelecom.db.inventory.manager.request.CreateDomainRequest;
import com.osstelecom.db.inventory.manager.request.DeleteDomainRequest;
import com.osstelecom.db.inventory.manager.request.UpdateDomainRequest;
import com.osstelecom.db.inventory.manager.resources.Domain;
import com.osstelecom.db.inventory.manager.response.CreateDomainResponse;
import com.osstelecom.db.inventory.manager.response.DeleteDomainResponse;
import com.osstelecom.db.inventory.manager.response.DomainResponse;
import com.osstelecom.db.inventory.manager.response.GetDomainsResponse;
import com.osstelecom.db.inventory.manager.response.UpdateDomainResponse;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 *
 * @author Lucas Nishimura <lucas.nishimura@gmail.com>
 * @created 15.12.2021
 */
@Service
public class DomainSession {

    @Autowired
    private DomainManager domainManager;

    /**
     * Deleta um domain, cuidado pois deleta tudo que está dentro do domain
     *
     * @param request
     * @return
     * @throws DomainNotFoundException
     * @throws ArangoDaoException
     * @throws ResourceNotFoundException
     * @throws IOException
     */
    public DeleteDomainResponse deleteDomain(DeleteDomainRequest request) throws DomainNotFoundException, ArangoDaoException, ResourceNotFoundException, IOException {
        return new DeleteDomainResponse(domainManager.deleteDomain(request.getPayLoad()));
    }

    /**
     * Cria um novo dominio
     *
     * @param domainRequest
     * @return
     * @throws DomainAlreadyExistsException
     * @throws GenericException
     */
    public CreateDomainResponse createDomain(CreateDomainRequest domainRequest) throws DomainAlreadyExistsException, GenericException {
        try {
            return new CreateDomainResponse(this.domainManager.createDomain(domainRequest.getPayLoad()));
        } catch (DomainAlreadyExistsException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new GenericException(ex.getMessage());
        }
    }

    /**
     * Recupera a lista dos dominios existentes
     *
     * @return
     */
    public GetDomainsResponse getAllDomains() {
        return new GetDomainsResponse(this.domainManager.getAllDomains());
    }

    /**
     * Recupera um domain a partir do nome
     *
     * @param domainName
     * @return
     * @throws DomainNotFoundException
     * @throws InvalidRequestException
     * @throws ArangoDaoException
     * @throws ResourceNotFoundException
     * @throws IOException
     */
    public DomainResponse getDomain(String domainName) throws DomainNotFoundException, InvalidRequestException, ArangoDaoException, ResourceNotFoundException, IOException {
        if (domainName == null) {
            throw new InvalidRequestException("domainName cannot be null");
        }
        return new DomainResponse(domainManager.getDomain(domainName));
    }

    /**
     * Atualiza um domain
     *
     * @param domainRequest
     * @return
     * @throws DomainNotFoundException
     * @throws ArangoDaoException
     */
    public UpdateDomainResponse updateDomain(UpdateDomainRequest domainRequest) throws DomainNotFoundException, ArangoDaoException {
        Domain currentDomain = domainManager.getDomain(domainRequest.getRequestDomain());
        if (currentDomain.getDomainDescription() != domainRequest.getPayLoad().getDomainDescription()) {
            currentDomain.setDomainDescription(domainRequest.getPayLoad().getDomainDescription());
            UpdateDomainResponse response = new UpdateDomainResponse(this.domainManager.updateDomain(currentDomain));
            return response;
        }
        return new UpdateDomainResponse(domainRequest.getPayLoad());
    }
}
