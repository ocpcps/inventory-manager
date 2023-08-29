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
package com.osstelecom.db.inventory.manager.session;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.osstelecom.db.inventory.manager.exception.ArangoDaoException;
import com.osstelecom.db.inventory.manager.exception.DomainNotFoundException;
import com.osstelecom.db.inventory.manager.exception.InvalidRequestException;
import com.osstelecom.db.inventory.manager.exception.ResourceNotFoundException;
import com.osstelecom.db.inventory.manager.operation.DomainManager;
import com.osstelecom.db.inventory.manager.operation.HistoryManager;
import com.osstelecom.db.inventory.manager.request.FindHistoryCircuitRequest;
import com.osstelecom.db.inventory.manager.request.FindHistoryConnectionRequest;
import com.osstelecom.db.inventory.manager.request.FindHistoryResourceRequest;
import com.osstelecom.db.inventory.manager.request.FindHistoryServiceRequest;
import com.osstelecom.db.inventory.manager.resources.Domain;
import com.osstelecom.db.inventory.manager.resources.History;
import com.osstelecom.db.inventory.manager.response.GetHistoryResponse;

/**
 *
 * @author Leonardo Rodrigues
 * @created 16.08.2023
 */
@Service
public class HistorySession {

    @Autowired
    private DomainManager domainManager;

    @Autowired
    private HistoryManager manager;

    public GetHistoryResponse getHistoryResourceById(FindHistoryResourceRequest request)
            throws DomainNotFoundException, ArangoDaoException, ResourceNotFoundException, InvalidRequestException {
        Domain domain = this.domainManager.getDomain(request.getDomainName());
        History resource = new History(domain, null);
        resource.setId(request.getResourceId());
        resource = this.manager.getHistoryResourceById(resource);
        return new GetHistoryResponse(resource);
    }

    public GetHistoryResponse getHistoryConnectionById(FindHistoryConnectionRequest request)
            throws DomainNotFoundException, ArangoDaoException, ResourceNotFoundException, InvalidRequestException {
        Domain domain = this.domainManager.getDomain(request.getDomainName());
        History connection = new History(domain, null);
        connection.setId(request.getConnectionId());
        connection = this.manager.getHistoryConnectionById(connection);
        return new GetHistoryResponse(connection);
    }

        public GetHistoryResponse getHistoryCircuitById(FindHistoryCircuitRequest request)
            throws DomainNotFoundException, ArangoDaoException, ResourceNotFoundException, InvalidRequestException {
        Domain domain = this.domainManager.getDomain(request.getDomainName());
        History circuit = new History(domain, null);
        circuit.setId(request.getCircuitId());
        circuit = this.manager.getHistoryCircuitById(circuit);
        return new GetHistoryResponse(circuit);
    }
    
    public GetHistoryResponse getHistoryServiceById(FindHistoryServiceRequest request)
            throws DomainNotFoundException, ArangoDaoException, ResourceNotFoundException, InvalidRequestException {
        Domain domain = this.domainManager.getDomain(request.getDomainName());
        History service = new History(domain, null);
        service.setId(request.getServiceId());
        service = this.manager.getHistoryServiceById(service);
        return new GetHistoryResponse(service);
    }
}
