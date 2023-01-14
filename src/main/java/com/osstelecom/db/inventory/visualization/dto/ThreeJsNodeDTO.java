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

/**
 *
 * @author Lucas Nishimura <lucas.nishimura@gmail.com>
 * @created 12.01.2023
 */
public class ThreeJsNodeDTO {

    private final String id;
    private final String name;
    private final String group;

    public ThreeJsNodeDTO(String id, String name, String group, String domain) {
        this.id = id;
        this.name = name;
        this.group = group;
        this.domain = domain;
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

}
