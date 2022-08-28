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

import com.osstelecom.db.inventory.manager.dto.DomainDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.osstelecom.db.inventory.manager.exception.ArangoDaoException;
import com.osstelecom.db.inventory.manager.exception.DomainNotFoundException;
import com.osstelecom.db.inventory.manager.exception.InvalidRequestException;
import com.osstelecom.db.inventory.manager.exception.ResourceNotFoundException;
import com.osstelecom.db.inventory.manager.exception.ServiceNotFoundException;
import com.osstelecom.db.inventory.manager.operation.DomainManager;
import com.osstelecom.db.inventory.manager.request.CreateServiceRequest;
import com.osstelecom.db.inventory.manager.request.DeleteServiceRequest;
import com.osstelecom.db.inventory.manager.request.GetServiceRequest;
import com.osstelecom.db.inventory.manager.request.PatchServiceRequest;
import com.osstelecom.db.inventory.manager.resources.CircuitResource;
import com.osstelecom.db.inventory.manager.resources.ServiceResource;
import com.osstelecom.db.inventory.manager.response.CreateServiceResponse;
import com.osstelecom.db.inventory.manager.response.DeleteServiceResponse;
import com.osstelecom.db.inventory.manager.response.GetServiceResponse;
import com.osstelecom.db.inventory.manager.response.PatchServiceResponse;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Lucas Nishimura <lucas.nishimura@gmail.com>
 * @created 18.08.2022
 */
@Service
public class ServiceSession {

    @Autowired
    private DomainManager domainManager;

    public GetServiceResponse getServiceByServiceId(GetServiceRequest request) throws ServiceNotFoundException, DomainNotFoundException, ArangoDaoException, InvalidRequestException {
        if (request.getPayLoad().getId() == null) {
            throw new InvalidRequestException("ID Field Missing");
        }

        if (request.getRequestDomain() == null) {
            throw new DomainNotFoundException("Domain With Name:[" + request.getRequestDomain() + "] not found");
        }
        request.getPayLoad().setDomain(domainManager.getDomain(request.getRequestDomain()));

        return new GetServiceResponse(domainManager.getService(request.getPayLoad()));
    }

    public DeleteServiceResponse deleteService(DeleteServiceRequest request) throws DomainNotFoundException, ArangoDaoException {
        if (request.getRequestDomain() == null) {
            throw new DomainNotFoundException("Domain With Name:[" + request.getRequestDomain() + "] not found");
        }
        request.getPayLoad().setDomain(domainManager.getDomain(request.getRequestDomain()));

        return new DeleteServiceResponse(domainManager.deleteService(request.getPayLoad()));
    }

    public CreateServiceResponse createService(CreateServiceRequest request) throws InvalidRequestException, ServiceNotFoundException, DomainNotFoundException, ResourceNotFoundException, ArangoDaoException {
        if (request.getRequestDomain() == null) {
            throw new DomainNotFoundException("Domain With Name:[" + request.getRequestDomain() + "] not found");
        }
        if (request == null || request.getPayLoad() == null) {
            throw new InvalidRequestException("Request is null please send a valid request");
        }
        request.getPayLoad().setDomain(domainManager.getDomain(request.getRequestDomain()));

        ServiceResource payload = request.getPayLoad();
        if (payload == null) {
            throw new InvalidRequestException("Payload not found");
        }

        if ((payload.getCircuits() == null || payload.getCircuits().isEmpty()) && (payload.getDependencies() == null || payload.getDependencies().isEmpty())) {
            throw new InvalidRequestException("Please give at least one circuit or dependency");
        }

        if (payload.getCircuits() != null && !payload.getCircuits().isEmpty()) {
            this.resolveCircuits(payload.getCircuits(), request.getPayLoad().getDomain());
        }

        if (payload.getDependencies() != null && !payload.getCircuits().isEmpty()) {
            this.resolveServices(payload.getDependencies());
        }

        return new CreateServiceResponse(domainManager.createService(payload));
    }

    public PatchServiceResponse updateService(PatchServiceRequest request) throws InvalidRequestException, ServiceNotFoundException, DomainNotFoundException, ResourceNotFoundException, ArangoDaoException {
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
        domainManager.getService(payload);

        if ((payload.getCircuits() == null || payload.getCircuits().isEmpty()) && (payload.getDependencies() == null || payload.getDependencies().isEmpty())) {
            throw new InvalidRequestException("Please give at least one circuit or dependency");
        }

        if (payload.getCircuits() != null && !payload.getCircuits().isEmpty()) {
            this.resolveCircuits(payload.getCircuits(), request.getPayLoad().getDomain());
        }

        if (payload.getDependencies() != null && !payload.getCircuits().isEmpty()) {
            this.resolveServices(payload.getDependencies());
        }

        return new PatchServiceResponse(domainManager.updateService(payload));
    }

    /**
     * Resolve as entidades com suas referencias do banco
     *
     * @param circuits
     * @throws ResourceNotFoundException
     * @throws ArangoDaoException
     */
    private void resolveCircuits(List<CircuitResource> circuits, DomainDTO domain) throws ResourceNotFoundException, ArangoDaoException {
        List<CircuitResource> resolvedCircuits = new ArrayList<>();
        for (CircuitResource circuit : circuits) {
            if (circuit.getDomain() == null) {
                circuit.setDomain(domain);
            }
            CircuitResource resolved = this.domainManager.findCircuitResource(circuit);
            resolvedCircuits.add(resolved);
        }
        circuits.clear();
        circuits.addAll(resolvedCircuits);

    }

    /**
     * Resolve o serviço com suas referencias do DB
     *
     * @param serviceResources
     * @throws ServiceNotFoundException
     * @throws ArangoDaoException
     */
    private void resolveServices(List<ServiceResource> serviceResources) throws ServiceNotFoundException, ArangoDaoException {
        List<ServiceResource> resolvedServices = new ArrayList<>();
        for (ServiceResource service : serviceResources) {
            ServiceResource resolved = this.domainManager.getService(service);
            resolvedServices.add(service);
        }
        serviceResources.clear();
        serviceResources.addAll(resolvedServices);
    }

}
