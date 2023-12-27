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

import java.util.Objects;

/**
 ** 01/09/2023 - Não está pronta ainda essa classe
 * @author Lucas Nishimura <lucas.nishimura@gmail.com>
 * @created 29.08.2023
 */
public class ConnectionElement {

    private String id;
    private Long atomicId;
    private NodeElement from;
    private NodeElement to;

    public Long getAtomicId() {
        return atomicId;
    }

    public ConnectionElement(String id, Long atomicId) {
        this.atomicId = atomicId;
        this.id = id;
    }

    public ConnectionElement(Long atomicId) {
        this.atomicId = atomicId;
    }

    public ConnectionElement() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public NodeElement getFrom() {
        return from;
    }

    public void setFrom(NodeElement from) {
        if (!from.getConnections().contains(this)) {
//            from.getConnections().add(this);
        }
        this.from = from;
    }

    public NodeElement getTo() {
        return to;
    }

    public void setTo(NodeElement to) {
        if (!to.getConnections().contains(this)) {
//            to.getConnections().add(this);
        }
        this.to = to;
    }

    public NodeElement getOtherNode(NodeElement node) {
        if (this.from.equals(node)) {
            return this.to;
        } else {
            return this.from;
        }
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 43 * hash + Objects.hashCode(this.id);
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
        final ConnectionElement other = (ConnectionElement) obj;
        return Objects.equals(this.id, other.id);
    }

    @Override
    public String toString() {
        return "ConnectionElement{" + "id=" + id + ", atomicId=" + atomicId + '}';
    }

}
