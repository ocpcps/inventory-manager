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

import com.osstelecom.db.inventory.manager.exception.InvalidRequestException;
import com.osstelecom.db.inventory.manager.exception.ServiceNotFoundException;
import com.osstelecom.db.inventory.manager.request.GetServiceRequest;
import com.osstelecom.db.inventory.manager.response.GetServiceResponse;
import org.springframework.stereotype.Service;

/**
 *
 * @author Lucas Nishimura <lucas.nishimura@gmail.com>
 * @created 18.08.2022
 */
@Service
public class ServiceSession {

    public GetServiceResponse getServiceByServiceId(GetServiceRequest request) throws ServiceNotFoundException, InvalidRequestException {
        
        if (request.getPayLoad().getId()==null){
            InvalidRequestException ex =  new InvalidRequestException("ID Field Missing");
            throw ex;
        }
        
        if (false) {
            throw new ServiceNotFoundException("Service with Id:[" + request.getPayLoad().getId() + "] not found");
        }
        return new GetServiceResponse(request.getPayLoad());
    }
}
