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
package com.osstelecom.db.inventory.topology.connection;

import com.osstelecom.db.inventory.topology.node.INetworkNode;
import com.osstelecom.db.inventory.topology.ITopology;

/**
 *
 * @author Nishisan
 */
public class DefaultNetworkConnection extends NetworkConnection {

    public DefaultNetworkConnection() {
    }

    public DefaultNetworkConnection(Integer id) {
        this.setId(id);
    }

    public DefaultNetworkConnection(INetworkNode source, INetworkNode target, ITopology topology) {
        super();
        this.setTopology(topology);
        this.setSource(source);
        this.setTarget(target);

        source.addConnection(this);

        target.addConnection(this);

        String autoName = source.getName() + "." + target.getName();
        if (this.getTopology().getConnectionByName(autoName) == null) {
            this.setName(autoName);
        }

    }

    public DefaultNetworkConnection(INetworkNode source, INetworkNode target, String name, ITopology topology) {
        super();
        this.setSource(source);

        this.setTarget(target);
        source.addConnection(this);
        target.addConnection(this);
        this.setName(name);
        this.setTopology(topology);
    }

    public DefaultNetworkConnection(INetworkNode source, INetworkNode target, String name, Object payLoad, ITopology topology) {
        super();
        this.setSource(source);
        source.addConnection(this);
        this.setTarget(target);
        target.addConnection(this);
        this.setPayLoad(payLoad);
        this.setName(name);
        this.setTopology(topology);
    }

    public DefaultNetworkConnection(INetworkNode source, INetworkNode target, Object payLoad, Integer id, String name, ITopology topology) {
        super();
        this.setSource(source);
        source.addConnection(this);
        this.setTarget(target);
        target.addConnection(this);
        this.setPayLoad(payLoad);
        this.setId(id);
        this.setName(name);
        this.setTopology(topology);
    }

}
