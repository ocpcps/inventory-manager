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
package com.osstelecom.db.inventory.manager.events;

import com.osstelecom.db.inventory.manager.resources.BasicResource;
import com.osstelecom.db.inventory.manager.resources.ResourceConnection;
import java.util.Date;

/**
 *
 * @author Lucas Nishimura <lucas.nishimura@gmail.com>
 * @created 10.04.2022
 */
public class ManagedResourceConnectionCreatedEvent extends BasicEvent {

    private BasicResource from;
    private BasicResource to;
    private ResourceConnection connection;

    public ManagedResourceConnectionCreatedEvent(BasicResource from, BasicResource to, ResourceConnection connection) {
        this.from = from;
        this.to = to;
        this.connection = connection;
    }

    public BasicResource getFrom() {
        return from;
    }

    public BasicResource getTo() {
        return to;
    }

    public ResourceConnection getConnection() {
        return connection;
    }

    public Date getEventDate() {
        return eventDate;
    }

}
