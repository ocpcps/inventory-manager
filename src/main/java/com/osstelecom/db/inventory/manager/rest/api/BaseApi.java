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

import com.osstelecom.db.inventory.manager.request.BasicRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.StandardClaimNames;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/**
 *
 * @author Lucas Nishimura <lucas.nishimura@gmail.com>
 * @created 21.07.2022
 */
public class BaseApi {

    /**
     * Take care of Json Serialization
     */
    protected Logger logger = LoggerFactory.getLogger(this.getClass());

    /**
     * Obtem os dados de usuário do JWT
     *
     * @param req
     */
    protected void setUserDetails(BasicRequest req) {
        if (req != null) {
            Object details = SecurityContextHolder.getContext().getAuthentication();
            if (details instanceof JwtAuthenticationToken) {
                JwtAuthenticationToken userDetails = (JwtAuthenticationToken) details;
                String userName = (String) userDetails.getToken().getClaimAsString(StandardClaimNames.NAME);
                String userLogin = (String) userDetails.getToken().getClaimAsString(StandardClaimNames.PREFERRED_USERNAME);
                String userId = (String) userDetails.getToken().getClaimAsString(StandardClaimNames.SUB);
                String email = (String) userDetails.getToken().getClaimAsString(StandardClaimNames.EMAIL);
                req.setUserId(userId);
                req.setUserEmail(email);
                req.setUserName(userName);
                req.setUserLogin(userLogin);
            }
        }
    }

}
