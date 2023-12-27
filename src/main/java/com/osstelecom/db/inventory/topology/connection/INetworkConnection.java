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
import com.osstelecom.db.inventory.topology.object.DefaultNetworkObject;
import com.osstelecom.db.inventory.topology.ITopology;
import java.util.ArrayList;

/**
 *
 * @author Nishisan
 */
public interface INetworkConnection extends DefaultNetworkObject {

    public void setSource(INetworkNode node);

    public INetworkNode getSource();

    public void setTarget(INetworkNode node);

    public INetworkNode getTarget();

    public void enable();

    public void disable();

    public String getName();

    public void setName(String name);

    public ITopology getTopology();

    public void setTopology(ITopology topology);

    public void leadsToEndPoint(Boolean lead);

    public Boolean leadsToEndpoint();

    public ArrayList<ArrayList<INetworkNode>> addPathList(ArrayList<INetworkNode> list);

    public void printPathList();

    public Boolean isRelatedToNode(INetworkNode node);

    public void disconnect(INetworkNode node);

}
