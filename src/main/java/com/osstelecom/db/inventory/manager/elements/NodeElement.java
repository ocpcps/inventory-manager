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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 ** 01/09/2023 - Não está pronta ainda essa classe
 * @author Lucas Nishimura <lucas.nishimura@gmail.com>
 * @created 29.08.2023
 */
public class NodeElement {

    private String id;
    private Long atomicId;

    public Long getAtomicId() {
        return atomicId;
    }
    public List<ConnectionElement> connections = new ArrayList<>();

    public NodeElement() {
    }

    public NodeElement(String id, Long atomicId) {
        this.atomicId = atomicId;
        this.id = id;
    }

    public NodeElement(Long atomicId) {
        this.atomicId = atomicId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public List<ConnectionElement> getConnections() {
        return connections;
    }

    public void setConnections(List<ConnectionElement> connections) {
        this.connections = connections;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 13 * hash + Objects.hashCode(this.id);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final NodeElement other = (NodeElement) obj;
        return Objects.equals(this.id, other.id);
    }

    @Override
    public String toString() {
        return "NodeElement{" + "id=" + id + ", atomicId=" + atomicId + '}';
    }

}
