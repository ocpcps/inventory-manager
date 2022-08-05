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

import com.google.common.collect.ImmutableMap;
import com.osstelecom.db.inventory.manager.exception.ApiSecurityException;
import javax.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

/**
 * Cuida da autenticação da API
 * @author Lucas Nishimura <lucas.nishimura@gmail.com>
 * @created 12.06.2022
 */
@Service
public class ApiSecuritySession {

    public void checkApiToken(HttpServletRequest request) throws ApiSecurityException {
        if (request.getHeader("x-auth-token") == null) {
            //
            // So we need an api token to continue... but its null...
            //
            ApiSecurityException ex = new ApiSecurityException("API Token Not Found!");
            //
            // Give the requester some details on the request.
            //
            ex.setDetails(ImmutableMap.of("path", request.getRequestURI(), "method", request.getMethod(), "remoteAddress", request.getRemoteAddr()));
            throw ex;
        }
//        throw new ApiSecurityException("API Token Not Found!");
    }

}