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
package com.osstelecom.db.inventory.manager.rest.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.osstelecom.db.inventory.manager.exception.ArangoDaoException;
import com.osstelecom.db.inventory.manager.exception.DomainNotFoundException;
import com.osstelecom.db.inventory.manager.exception.InvalidRequestException;
import com.osstelecom.db.inventory.manager.exception.ResourceNotFoundException;
import com.osstelecom.db.inventory.manager.exception.ServiceNotFoundException;
import com.osstelecom.db.inventory.manager.request.CreateServiceRequest;
import com.osstelecom.db.inventory.manager.request.DeleteServiceRequest;
import com.osstelecom.db.inventory.manager.request.GetServiceRequest;
import com.osstelecom.db.inventory.manager.request.PatchServiceRequest;
import com.osstelecom.db.inventory.manager.resources.ServiceResource;
import com.osstelecom.db.inventory.manager.response.CreateServiceResponse;
import com.osstelecom.db.inventory.manager.response.DeleteServiceResponse;
import com.osstelecom.db.inventory.manager.response.GetServiceResponse;
import com.osstelecom.db.inventory.manager.response.PatchServiceResponse;
import com.osstelecom.db.inventory.manager.security.model.AuthenticatedCall;
import com.osstelecom.db.inventory.manager.session.ServiceSession;

/**
 *
 * @author Lucas Nishimura <lucas.nishimura@gmail.com>
 * @created 18.08.2022
 */
@RestController
@RequestMapping("inventory/v1/service")
public class ServiceApi {

    @Autowired
    private ServiceSession serviceSession;

    @AuthenticatedCall(role = { "user" })
    @GetMapping(path = "/{domain}/{serviceId}", produces = "application/json")
    public GetServiceResponse getServiceById(@PathVariable("domain") String domainName,
            @PathVariable("serviceId") String serviceId) throws InvalidRequestException, ServiceNotFoundException, DomainNotFoundException, ArangoDaoException {
        GetServiceRequest request = new GetServiceRequest();
        request.setRequestDomain(domainName);
        request.setPayLoad(new ServiceResource(serviceId));
        return serviceSession.getServiceByServiceId(request);
    }

    @AuthenticatedCall(role = {"user"})
    @DeleteMapping(path = "/{domainName}/{serviceId}", produces = "application/json")
    public DeleteServiceResponse deletService(@PathVariable("serviceId") String serviceId,@PathVariable("domainName") String domainName) throws DomainNotFoundException, ArangoDaoException {
        DeleteServiceRequest request = new DeleteServiceRequest(serviceId, domainName);
        return serviceSession.deleteService(request);
    }

    @AuthenticatedCall(role = {"user"})
    @PutMapping(path = "/{domain}", produces = "application/json", consumes = "application/json")
    public CreateServiceResponse createService(@RequestBody CreateServiceRequest request, @PathVariable("domain") String domain) throws InvalidRequestException, ServiceNotFoundException, DomainNotFoundException, ResourceNotFoundException, ArangoDaoException  {
        request.setRequestDomain(domain);
        return serviceSession.createService(request);
    }

    @AuthenticatedCall(role = {"user"})
    @PatchMapping(path = "/{domain}/{serviceId}", produces = "application/json", consumes = "application/json")
    public PatchServiceResponse patchManagedResource(@RequestBody PatchServiceRequest request, @PathVariable("domain") String domainName, @PathVariable("resourceId") String resourceId) throws InvalidRequestException, ServiceNotFoundException, DomainNotFoundException, ResourceNotFoundException, ArangoDaoException {
        request.setRequestDomain(domainName);
        request.getPayLoad().setId(resourceId);
        return serviceSession.updateService(request);
    }

}
