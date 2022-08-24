/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
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
