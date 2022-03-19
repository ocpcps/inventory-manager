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
package com.osstelecom.db.inventory.manager.rest.api;

import com.arangodb.ArangoDBException;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.osstelecom.db.inventory.manager.exception.DomainAlreadyExistsException;
import com.osstelecom.db.inventory.manager.exception.DomainNotFoundException;
import com.osstelecom.db.inventory.manager.exception.GenericException;
import com.osstelecom.db.inventory.manager.exception.InvalidRequestException;
import com.osstelecom.db.inventory.manager.exception.ResourceNotFoundException;
import com.osstelecom.db.inventory.manager.exception.SchemaNotFoundException;
import com.osstelecom.db.inventory.manager.exception.ScriptRuleException;
import com.osstelecom.db.inventory.manager.request.CreateCircuitPathRequest;
import com.osstelecom.db.inventory.manager.request.CreateCircuitRequest;
import com.osstelecom.db.inventory.manager.request.CreateConnectionRequest;
import com.osstelecom.db.inventory.manager.request.CreateDomainRequest;
import com.osstelecom.db.inventory.manager.request.CreateManagedResourceRequest;
import com.osstelecom.db.inventory.manager.request.CreateResourceLocationRequest;
import com.osstelecom.db.inventory.manager.request.CreateServiceRequest;
import com.osstelecom.db.inventory.manager.request.FilterRequest;
import com.osstelecom.db.inventory.manager.request.GetCircuitPathRequest;
import com.osstelecom.db.inventory.manager.resources.exception.AttributeConstraintViolationException;
import com.osstelecom.db.inventory.manager.resources.exception.ConnectionAlreadyExistsException;
import com.osstelecom.db.inventory.manager.resources.exception.MetricConstraintException;
import com.osstelecom.db.inventory.manager.resources.exception.NoResourcesAvailableException;
import com.osstelecom.db.inventory.manager.resources.model.ResourceSchemaModel;
import com.osstelecom.db.inventory.manager.response.CreateCircuitPathResponse;
import com.osstelecom.db.inventory.manager.response.CreateCircuitResponse;
import com.osstelecom.db.inventory.manager.response.CreateDomainResponse;
import com.osstelecom.db.inventory.manager.response.CreateManagedResourceResponse;
import com.osstelecom.db.inventory.manager.response.CreateResourceConnectionResponse;
import com.osstelecom.db.inventory.manager.response.CreateResourceLocationResponse;
import com.osstelecom.db.inventory.manager.response.CreateServiceResponse;
import com.osstelecom.db.inventory.manager.response.FilterResponse;
import com.osstelecom.db.inventory.manager.response.GetCircuitPathResponse;
import com.osstelecom.db.inventory.manager.session.DomainSession;
import com.osstelecom.db.inventory.manager.session.ResourceSession;
import com.osstelecom.db.inventory.manager.session.SchemaSession;
import java.util.logging.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Classe que representa os elementos do Inventário
 *
 * @author Lucas Nishimura <lucas.nishimura@gmail.com>
 * @created 14.12.2021
 */
@RestController
@RequestMapping("inventory/v1")
public class InventoryApi {

    private Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private Logger logger = LoggerFactory.getLogger(InventoryApi.class);

    @Autowired
    private SchemaSession schemaSession;

    @Autowired
    private DomainSession domainSession;

    @Autowired
    private ResourceSession resourceSession;

    @PostMapping(path = "/schema", produces = "application/json", consumes = "application/json")
    public String createSchema(@RequestBody String reqBody) {
        ResourceSchemaModel model = gson.fromJson(reqBody, ResourceSchemaModel.class);
        return gson.toJson(model);
    }

    /**
     * Recupera a representação do JSON do Schema
     *
     * @param schema
     * @return
     */
    @GetMapping(path = "/schema/{schema}", produces = "application/json")
    public String getSchameDefinition(@PathVariable("schema") String schema) throws GenericException, SchemaNotFoundException {
        try {
            return gson.toJson(schemaSession.loadSchema(schema));
        } catch (SchemaNotFoundException ex) {
            java.util.logging.Logger.getLogger(InventoryApi.class.getName()).log(Level.SEVERE, null, ex);
            logger.error("Failed To Load Schema", ex);
            throw ex;
        } catch (GenericException ex) {
            java.util.logging.Logger.getLogger(InventoryApi.class.getName()).log(Level.SEVERE, null, ex);
            logger.error("Generic EX  Load Schema", ex);
            throw ex;
        }
    }

