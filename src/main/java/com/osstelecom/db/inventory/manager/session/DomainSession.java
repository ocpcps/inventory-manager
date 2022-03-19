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

import com.osstelecom.db.inventory.manager.exception.DomainAlreadyExistsException;
import com.osstelecom.db.inventory.manager.exception.GenericException;
import com.osstelecom.db.inventory.manager.operation.DomainManager;

import com.osstelecom.db.inventory.manager.request.CreateDomainRequest;
import com.osstelecom.db.inventory.manager.response.CreateDomainResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 *
 * @author Lucas Nishimura <lucas.nishimura@gmail.com>
 * @created 15.12.2021
 */
@Service
public class DomainSession {
   
    @Autowired
    private DomainManager domainManager;

    public CreateDomainResponse createDomain(CreateDomainRequest domainRequest) throws DomainAlreadyExistsException, GenericException {
        try {
            CreateDomainResponse response = new CreateDomainResponse(this.domainManager.createDomain(domainRequest.getPayLoad()));
            return response;
        } catch (DomainAlreadyExistsException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new GenericException(ex.getMessage());
        }
    }

}
