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
import com.osstelecom.db.inventory.manager.exception.GenericException;
import com.osstelecom.db.inventory.manager.exception.InvalidRequestException;
import com.osstelecom.db.inventory.manager.exception.ResourceNotFoundException;
import com.osstelecom.db.inventory.manager.exception.SchemaNotFoundException;
import com.osstelecom.db.inventory.manager.exception.ScriptRuleException;
import com.osstelecom.db.inventory.manager.operation.DomainManager;
import com.osstelecom.db.inventory.manager.operation.ServiceManager;
import com.osstelecom.db.inventory.manager.request.CreateServiceRequest;
import com.osstelecom.db.inventory.manager.request.DeleteServiceRequest;
import com.osstelecom.db.inventory.manager.request.FilterRequest;
import com.osstelecom.db.inventory.manager.request.GetServiceRequest;
import com.osstelecom.db.inventory.manager.request.PatchServiceRequest;
import com.osstelecom.db.inventory.manager.resources.Domain;
import com.osstelecom.db.inventory.manager.resources.GraphList;
import com.osstelecom.db.inventory.manager.resources.ServiceResource;
import com.osstelecom.db.inventory.manager.resources.exception.AttributeConstraintViolationException;
import com.osstelecom.db.inventory.manager.response.CreateServiceResponse;
import com.osstelecom.db.inventory.manager.response.DeleteServiceResponse;
import com.osstelecom.db.inventory.manager.response.FilterResponse;
import com.osstelecom.db.inventory.manager.response.GetServiceResponse;
import com.osstelecom.db.inventory.manager.response.PatchServiceResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Lucas Nishimura
 * @created 18.08.2022
 */
@Service
public class ServiceSession {

    @Autowired
    private ServiceManager serviceManager;

    @Autowired
    private DomainManager domainManager;

    private Logger logger = LoggerFactory.getLogger(ServiceSession.class);

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
        payload = serviceManager.resolveCircuitsAndServices(payload);

        return new CreateServiceResponse(serviceManager.createService(payload));
    }

    public PatchServiceResponse updateService(PatchServiceRequest patchRequest) throws InvalidRequestException, DomainNotFoundException, ResourceNotFoundException, ArangoDaoException, SchemaNotFoundException, GenericException, AttributeConstraintViolationException, ScriptRuleException {

        //
        //
        //
        ServiceResource requestedPatch = patchRequest.getPayLoad();
        ServiceResource searchObj = null;
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
            searchObj = new ServiceResource(domainManager.getDomain(requestedPatch.getDomainName()), requestedPatch.getId());
        } else {
            searchObj = requestedPatch;
        }

        ServiceResource fromDBResource = this.findServiceResource(searchObj);

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
            if (fromDBResource.getAttributes().isEmpty()) {
                fromDBResource.getAttributes().putAll(requestedPatch.getAttributes());
            } else {
                requestedPatch.getAttributes().forEach((name, value) -> {
                    if (fromDBResource.getAttributes().containsKey(name)) {
                        logger.debug("Update Key:[{}] With Value:[{}]", name, value);
                        fromDBResource.getAttributes().replace(name, value);
                    } else {
                        fromDBResource.getAttributes().put(name, value);
                    }
                });
            }
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

        fromDBResource.setConsumableMetric(requestedPatch.getConsumableMetric());
        fromDBResource.setConsumerMetric(requestedPatch.getConsumerMetric());

        /**
         * Agora vamos lidar de resolver os circuitos e services
         */
        if (requestedPatch.getDependencies() != null) {
            fromDBResource.setDependencies(requestedPatch.getDependencies());
        }
        if (requestedPatch.getCircuits() != null) {
            fromDBResource.setCircuits(requestedPatch.getCircuits());
        }

        ServiceResource result = serviceManager.resolveCircuitsAndServices(fromDBResource);
        if ((result.getCircuits() == null || result.getCircuits().isEmpty()) && (result.getDependencies() == null || result.getDependencies().isEmpty())) {
            throw new InvalidRequestException("Please give at least one circuit or dependency");
        }
        result = this.serviceManager.updateService(fromDBResource);

        //
        // Resolveu aqui, será que não faz sentido ir para o manager inteiro ?
        //
        return new PatchServiceResponse(serviceManager.updateService(result));
    }

    public FilterResponse findServiceByFilter(FilterRequest filter) throws InvalidRequestException, ArangoDaoException, DomainNotFoundException, ResourceNotFoundException {
        //
        // Validação para evitar abusos de uso da API
        //
      
        if (filter.getPayLoad() != null) {
            if (filter.getPayLoad().getLimit() != null) {
                if (filter.getPayLoad().getLimit() > 1000) {
                    throw new InvalidRequestException("Result Set Limit cannot be over 1000, please descrease limit value to a range between 0 and 1000");
                }
            }
        }
        FilterResponse response = new FilterResponse(filter.getPayLoad());
        if (filter.getPayLoad().getObjects().contains("service") || filter.getPayLoad().getObjects().contains("services")) {
            Domain domain = domainManager.getDomain(filter.getRequestDomain());
            GraphList<ServiceResource> graphList = serviceManager.findServiceByFilter(filter.getPayLoad(), domain);
            response.getPayLoad().setServices(graphList.toList());
            response.getPayLoad().setServiceCount(graphList.size());
            response.setSize(graphList.size());
        } else {
            throw new InvalidRequestException("Filter object does not have service");
        }

        return response;
    }

    public ServiceResource findServiceResource(ServiceResource resource)
            throws ResourceNotFoundException, ArangoDaoException, InvalidRequestException {
        return this.serviceManager.findServiceResource(resource);
    }

}
