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

import com.osstelecom.db.inventory.manager.resources.ResourceConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 *
 * @author Lucas Nishimura
 * @created 12.01.2023
 */
public class ThreeJSLinkDTO extends BaseGraphDTO {

    private final String source;
    private final String target;
    private final String id;
    private List<String> circuits;

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 71 * hash + Objects.hashCode(this.source);
        hash = 71 * hash + Objects.hashCode(this.target);
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
        final ThreeJSLinkDTO other = (ThreeJSLinkDTO) obj;
        if (!Objects.equals(this.source, other.source)) {
            return false;
        }
        return Objects.equals(this.target, other.target);
    }

    public ThreeJSLinkDTO(ResourceConnection con) {
        this.source = con.getFromKeyWithoutCollection();
        this.target = con.getToKeyWithoutCollection();
        this.id = con.getKey();
        if (con.getCircuits() != null) {
            if (!con.getCircuits().isEmpty()) {
                if (this.circuits == null) {
                    this.circuits = new ArrayList<>();
                }
                con.getCircuits().forEach(id -> {
                    //
                    // Pega somente a KEY
                    //
                    if (id.contains("/")) {
                        this.circuits.add(id.split("/")[1]);
                    }
                });

            }
        }
    }

    public String getSource() {
        return source;
    }

    public String getTarget() {
        return target;
    }

    /**
     * @return the id
     */
    public String getId() {
        return id;
    }

    /**
     * @return the circuits
     */
    public List<String> getCircuits() {
        return circuits;
    }

}