    /**
     * Cria um novo Domain xD
     *
     * @param request
     * @return
     * @throws DomainAlreadyExistsException
     * @throws InvalidRequestException
     * @throws GenericException
     */
    @PutMapping(path = "/domain", produces = "application/json", consumes = "application/json")
    public CreateDomainResponse createDomain(@RequestBody CreateDomainRequest request) throws DomainAlreadyExistsException, InvalidRequestException, GenericException {
        logger.info("Request For Creating a new Domain named: [" + request + "] Received");
        if (request != null) {
            if (request.getPayLoad() != null) {
                CreateDomainResponse response = domainSession.createDomain(request);
                return response;
            } else {
                throw new InvalidRequestException("Request Body is null");
            }
        } else {
            throw new InvalidRequestException("Request Body is null");
        }
    }

    /**
     * Cria um novo Location
     *
     * @param request
     * @param domain
     * @return
     * @throws GenericException
     */
    @PutMapping(path = "/{domain}/location", produces = "application/json", consumes = "application/json")
    public CreateResourceLocationResponse createLocation(@RequestBody CreateResourceLocationRequest request, @PathVariable("domain") String domain) throws GenericException, SchemaNotFoundException, AttributeConstraintViolationException, ScriptRuleException, InvalidRequestException, DomainNotFoundException {
        try {
            //
            // Prevalesce o domain da URL.... será que deixo assim ?
            //
            request.setRequestDomain(domain);
            return resourceSession.createResourceLocation(request);
        } catch (ArangoDBException ex) {
            GenericException exa = new GenericException(ex.getMessage());
            exa.setStatusCode(ex.getResponseCode());
            throw exa;
        }
    }

    @PutMapping(path = "/{domain}/resource", produces = "application/json", consumes = "application/json")
    public CreateManagedResourceResponse createManagedResource(@RequestBody String requestBody, @PathVariable("domain") String domain) throws GenericException, SchemaNotFoundException, AttributeConstraintViolationException, ScriptRuleException, InvalidRequestException, DomainNotFoundException {
        try {
            CreateManagedResourceRequest request = gson.fromJson(requestBody, CreateManagedResourceRequest.class);
            request.setRequestDomain(domain);
            return resourceSession.createManagedResource(request);
        } catch (ArangoDBException ex) {
            GenericException exa = new GenericException(ex.getMessage());
            exa.setStatusCode(ex.getResponseCode());
            throw exa;
        }
    }

    @PutMapping(path = "/{domain}/service", produces = "application/json", consumes = "application/json")
    public CreateServiceResponse createService(@RequestBody CreateServiceRequest request, @PathVariable("domain") String domain) throws GenericException {
        try {
            request.setRequestDomain(domain);
            return resourceSession.createService(request);
        } catch (ArangoDBException ex) {
            GenericException exa = new GenericException(ex.getMessage());
            exa.setStatusCode(ex.getResponseCode());
            throw exa;
        }
    }

    /**
     * Cria uma nova conexão
     *
     * @param request
     * @param domain
     * @return
     * @throws GenericException
     */
    @PutMapping(path = "/{domain}/resource/connection", produces = "application/json", consumes = "application/json")
    public CreateResourceConnectionResponse createResourceConnection(@RequestBody CreateConnectionRequest request, @PathVariable("domain") String domain) throws GenericException, SchemaNotFoundException, AttributeConstraintViolationException, ScriptRuleException, InvalidRequestException, ResourceNotFoundException, ConnectionAlreadyExistsException, MetricConstraintException, NoResourcesAvailableException, DomainNotFoundException {
        try {
            request.setRequestDomain(domain);
            return resourceSession.createResourceConnection(request);
        } catch (ArangoDBException ex) {
            ex.printStackTrace();
            GenericException exa = new GenericException(ex.getMessage());
            exa.setStatusCode(ex.getResponseCode());
            throw exa;
        }
    }

