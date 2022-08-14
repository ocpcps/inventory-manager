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

import com.osstelecom.db.inventory.manager.dto.DomainDTO;
import com.osstelecom.db.inventory.manager.exception.ArangoDaoException;
import com.osstelecom.db.inventory.manager.exception.DomainNotFoundException;
import com.osstelecom.db.inventory.manager.exception.GenericException;
import com.osstelecom.db.inventory.manager.exception.InvalidRequestException;
import com.osstelecom.db.inventory.manager.exception.ResourceNotFoundException;
import com.osstelecom.db.inventory.manager.exception.SchemaNotFoundException;
import com.osstelecom.db.inventory.manager.exception.ScriptRuleException;
import com.osstelecom.db.inventory.manager.operation.DomainManager;
import com.osstelecom.db.inventory.manager.request.CreateConnectionRequest;
import com.osstelecom.db.inventory.manager.request.CreateManagedResourceRequest;
import com.osstelecom.db.inventory.manager.request.CreateResourceLocationRequest;
import com.osstelecom.db.inventory.manager.request.CreateServiceRequest;
import com.osstelecom.db.inventory.manager.request.FilterRequest;
import com.osstelecom.db.inventory.manager.request.FindManagedResourceRequest;
import com.osstelecom.db.inventory.manager.request.PatchManagedResourceRequest;
import com.osstelecom.db.inventory.manager.resources.ManagedResource;
import com.osstelecom.db.inventory.manager.resources.ResourceConnection;
import com.osstelecom.db.inventory.manager.resources.ResourceLocation;
import com.osstelecom.db.inventory.manager.resources.exception.AttributeConstraintViolationException;
import com.osstelecom.db.inventory.manager.resources.exception.ConnectionAlreadyExistsException;
import com.osstelecom.db.inventory.manager.resources.exception.MetricConstraintException;
import com.osstelecom.db.inventory.manager.resources.exception.NoResourcesAvailableException;
import com.osstelecom.db.inventory.manager.response.CreateManagedResourceResponse;
import com.osstelecom.db.inventory.manager.response.CreateResourceConnectionResponse;
import com.osstelecom.db.inventory.manager.response.CreateResourceLocationResponse;
import com.osstelecom.db.inventory.manager.response.CreateServiceResponse;
import com.osstelecom.db.inventory.manager.response.FilterResponse;
import com.osstelecom.db.inventory.manager.response.FindManagedResourceResponse;
import com.osstelecom.db.inventory.manager.response.PatchManagedResourceResponse;
import java.util.Date;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    
    private Logger logger = LoggerFactory.getLogger(ResourceSession.class);

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
            //
            // Avaliar se podemos mudar o node address
            //
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
    public CreateResourceConnectionResponse createResourceConnection(CreateConnectionRequest request) throws ResourceNotFoundException, ConnectionAlreadyExistsException, MetricConstraintException, NoResourcesAvailableException, GenericException, SchemaNotFoundException, AttributeConstraintViolationException, ScriptRuleException, InvalidRequestException, DomainNotFoundException, ArangoDaoException {

//        ManagedResource from = domainManager.findManagedResource(request.getPayLoad().getFromName(), request.getPayLoad().getFromNodeAddress(), request.getPayLoad().getFromClassName(), request.getRequestDomain());
//        ManagedResource to = domainManager.findManagedResource(request.getPayLoad().getToName(), request.getPayLoad().getToNodeAddress(), request.getPayLoad().getToClassName(), request.getRequestDomain());
        ResourceConnection connection = new ResourceConnection(domainManager.getDomain(request.getRequestDomain()));
        connection.setName(request.getPayLoad().getConnectionName());
        connection.setClassName(request.getPayLoad().getConnectionClass());

        //
        // @since 08-08-2022: Prioriza os iDS aos Nomes
        //
        if (request.getPayLoad().getFromId() != null && request.getPayLoad().getToId() != null) {
            //
            // Temos dois IDs podemos proesseguir com a validação por aqui
            //
            FindManagedResourceRequest fromResourceRequest = new FindManagedResourceRequest(request.getPayLoad().getFromId(), request.getRequestDomain());
            DomainDTO fromDomain = this.domainManager.getDomain(fromResourceRequest.getRequestDomain());
            
            fromResourceRequest.setRequestDomain(request.getRequestDomain());
            ManagedResource fromResource = domainManager.findManagedResourceById(fromResourceRequest.getResourceId(), fromDomain);
            
            FindManagedResourceRequest toResourceRequest = new FindManagedResourceRequest(request.getPayLoad().getToId(), request.getRequestDomain());
            DomainDTO toDomain = this.domainManager.getDomain(toResourceRequest.getRequestDomain());
            toResourceRequest.setRequestDomain(request.getRequestDomain());
            ManagedResource toResource = domainManager.findManagedResourceById(toResourceRequest.getResourceId(), toDomain);
            
            connection.setFrom(fromResource);
            connection.setTo(toResource);
            
        } else {
            //
            // Valida se é uma conexão entre location e Resource por nodeAddress
            //
            if (request.getPayLoad().getFromClassName().contains("location")) {
                connection.setFrom(domainManager.findResourceLocation(request.getPayLoad().getFromName(), request.getPayLoad().getFromNodeAddress(), request.getPayLoad().getFromClassName(), request.getRequestDomain()));
            } else if (request.getPayLoad().getFromClassName().contains("resource")) {
                connection.setFrom(domainManager.findManagedResource(request.getPayLoad().getFromName(), request.getPayLoad().getFromNodeAddress(), request.getPayLoad().getFromClassName(), request.getRequestDomain()));
            } else {
                throw new InvalidRequestException("Invalid From Class");
            }

            //
            // Valida se é uma conexão entre Location ou Resource
            //
            if (request.getPayLoad().getToClassName().contains("location")) {
                connection.setTo(domainManager.findResourceLocation(request.getPayLoad().getToName(), request.getPayLoad().getToNodeAddress(), request.getPayLoad().getToClassName(), request.getRequestDomain()));
            } else if (request.getPayLoad().getToClassName().contains("resource")) {
                connection.setTo(domainManager.findManagedResource(request.getPayLoad().getToName(), request.getPayLoad().getToNodeAddress(), request.getPayLoad().getToClassName(), request.getRequestDomain()));
            } else {
                throw new InvalidRequestException("Invalid TO Class");
            }
        }
        
        if (request.getPayLoad().getNodeAddress() != null) {
            connection.setNodeAddress(request.getPayLoad().getNodeAddress());
        } else {
            connection.setNodeAddress(request.getPayLoad().getFromNodeAddress() + "." + request.getPayLoad().getToNodeAddress());
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
     * Obtem o managed Resource By ID
     *
     * @param request
     * @return
     * @throws InvalidRequestException
     * @throws DomainNotFoundException
     * @throws ResourceNotFoundException
     * @throws ArangoDaoException
     */
    public FindManagedResourceResponse findManagedResourceById(FindManagedResourceRequest request) throws InvalidRequestException, DomainNotFoundException, ResourceNotFoundException, ArangoDaoException {
        if (request.getResourceId() == null) {
            throw new InvalidRequestException("Field resourceId cannot be empty or null");
        } else {
            try {
                UUID uuid = UUID.fromString(request.getResourceId());
                DomainDTO domainDto = this.domainManager.getDomain(request.getRequestDomain());
                FindManagedResourceResponse response = new FindManagedResourceResponse(this.domainManager.findManagedResourceById(request.getResourceId(), domainDto));
                return response;
            } catch (IllegalArgumentException exception) {
                throw new InvalidRequestException("ResourceId Invalid UUID:[" + request.getResourceId() + "]");
            }
            
        }
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
    public CreateResourceConnectionResponse createResourceLocationConnection(CreateConnectionRequest request) throws ResourceNotFoundException, ConnectionAlreadyExistsException, MetricConstraintException, NoResourcesAvailableException, GenericException, SchemaNotFoundException, AttributeConstraintViolationException, ScriptRuleException, DomainNotFoundException, ArangoDaoException {
        
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
    public CreateManagedResourceResponse createManagedResource(CreateManagedResourceRequest request) throws SchemaNotFoundException, AttributeConstraintViolationException, GenericException, ScriptRuleException, InvalidRequestException, DomainNotFoundException, ArangoDaoException {
        ManagedResource resource = request.getPayLoad();
        if (request == null) {
            throw new InvalidRequestException("Request is NULL!");
        }
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
        
        if (request.getPayLoad().getOperationalStatus() == null) {
            request.getPayLoad().setOperationalStatus("UP");
        }
        
        if (request.getPayLoad().getAdminStatus() == null) {
            request.getPayLoad().setAdminStatus("UP");
        }

        //
        // Avaliar se podemos melhorar isso, usando um nome canonico, com o className + name
        //
        if (request.getPayLoad().getNodeAddress() == null) {
            request.getPayLoad().setNodeAddress(request.getPayLoad().getName());
        }
        
        resource.setInsertedDate(new Date());
        resource = domainManager.createManagedResource(resource);
        CreateManagedResourceResponse response = new CreateManagedResourceResponse(resource);
        return response;
    }
    
    public CreateServiceResponse createService(CreateServiceRequest request) {
        CreateServiceResponse result = null;
        return result;
    }
    
    public FilterResponse getElementsByFilter(FilterRequest filter) throws DomainNotFoundException, ResourceNotFoundException, ArangoDaoException {
        
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
    
    public ManagedResource findManagedResource(ManagedResource resource) throws ResourceNotFoundException, DomainNotFoundException, ArangoDaoException {
        return this.domainManager.findManagedResource(resource);
    }
    
    /**
     * Atualiza um Managed Resource
     * @param patchRequest
     * @return
     * @throws DomainNotFoundException
     * @throws ResourceNotFoundException
     * @throws ArangoDaoException
     * @throws InvalidRequestException 
     */
    public PatchManagedResourceResponse patchManagedResource(PatchManagedResourceRequest patchRequest) throws DomainNotFoundException, ResourceNotFoundException, ArangoDaoException, InvalidRequestException {
        //
        //
        //
        ManagedResource requestedPatch = patchRequest.getPayLoad();
        //
        // Arruma o domain para funcionar certinho
        //
        requestedPatch.setDomain(this.domainManager.getDomain(patchRequest.getRequestDomain()));
        requestedPatch.setDomainName(requestedPatch.getDomain().getDomainName());
        
        ManagedResource fromDBResource = this.findManagedResource(requestedPatch);

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
        
        if (requestedPatch.getClassName() != null) {
            if (!requestedPatch.getClassName().equals("Default")) {
                fromDBResource.setClassName(requestedPatch.getClassName());
            }
        }
        
        if (requestedPatch.getOperationalStatus() != null) {
            fromDBResource.setOperationalStatus(requestedPatch.getOperationalStatus());
        }

        //
        // Pode atualizar o AtributeSchemaModel ? isso é bem custosooo vamos tratar isso em outro lugar...
        //
        if (requestedPatch.getAdminStatus() != null) {
            fromDBResource.setAdminStatus(requestedPatch.getAdminStatus());
        }
        
        ManagedResource result = this.domainManager.updateManagedResource(fromDBResource);
        return new PatchManagedResourceResponse(result);
        
    }
}
