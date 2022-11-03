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

import com.arangodb.entity.DocumentUpdateEntity;
import com.osstelecom.db.inventory.manager.dto.FilterDTO;
import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.osstelecom.db.inventory.manager.exception.ArangoDaoException;
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
import com.osstelecom.db.inventory.manager.request.FilterRequest;
import com.osstelecom.db.inventory.manager.request.FindManagedResourceRequest;
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
import com.osstelecom.db.inventory.manager.response.FilterResponse;
import com.osstelecom.db.inventory.manager.response.FindManagedResourceResponse;
import com.osstelecom.db.inventory.manager.response.PatchManagedResourceResponse;
import com.osstelecom.db.inventory.manager.response.PatchResourceConnectionResponse;
import com.osstelecom.db.inventory.manager.response.TypedListResponse;
import java.util.HashMap;
import java.util.Map;

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
            Domain fromDomain = this.domainManager.getDomain(fromResourceRequest.getRequestDomain());

            fromResourceRequest.setRequestDomain(request.getRequestDomain());
            ManagedResource fromResource = manager.findManagedResource(new ManagedResource(fromDomain, fromResourceRequest.getResourceId()));

            FindManagedResourceRequest toResourceRequest = new FindManagedResourceRequest(request.getPayLoad().getToId(), request.getRequestDomain());
            Domain toDomain = this.domainManager.getDomain(toResourceRequest.getRequestDomain());
            toResourceRequest.setRequestDomain(request.getRequestDomain());
            ManagedResource toResource = manager.findManagedResource(new ManagedResource(toDomain, toResourceRequest.getResourceId()));

            connection.setFrom(fromResource);
            connection.setTo(toResource);

        } else {
            //
            // Valida se é uma conexão entre location e Resource por nodeAddress
            //
            Domain requestDomain = this.domainManager.getDomain(request.getRequestDomain());
            if (request.getPayLoad().getFromClassName().contains("location")) {
                connection.setFrom(resourceLocationManager.findResourceLocation(request.getPayLoad().getFromName(), request.getPayLoad().getFromNodeAddress(), request.getPayLoad().getFromClassName(), request.getRequestDomain()));
            } else if (request.getPayLoad().getFromClassName().contains("resource")) {
                connection.setFrom(manager.findManagedResource(new ManagedResource(requestDomain, request.getPayLoad().getFromName(), request.getPayLoad().getFromNodeAddress(), request.getPayLoad().getFromClassName())));
            } else {
                throw new InvalidRequestException("Invalid From Class");
            }

            //
            // Valida se é uma conexão entre Location ou Resource
            //
            if (request.getPayLoad().getToClassName().contains("location")) {
                connection.setTo(resourceLocationManager.findResourceLocation(request.getPayLoad().getToName(), request.getPayLoad().getToNodeAddress(), request.getPayLoad().getToClassName(), request.getRequestDomain()));
            } else if (request.getPayLoad().getToClassName().contains("resource")) {
                //connection.setTo(domainManager.findManagedResource(request.getPayLoad().getToName(), request.getPayLoad().getToNodeAddress(), request.getPayLoad().getToClassName(), request.getRequestDomain()));
                connection.setTo(manager.findManagedResource(new ManagedResource(requestDomain, request.getPayLoad().getToName(), request.getPayLoad().getToNodeAddress(), request.getPayLoad().getToClassName())));

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

        resourceConnectionManager.createResourceConnection(connection);
        return response;
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
    public DeleteManagedResourceResponse deleteManagedResource(DeleteManagedResourceRequest request) throws InvalidRequestException, DomainNotFoundException, ArangoDaoException, ResourceNotFoundException {
        if (request.getResourceId() == null) {
            throw new InvalidRequestException("Please Provide Resource ID to delete");
        } else if (request.getRequestDomain() == null) {
            throw new InvalidRequestException("Please Provide Domain Name of Resource to  delete");
        }

        Domain domain = this.domainManager.getDomain(request.getRequestDomain());
        //
        // Precisa Enconrar o Recurso.
        //
        ManagedResource resource = this.managedResourceManager.findManagedResourceById(new ManagedResource(domain, request.getResourceId()));

        //
        // O Problema do delete de um recurso é que ele pode ser necessário para alguma conexão e consequentemente um circuito...
        // Então vamos ver se esse recurso é necessário para alguma conexão deste dominio
        //
        Map<String, Object> bindings = new HashMap<>();
        bindings.put("resourceId", resource.getId());
        FilterDTO connectionFilter = new FilterDTO();
        connectionFilter.setAqlFilter("doc.fromResource._id == @resourceId or doc.toResource._id == @resourceId ");
        connectionFilter.getObjects().add("connections");
        connectionFilter.setBindings(bindings);
        try {
            GraphList<ResourceConnection> connections = resourceConnectionManager.getConnectionsByFilter(connectionFilter, domain.getDomainName());
            //
            // Se chegou aqui é porque tem conexões que dependem do recurso, não podemos deletar
            //
            throw new InvalidRequestException(("Resource ID is Used By:[" + connections.size() + "] Connections, please remove theses dependencies, before delete"));
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
                throw new InvalidRequestException(("Resource ID is Used By:[" + circuits.size() + "] Circuits, please remove theses dependencies, before delete"));
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
    public FindManagedResourceResponse findManagedResourceById(FindManagedResourceRequest request) throws InvalidRequestException, DomainNotFoundException, ResourceNotFoundException, ArangoDaoException {
        if (request.getResourceId() == null) {
            if (request.getRequestDomain() == null) {
                throw new InvalidRequestException("Field resourceId and domain cannot be empty or null");
            } else {
                try {
                    Domain domain = this.domainManager.getDomain(request.getRequestDomain());
                    return new FindManagedResourceResponse(this.manager.findManagedResource(new ManagedResource(domain, request.getResourceId())));
                } catch (IllegalArgumentException exception) {
                    throw new InvalidRequestException("ResourceId Invalid DOMAIN:[" + request.getRequestDomain() + "]");
                }
            }
        } else {
            try {
                Domain domain = this.domainManager.getDomain(request.getRequestDomain());
                return new FindManagedResourceResponse(this.manager.findManagedResource(new ManagedResource(domain, request.getResourceId())));
            } catch (IllegalArgumentException exception) {
                throw new InvalidRequestException("ResourceId Invalid UUID:[" + request.getResourceId() + "]");
            }

        }

    }

    public TypedListResponse listManagedResources(ListManagedResourceRequest request) throws DomainNotFoundException, ResourceNotFoundException, ArangoDaoException, InvalidRequestException {
        FilterDTO filter = new FilterDTO();
        filter.setAqlFilter(" ");
        filter.setSortCondition("sort doc.nodeAddress asc");
        filter.getObjects().add("nodes");
        TypedListResponse response
                = new TypedListResponse(this.managedResourceManager
                        .getNodesByFilter(filter, request.getRequestDomain()).toList());
        return response;
    }

    public TypedListResponse listResourceConnection(ListResourceConnectionRequest request) throws DomainNotFoundException, ResourceNotFoundException, ArangoDaoException, InvalidRequestException {
        FilterDTO filter = new FilterDTO();
        filter.setAqlFilter(" ");
        filter.setSortCondition("sort doc.nodeAddress asc");
        filter.getObjects().add("connections");
        TypedListResponse response
                = new TypedListResponse(this.resourceConnectionManager
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
    public CreateManagedResourceResponse createManagedResource(CreateManagedResourceRequest request) throws SchemaNotFoundException, AttributeConstraintViolationException, GenericException, ScriptRuleException, InvalidRequestException, DomainNotFoundException, ArangoDaoException, ResourceNotFoundException {
        if (request == null) {
            throw new InvalidRequestException("Request is NULL!");
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
        }

        if (resource.getClassName() == null) {
            resource.setClassName("resource.Default");
        } else if (!resource.getClassName().startsWith("resource")) {
            throw new InvalidRequestException("Class Name Has to Start with resource.");
        }

        if (resource.getOperationalStatus() == null) {
            resource.setOperationalStatus("UP");
        }

        if (resource.getAdminStatus() == null) {
            resource.setAdminStatus("UP");
        }

        //
        // Avaliar se podemos melhorar isso, usando um nome canonico, com o className + name
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
        return new CreateManagedResourceResponse(resource);
    }

    public FilterResponse findManagedResourceByFilter(FilterRequest filter) throws InvalidRequestException, ArangoDaoException, DomainNotFoundException, ResourceNotFoundException {

        FilterResponse response = new FilterResponse(filter.getPayLoad());
        if (filter.getPayLoad().getObjects().contains("nodes") || filter.getPayLoad().getObjects().contains("node")) {
            response.getPayLoad().setNodes(managedResourceManager.getNodesByFilter(filter.getPayLoad(), filter.getRequestDomain()).toList());
            response.getPayLoad().setNodeCount(response.getPayLoad().getNodes().size());
        } else if (filter.getPayLoad().getObjects().contains("connections") || filter.getPayLoad().getObjects().contains("connection")) {
            response.getPayLoad().setConnections(resourceConnectionManager.getConnectionsByFilter(filter.getPayLoad(), filter.getRequestDomain()).toList());
            if (filter.getPayLoad().getComputeWeakLinks()) {
                //
                // Computação de Links Fracos Desabilitada
                //
                throw new InvalidRequestException("Weak Links Calculation is Disabled on this system");
            }
        }

        return response;
    }

    public ManagedResource findManagedResource(ManagedResource resource) throws ResourceNotFoundException, ArangoDaoException {
        return this.manager.findManagedResource(resource);
    }

    public ResourceConnection findResourceConnection(ResourceConnection connection) throws ResourceNotFoundException, ArangoDaoException {
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
     */
    public PatchManagedResourceResponse patchManagedResource(PatchManagedResourceRequest patchRequest) throws DomainNotFoundException, ResourceNotFoundException, ArangoDaoException, InvalidRequestException, AttributeConstraintViolationException, ScriptRuleException {
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
        // Pode atualizar o AtributeSchemaModel ? isso é bem custosooo vamos tratar isso em outro lugar...
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
                ManagedResource structureResrouce = new ManagedResource(fromDBResource.getDomain(), requestedPatch.getStructureId());
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
            // Agora vamos ver se o serviço é de um dominio diferente do recurso... não podem ser do mesmo
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

        ManagedResource result = this.managedResourceManager.updateManagedResource(fromDBResource);
        return new PatchManagedResourceResponse(result);
    }

    public PatchResourceConnectionResponse patchResourceConnection(PatchResourceConnectionRequest request) throws DomainNotFoundException, ResourceNotFoundException, ArangoDaoException, InvalidRequestException, AttributeConstraintViolationException {
        //
        //
        //
        ResourceConnection requestedPatch = request.getPayLoad();

        //
        // Arruma o domain para funcionar certinho
        //
        requestedPatch.setDomain(this.domainManager.getDomain(request.getRequestDomain()));
        requestedPatch.setDomainName(requestedPatch.getDomain().getDomainName());

        ResourceConnection fromDBResource = this.findResourceConnection(requestedPatch);

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
        // Pode atualizar o AtributeSchemaModel ? isso é bem custosooo vamos tratar isso em outro lugar...
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
            // Agora vamos ver se o serviço é de um dominio diferente do recurso... não podem ser do mesmo
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

        DocumentUpdateEntity<ResourceConnection> result = this.resourceConnectionManager.updateResourceConnection(fromDBResource);
        return new PatchResourceConnectionResponse(result.getNew());
    }

}
