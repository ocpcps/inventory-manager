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
import com.osstelecom.db.inventory.manager.operation.IconManager;
import com.osstelecom.db.inventory.manager.operation.ServiceManager;
import com.osstelecom.db.inventory.manager.request.CreateIconRequest;
import com.osstelecom.db.inventory.manager.request.CreateServiceRequest;
import com.osstelecom.db.inventory.manager.request.DeleteIconRequest;
import com.osstelecom.db.inventory.manager.request.DeleteServiceRequest;
import com.osstelecom.db.inventory.manager.request.FilterRequest;
import com.osstelecom.db.inventory.manager.request.GetIconRequest;
import com.osstelecom.db.inventory.manager.request.GetServiceRequest;
import com.osstelecom.db.inventory.manager.request.PatchIconRequest;
import com.osstelecom.db.inventory.manager.request.PatchServiceRequest;
import com.osstelecom.db.inventory.manager.resources.Domain;
import com.osstelecom.db.inventory.manager.resources.GraphList;
import com.osstelecom.db.inventory.manager.resources.ServiceResource;
import com.osstelecom.db.inventory.manager.resources.exception.AttributeConstraintViolationException;
import com.osstelecom.db.inventory.manager.resources.model.IconModel;
import com.osstelecom.db.inventory.manager.response.CreateIconResponse;
import com.osstelecom.db.inventory.manager.response.CreateServiceResponse;
import com.osstelecom.db.inventory.manager.response.DeleteIconResponse;
import com.osstelecom.db.inventory.manager.response.DeleteServiceResponse;
import com.osstelecom.db.inventory.manager.response.FilterResponse;
import com.osstelecom.db.inventory.manager.response.GetIconResponse;
import com.osstelecom.db.inventory.manager.response.GetServiceResponse;
import com.osstelecom.db.inventory.manager.response.PatchIconResponse;
import com.osstelecom.db.inventory.manager.response.PatchServiceResponse;

/**
 *
 * @author Lucas Nishimura <lucas.nishimura@gmail.com>
 * @created 18.08.2022
 */
@Service
public class IconSession {

    @Autowired
    private IconManager iconManager;

    @Autowired
    private DomainManager domainManager;

    public GetIconResponse getIconById(GetIconRequest request) throws ResourceNotFoundException, DomainNotFoundException, ArangoDaoException, InvalidRequestException {
        if (request.getPayLoad().getSchemaName() == null) {
            
        }

        return new GetIconResponse(IconManager.getIconById(request.getPayLoad().getSchemaName()));
    }

    public DeleteIconResponse deleteIcon(DeleteIconRequest request) throws DomainNotFoundException, ArangoDaoException, InvalidRequestException {
        if (request.getPayLoad().getSchemaName() == null) {
            throw new InvalidRequestException("Schema name Field Missing");
        }
        

        return new DeleteIconResponse(iconManager.deleteIcon(request.getPayLoad()));
    }

    public CreateIconResponse createIcon(CreateIconRequest request) throws InvalidRequestException, DomainNotFoundException, ResourceNotFoundException, ArangoDaoException {

        if (request == null || request.getPayLoad() == null) {
            throw new InvalidRequestException("Request is null please send a valid request");
        }

        if (request.getPayLoad().getSchemaName() == null) {
            throw new InvalidRequestException("Schema name Field Missing");
        }

        IconModel payload = request.getPayLoad();
        if (payload == null) {
            throw new InvalidRequestException("Payload not found");
        }
        //
        // Treat Defaults
        //

         //getConteudo
        // Validate minimun requirements fields
        //
        if (payload.getSchemaName() != null) {
            if (payload.getConteudo() == null) {
                payload.setConteudo(payload.getConteudo());
            }
            payload.setSchemaName(payload.getSchemaName());
        }

        if (payload.getSchemaName() != null) {
            if (payload.getMimeType() == null) {
                payload.setMimeType(payload.getMimeType());
            }
        }

        if (payload.getSchemaName() == null) {
            throw new InvalidRequestException("Plase set name, or nodeAddress values");
        }
        //
        // Aqui resolve os circuitos e recursos.
        //
        payload = iconManager.resolveService(payload);

        return new CreateIconResponse(iconManager.createService(payload));
    }

    public PatchIconResponse updateIcon(PatchIconRequest request) throws InvalidRequestException, DomainNotFoundException, ResourceNotFoundException, ArangoDaoException, SchemaNotFoundException, GenericException, AttributeConstraintViolationException, ScriptRuleException {
        if (request.getPayLoad().getSchemaName() == null && request.getPayLoad().getMimeType() == null && request.getPayLoad().getConteudo() == null) {
            throw new InvalidRequestException("ID Field Missing");
        }

        if (request.getSchemaName() == null) {
            throw new DomainNotFoundException("Domain With Name:[" + request.getRequestDomain() + "] not found");
        }
        request.getPayLoad().setSchemaName(domainManager.getSchemaName(request.getSchemaName()));

        IconResource payload = request.getPayLoad();
        if (payload == null) {
            throw new InvalidRequestException("Payload not found");
        }
        IconResource old = null;
        if (payload.getId() != null) {
            old = iconManager.getServiceById(payload);
        } else {
            old = iconManager.getIcon(payload);
        }
        payload.setKey(old.getKey());

        if ((payload.getCircuits() == null || payload.getCircuits().isEmpty()) && (payload.getDependencies() == null || payload.getDependencies().isEmpty())) {
            throw new InvalidRequestException("Please give at least one circuit or dependency");
        }
        //
        // Resolveu aqui, será que não faz sentido ir para o manager inteiro ?
        //
        payload = serviceManager.resolveService(payload);

        return new PatchIconResponse(serviceManager.updateService(payload));
    }

    // public FilterResponse findServiceByFilter(FilterRequest filter) throws InvalidRequestException, ArangoDaoException, DomainNotFoundException, ResourceNotFoundException {
    //     //
    //     // Validação para evitar abusos de uso da API
    //     //
    //     if (filter.getPayLoad() != null) {
    //         if (filter.getPayLoad().getLimit() != null) {
    //             if (filter.getPayLoad().getLimit() > 1000) {
    //                 throw new InvalidRequestException("Result Set Limit cannot be over 1000, please descrease limit value to a range between 0 and 1000");
    //             }
    //         }
    //     }
    //     FilterResponse response = new FilterResponse(filter.getPayLoad());
    //     if (filter.getPayLoad().getObjects().contains("service") || filter.getPayLoad().getObjects().contains("services")) {
    //         Domain domain = domainManager.getDomain(filter.getRequestDomain());
    //         GraphList<ServiceResource> graphList = serviceManager.findServiceByFilter(filter.getPayLoad(), domain);
    //         response.getPayLoad().setServices(graphList.toList());
    //         response.getPayLoad().setServiceCount(graphList.size());
    //         response.setSize(graphList.size());
    //     } else {
    //         throw new InvalidRequestException("Filter object does not have service");
    //     }

    //     return response;
    // }

}
