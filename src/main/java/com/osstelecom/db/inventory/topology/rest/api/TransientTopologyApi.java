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
package com.osstelecom.db.inventory.topology.rest.api;

import com.osstelecom.db.inventory.manager.request.ComputeTransientTopologyRequest;
import com.osstelecom.db.inventory.manager.response.ComputeTransientTopologyResponse;
import com.osstelecom.db.inventory.manager.security.model.AuthenticatedCall;
import com.osstelecom.db.inventory.topology.session.TransientTopologySession;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @author Lucas Nishimura
 * @created 08.11.2022
 */
@RestController
@RequestMapping("topology/v1")
@SecurityRequirement(name = "SecuredAPI")
public class TransientTopologyApi {

    @Autowired
    private TransientTopologySession transientTopologySession;

    @AuthenticatedCall(role = {"user", "operator"})
    @PostMapping(path = "/transient", produces = "application/json", consumes = "application/json")
    public ComputeTransientTopologyResponse computeTopology(@RequestBody ComputeTransientTopologyRequest request, HttpServletRequest httpRequest) throws IOException {
        httpRequest.setAttribute("request", request);
        return transientTopologySession.computeTransientTopologyRequest(request);
    }

}
