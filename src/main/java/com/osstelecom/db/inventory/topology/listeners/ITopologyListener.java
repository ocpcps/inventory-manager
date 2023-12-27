/*
 * Copyright (C) 2021 Lucas Nishimura <lucas.nishimura@gmail.com>
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
package com.osstelecom.db.inventory.topology.listeners;

import com.osstelecom.db.inventory.topology.connection.INetworkConnection;
import com.osstelecom.db.inventory.topology.node.INetworkNode;

/**
 *
 * @author Nishisan
 */
public interface ITopologyListener {

    public void onNodeAdded(INetworkNode node);

    public void onNodeRemoved(INetworkNode node);

    public void onConnectionAdded(INetworkConnection connection);

    public void onConnectionRemoved(INetworkConnection connection);

    public void onConnectionReconnection(INetworkConnection connection);
}
