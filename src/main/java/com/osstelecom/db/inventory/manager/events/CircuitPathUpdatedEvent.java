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
package com.osstelecom.db.inventory.manager.events;

import com.arangodb.entity.DocumentCreateEntity;
import com.arangodb.entity.DocumentDeleteEntity;
import com.arangodb.entity.DocumentUpdateEntity;
import com.osstelecom.db.inventory.manager.resources.CircuitResource;

/**
 *
 * @author Lucas Nishimura
 * @created 21.07.2023
 */
public class CircuitPathUpdatedEvent extends BasicResourceEvent<CircuitResource> {

    public CircuitPathUpdatedEvent(CircuitResource resource) {
        super(resource);
    }

    public CircuitPathUpdatedEvent(DocumentUpdateEntity<CircuitResource> entity) {
        super(entity);
    }

    public CircuitPathUpdatedEvent(DocumentCreateEntity<CircuitResource> entity) {
        super(entity);
    }

    public CircuitPathUpdatedEvent(DocumentDeleteEntity<CircuitResource> entity) {
        super(entity);
    }

    public CircuitPathUpdatedEvent(CircuitResource oldResource, CircuitResource newResource) {
        super(oldResource, newResource);
    }

}
