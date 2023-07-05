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

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.arangodb.entity.DocumentUpdateEntity;
import com.osstelecom.db.inventory.manager.dto.FilterDTO;
import com.osstelecom.db.inventory.manager.exception.ArangoDaoException;
import com.osstelecom.db.inventory.manager.exception.AttributeNotFoundException;
import com.osstelecom.db.inventory.manager.exception.DomainNotFoundException;
import com.osstelecom.db.inventory.manager.exception.GenericException;
import com.osstelecom.db.inventory.manager.exception.InvalidRequestException;
import com.osstelecom.db.inventory.manager.exception.ResourceNotFoundException;
import com.osstelecom.db.inventory.manager.exception.SchemaNotFoundException;
import com.osstelecom.db.inventory.manager.exception.ScriptRuleException;
import com.osstelecom.db.inventory.manager.operation.CircuitResourceManager;
import com.osstelecom.db.inventory.manager.operation.DomainManager;
import com.osstelecom.db.inventory.manager.operation.ManagedResourceManager;
import com.osstelecom.db.inventory.manager.operation.ResourceConnectionManager;
import com.osstelecom.db.inventory.manager.operation.ResourceLocationManager;
import com.osstelecom.db.inventory.manager.operation.ServiceManager;
import com.osstelecom.db.inventory.manager.request.CreateConnectionRequest;
import com.osstelecom.db.inventory.manager.request.CreateManagedResourceRequest;
import com.osstelecom.db.inventory.manager.request.DeleteManagedResourceRequest;
import com.osstelecom.db.inventory.manager.request.DeleteResourceConnectionRequest;
import com.osstelecom.db.inventory.manager.request.FilterRequest;
import com.osstelecom.db.inventory.manager.request.FindManagedResourceRequest;
import com.osstelecom.db.inventory.manager.request.FindResourceConnectionRequest;
import com.osstelecom.db.inventory.manager.request.ListManagedResourceRequest;
import com.osstelecom.db.inventory.manager.request.ListResourceConnectionRequest;
import com.osstelecom.db.inventory.manager.request.PatchManagedResourceRequest;
import com.osstelecom.db.inventory.manager.request.PatchResourceConnectionRequest;
import com.osstelecom.db.inventory.manager.resources.CircuitResource;
import com.osstelecom.db.inventory.manager.resources.Domain;
import com.osstelecom.db.inventory.manager.resources.GraphList;
import com.osstelecom.db.inventory.manager.resources.ManagedResource;
import com.osstelecom.db.inventory.manager.resources.ResourceConnection;
import com.osstelecom.db.inventory.manager.resources.ServiceResource;
import com.osstelecom.db.inventory.manager.resources.exception.AttributeConstraintViolationException;
import com.osstelecom.db.inventory.manager.resources.exception.ConnectionAlreadyExistsException;
import com.osstelecom.db.inventory.manager.resources.exception.MetricConstraintException;
import com.osstelecom.db.inventory.manager.resources.exception.NoResourcesAvailableException;
import com.osstelecom.db.inventory.manager.response.CreateManagedResourceResponse;
import com.osstelecom.db.inventory.manager.response.CreateResourceConnectionResponse;
import com.osstelecom.db.inventory.manager.response.DeleteManagedResourceResponse;
import com.osstelecom.db.inventory.manager.response.DeleteResourceConnectionResponse;
import com.osstelecom.db.inventory.manager.response.FilterResponse;
import com.osstelecom.db.inventory.manager.response.FindManagedResourceResponse;
import com.osstelecom.db.inventory.manager.response.FindResourceConnectionResponse;
import com.osstelecom.db.inventory.manager.response.PatchManagedResourceResponse;
import com.osstelecom.db.inventory.manager.response.PatchResourceConnectionResponse;
import com.osstelecom.db.inventory.manager.response.TypedListResponse;

