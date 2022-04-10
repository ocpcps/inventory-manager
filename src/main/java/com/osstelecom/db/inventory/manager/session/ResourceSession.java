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

import com.osstelecom.db.inventory.manager.dto.CircuitPathDTO;
import com.osstelecom.db.inventory.manager.exception.DomainNotFoundException;
import com.osstelecom.db.inventory.manager.exception.GenericException;
import com.osstelecom.db.inventory.manager.exception.InvalidRequestException;
import com.osstelecom.db.inventory.manager.exception.ResourceNotFoundException;
import com.osstelecom.db.inventory.manager.exception.SchemaNotFoundException;
import com.osstelecom.db.inventory.manager.exception.ScriptRuleException;
import com.osstelecom.db.inventory.manager.operation.DomainManager;
import com.osstelecom.db.inventory.manager.request.CreateCircuitPathRequest;
import com.osstelecom.db.inventory.manager.request.CreateCircuitRequest;
import com.osstelecom.db.inventory.manager.request.CreateConnectionRequest;
import com.osstelecom.db.inventory.manager.request.CreateManagedResourceRequest;
import com.osstelecom.db.inventory.manager.request.CreateResourceLocationRequest;
import com.osstelecom.db.inventory.manager.request.CreateServiceRequest;
import com.osstelecom.db.inventory.manager.request.FilterRequest;
import com.osstelecom.db.inventory.manager.request.GetCircuitPathRequest;
import com.osstelecom.db.inventory.manager.resources.CircuitResource;
import com.osstelecom.db.inventory.manager.resources.ManagedResource;
import com.osstelecom.db.inventory.manager.resources.ResourceConnection;
import com.osstelecom.db.inventory.manager.resources.ResourceLocation;
import com.osstelecom.db.inventory.manager.resources.exception.AttributeConstraintViolationException;
import com.osstelecom.db.inventory.manager.resources.exception.ConnectionAlreadyExistsException;
import com.osstelecom.db.inventory.manager.resources.exception.MetricConstraintException;
import com.osstelecom.db.inventory.manager.resources.exception.NoResourcesAvailableException;
import com.osstelecom.db.inventory.manager.response.CreateCircuitPathResponse;
import com.osstelecom.db.inventory.manager.response.CreateCircuitResponse;
import com.osstelecom.db.inventory.manager.response.CreateManagedResourceResponse;
import com.osstelecom.db.inventory.manager.response.CreateResourceConnectionResponse;
import com.osstelecom.db.inventory.manager.response.CreateResourceLocationResponse;
import com.osstelecom.db.inventory.manager.response.CreateServiceResponse;
import com.osstelecom.db.inventory.manager.response.FilterResponse;
import com.osstelecom.db.inventory.manager.response.GetCircuitPathResponse;
import com.osstelecom.db.inventory.topology.DefaultTopology;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 *
 * @author Lucas Nishimura <lucas.nishimura@gmail.com>
 * @created 15.12.2021
 */
@Service
public class ResourceSession {

    @Autowired
    private DomainManager domainManager;

    @Autowired
    private UtilSession utils;

    /**
     * Cria uma localidade
     *
     * @param request
     * @return
     * @throws GenericException
     * @throws SchemaNotFoundException
     * @throws AttributeConstraintViolationException
     * @throws ScriptRuleException
     */
    public CreateResourceLocationResponse createResourceLocation(CreateResourceLocationRequest request) throws GenericException, SchemaNotFoundException, AttributeConstraintViolationException, ScriptRuleException, InvalidRequestException, DomainNotFoundException {

        if (request.getPayLoad().getName() == null || request.getPayLoad().getName().trim().equals("")) {
            throw new InvalidRequestException("Please Give a name");
        }

        if (request.getPayLoad().getNodeAddress() == null) {
            request.getPayLoad().setNodeAddress(request.getPayLoad().getName());
        }

        if (request.getPayLoad().getAttributeSchemaName().equals("default")) {
            request.getPayLoad().setAttributeSchemaName("location.default");
        }

        if (request.getPayLoad().getClassName().equalsIgnoreCase("Default")) {
            request.getPayLoad().setClassName("location.Default");
        }

        request.getPayLoad().setDomain(domainManager.getDomain(request.getRequestDomain()));

        if (request.getPayLoad().getDomain() == null) {
            throw new DomainNotFoundException("Domain WIth Name:[" + request.getRequestDomain() + "] not found");
        }
        request.getPayLoad().setInsertedDate(new Date());
        domainManager.createResourceLocation(request.getPayLoad());
        CreateResourceLocationResponse response = new CreateResourceLocationResponse(request.getPayLoad());
        return response;
    }

