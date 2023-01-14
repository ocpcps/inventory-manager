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
package com.osstelecom.db.inventory.visualization.session;

import com.osstelecom.db.inventory.manager.dto.FilterDTO;
import com.osstelecom.db.inventory.manager.exception.ArangoDaoException;
import com.osstelecom.db.inventory.manager.exception.DomainNotFoundException;
import com.osstelecom.db.inventory.manager.exception.InvalidRequestException;
import com.osstelecom.db.inventory.manager.exception.ResourceNotFoundException;
import com.osstelecom.db.inventory.manager.operation.DomainManager;
import com.osstelecom.db.inventory.manager.request.FilterRequest;
import com.osstelecom.db.inventory.manager.resources.Domain;
import com.osstelecom.db.inventory.manager.response.FilterResponse;
import com.osstelecom.db.inventory.manager.session.ResourceSession;
import com.osstelecom.db.inventory.visualization.dto.ThreeJSViewDTO;
import com.osstelecom.db.inventory.visualization.response.ThreeJsViewResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 *
 * @author Lucas Nishimura <lucas.nishimura@gmail.com>
 * @created 12.01.2023
 */
@Service
public class FilterViewSession {

    @Autowired
    private ResourceSession resourceSession;

    @Autowired
    private DomainManager domainManager;

    public ThreeJsViewResponse getSampleResult(Long limitSize) throws DomainNotFoundException, ArangoDaoException, InvalidRequestException, ResourceNotFoundException {
        Domain domain = domainManager.getDomain("co");
        FilterRequest request = new FilterRequest(new FilterDTO());
        request.getPayLoad().setAqlFilter(" doc.nodeAddress != 'xyz' ");
        request.getPayLoad().setLimit(limitSize);
        request.getPayLoad().setOffSet(0L);
        request.setRequestDomain(domain.getDomainName());
        request.getPayLoad().getObjects().add("connections");
        FilterResponse filterResponse = resourceSession.findManagedResourceByFilter(request);
        ThreeJsViewResponse response = new ThreeJsViewResponse(new ThreeJSViewDTO(filterResponse));
        return response;
    }
}
