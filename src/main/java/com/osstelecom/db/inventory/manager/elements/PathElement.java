/*
 * Copyright (C) 2023 Lucas Nishimura <lucas.nishimura@gmail.com>
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
package com.osstelecom.db.inventory.manager.elements;

import com.osstelecom.db.inventory.manager.resources.BasicResource;
import com.osstelecom.db.inventory.manager.resources.ResourceConnection;
import java.util.Objects;

/**
 ** 01/09/2023 - Não está pronta ainda essa classe
 *
 * @author Lucas Nishimura <lucas.nishimura@gmail.com>
 * @created 29.08.2023
 */
public class PathElement {

    private final BasicResource node;
    private final ResourceConnection connection;

    public PathElement(BasicResource node, ResourceConnection connection) {
        this.node = node;
        this.connection = connection;
    }

    public BasicResource getNode() {
        return node;
    }

    public ResourceConnection getConnection() {
        return connection;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PathElement that = (PathElement) o;
        return Objects.equals(node, that.node)
                && Objects.equals(connection, that.connection);
    }

    @Override
    public int hashCode() {
        return Objects.hash(node, connection);
    }

}
