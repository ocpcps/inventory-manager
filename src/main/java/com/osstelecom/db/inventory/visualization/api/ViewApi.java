/*
 * Copyright (C) 2023 Lucas Nishimura <lucas.nishimura@gmail.com>
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
package com.osstelecom.db.inventory.visualization.api;

import com.osstelecom.db.inventory.manager.exception.ArangoDaoException;
import com.osstelecom.db.inventory.manager.exception.DomainNotFoundException;
import com.osstelecom.db.inventory.manager.exception.InvalidRequestException;
import com.osstelecom.db.inventory.manager.exception.ResourceNotFoundException;
import com.osstelecom.db.inventory.manager.resources.ManagedResource;
import com.osstelecom.db.inventory.manager.rest.api.BaseApi;
import com.osstelecom.db.inventory.visualization.request.GetStructureDependencyRequest;
import com.osstelecom.db.inventory.visualization.response.ThreeJsViewResponse;
import com.osstelecom.db.inventory.visualization.session.FilterViewSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @author Lucas Nishimura <lucas.nishimura@gmail.com>
 * @created 12.01.2023
 */
@RestController
@RequestMapping("inventory/v1/view")

public class ViewApi extends BaseApi {

    @Autowired
    private FilterViewSession viewSession;

    private Logger logger = LoggerFactory.getLogger(ViewApi.class);

    @GetMapping(path = "/sample/{limit}", produces = "application/json")
    public ThreeJsViewResponse getSampleView(@PathVariable("limit") Long limit) throws DomainNotFoundException, ArangoDaoException, InvalidRequestException, ResourceNotFoundException {
        return viewSession.getSampleResult(limit);
    }

    @GetMapping(path = "{domain}/resource/{resourceKey}", produces = "application/json")
    public ThreeJsViewResponse getResourceStrucureDependency(@PathVariable("domain") String domain, @PathVariable("resourceKey") String resourceKey) throws DomainNotFoundException, ArangoDaoException, ResourceNotFoundException, InvalidRequestException {
        GetStructureDependencyRequest request = new GetStructureDependencyRequest();
        request.setPayLoad(new ManagedResource());
        request.getPayLoad().setKey(resourceKey);
        request.setRequestDomain(domain);
        this.setUserDetails(request);
        return this.viewSession.getResourceStrucureDependency(request);
    }

}