    /**
     * Cria uma conexão entre recursos.
     *
     * @param request
     * @return
     * @throws ResourceNotFoundException
     * @throws ConnectionAlreadyExistsException
     * @throws MetricConstraintException
     * @throws NoResourcesAvailableException
     * @throws GenericException
     * @throws SchemaNotFoundException
     * @throws AttributeConstraintViolationException
     */
    public CreateResourceConnectionResponse createResourceConnection(CreateConnectionRequest request) throws ResourceNotFoundException, ConnectionAlreadyExistsException, MetricConstraintException, NoResourcesAvailableException, GenericException, SchemaNotFoundException, AttributeConstraintViolationException, ScriptRuleException, InvalidRequestException, DomainNotFoundException, IOException {

//        ManagedResource from = domainManager.findManagedResource(request.getPayLoad().getFromName(), request.getPayLoad().getFromNodeAddress(), request.getPayLoad().getFromClassName(), request.getRequestDomain());
//        ManagedResource to = domainManager.findManagedResource(request.getPayLoad().getToName(), request.getPayLoad().getToNodeAddress(), request.getPayLoad().getToClassName(), request.getRequestDomain());
        ResourceConnection connection = new ResourceConnection(domainManager.getDomain(request.getRequestDomain()));
        connection.setName(request.getPayLoad().getConnectionName());
        connection.setClassName(request.getPayLoad().getConnectionClass());

        if (request.getPayLoad().getFromClassName().contains("location")) {
            connection.setFrom(domainManager.findResourceLocation(request.getPayLoad().getFromName(), request.getPayLoad().getFromNodeAddress(), request.getPayLoad().getFromClassName(), request.getRequestDomain()));
        } else if (request.getPayLoad().getFromClassName().contains("resource")) {
            connection.setFrom(domainManager.findManagedResource(request.getPayLoad().getFromName(), request.getPayLoad().getFromNodeAddress(), request.getPayLoad().getFromClassName(), request.getRequestDomain()));
        } else {
            throw new InvalidRequestException("Invalid From Class");
        }

        if (request.getPayLoad().getToClassName().contains("location")) {
            connection.setTo(domainManager.findResourceLocation(request.getPayLoad().getToName(), request.getPayLoad().getToNodeAddress(), request.getPayLoad().getToClassName(), request.getRequestDomain()));
        } else if (request.getPayLoad().getToClassName().contains("resource")) {
            connection.setTo(domainManager.findManagedResource(request.getPayLoad().getToName(), request.getPayLoad().getToNodeAddress(), request.getPayLoad().getToClassName(), request.getRequestDomain()));
        } else {
            throw new InvalidRequestException("Invalid TO Class");
        }

        if (request.getPayLoad().getNodeAddress() != null) {
            connection.setNodeAddress(request.getPayLoad().getNodeAddress());
        } else {
            connection.setNodeAddress(connection.getName());
        }
        connection.setOperationalStatus(request.getPayLoad().getOperationalStatus());
        connection.setAttributeSchemaName(request.getPayLoad().getAttributeSchemaName());
        connection.setDomain(domainManager.getDomain(request.getRequestDomain()));
        connection.setAttributes(request.getPayLoad().getAttributes());
        connection.setPropagateOperStatus(request.getPayLoad().getPropagateOperStatus());
        CreateResourceConnectionResponse response = new CreateResourceConnectionResponse(connection);
        connection.setInsertedDate(new Date());
        domainManager.createResourceConnection(connection);
        return response;
    }

