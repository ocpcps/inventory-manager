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
package com.osstelecom.db.inventory.manager.resources.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.HashMap;

/**
 *
 * @author Lucas Nishimura <lucas.nishimura@gmail.com>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ResourceSchemaModel {

    private String schemaName;
    private String fromSchema;
    private String owner;
    private String author;
    private Boolean allowAll = false;
    private HashMap<String, ResourceAttributeModel> attributes = new HashMap<>();

    /**
     * @return the schemaName
     */
    public String getSchemaName() {
        return schemaName;
    }

    /**
     * @param schemaName the schemaName to set
     */
    public void setSchemaName(String schemaName) {
        this.schemaName = schemaName;
    }

    /**
     * @return the fromSchema
     */
    public String getFromSchema() {
        return fromSchema;
    }

    /**
     * @param fromSchema the fromSchema to set
     */
    public void setFromSchema(String fromSchema) {
        this.fromSchema = fromSchema;
    }

    /**
     * @return the owner
     */
    public String getOwner() {
        return owner;
    }

    /**
     * @param owner the owner to set
     */
    public void setOwner(String owner) {
        this.owner = owner;
    }

    /**
     * @return the author
     */
    public String getAuthor() {
        return author;
    }

    /**
     * @param author the author to set
     */
    public void setAuthor(String author) {
        this.author = author;
    }

    /**
     * @return the attributes
     */
    public HashMap<String, ResourceAttributeModel> getAttributes() {
        return attributes;
    }

    /**
     * @param attributes the attributes to set
     */
    public void setAttributes(HashMap<String, ResourceAttributeModel> attributes) {
        this.attributes = attributes;
    }

    /**
     * @return the allowAll
     */
    public Boolean getAllowAll() {
        return allowAll;
    }

    /**
     * @param allowAll the allowAll to set
     */
    public void setAllowAll(Boolean allowAll) {
        this.allowAll = allowAll;
    }
}
