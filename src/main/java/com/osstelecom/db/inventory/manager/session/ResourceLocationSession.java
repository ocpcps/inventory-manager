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
import com.osstelecom.db.inventory.manager.resources.LocationConnection;
import com.osstelecom.db.inventory.manager.resources.ResourceLocation;
import com.osstelecom.db.inventory.manager.resources.exception.AttributeConstraintViolationException;
import com.osstelecom.db.inventory.manager.resources.exception.ConnectionAlreadyExistsException;
import com.osstelecom.db.inventory.manager.resources.exception.MetricConstraintException;
import com.osstelecom.db.inventory.manager.resources.exception.NoResourcesAvailableException;
import com.osstelecom.db.inventory.manager.response.CreateLocationConnectionResponse;
import com.osstelecom.db.inventory.manager.response.CreateResourceConnectionResponse;
import com.osstelecom.db.inventory.manager.response.CreateResourceLocationResponse;
import java.util.Date;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 *
 * @author Lucas Nishimura
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

    @Autowired
    private UtilSession utils;

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
    public CreateLocationConnectionResponse createResourceLocationConnection(CreateConnectionRequest request) throws ResourceNotFoundException, GenericException, SchemaNotFoundException, AttributeConstraintViolationException, ScriptRuleException, DomainNotFoundException, ArangoDaoException, InvalidRequestException {

        ResourceLocation from = resourceLocationManager.findResourceLocation(request.getPayLoad().getFromName(), request.getPayLoad().getFromNodeAddress(), request.getPayLoad().getFromClassName(), request.getRequestDomain());
        ResourceLocation to = resourceLocationManager.findResourceLocation(request.getPayLoad().getToName(), request.getPayLoad().getToNodeAddress(), request.getPayLoad().getToClassName(), request.getRequestDomain());

        LocationConnection connection = new LocationConnection(domainManager.getDomain(request.getRequestDomain()));

        if (request.getPayLoad().getConnectionName() == null && request.getPayLoad().getNodeAddress() == null) {
            throw new InvalidRequestException("Please Provide at Least a Name[name] or Node Address [nodeAddress]");
        }

        if (request.getPayLoad().getConnectionName() != null && !request.getPayLoad().getConnectionName().trim().equals("")) {
            connection.setName(request.getPayLoad().getConnectionName());
        }

        if (request.getPayLoad().getNodeAddress() != null) {
            connection.setNodeAddress(request.getPayLoad().getNodeAddress());
        } else {
            connection.setNodeAddress(connection.getName());
        }
        //
        // Validação dos nomes
        //
        utils.validadeNodeAddressAndName(connection);
        connection.setClassName(request.getPayLoad().getConnectionClass());
        connection.setFrom(from);
        connection.setTo(to);
        connection.setAttributeSchemaName(request.getPayLoad().getAttributeSchemaName());
        connection.setDomain(domainManager.getDomain(request.getRequestDomain()));
        connection.setAttributes(request.getPayLoad().getAttributes());
        connection.setPropagateOperStatus(request.getPayLoad().getPropagateOperStatus());
        connection.setOperationalStatus(request.getPayLoad().getOperationalStatus());
        utils.validateCanonicalName(connection);

        //
        // Dependencias de Location deveriam começar com connection ? Ainda tenho dúvidas.
        //
        if (!connection.getClassName().startsWith("connection") || !connection.getAttributeSchemaName().startsWith("connection")) {
            throw new InvalidRequestException("Class Name and Atribute Schema Name has to start with 'connection', provided values: className:[" + connection.getClassName() + "] attributeSchemaName:[" + connection.getAttributeSchemaName() + "]");
        }
    
        connection.setInsertedDate(new Date());
        CreateLocationConnectionResponse response = new CreateLocationConnectionResponse(resourceConnectionManager.createLocationConnection(connection));
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
    public CreateResourceLocationResponse createResourceLocation(CreateResourceLocationRequest request) throws GenericException, SchemaNotFoundException, AttributeConstraintViolationException, ScriptRuleException, InvalidRequestException, DomainNotFoundException, ArangoDaoException {

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
        } else {
            if (!request.getPayLoad().getClassName().startsWith("location")) {
                throw new InvalidRequestException("Location Resource class needs to start with location.");
            }
        }
        //
        // Valida se o name e nodeAddress contém os caracteres aceitos
        //
        utils.validadeNodeAddressAndName(request.getPayLoad());
        //
        // Valida o className e o AttributeSchema Name
        //
        utils.validateCanonicalName(request.getPayLoad());

        //
        // Para criação Identifica o usuário criador e assume como dono
        //
        request.getPayLoad().setOwner(request.getUserId());
        request.getPayLoad().setAuthor(request.getUserId());

        request.getPayLoad().setDomain(domainManager.getDomain(request.getRequestDomain()));

        if (request.getPayLoad().getDomain() == null) {
            throw new DomainNotFoundException("Domain WIth Name:[" + request.getRequestDomain() + "] not found");
        }
        request.getPayLoad().setInsertedDate(new Date());
        resourceLocationManager.createResourceLocation(request.getPayLoad());
        return new CreateResourceLocationResponse(request.getPayLoad());
    }

}