import ch.qos.logback.core.filter.Filter;

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
    private ManagedResourceManager manager;

    @Autowired
    private ResourceLocationManager resourceLocationManager;

    @Autowired
    private ResourceConnectionManager resourceConnectionManager;

    @Autowired
    private ManagedResourceManager managedResourceManager;

    @Autowired
    private CircuitResourceManager circuitManager;

    @Autowired
    private ServiceManager serviceManager;

    @Autowired
    private UtilSession utils;

    @Autowired
    private FilterProjectionSession filterProjectionSession;

    private Logger logger = LoggerFactory.getLogger(CircuitSession.class);

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
    public CreateResourceConnectionResponse createResourceConnection(CreateConnectionRequest request)
            throws ResourceNotFoundException, ConnectionAlreadyExistsException, MetricConstraintException,
            NoResourcesAvailableException, GenericException, SchemaNotFoundException,
            AttributeConstraintViolationException, ScriptRuleException, InvalidRequestException,
            DomainNotFoundException, ArangoDaoException {

        //
        // @since 08-08-2022: Prioriza os iDS aos Nomes
        //
        if (request.getPayLoad().getFromId() != null && request.getPayLoad().getToId() != null) {
            ResourceConnection connection = new ResourceConnection(domainManager.getDomain(request.getRequestDomain()));
            connection.setName(request.getPayLoad().getConnectionName());
            connection.setClassName(request.getPayLoad().getConnectionClass());
            //
            // Temos dois IDs podemos proesseguir com a validação por aqui
            //
            FindManagedResourceRequest fromResourceRequest = new FindManagedResourceRequest(
                    request.getPayLoad().getFromId(), request.getRequestDomain());
            Domain fromDomain = this.domainManager.getDomain(fromResourceRequest.getRequestDomain());

            fromResourceRequest.setRequestDomain(request.getRequestDomain());
            ManagedResource fromResource = manager
                    .findManagedResource(new ManagedResource(fromDomain, fromResourceRequest.getResourceId()));

            FindManagedResourceRequest toResourceRequest = new FindManagedResourceRequest(
                    request.getPayLoad().getToId(), request.getRequestDomain());
            Domain toDomain = this.domainManager.getDomain(toResourceRequest.getRequestDomain());
            toResourceRequest.setRequestDomain(request.getRequestDomain());
            ManagedResource toResource = manager
                    .findManagedResource(new ManagedResource(toDomain, toResourceRequest.getResourceId()));

            connection.setFrom(fromResource);
            connection.setTo(toResource);
            if (request.getPayLoad().getNodeAddress() != null) {
                connection.setNodeAddress(request.getPayLoad().getNodeAddress());
            } else {
                connection.setNodeAddress(
                        request.getPayLoad().getFromNodeAddress() + "." + request.getPayLoad().getToNodeAddress());
            }
            connection.setOperationalStatus(request.getPayLoad().getOperationalStatus());
            connection.setAttributeSchemaName(request.getPayLoad().getAttributeSchemaName());
            connection.setDomain(domainManager.getDomain(request.getRequestDomain()));
            connection.setAttributes(request.getPayLoad().getAttributes());
            connection.setPropagateOperStatus(request.getPayLoad().getPropagateOperStatus());
            connection.setName(request.getPayLoad().getConnectionName());
            connection.setDescription(request.getPayLoad().getDescription());
            connection.setOwner(request.getUserId());
            connection.setAdminStatus(request.getPayLoad().getAdminStatus());
            connection.setBusinessStatus(request.getPayLoad().getBusinessStatus());
            connection.setCategory(request.getPayLoad().getCategory());

            if (request.getPayLoad().getKey() != null) {
                //
                // Manda um upsert
                //
                connection.setKey(request.getPayLoad().getKey());
            } else {
                connection.setInsertedDate(new Date());
            }

            connection = resourceConnectionManager.createResourceConnection(connection);
            CreateResourceConnectionResponse response = new CreateResourceConnectionResponse(connection);
            return response;

        } else if (request.getPayLoad().getFromKey() != null && request.getPayLoad().getToKey() != null) {
            ResourceConnection connection = new ResourceConnection(domainManager.getDomain(request.getRequestDomain()));
            connection.setName(request.getPayLoad().getConnectionName());
            connection.setClassName(request.getPayLoad().getConnectionClass());
            //
            // Temos dois IDs podemos proesseguir com a validação por aqui
            //
            Domain domain = this.domainManager.getDomain(request.getRequestDomain());
            ManagedResource fromResource = new ManagedResource(domain);
            fromResource.setKey(request.getPayLoad().getFromKey());
            //
            // Como temos a KEY, vamos ignorar o attributeSchemaName
            //
            fromResource.setAttributeSchemaName(null);
            fromResource = manager.findManagedResource(fromResource);

            ManagedResource toResource = new ManagedResource(domain);
            toResource.setKey(request.getPayLoad().getToKey());
            //
            // Como temos a KEY, vamos ignorar o attributeSchemaName
            //
            toResource.setAttributeSchemaName(null);
            toResource = manager.findManagedResource(toResource);

            connection.setFrom(fromResource);
            connection.setTo(toResource);
            if (request.getPayLoad().getNodeAddress() != null) {
                connection.setNodeAddress(request.getPayLoad().getNodeAddress());
            } else {
                connection.setNodeAddress(
                        request.getPayLoad().getFromNodeAddress() + "." + request.getPayLoad().getToNodeAddress());
            }
            connection.setOperationalStatus(request.getPayLoad().getOperationalStatus());
            connection.setAttributeSchemaName(request.getPayLoad().getAttributeSchemaName());
            connection.setDomain(domainManager.getDomain(request.getRequestDomain()));
            connection.setAttributes(request.getPayLoad().getAttributes());
            connection.setPropagateOperStatus(request.getPayLoad().getPropagateOperStatus());
            connection.setName(request.getPayLoad().getConnectionName());
            connection.setDescription(request.getPayLoad().getDescription());
            connection.setOwner(request.getUserId());
            connection.setAdminStatus(request.getPayLoad().getAdminStatus());
            connection.setBusinessStatus(request.getPayLoad().getBusinessStatus());
            connection.setCategory(request.getPayLoad().getCategory());

            if (request.getPayLoad().getKey() != null) {
                //
                // Manda um upsert
                //
                connection.setKey(request.getPayLoad().getKey());
            } else {
                connection.setInsertedDate(new Date());
            }

            connection = resourceConnectionManager.createResourceConnection(connection);
            CreateResourceConnectionResponse response = new CreateResourceConnectionResponse(connection);
            return response;

        } else if (request.getPayLoad().getFromNodeAddress() != null
                && request.getPayLoad().getToNodeAddress() != null) {
            //
            // Vou fazer de forma preguiçosa, eu tô resfriado e doente essa semana, não me
            // julguem
            //
            ResourceConnection connection = new ResourceConnection(domainManager.getDomain(request.getRequestDomain()));
            connection.setName(request.getPayLoad().getConnectionName());
            connection.setClassName(request.getPayLoad().getConnectionClass());
            //
            // Temos dois IDs podemos proesseguir com a validação por aqui
            //
            Domain domain = this.domainManager.getDomain(request.getRequestDomain());
            ManagedResource fromResource = new ManagedResource(domain);
            fromResource.setNodeAddress(request.getPayLoad().getFromNodeAddress());
            fromResource.setClassName(request.getPayLoad().getFromClassName());
            fromResource.setAttributeSchemaName(null);
            fromResource = this.manager.findManagedResource(fromResource);

            ManagedResource toResource = new ManagedResource(domain);
            toResource.setNodeAddress(request.getPayLoad().getToNodeAddress());
            toResource.setClassName(request.getPayLoad().getToClassName());

            toResource.setAttributeSchemaName(null);
            toResource = manager.findManagedResource(toResource);
            connection.setFrom(fromResource);
            connection.setTo(toResource);
            if (request.getPayLoad().getNodeAddress() != null) {
                connection.setNodeAddress(request.getPayLoad().getNodeAddress());
            } else {
                connection.setNodeAddress(
                        request.getPayLoad().getFromNodeAddress() + "." + request.getPayLoad().getToNodeAddress());
            }
            connection.setOperationalStatus(request.getPayLoad().getOperationalStatus());
            connection.setAttributeSchemaName(request.getPayLoad().getAttributeSchemaName());
            connection.setDomain(domainManager.getDomain(request.getRequestDomain()));
            connection.setAttributes(request.getPayLoad().getAttributes());
            connection.setPropagateOperStatus(request.getPayLoad().getPropagateOperStatus());
            connection.setName(request.getPayLoad().getConnectionName());
            connection.setDescription(request.getPayLoad().getDescription());
            connection.setOwner(request.getUserId());
            connection.setAdminStatus(request.getPayLoad().getAdminStatus());
            connection.setBusinessStatus(request.getPayLoad().getBusinessStatus());
            connection.setCategory(request.getPayLoad().getCategory());

            if (request.getPayLoad().getKey() != null) {
                //
                // Manda um upsert
                //
                connection.setKey(request.getPayLoad().getKey());
            } else {
                connection.setInsertedDate(new Date());
            }

            connection = resourceConnectionManager.createResourceConnection(connection);
            CreateResourceConnectionResponse response = new CreateResourceConnectionResponse(connection);
            return response;

        } else {
            throw new InvalidRequestException("Cannot Create Connection");
        }
    }

    public DeleteResourceConnectionResponse deleteResourceConnection(DeleteResourceConnectionRequest request)
            throws InvalidRequestException, DomainNotFoundException, ArangoDaoException, ResourceNotFoundException {

        if (request.getResourceId() == null) {
            throw new InvalidRequestException("Please Provide Resource ID to delete");
        } else if (request.getRequestDomain() == null) {
            throw new InvalidRequestException("Please Provide Domain Name of Resource to  delete");
        }

        Domain domain = this.domainManager.getDomain(request.getRequestDomain());
        //
        // Precisa Enconrar A Connection.
        //
        ResourceConnection connection = this.resourceConnectionManager
                .findResourceConnection(new ResourceConnection(domain, request.getResourceId()));

        //
        // Uma vez que achamos a conecction, a gente precisa ver se algum circuito usa
        // ela, pois se usar ela não pode ser removida!
        //
        Map<String, Object> bindings = new HashMap<>();
        bindings.put("connectionId", connection.getId());
        FilterDTO connectionFilter = new FilterDTO();
        connectionFilter.setAqlFilter("@connectionId in doc.circuitPath[*] ");
        connectionFilter.getObjects().add("circuits");
        connectionFilter.setBindings(bindings);

        try {

            GraphList<CircuitResource> circuits = circuitManager.findCircuitsByFilter(connectionFilter, domain);

            //
            // Se chegou aqui é porque tem conexões que dependem do recurso, não podemos
            // deletar
            //
            throw new InvalidRequestException(("Resource ID is Used By:[" + circuits.size()
                    + "] Connections, please remove theses dependencies, before delete"));
        } catch (ResourceNotFoundException ex) {

            connection = this.resourceConnectionManager.deleteResourceConnection(connection);
            DeleteResourceConnectionResponse response = new DeleteResourceConnectionResponse(connection);
            return response;
        }
    }

    /**
     * Try to delete a resource on the domain
     *
     * @param request
     * @return
     * @throws InvalidRequestException
     * @throws DomainNotFoundException
     * @throws ArangoDaoException
     * @throws ResourceNotFoundException
     */
    public DeleteManagedResourceResponse deleteManagedResource(DeleteManagedResourceRequest request)
            throws InvalidRequestException, DomainNotFoundException, ArangoDaoException, ResourceNotFoundException {
        if (request.getResourceId() == null) {
            throw new InvalidRequestException("Please Provide Resource ID to delete");
        } else if (request.getRequestDomain() == null) {
            throw new InvalidRequestException("Please Provide Domain Name of Resource to  delete");
        }

        Domain domain = this.domainManager.getDomain(request.getRequestDomain());
        //
        // Precisa Enconrar o Recurso.
        //
        ManagedResource resource = this.managedResourceManager
                .findManagedResourceById(new ManagedResource(domain, request.getResourceId()));

        //
        // O Problema do delete de um recurso é que ele pode ser necessário para alguma
        // conexão e consequentemente um circuito...
        // Então vamos ver se esse recurso é necessário para alguma conexão deste
        // dominio
        //
        Map<String, Object> bindings = new HashMap<>();
        bindings.put("resourceId", resource.getId());
        FilterDTO connectionFilter = new FilterDTO();
        connectionFilter.setAqlFilter("doc.fromResource._id == @resourceId or doc.toResource._id == @resourceId ");
        connectionFilter.getObjects().add("connections");
        connectionFilter.setBindings(bindings);
        try {
            GraphList<ResourceConnection> connections = resourceConnectionManager
                    .getConnectionsByFilter(connectionFilter, domain.getDomainName());
            //
            // Se chegou aqui é porque tem conexões que dependem do recurso, não podemos
            // deletar
            //
            throw new InvalidRequestException(("Resource ID is Used By:[" + connections.size()
                    + "] Connections, please remove theses dependencies, before delete"));
        } catch (ResourceNotFoundException ex) {
            //
            // Neste Caso isso é desejado, não tem conexões que dependem dele...
            //

            FilterDTO circuitFilter = new FilterDTO();
            circuitFilter.setAqlFilter("doc.aPoint._id == @resourceId or doc.zPoint._id == @resourceId ");
            circuitFilter.getObjects().add("connections");
            circuitFilter.setBindings(bindings);
            try {
                GraphList<CircuitResource> circuits = circuitManager.findCircuitsByFilter(circuitFilter, domain);
                throw new InvalidRequestException(("Resource ID is Used By:[" + circuits.size()
                        + "] Circuits, please remove theses dependencies, before delete"));
            } catch (ResourceNotFoundException exCic) {
                resource = this.managedResourceManager.delete(resource);
                DeleteManagedResourceResponse response = new DeleteManagedResourceResponse(resource);
                //
                // Precisa de um evento de Delete ?
                //
                return response;
            }
        }

    }

    /**
     * Obtem o managed Resource By ID
     *
     * @param request
     * @return
     * @throws InvalidRequestException
     * @throws DomainNotFoundException
     * @throws ResourceNotFoundException
     * @throws ArangoDaoException
     */
    public FindManagedResourceResponse findManagedResourceById(FindManagedResourceRequest request)
            throws InvalidRequestException, DomainNotFoundException, ResourceNotFoundException, ArangoDaoException {
        if (request.getResourceId() == null) {
            if (request.getRequestDomain() == null) {
                throw new InvalidRequestException("Field resourceId and domain cannot be empty or null");
            } else {
                try {
                    Domain domain = this.domainManager.getDomain(request.getRequestDomain());
                    return new FindManagedResourceResponse(
                            this.manager.findManagedResource(new ManagedResource(domain, request.getResourceId())));
                } catch (IllegalArgumentException exception) {
                    throw new InvalidRequestException("ResourceId Invalid DOMAIN:[" + request.getRequestDomain() + "]");
                }
            }
        } else {
            try {
                Domain domain = this.domainManager.getDomain(request.getRequestDomain());
                return new FindManagedResourceResponse(
                        this.manager.findManagedResource(new ManagedResource(domain, request.getResourceId())));
            } catch (IllegalArgumentException exception) {
                throw new InvalidRequestException("ResourceId Invalid UUID:[" + request.getResourceId() + "]");
            }

        }

    }

    public TypedListResponse listManagedResources(ListManagedResourceRequest request)
            throws DomainNotFoundException, ResourceNotFoundException, ArangoDaoException, InvalidRequestException {
        FilterDTO filter = new FilterDTO();
        filter.setAqlFilter(" ");
        filter.setSortCondition("sort doc.nodeAddress asc");
        filter.getObjects().add("nodes");
        filter.setLimit(10L);
        filter.setOffSet(0L);
        TypedListResponse response = new TypedListResponse(this.managedResourceManager
                .getNodesByFilter(filter, request.getRequestDomain()).toList());
        return response;
    }

    public TypedListResponse listResourceConnection(ListResourceConnectionRequest request)
            throws DomainNotFoundException, ResourceNotFoundException, ArangoDaoException, InvalidRequestException {
        FilterDTO filter = new FilterDTO();
        filter.setAqlFilter(" ");
        filter.setSortCondition("sort doc.nodeAddress asc");
        filter.getObjects().add("connections");
        filter.setLimit(10L);
        filter.setOffSet(0L);
        TypedListResponse response = new TypedListResponse(this.resourceConnectionManager
                .getConnectionsByFilter(filter, request.getRequestDomain()).toList());
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
    public CreateManagedResourceResponse createManagedResource(CreateManagedResourceRequest request)
            throws SchemaNotFoundException, AttributeConstraintViolationException, GenericException,
            ScriptRuleException, InvalidRequestException, DomainNotFoundException, ArangoDaoException,
            ResourceNotFoundException, AttributeNotFoundException {
        Long start = System.currentTimeMillis();
        if (request == null) {
            throw new InvalidRequestException("Request is NULL!");
        }

        if (request.getPayLoad() == null) {
            logger.warn("Invalid Request Received:[{}]", this.utils.toJson(request));
            throw new InvalidRequestException("Please Provide a Resrouce !");
        }

        ManagedResource resource = request.getPayLoad();
        resource.setDomain(domainManager.getDomain(request.getRequestDomain()));

        if (resource.getDomain() == null) {
            throw new DomainNotFoundException("Domain WIth Name:[" + request.getRequestDomain() + "] not found");
        }
        if (resource.getName() == null || resource.getName().trim().equals("")) {
            if (resource.getNodeAddress() != null && !resource.getNodeAddress().trim().equals("")) {
                resource.setName(resource.getNodeAddress());
            } else {
                throw new InvalidRequestException("Please Give a name");
            }
        }

        if (resource.getAttributeSchemaName() == null) {
            resource.setAttributeSchemaName("resource.default");
        } else if (!resource.getAttributeSchemaName().startsWith("resource")) {
            throw new InvalidRequestException("Schema Name Has to Start with resource.");
        }

        if (resource.getClassName() == null) {
            resource.setClassName("resource.Default");
        } else if (!resource.getClassName().startsWith("resource")) {
            throw new InvalidRequestException("Class Name Has to Start with resource.");
        }

        if (resource.getOperationalStatus() == null) {
            resource.setOperationalStatus("Up");
        }

        if (resource.getAdminStatus() == null) {
            resource.setAdminStatus("Up");
        }

        //
        // Avaliar se podemos melhorar isso, usando um nome canonico, com o className +
        // name
        //
        if (resource.getNodeAddress() == null) {
            resource.setNodeAddress(resource.getName());
        }

        if (resource.getStructureId() != null && !resource.getStructureId().equals("")) {
            ManagedResource structureResource = new ManagedResource(resource.getDomain(), resource.getStructureId());
            //
            // Valida se o id de Estrutura Existe
            //
            structureResource = this.findManagedResource(structureResource);
            resource.setStructureId(structureResource.getKey());
        }

        resource.setOwner(request.getUserId());
        resource.setAuthor(request.getUserId());
        resource.setInsertedDate(new Date());
        resource = manager.create(resource);
        Long end = System.currentTimeMillis();
        Long took = end - start;
        logger.debug("Create Resource:[{}] Took: {} ms", resource.getNodeAddress(), took);
        return new CreateManagedResourceResponse(resource);
    }

    public FilterResponse findManagedResourceByFilter(FilterRequest filter)
            throws InvalidRequestException, ArangoDaoException, DomainNotFoundException, ResourceNotFoundException {

        FilterResponse response = new FilterResponse(filter.getPayLoad());

        //
        // Validação para evitar abusos de uso da API
        //
        if (filter.getPayLoad() != null) {
            if (filter.getPayLoad().getLimit() != null) {
                if (filter.getPayLoad().getLimit() > 10000) {
                    throw new InvalidRequestException(
                            "Result Set Limit cannot be over 1000, please descrease limit value to a range between 0 and 1000");
                } else {
                    if (filter.getPayLoad().getLimit() < 0L) {
                        filter.getPayLoad().setLimit(1000L);
                    }
                }
            } else {
                filter.getPayLoad().setLimit(1000L);
            }
        }

        if (filter.getPayLoad().getObjects().contains("nodes") || filter.getPayLoad().getObjects().contains("node")) {
            GraphList<ManagedResource> nodesGraph = managedResourceManager.getNodesByFilter(filter.getPayLoad(),
                    filter.getRequestDomain());
            List<ManagedResource> nodes = nodesGraph.toList();
            response.getPayLoad().setNodes(nodes);
            response.getPayLoad().setNodeCount(nodesGraph.size());
            response.setSize(nodesGraph.size());
            response.setArangoStats(nodesGraph.getStats());
        } else if (filter.getPayLoad().getObjects().contains("connections")
                || filter.getPayLoad().getObjects().contains("connection")) {
            // response.getPayLoad().setConnections(resourceConnectionManager.getConnectionsByFilter(filter.getPayLoad(),
            // filter.getRequestDomain()).toList());

            GraphList<ResourceConnection> connectionsGraph = resourceConnectionManager
                    .getConnectionsByFilter(filter.getPayLoad(), filter.getRequestDomain());
            List<ResourceConnection> connections = connectionsGraph.toList();
            response.getPayLoad().setConnections(connections);
            response.getPayLoad().setConnectionsCount(connectionsGraph.size());
            response.setSize(connectionsGraph.size());
            response.setArangoStats(connectionsGraph.getStats());
            if (filter.getPayLoad().getComputeWeakLinks()) {
                //
                // Computação de Links Fracos Desabilitada
                //
                throw new InvalidRequestException("Weak Links Calculation is Disabled on this system");
            }
        }

        return this.filterProjectionSession.filterProjection(filter.getPayLoad(), response);
    }

    public GraphList<ManagedResource> findManagedResourceByFilter(FilterDTO filter)
            throws InvalidRequestException, ArangoDaoException, DomainNotFoundException, ResourceNotFoundException {
        if (filter.getObjects() != null) {
            if (filter.getObjects().contains("nodes") || filter.getObjects().contains("node")) {
                GraphList<ManagedResource> nodesGraph = this.managedResourceManager.getNodesByFilter(filter,
                        filter.getDomainName());
                return nodesGraph;
            } else {
                throw new InvalidRequestException("Filter Object is now known")
                        .addDetails("filter", filter);
            }
        } else {
            throw new InvalidRequestException("No Object Found for filter")
                    .addDetails("filter", filter);
        }

    }

    public GraphList<ResourceConnection> findResourceConnectionByFilter(FilterDTO filter)
            throws InvalidRequestException, ArangoDaoException, DomainNotFoundException, ResourceNotFoundException {
        if (filter.getObjects().contains("connections") || filter.getObjects().contains("connection")) {
            GraphList<ResourceConnection> nodesGraph = this.resourceConnectionManager.getConnectionsByFilter(filter,
                    filter.getDomainName());
            return nodesGraph;
        } else {
            throw new InvalidRequestException("Invalida Object Type:[" + String.join(",", filter.getObjects()) + "]")
                    .addDetails("filter", filter);
        }
    }

    /**
     * Retorna as areastas de um circuito no grafo
     *
     * @param cic
     * @return
     */
    public GraphList<ResourceConnection> findResourceConnectionByCircuit(CircuitResource cic)
            throws InvalidRequestException, ArangoDaoException, DomainNotFoundException, ResourceNotFoundException {
        FilterDTO filter = new FilterDTO();
        filter.setDomainName(cic.getDomain().getDomainName());
        filter.addBinding("circuitId", cic.getId());
        // in doc.relatedNodes[*]
        filter.setAqlFilter(" @circuitId in doc.circuits[*]");
        filter.addObject("connections");
        return this.findResourceConnectionByFilter(filter);
    }

    /**
     * Retorna as areastas de um servico no grafo
     *
     * @param service
     * @return
     */
    public GraphList<ResourceConnection> findResourceConnectionByService(ServiceResource service)
            throws InvalidRequestException, ArangoDaoException, DomainNotFoundException, ResourceNotFoundException {
        FilterDTO filter = new FilterDTO();
        filter.setDomainName(service.getDomain().getDomainName());
        filter.addBinding("serviceId", service.getId());
        // in doc.relatedNodes[*]
        filter.setAqlFilter(" @serviceId in doc.services[*]");
        filter.addObject("connections");
        return this.findResourceConnectionByFilter(filter);
    }

    public ManagedResource findManagedResource(ManagedResource resource)
            throws ResourceNotFoundException, ArangoDaoException, InvalidRequestException {
        return this.manager.findManagedResource(resource);
    }

    public ResourceConnection findResourceConnection(ResourceConnection connection)
            throws ResourceNotFoundException, ArangoDaoException, InvalidRequestException {

        return this.resourceConnectionManager.findResourceConnection(connection);
    }

    /**
     * Atualiza um Managed Resource
     *
     * @param patchRequest
     * @return
     * @throws DomainNotFoundException
     * @throws ResourceNotFoundException
     * @throws ArangoDaoException
     * @throws InvalidRequestException
     * @throws AttributeNotFoundException
     * @throws GenericException
     * @throws SchemaNotFoundException
     */
    public PatchManagedResourceResponse patchManagedResource(PatchManagedResourceRequest patchRequest)
            throws DomainNotFoundException, ResourceNotFoundException, ArangoDaoException, InvalidRequestException,
            AttributeConstraintViolationException, ScriptRuleException, SchemaNotFoundException, GenericException,
            AttributeNotFoundException {
        //
        //
        //
        ManagedResource requestedPatch = patchRequest.getPayLoad();
        ManagedResource searchObj = null;
        //
        // Arruma o domain para funcionar certinho
        //
        requestedPatch.setDomain(this.domainManager.getDomain(patchRequest.getRequestDomain()));
        requestedPatch.setDomainName(requestedPatch.getDomain().getDomainName());

        //
        // Garante que vamos priorizar o ID ou, o KEY ( FUTURO )
        //
        if (requestedPatch.getId() != null && !requestedPatch.getId().trim().equals("")) {
            //
            // Temos um ID, sanitiza para ficar bom
            //
            searchObj = new ManagedResource(requestedPatch.getDomain(), requestedPatch.getId());
        } else {
            searchObj = requestedPatch;
        }

        ManagedResource fromDBResource = this.findManagedResource(searchObj);

        //
        // Se chegamos aqui, temos coisas para atualizar...
        // @ Todo, comparar para ver se houve algo que realmente mudou..
        //
        if (requestedPatch.getName() != null) {
            fromDBResource.setName(requestedPatch.getName());
        }

        if (requestedPatch.getNodeAddress() != null) {
            fromDBResource.setNodeAddress(requestedPatch.getNodeAddress());
        }

        if (requestedPatch.getClassName() != null && !requestedPatch.getClassName().equals("Default")) {
            fromDBResource.setClassName(requestedPatch.getClassName());
        }

        if (requestedPatch.getOperationalStatus() != null) {
            fromDBResource.setOperationalStatus(requestedPatch.getOperationalStatus());
        }

        //
        // Pode atualizar o AtributeSchemaModel ? isso é bem custosooo vamos tratar isso
        // em outro lugar...
        //
        if (requestedPatch.getAdminStatus() != null) {
            fromDBResource.setAdminStatus(requestedPatch.getAdminStatus());
        }

        //
        // Atualiza os atributos
        //
        if (requestedPatch.getAttributes() != null && !requestedPatch.getAttributes().isEmpty()) {
            requestedPatch.getAttributes().forEach((name, attribute) -> {
                if (fromDBResource.getAttributes() != null) {
                    if (fromDBResource.getAttributes().containsKey(name)) {
                        fromDBResource.getAttributes().replace(name, attribute);
                    } else {
                        fromDBResource.getAttributes().put(name, attribute);
                    }
                }
            });
        }

        if (requestedPatch.getDescription() != null && !requestedPatch.getDescription().trim().equals("")) {
            if (!requestedPatch.getDescription().equals(fromDBResource.getDescription())) {
                fromDBResource.setDescription(requestedPatch.getDescription());
            }
        }

        if (requestedPatch.getResourceType() != null && !requestedPatch.getResourceType().trim().equals("")) {
            if (!requestedPatch.getResourceType().equals(fromDBResource.getResourceType())) {
                fromDBResource.setResourceType(requestedPatch.getResourceType());
            }
        }

        if (requestedPatch.getStructureId() != null && !requestedPatch.getStructureId().trim().equals("")) {
            if (!requestedPatch.getStructureId().equals(fromDBResource.getStructureId())) {
                ManagedResource structureResrouce = new ManagedResource(fromDBResource.getDomain(),
                        requestedPatch.getStructureId());
                //
                // Valida se o id de Estrutura Existe
                //
                structureResrouce = this.findManagedResource(structureResrouce);

                fromDBResource.setStructureId(requestedPatch.getStructureId());
            }
        }

        if (requestedPatch.getCategory() != null && !requestedPatch.getCategory().trim().equals("")) {
            if (!requestedPatch.getCategory().equals("default")) {
                if (!requestedPatch.getCategory().equals(fromDBResource.getCategory())) {
                    fromDBResource.setCategory(requestedPatch.getCategory());
                }
            }
        }

        if (requestedPatch.getBusinessStatus() != null && !requestedPatch.getBusinessStatus().trim().equals("")) {
            if (!requestedPatch.getBusinessStatus().equals(fromDBResource.getBusinessStatus())) {
                fromDBResource.setBusinessStatus(requestedPatch.getBusinessStatus());
            }
        }

        //
        // Atualiza os atributos de rede
        //
        if (requestedPatch.getDiscoveryAttributes() != null && !requestedPatch.getDiscoveryAttributes().isEmpty()) {
            requestedPatch.getDiscoveryAttributes().forEach((name, attribute) -> {
                if (fromDBResource.getDiscoveryAttributes() != null) {
                    if (fromDBResource.getDiscoveryAttributes().containsKey(name)) {
                        fromDBResource.getDiscoveryAttributes().replace(name, attribute);
                    } else {
                        fromDBResource.getDiscoveryAttributes().put(name, attribute);
                    }
                }
            });
        }

        if (requestedPatch.getDependentService() != null) {
            //
            // Valida se o serviço existe
            //
            ServiceResource service = this.serviceManager.getService(requestedPatch.getDependentService());

            //
            // Atualiza para referencia do DB
            //
            requestedPatch.setDependentService(service);

            //
            // Agora vamos ver se o serviço é de um dominio diferente do recurso... não
            // podem ser do mesmo
            //
            if (service.getDomain().getDomainName().equals(requestedPatch.getDomain().getDomainName())) {
                throw new InvalidRequestException("Resource and Parent Service cannot be in the same domain.");
            }

            if (fromDBResource.getDependentService() == null) {
                //
                // Está criando a dependencia...
                //
                fromDBResource.setDependentService(requestedPatch.getDependentService());
            } else if (!fromDBResource.getDependentService().equals(service)) {
                fromDBResource.setDependentService(requestedPatch.getDependentService());
            }
        }

        fromDBResource.setConsumableMetric(requestedPatch.getConsumableMetric());
        fromDBResource.setConsumerMetric(requestedPatch.getConsumerMetric());

        ManagedResource result = this.managedResourceManager.update(fromDBResource);
        return new PatchManagedResourceResponse(result);
    }

    public PatchResourceConnectionResponse patchResourceConnection(PatchResourceConnectionRequest request)
            throws DomainNotFoundException, ResourceNotFoundException, ArangoDaoException, InvalidRequestException,
            AttributeConstraintViolationException {
        //
        //
        //
        ResourceConnection requestedPatch = request.getPayLoad();

        //
        // Arruma o domain para funcionar certinho
        //
        requestedPatch.setDomain(this.domainManager.getDomain(request.getRequestDomain()));
        requestedPatch.setDomainName(requestedPatch.getDomain().getDomainName());

        ResourceConnection connection = new ResourceConnection(domainManager.getDomain(request.getRequestDomain()));
        connection.setId(requestedPatch.getId());
        ResourceConnection fromDBResource = this.findResourceConnection(connection);

        //
        // Se chegamos aqui, temos coisas para atualizar...
        // @ Todo, comparar para ver se houve algo que realmente mudou..
        //
        if (requestedPatch.getName() != null) {
            fromDBResource.setName(requestedPatch.getName());
        }

        if (requestedPatch.getNodeAddress() != null) {
            fromDBResource.setNodeAddress(requestedPatch.getNodeAddress());
        }

        if (requestedPatch.getClassName() != null && !requestedPatch.getClassName().equals("Default")) {
            fromDBResource.setClassName(requestedPatch.getClassName());
        }

        if (requestedPatch.getOperationalStatus() != null) {
            fromDBResource.setOperationalStatus(requestedPatch.getOperationalStatus());
        }

        //
        // Pode atualizar o AtributeSchemaModel ? isso é bem custosooo vamos tratar isso
        // em outro lugar...
        //
        if (requestedPatch.getAdminStatus() != null) {
            fromDBResource.setAdminStatus(requestedPatch.getAdminStatus());
        }

        //
        // Atualiza os atributos
        //
        if (requestedPatch.getAttributes() != null && !requestedPatch.getAttributes().isEmpty()) {
            requestedPatch.getAttributes().forEach((name, attribute) -> {
                if (fromDBResource.getAttributes() != null) {
                    if (fromDBResource.getAttributes().containsKey(name)) {
                        fromDBResource.getAttributes().replace(name, attribute);
                    } else {
                        fromDBResource.getAttributes().put(name, attribute);
                    }
                }
            });
        }

        //
        // Atualiza os atributos de rede
        //
        if (requestedPatch.getDiscoveryAttributes() != null && !requestedPatch.getDiscoveryAttributes().isEmpty()) {
            requestedPatch.getDiscoveryAttributes().forEach((name, attribute) -> {
                if (fromDBResource.getDiscoveryAttributes() != null) {
                    if (fromDBResource.getDiscoveryAttributes().containsKey(name)) {
                        fromDBResource.getDiscoveryAttributes().replace(name, attribute);
                    } else {
                        fromDBResource.getDiscoveryAttributes().put(name, attribute);
                    }
                }
            });
        }

        if (requestedPatch.getDependentService() != null) {
            //
            // Valida se o serviço existe
            //
            ServiceResource service = this.serviceManager.getService(requestedPatch.getDependentService());

            //
            // Atualiza para referencia do DB
            //
            requestedPatch.setDependentService(service);

            //
            // Agora vamos ver se o serviço é de um dominio diferente do recurso... não
            // podem ser do mesmo
            //
            if (service.getDomain().getDomainName().equals(requestedPatch.getDomain().getDomainName())) {
                throw new InvalidRequestException("Resource and Parent Service cannot be in the same domain.");
            }

            if (fromDBResource.getDependentService() == null) {
                //
                // Está criando a dependencia...
                //
                fromDBResource.setDependentService(requestedPatch.getDependentService());
            } else if (!fromDBResource.getDependentService().equals(service)) {
                fromDBResource.setDependentService(requestedPatch.getDependentService());
            }
        }

        DocumentUpdateEntity<ResourceConnection> result = this.resourceConnectionManager
                .updateResourceConnection(fromDBResource);
        return new PatchResourceConnectionResponse(result.getNew());
    }

    public FindResourceConnectionResponse findResourceConnectionById(FindResourceConnectionRequest request)
            throws ResourceNotFoundException, ArangoDaoException, InvalidRequestException, DomainNotFoundException {
        //
        // Fix Domain
        //

        Domain domain = this.domainManager.getDomain(request.getRequestDomain());
        request.getPayLoad().setDomain(domain);

        return new FindResourceConnectionResponse(this.findResourceConnection(request.getPayLoad()));
    }

}
