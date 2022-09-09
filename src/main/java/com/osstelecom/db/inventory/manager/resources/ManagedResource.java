/*
 * Copyright (C) 2021 Lucas Nishimura <lucas.nishimura@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.osstelecom.db.inventory.manager.resources;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * This class represents the Resource that needs to be managed
 *
 * @author Lucas Nishimura <lucas.nishimura@gmail.com>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "This Represents a Managed Object (node) in the dependecies network")
public class ManagedResource extends BasicResource {

    public ManagedResource(String attributeSchema, Domain domain) {
        super(attributeSchema, domain);
    }

    public ManagedResource(Domain domain, String uid, String id) {
        super(domain, uid, id);
        this.setClassName(null);
        this.setAttributeSchemaName(null);
    }

    public ManagedResource(Domain domain, String id) {
        super(domain, id);
        //
        // Prioritize ID
        //
        this.setClassName(null);
        this.setAttributeSchemaName(null);
    }

    public ManagedResource(Domain domain) {
        super(domain);
    }

    public ManagedResource(Domain domain, String name, String nodeAddress, String className) {
        super(domain, name, nodeAddress, className);
        //
        // Prioritize name,nodeAddress
        //
        this.setClassName(null);
        this.setAttributeSchemaName(null);
    }

    public ManagedResource() {
    }

}