    /**
     * Cria uma conexão entre localidades
     *
     * @param request
     * @return
     * @throws ResourceNotFoundException
     * @throws ConnectionAlreadyExistsException
     * @throws MetricConstraintException
     * @throws NoResourcesAvailableException
     * @throws GenericException
     * @throws SchemaNotFoundException
     * @throws AttributeConstraintViolationException
     */
    public CreateResourceConnectionResponse createResourceLocationConnection(CreateConnectionRequest request) throws ResourceNotFoundException, ConnectionAlreadyExistsException, MetricConstraintException, NoResourcesAvailableException, GenericException, SchemaNotFoundException, AttributeConstraintViolationException, ScriptRuleException, DomainNotFoundException, IOException {

        ResourceLocation from = domainManager.findResourceLocation(request.getPayLoad().getFromName(), request.getPayLoad().getFromNodeAddress(), request.getPayLoad().getFromClassName(), request.getRequestDomain());
        ResourceLocation to = domainManager.findResourceLocation(request.getPayLoad().getToName(), request.getPayLoad().getToNodeAddress(), request.getPayLoad().getToClassName(), request.getRequestDomain());

        ResourceConnection connection = new ResourceConnection(domainManager.getDomain(request.getRequestDomain()));
        connection.setName(request.getPayLoad().getConnectionName());

        if (request.getPayLoad().getNodeAddress() != null) {
            connection.setNodeAddress(request.getPayLoad().getNodeAddress());
        } else {
            connection.setNodeAddress(connection.getName());
        }
        connection.setClassName(request.getPayLoad().getConnectionClass());
        connection.setFrom(from);
        connection.setTo(to);
        connection.setAttributeSchemaName(request.getPayLoad().getAttributeSchemaName());
        connection.setDomain(domainManager.getDomain(request.getRequestDomain()));
        connection.setAttributes(request.getPayLoad().getAttributes());
        connection.setPropagateOperStatus(request.getPayLoad().getPropagateOperStatus());
        connection.setOperationalStatus(request.getPayLoad().getOperationalStatus());
        CreateResourceConnectionResponse response = new CreateResourceConnectionResponse(connection);
        connection.setInsertedDate(new Date());
        domainManager.createResourceConnection(connection);
        return response;
    }

    /**
     * Cria um recurso gerenciavel
     *
     * @param request
     * @return
     * @throws SchemaNotFoundException
     * @throws AttributeConstraintViolationException
     * @throws GenericException
     * @throws ScriptRuleException
     */
    public CreateManagedResourceResponse createManagedResource(CreateManagedResourceRequest request) throws SchemaNotFoundException, AttributeConstraintViolationException, GenericException, ScriptRuleException, InvalidRequestException, DomainNotFoundException {
        ManagedResource resource = request.getPayLoad();
        request.getPayLoad().setDomain(domainManager.getDomain(request.getRequestDomain()));

        if (request.getPayLoad().getDomain() == null) {
            throw new DomainNotFoundException("Domain WIth Name:[" + request.getRequestDomain() + "] not found");
        }
        if (request.getPayLoad().getName() == null || request.getPayLoad().getName().trim().equals("")) {
            throw new InvalidRequestException("Please Give a name");
        }

        if (request.getPayLoad().getAttributeSchemaName().equals("default")) {
            request.getPayLoad().setAttributeSchemaName("resource.default");
        }

        if (request.getPayLoad().getNodeAddress() == null) {
            request.getPayLoad().setNodeAddress(request.getPayLoad().getName());
        }

        resource.setInsertedDate(new Date());
        resource = domainManager.createManagedResource(resource);
        CreateManagedResourceResponse response = new CreateManagedResourceResponse(resource);
        return response;
    }

    public CreateCircuitPathResponse createCircuitPath(CreateCircuitPathRequest request) throws DomainNotFoundException, ResourceNotFoundException, IOException {
        CreateCircuitPathResponse r = new CreateCircuitPathResponse(request.getPayLoad());
        //
        // Valida se temos paths...na request
        //

        CircuitResource circuit = request.getPayLoad().getCircuit();
        circuit.setDomain(domainManager.getDomain(circuit.getDomainName()));
        circuit = domainManager.findCircuitResource(circuit);

        request.getPayLoad().setCircuit(circuit);
        if (!request.getPayLoad().getPaths().isEmpty()) {
            ArrayList<ResourceConnection> resolved = new ArrayList<>();
            for (ResourceConnection a : request.getPayLoad().getPaths()) {

                a.setDomain(domainManager.getDomain(a.getDomainName()));
                ResourceConnection b = domainManager.findResourceConnection(a);
                if (!b.getCircuits().contains(circuit.getId())) {
                    b.getCircuits().add(circuit.getId());
                    //
                    // This needs updates
                    //
                    b = domainManager.updateResourceConnection(b);
                    if (!circuit.getCircuitPath().contains(b.getId())) {
                        circuit.getCircuitPath().add(b.getId());
                        circuit = domainManager.updateCircuitResource(circuit);
                    }
                } else {
                    System.out.println("ALREADY HEREEE!!!");
                }
                resolved.add(b);

            }
            //
            // Melhorar esta validação!
            // 
            if (resolved.size() == request.getPayLoad().getPaths().size()) {
                request.getPayLoad().getPaths().clear();
                request.getPayLoad().getPaths().addAll(resolved);
            }
        }
        return r;
    }

