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

import com.osstelecom.db.inventory.manager.exception.ArangoDaoException;
import com.osstelecom.db.inventory.manager.exception.DomainNotFoundException;
import com.osstelecom.db.inventory.manager.exception.GenericException;
import com.osstelecom.db.inventory.manager.exception.InvalidRequestException;
import com.osstelecom.db.inventory.manager.exception.ResourceNotFoundException;
import com.osstelecom.db.inventory.manager.exception.SchemaNotFoundException;
import com.osstelecom.db.inventory.manager.exception.ScriptRuleException;
import com.osstelecom.db.inventory.manager.operation.DomainManager;
import com.osstelecom.db.inventory.manager.operation.ResourceConnectionManager;
import com.osstelecom.db.inventory.manager.operation.ResourceLocationManager;
import com.osstelecom.db.inventory.manager.request.CreateConnectionRequest;
import com.osstelecom.db.inventory.manager.request.CreateResourceLocationRequest;
import com.osstelecom.db.inventory.manager.resources.ResourceConnection;
import com.osstelecom.db.inventory.manager.resources.ResourceLocation;
import com.osstelecom.db.inventory.manager.resources.exception.AttributeConstraintViolationException;
import com.osstelecom.db.inventory.manager.response.CreateResourceConnectionResponse;
import com.osstelecom.db.inventory.manager.response.CreateResourceLocationResponse;
import java.util.Date;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 *
 * @author Lucas Nishimura <lucas.nishimura@gmail.com>
 * @created 20.09.2022
 */
@Service
public class ResourceLocationSession {

    @Autowired
    private DomainManager domainManager;

    @Autowired
    private ResourceLocationManager resourceLocationManager;

    @Autowired
    private ResourceConnectionManager resourceConnectionManager;

    private Logger logger = LoggerFactory.getLogger(ResourceLocationSession.class);

    /**
     * Cria uma conexão entre localidades
     *
     * @param request
     * @return
     * @throws ResourceNotFoundException
     * @throws ConnectionAlreadyExistsException
     * @throws MetricConstraintException
     * @throws NoResourcesAvailableException
     * @throws GenericException
     * @throws SchemaNotFoundException
     * @throws AttributeConstraintViolationException
     */
    public CreateResourceConnectionResponse createResourceLocationConnection(CreateConnectionRequest request) throws ResourceNotFoundException, GenericException, SchemaNotFoundException, AttributeConstraintViolationException, ScriptRuleException, DomainNotFoundException, ArangoDaoException, InvalidRequestException {

        ResourceLocation from = resourceLocationManager.findResourceLocation(request.getPayLoad().getFromName(), request.getPayLoad().getFromNodeAddress(), request.getPayLoad().getFromClassName(), request.getRequestDomain());
        ResourceLocation to = resourceLocationManager.findResourceLocation(request.getPayLoad().getToName(), request.getPayLoad().getToNodeAddress(), request.getPayLoad().getToClassName(), request.getRequestDomain());

        ResourceConnection connection = new ResourceConnection(domainManager.getDomain(request.getRequestDomain()));
        connection.setName(request.getPayLoad().getConnectionName());

        if (request.getPayLoad().getNodeAddress() != null) {
            connection.setNodeAddress(request.getPayLoad().getNodeAddress());
        } else {
            connection.setNodeAddress(connection.getName());
        }
        connection.setClassName(request.getPayLoad().getConnectionClass());
        connection.setFrom(from);
        connection.setTo(to);
        connection.setAttributeSchemaName(request.getPayLoad().getAttributeSchemaName());
        connection.setDomain(domainManager.getDomain(request.getRequestDomain()));
        connection.setAttributes(request.getPayLoad().getAttributes());
        connection.setPropagateOperStatus(request.getPayLoad().getPropagateOperStatus());
        connection.setOperationalStatus(request.getPayLoad().getOperationalStatus());
        CreateResourceConnectionResponse response = new CreateResourceConnectionResponse(connection);
        connection.setInsertedDate(new Date());
        resourceConnectionManager.createResourceConnection(connection);
        return response;
    }

    /**
     * Cria uma localidade
     *
     * @param request
     * @return
     * @throws GenericException
     * @throws SchemaNotFoundException
     * @throws AttributeConstraintViolationException
     * @throws ScriptRuleException
     */
    public CreateResourceLocationResponse createResourceLocation(CreateResourceLocationRequest request) throws GenericException, SchemaNotFoundException, AttributeConstraintViolationException, ScriptRuleException, InvalidRequestException, DomainNotFoundException {

        if (request.getPayLoad().getName() == null || request.getPayLoad().getName().trim().equals("")) {
            throw new InvalidRequestException("Please Give a name");
        }

        if (request.getPayLoad().getNodeAddress() == null) {
            //
            // Avaliar se podemos mudar o node address
            //
            request.getPayLoad().setNodeAddress(request.getPayLoad().getName());
        }

        if (request.getPayLoad().getAttributeSchemaName().equals("default")) {
            request.getPayLoad().setAttributeSchemaName("location.default");
        }

        if (request.getPayLoad().getClassName().equalsIgnoreCase("Default")) {
            request.getPayLoad().setClassName("location.Default");
        }

        request.getPayLoad().setDomain(domainManager.getDomain(request.getRequestDomain()));

        if (request.getPayLoad().getDomain() == null) {
            throw new DomainNotFoundException("Domain WIth Name:[" + request.getRequestDomain() + "] not found");
        }
        request.getPayLoad().setInsertedDate(new Date());
        resourceLocationManager.createResourceLocation(request.getPayLoad());
        return new CreateResourceLocationResponse(request.getPayLoad());
    }

}
