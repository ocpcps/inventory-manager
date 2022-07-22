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

import com.osstelecom.db.inventory.manager.exception.GenericException;
import com.osstelecom.db.inventory.manager.exception.InvalidRequestException;
import com.osstelecom.db.inventory.manager.exception.SchemaNotFoundException;
import com.osstelecom.db.inventory.manager.request.CreateResourceSchemaModelRequest;
import com.osstelecom.db.inventory.manager.resources.model.ResourceSchemaModel;
import com.osstelecom.db.inventory.manager.response.CreateResourceSchemaModelResponse;
import com.osstelecom.db.inventory.manager.response.EmptyOkResponse;
import com.osstelecom.db.inventory.manager.response.ResourceSchemaResponse;
import com.osstelecom.db.inventory.manager.response.TypedMapResponse;
import com.osstelecom.db.inventory.manager.security.model.AuthenticatedCall;
import com.osstelecom.db.inventory.manager.session.SchemaSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @author Lucas Nishimura <lucas.nishimura@gmail.com>
 * @created 21.07.2022
 */
@RestController
@RequestMapping("inventory/v1/schema")
public class SchemaApi extends BaseApi {

    @Autowired
    private SchemaSession schemaSession;

    /**
     * Recupera a representação do JSON do Schema
     *
     * @param schema
     * @return
     */
    @AuthenticatedCall(role = {"user"})
    @GetMapping(path = "/{schema}", produces = "application/json")
    public String getSchameDefinition(@PathVariable("schema") String schema) throws GenericException, SchemaNotFoundException {
        try {
            return gson.toJson(new ResourceSchemaResponse(schemaSession.loadSchema(schema)));
        } catch (SchemaNotFoundException ex) {
            logger.error("Failed To Load Schema", ex);
            throw ex;
        } catch (GenericException ex) {
            logger.error("Generic EX  Load Schema", ex);
            throw ex;
        }
    }

    /**
     * Teste
     *
     * @param reqBody
     * @return
     */
    @AuthenticatedCall(role = {"user", "operator"})
    @PostMapping(path = "/", produces = "application/json", consumes = "application/json")
    public String createSchema(@RequestBody String reqBody) throws GenericException, SchemaNotFoundException, InvalidRequestException {
        CreateResourceSchemaModelRequest model = gson.fromJson(reqBody, CreateResourceSchemaModelRequest.class);
        ResourceSchemaModel createdModel = this.schemaSession.createResourceSchemaModel(model.getPayLoad());
        return gson.toJson(new CreateResourceSchemaModelResponse(createdModel));
    }

    @AuthenticatedCall(role = {"user", "operator"})
    @PostMapping(path = "/cache/clear", produces = "application/json")
    public String clearCachedSchema() {
        this.schemaSession.clearSchemaCache();
        return gson.toJson(new EmptyOkResponse());
    }

    @AuthenticatedCall(role = {"user", "operator"})
    @GetMapping(path = "/cache", produces = "application/json")
    public String getCachedSchemas() {
        return gson.toJson(new TypedMapResponse(this.schemaSession.getCachedSchemas()));
    }

}
