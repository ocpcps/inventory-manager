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
import com.osstelecom.db.inventory.manager.operation.ServiceManager;
import com.osstelecom.db.inventory.manager.request.CreateServiceRequest;
import com.osstelecom.db.inventory.manager.request.DeleteServiceRequest;
import com.osstelecom.db.inventory.manager.request.FilterRequest;
import com.osstelecom.db.inventory.manager.request.GetServiceRequest;
import com.osstelecom.db.inventory.manager.request.PatchServiceRequest;
import com.osstelecom.db.inventory.manager.resources.Domain;
import com.osstelecom.db.inventory.manager.resources.ServiceResource;
import com.osstelecom.db.inventory.manager.response.CreateServiceResponse;
import com.osstelecom.db.inventory.manager.response.DeleteServiceResponse;
import com.osstelecom.db.inventory.manager.response.FilterResponse;
import com.osstelecom.db.inventory.manager.response.GetServiceResponse;
import com.osstelecom.db.inventory.manager.response.PatchServiceResponse;

/**
 *
 * @author Lucas Nishimura <lucas.nishimura@gmail.com>
 * @created 18.08.2022
 */
@Service
public class ServiceSession {

    @Autowired
    private ServiceManager serviceManager;

    @Autowired
    private DomainManager domainManager;

    public GetServiceResponse getServiceById(GetServiceRequest request) throws ResourceNotFoundException, DomainNotFoundException, ArangoDaoException, InvalidRequestException {
        if (request.getPayLoad().getId() == null) {
            throw new InvalidRequestException("ID Field Missing");
        }

        if (request.getRequestDomain() == null) {
            throw new DomainNotFoundException("Domain With Name:[" + request.getRequestDomain() + "] not found");
        }
        request.getPayLoad().setDomain(domainManager.getDomain(request.getRequestDomain()));

        return new GetServiceResponse(serviceManager.getServiceById(request.getPayLoad()));
    }

    public DeleteServiceResponse deleteService(DeleteServiceRequest request) throws DomainNotFoundException, ArangoDaoException {
        if (request.getRequestDomain() == null) {
            throw new DomainNotFoundException("Domain With Name:[" + request.getRequestDomain() + "] not found");
        }
        request.getPayLoad().setDomain(domainManager.getDomain(request.getRequestDomain()));

        return new DeleteServiceResponse(serviceManager.deleteService(request.getPayLoad()));
    }

    public CreateServiceResponse createService(CreateServiceRequest request) throws InvalidRequestException, DomainNotFoundException, ResourceNotFoundException, ArangoDaoException {

        if (request == null || request.getPayLoad() == null) {
            throw new InvalidRequestException("Request is null please send a valid request");
        }

        if (request.getRequestDomain() == null) {
            throw new DomainNotFoundException("Domain With Name:[" + request.getRequestDomain() + "] not found");
        }

        request.getPayLoad().setDomain(domainManager.getDomain(request.getRequestDomain()));

        ServiceResource payload = request.getPayLoad();
        if (payload == null) {
            throw new InvalidRequestException("Payload not found");
        }

        if ((payload.getCircuits() == null || payload.getCircuits().isEmpty()) && (payload.getDependencies() == null || payload.getDependencies().isEmpty())) {
            throw new InvalidRequestException("Please give at least one circuit or dependency");
        }

        //
        // Treat Defaults
        //
        if (payload.getAttributeSchemaName() == null) {
            payload.setAttributeSchemaName("service.default");
        }

        if (payload.getClassName() == null) {
            payload.setClassName("service.Default");
        }

        //
        // Validate minimun requirements fields
        //
        if (payload.getName() != null) {
            if (payload.getNodeAddress() == null) {
                payload.setNodeAddress(payload.getName());
            }
        }

        if (payload.getNodeAddress() != null) {
            if (payload.getName() == null) {
                payload.setName(payload.getNodeAddress());
            }
        }

        if (payload.getNodeAddress() == null) {
            throw new InvalidRequestException("Plase set name, or nodeAddress values");
        }
        //
        // Aqui resolve os circuitos e recursos.
        //
        payload = serviceManager.resolveService(payload);

        return new CreateServiceResponse(serviceManager.createService(payload));
    }

    public PatchServiceResponse updateService(PatchServiceRequest request) throws InvalidRequestException, DomainNotFoundException, ResourceNotFoundException, ArangoDaoException {
        if (request.getPayLoad().getId() == null && request.getPayLoad().getNodeAddress() == null && request.getPayLoad().getDomain() == null) {
            throw new InvalidRequestException("ID Field Missing");
        }

        if (request.getRequestDomain() == null) {
            throw new DomainNotFoundException("Domain With Name:[" + request.getRequestDomain() + "] not found");
        }
        request.getPayLoad().setDomain(domainManager.getDomain(request.getRequestDomain()));

        ServiceResource payload = request.getPayLoad();
        if (payload == null) {
            throw new InvalidRequestException("Payload not found");
        }
        ServiceResource old = null;
        if (payload.getId() != null) {
            old = serviceManager.getServiceById(payload);
        } else {
            old = serviceManager.getService(payload);
        }
        payload.setKey(old.getKey());

        if ((payload.getCircuits() == null || payload.getCircuits().isEmpty()) && (payload.getDependencies() == null || payload.getDependencies().isEmpty())) {
            throw new InvalidRequestException("Please give at least one circuit or dependency");
        }
        //
        // Resolveu aqui, será que não faz sentido ir para o manager inteiro ?
        //
        payload = serviceManager.resolveService(payload);

        return new PatchServiceResponse(serviceManager.updateService(payload));
    }

    public FilterResponse findServiceByFilter(FilterRequest filter) throws InvalidRequestException, ArangoDaoException, DomainNotFoundException, ResourceNotFoundException {
        FilterResponse response = new FilterResponse(filter.getPayLoad());
        if (filter.getPayLoad().getObjects().contains("service") || filter.getPayLoad().getObjects().contains("services")) {
            Domain domain = domainManager.getDomain(filter.getRequestDomain());
            response.getPayLoad().setServices(serviceManager.findServiceByFilter(filter.getPayLoad(), domain).toList());
            response.getPayLoad().setServiceCount(response.getPayLoad().getServices().size());
        } else {
            throw new InvalidRequestException("Filter object does not have service");
        }

        return response;
    }

}
