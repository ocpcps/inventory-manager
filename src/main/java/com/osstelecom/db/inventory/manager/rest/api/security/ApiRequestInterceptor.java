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
package com.osstelecom.db.inventory.manager.rest.api.security;

import com.google.common.collect.ImmutableMap;
import com.osstelecom.db.inventory.manager.exception.ApiSecurityException;
import com.osstelecom.db.inventory.manager.security.model.AuthenticatedCall;
import com.osstelecom.db.inventory.manager.session.ApiSecuritySession;
import com.osstelecom.db.inventory.manager.session.UtilSession;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 *
 * @author Lucas Nishimura <lucas.nishimura@gmail.com>
 * @created 24.05.2022
 */
@Component
public class ApiRequestInterceptor implements HandlerInterceptor {

    @Autowired
    private UtilSession utilsSession;

    @Autowired
    private ApiSecuritySession securitySession;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        HandlerMethod method = (HandlerMethod) handler;
        AuthenticatedCall authenticatedCall = method.getMethod().getAnnotation(AuthenticatedCall.class);
        response.setHeader("x-response-id", utilsSession.getResponseId());
        if (authenticatedCall != null) {
            if (authenticatedCall.requiresAuth()) {
                securitySession.checkApiToken(request);
                response.setHeader("x-authenticated", "true");
                return true;
            } else {
                response.setHeader("x-authenticated", "true");
                return true;
            }
        } else {
            response.setHeader("x-authenticated", "false");
        }

        return true;
    }

}
