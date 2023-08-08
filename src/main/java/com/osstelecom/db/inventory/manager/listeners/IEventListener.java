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
package com.osstelecom.db.inventory.manager.listeners;

import com.osstelecom.db.inventory.manager.events.BasicEvent;
import com.osstelecom.db.inventory.manager.events.BasicResourceEvent;
import com.osstelecom.db.inventory.manager.events.BasicUpdateEvent;

/**
 *
 * @author Lucas Nishimura
 * @created 04.08.2023
 */
public interface IEventListener {

    public void registerListener(Object listener);

    public boolean notifyResourceEvent(BasicResourceEvent event);

    public boolean notifyGenericEvent(BasicEvent genericEvent);

    public boolean notifyGenericEvent(BasicUpdateEvent updateEvent);
}