    /**
     * Cria uma nova conexão
     *
     * @param request
     * @param domain
     * @return
     * @throws GenericException
     */
    @PutMapping(path = "/{domain}/location/connection", produces = "application/json", consumes = "application/json")
    public CreateResourceConnectionResponse createResourceLocationConnection(@RequestBody CreateConnectionRequest request, @PathVariable("domain") String domain) throws GenericException, SchemaNotFoundException, AttributeConstraintViolationException, ScriptRuleException, InvalidRequestException, ResourceNotFoundException, ConnectionAlreadyExistsException, MetricConstraintException, NoResourcesAvailableException, DomainNotFoundException {
        try {
            request.setRequestDomain(domain);
            return resourceSession.createResourceLocationConnection(request);
        } catch (ArangoDBException ex) {
            ex.printStackTrace();
            GenericException exa = new GenericException(ex.getMessage());
            exa.setStatusCode(ex.getResponseCode());
            throw exa;
        }
    }

    /**
     * Cria um cirtuito....
     *
     * @todo melhorar o tratamento de exception
     * @param request
     * @param domain
     * @return
     * @throws ResourceNotFoundException
     * @throws GenericException
     * @throws SchemaNotFoundException
     * @throws AttributeConstraintViolationException
     * @throws ScriptRuleException
     * @throws AttributeConstraintViolationException
     */
    @PutMapping(path = "/{domain}/circuit", produces = "application/json", consumes = "application/json")
    public CreateCircuitResponse createCircuit(@RequestBody CreateCircuitRequest request, @PathVariable("domain") String domain) throws ResourceNotFoundException, GenericException, SchemaNotFoundException, AttributeConstraintViolationException, ScriptRuleException, AttributeConstraintViolationException, DomainNotFoundException {
        request.setRequestDomain(domain);
        return resourceSession.createCircuit(request);
    }

    /**
     * Cria um path, utilizei o GSON pois parece melhor que o nativo do spring..
     *
     * @param strReq
     * @param domain
     * @return
     * @throws ResourceNotFoundException
     * @throws GenericException
     * @throws SchemaNotFoundException
     * @throws AttributeConstraintViolationException
     * @throws ScriptRuleException
     * @throws AttributeConstraintViolationException
     */
    @PutMapping(path = "/{domain}/circuit/path", produces = "application/json", consumes = "application/json")
    public CreateCircuitPathResponse createCircuitPath(@RequestBody String strReq, @PathVariable("domain") String domain) throws ResourceNotFoundException, GenericException, SchemaNotFoundException, AttributeConstraintViolationException, ScriptRuleException, AttributeConstraintViolationException, DomainNotFoundException {
        CreateCircuitPathRequest request = gson.fromJson(strReq, CreateCircuitPathRequest.class);
        request.setRequestDomain(domain);
        return resourceSession.createCircuitPath(request);
    }

    @PostMapping(path = "/{domain}/circuit/path", produces = "application/json", consumes = "application/json")
    public CreateCircuitPathResponse getCircuitPath(@RequestBody String strReq, @PathVariable("domain") String domain) throws ResourceNotFoundException, GenericException, SchemaNotFoundException, AttributeConstraintViolationException, ScriptRuleException, AttributeConstraintViolationException, DomainNotFoundException {
        GetCircuitPathRequest request = gson.fromJson(strReq, GetCircuitPathRequest.class);
        request.setRequestDomain(domain);
        return resourceSession.getCircuitPath(request);
    }

    @PostMapping(path = "/{domain}/filter", produces = "application/json", consumes = "application/json")
    public FilterResponse getElementsByFilter(@RequestBody FilterRequest filter, @PathVariable("domain") String domain) throws ResourceNotFoundException, GenericException, SchemaNotFoundException, AttributeConstraintViolationException, ScriptRuleException, AttributeConstraintViolationException, DomainNotFoundException {
        System.out.println(":::::::::" + gson.toJson(filter));
        filter.setRequestDomain(domain);

        return resourceSession.getElementsByFilter(filter);
    }

    @PostMapping(path = "test/{threads}", produces = "application/json", consumes = "application/json")
    public GetCircuitPathResponse test(@RequestBody String strReq, @PathVariable("threads") Integer threads) {
        GetCircuitPathRequest request = gson.fromJson(strReq, GetCircuitPathRequest.class);
        GetCircuitPathResponse resp = new GetCircuitPathResponse(request.getPayLoad());
        resourceSession.test(request.getPayLoad().getCircuit().getNodeAddress(), threads);
        return resp;
    }
}
