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
package com.osstelecom.db.inventory.visualization.dto;

import com.osstelecom.db.inventory.manager.resources.BasicResource;

import java.util.Objects;

/**
 *
 * @author Lucas Nishimura
 * @created 12.01.2023
 */
public class ThreeJsNodeDTO extends BaseGraphDTO {

    private final String id;
    private final String name;
    private final String group;
    private final Boolean isLeaf;
    private final String graphItemColor;
    // private final String graphItemColor;

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 89 * hash + Objects.hashCode(this.id);
        hash = 89 * hash + Objects.hashCode(this.name);
        hash = 89 * hash + Objects.hashCode(this.group);
        hash = 89 * hash + Objects.hashCode(this.domain);
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
        final ThreeJsNodeDTO other = (ThreeJsNodeDTO) obj;
        if (!Objects.equals(this.id, other.id)) {
            return false;
        }
        if (!Objects.equals(this.name, other.name)) {
            return false;
        }
        if (!Objects.equals(this.group, other.group)) {
            return false;
        }
        return Objects.equals(this.domain, other.domain);
    }

    public ThreeJsNodeDTO(BasicResource res) {
        this.id = res.getKey();
        this.name = res.getName();
        this.group = res.getAttributeSchemaName();
        this.domain = res.getDomainName();
        this.operStatus = res.getOperationalStatus();
        this.isLeaf = res.getIsLeaf();
        this.graphItemColor = res.getSchemaModel().getGraphItemColor();
    }

    private final String domain;

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getGroup() {
        return group;
    }

    public String getDomain() {
        return domain;
    }

    public String getGraphItemColor() {
        return graphItemColor;
    }

}
