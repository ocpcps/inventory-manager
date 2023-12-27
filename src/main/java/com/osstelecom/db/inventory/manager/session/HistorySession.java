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

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.osstelecom.db.inventory.manager.exception.ArangoDaoException;
import com.osstelecom.db.inventory.manager.exception.DomainNotFoundException;
import com.osstelecom.db.inventory.manager.operation.DomainManager;
import com.osstelecom.db.inventory.manager.operation.HistoryManager;
import com.osstelecom.db.inventory.manager.request.FindHistoryCircuitRequest;
import com.osstelecom.db.inventory.manager.request.FindHistoryConnectionRequest;
import com.osstelecom.db.inventory.manager.request.FindHistoryResourceRequest;
import com.osstelecom.db.inventory.manager.request.FindHistoryServiceRequest;
import com.osstelecom.db.inventory.manager.resources.CircuitResource;
import com.osstelecom.db.inventory.manager.resources.Domain;
import com.osstelecom.db.inventory.manager.resources.History;
import com.osstelecom.db.inventory.manager.resources.ManagedResource;
import com.osstelecom.db.inventory.manager.resources.ResourceConnection;
import com.osstelecom.db.inventory.manager.resources.ServiceResource;
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
            throws DomainNotFoundException, ArangoDaoException {
        Domain domain = this.domainManager.getDomain(request.getDomainName());
        History history = new History(request.getResourceId(), ManagedResource.class.getSimpleName(),domain);
        List<History> result = this.manager.getHistoryResourceById(history);
        return new GetHistoryResponse(result);
    }

    public GetHistoryResponse getHistoryConnectionById(FindHistoryConnectionRequest request)
            throws DomainNotFoundException, ArangoDaoException {
        Domain domain = this.domainManager.getDomain(request.getDomainName());
        History history = new History(request.getConnectionId(), ResourceConnection.class.getSimpleName(),domain);
        List<History> result = this.manager.getHistoryConnectionById(history);
        return new GetHistoryResponse(result);
    }

    public GetHistoryResponse getHistoryCircuitById(FindHistoryCircuitRequest request)
            throws DomainNotFoundException, ArangoDaoException {
        Domain domain = this.domainManager.getDomain(request.getDomainName());
        History history = new History(request.getCircuitId(), CircuitResource.class.getSimpleName(),domain);
        List<History> result = this.manager.getHistoryCircuitById(history);
        return new GetHistoryResponse(result);
    } 
    
    public GetHistoryResponse getHistoryServiceById(FindHistoryServiceRequest request)
            throws DomainNotFoundException, ArangoDaoException {
        Domain domain = this.domainManager.getDomain(request.getDomainName());
        History history = new History(request.getServiceId(), ServiceResource.class.getSimpleName(), domain);
        List<History> result = this.manager.getHistoryServiceById(history);
        return new GetHistoryResponse(result);
    }
}