    /**
     * Cria um circuito
     *
     * @param request
     * @return
     * @throws ResourceNotFoundException
     * @throws GenericException
     * @throws SchemaNotFoundException
     * @throws AttributeConstraintViolationException
     * @throws ScriptRuleException
     * @throws DomainNotFoundException
     */
    public CreateCircuitResponse createCircuit(CreateCircuitRequest request) throws ResourceNotFoundException, GenericException, SchemaNotFoundException, AttributeConstraintViolationException, ScriptRuleException, DomainNotFoundException, IOException {
        if (request.getPayLoad().getaPoint().getDomain() == null) {
            if (request.getPayLoad().getaPoint().getDomainName() != null) {
                request.getPayLoad().getaPoint().setDomain(domainManager.getDomain(request.getPayLoad().getaPoint().getDomainName()));
            } else {
                request.getPayLoad().getaPoint().setDomain(domainManager.getDomain(request.getRequestDomain()));
            }

        }

        if (request.getPayLoad().getzPoint().getDomain() == null) {
            if (request.getPayLoad().getzPoint().getDomainName() != null) {
                request.getPayLoad().getzPoint().setDomain(domainManager.getDomain(request.getPayLoad().getzPoint().getDomainName()));
            } else {
                request.getPayLoad().getzPoint().setDomain(domainManager.getDomain(request.getRequestDomain()));
            }

        }

        if (request.getPayLoad().getNodeAddress() == null) {
            request.getPayLoad().setNodeAddress(request.getPayLoad().getName());
        }

        ManagedResource aPoint = domainManager.findManagedResource(request.getPayLoad().getaPoint());
        ManagedResource zPoint = domainManager.findManagedResource(request.getPayLoad().getzPoint());

        CircuitResource circuit = request.getPayLoad();
        if (circuit.getAttributeSchemaName().equalsIgnoreCase("default")) {
            circuit.setAttributeSchemaName("circuit.default");
        }

        if (circuit.getClassName().equalsIgnoreCase("Default")) {
            circuit.setClassName("circuit.Default");
        }

        circuit.setaPoint(aPoint);
        circuit.setzPoint(zPoint);
        circuit.setDomain(domainManager.getDomain(request.getRequestDomain()));
        circuit.setInsertedDate(new Date());
        CreateCircuitResponse response = new CreateCircuitResponse(domainManager.createCircuitResource(circuit));
        return response;
    }

    /**
     * Lista o path de um circuito
     *
     * @param request
     * @return
     * @throws ResourceNotFoundException
     */
    public GetCircuitPathResponse getCircuitPath(GetCircuitPathRequest request) throws ResourceNotFoundException, DomainNotFoundException, IOException {
        CircuitPathDTO circuitDto = request.getPayLoad();
        CircuitResource circuit = circuitDto.getCircuit();
        circuit.setDomain(domainManager.getDomain(circuit.getDomainName()));
        circuit = domainManager.findCircuitResource(circuitDto.getCircuit());
        circuitDto.setCircuit(circuit);
        circuitDto.setPaths(domainManager.findCircuitPath(circuit));

        for (ResourceConnection connection : circuitDto.getPaths()) {
            if (!connection.getOperationalStatus().equalsIgnoreCase("UP")) {
                circuitDto.setDegrated(true);
            }
        }
        GetCircuitPathResponse response = new GetCircuitPathResponse(circuitDto);
        return response;
    }

    public CreateServiceResponse createService(CreateServiceRequest request) {
        CreateServiceResponse result = null;
        return result;
    }

    public void test(String filter, Integer threads) {
        domainManager.test(filter, threads);
    }

    public FilterResponse getElementsByFilter(FilterRequest filter) throws DomainNotFoundException, ResourceNotFoundException, IOException {

        FilterResponse response = new FilterResponse(filter.getPayLoad());
        if (filter.getPayLoad().getObjects().contains("nodes")) {
            response.setNodes(domainManager.getNodesByFilter(filter.getPayLoad(), filter.getRequestDomain()));
            response.setNodeCount(response.getNodes().size());
        }

        if (filter.getPayLoad().getObjects().contains("connections")) {
            response.setConnections(domainManager.getConnectionsByFilter(filter.getPayLoad(), filter.getRequestDomain()));
            if (filter.getPayLoad().getComputeWeakLinks()) {
                //
                // Se vamos computar os links fracos monta um fake "in memory" topology
                //

                domainManager.findWeakLinks(response.getConnections(), filter.getPayLoad());
            }
        }

        return response;
    }
}
