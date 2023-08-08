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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.osstelecom.db.inventory.manager.exception.GenericException;
import com.osstelecom.db.inventory.manager.exception.InvalidRequestException;
import com.osstelecom.db.inventory.manager.exception.SchemaNotFoundException;
import com.osstelecom.db.inventory.manager.request.CreateResourceSchemaModelRequest;
import com.osstelecom.db.inventory.manager.request.PatchResourceSchemaModelRequest;
import com.osstelecom.db.inventory.manager.response.CreateResourceSchemaModelResponse;
import com.osstelecom.db.inventory.manager.response.EmptyOkResponse;
import com.osstelecom.db.inventory.manager.response.GetSchemasResponse;
import com.osstelecom.db.inventory.manager.response.ListSchemasResponse;
import com.osstelecom.db.inventory.manager.response.PatchResourceSchemaModelResponse;
import com.osstelecom.db.inventory.manager.response.ResourceSchemaResponse;
import com.osstelecom.db.inventory.manager.response.TypedListResponse;
import com.osstelecom.db.inventory.manager.response.TypedMapResponse;
import com.osstelecom.db.inventory.manager.security.model.AuthenticatedCall;
import com.osstelecom.db.inventory.manager.session.SchemaSession;
import javax.servlet.http.HttpServletRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Handles the Dynamic Schema Mapping and Operations
 *
 * @author Lucas Nishimura
 * @created 21.07.2022
 * @Changelog: 02-08-2022: Removed GSON fromJson
 */
@RestController
@RequestMapping("inventory/v1/schema")
public class SchemaApi extends BaseApi {

    @Autowired
    private SchemaSession schemaSession;

    @AuthenticatedCall(role = {"user"})
    @GetMapping(path = "/detail/{filter}", produces = "application/json")
    public ListSchemasResponse listSchemas(@RequestParam(
            value = "page",
            required = false,
            defaultValue = "0") int page,
            @RequestParam(
                    value = "size",
                    required = false,
                    defaultValue = "10") int size,
            @RequestParam(value = "sort", required = false, defaultValue = "id") String sort,
            @RequestParam(value = "direction", required = false, defaultValue = "asc") String sortDirection,
            @PathVariable("filter") String filter) throws SchemaNotFoundException, GenericException {
        return schemaSession.listSchemas(page, size, sort, sortDirection, filter);
    }

    /**
     * Retrieves the schema representation
     *
     * @param schema
     * @return
     */
    @AuthenticatedCall(role = {"user"})
    @GetMapping(produces = "application/json")
    public GetSchemasResponse getSchemasDefinition() throws SchemaNotFoundException, GenericException {
        return schemaSession.loadSchemas();
    }

    /**
     * Retrieves the schema representation
     *
     * @param schema
     * @return
     */
    @AuthenticatedCall(role = {"user"})
    @GetMapping(path = "/{schema}", produces = "application/json")
    public ResourceSchemaResponse getSchemaDefinition(@PathVariable("schema") String schema, HttpServletRequest httpRequest)
            throws GenericException, SchemaNotFoundException {
        httpRequest.setAttribute("request", schema);
        return schemaSession.loadSchemaByName(schema);
    }

    /**
     * Retrieves the schema representation
     *
     * @param schema
     * @return
     */
    @AuthenticatedCall(role = {"user"})
    @GetMapping(path = "/filter/{filter}", produces = "application/json")
    public GetSchemasResponse getSchemaByFilter(@PathVariable("filter") String filter, HttpServletRequest httpRequest)
            throws GenericException, SchemaNotFoundException {
        httpRequest.setAttribute("request", filter);
        return schemaSession.getSchemaByFilter(filter);
    }

    @AuthenticatedCall(role = {"user"})
    @GetMapping(path = "/types", produces = "application/json")
    public TypedListResponse getSupportedTypes()
            throws GenericException, SchemaNotFoundException {
        TypedListResponse response = new TypedListResponse(schemaSession.validAttributesType());
        return response;
    }

    /**
     * Update the schema representation
     *
     * @param schemaName
     * @return
     */
    @AuthenticatedCall(role = {"user"})
    @PatchMapping(path = "/{schema}", produces = "application/json")
    public PatchResourceSchemaModelResponse patchSchameDefinition(@PathVariable("schema") String schemaName,
            @RequestBody PatchResourceSchemaModelRequest request, HttpServletRequest httpRequest)
            throws GenericException, SchemaNotFoundException, InvalidRequestException {
        request.getPayLoad().setSchemaName(schemaName);
        this.setUserDetails(request);
        httpRequest.setAttribute("request", request);
        return schemaSession.patchSchemaModel(request.getPayLoad());
    }

    /**
     * Creates a new ResourceSchemaModel
     *
     * @param reqBody
     * @return
     */
    @AuthenticatedCall(role = {"user", "operator"})
    @PostMapping(path = "/", produces = "application/json", consumes = "application/json")
    public CreateResourceSchemaModelResponse createSchema(@RequestBody CreateResourceSchemaModelRequest request, HttpServletRequest httpRequest)
            throws GenericException, SchemaNotFoundException, InvalidRequestException {
        this.setUserDetails(request);
        httpRequest.setAttribute("request", request);
        return this.schemaSession.createResourceSchemaModel(request.getPayLoad());
    }

    /**
     * clears the schema cache
     *
     * @return
     */
    @AuthenticatedCall(role = {"user", "operator"})
    @PostMapping(path = "/cache/clear", produces = "application/json")
    public EmptyOkResponse clearCachedSchema() {
        this.schemaSession.clearSchemaCache();
        return new EmptyOkResponse();
    }

    /**
     * list cached entries
     *
     * @return
     */
    @AuthenticatedCall(role = {"user", "operator"})
    @GetMapping(path = "/cache", produces = "application/json")
    public TypedMapResponse getCachedSchemas() {
        return new TypedMapResponse(this.schemaSession.getCachedSchemas());
    }

}
