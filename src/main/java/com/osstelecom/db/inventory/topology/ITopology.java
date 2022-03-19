/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.osstelecom.db.inventory.topology;

import com.osstelecom.db.inventory.topology.connection.INetworkConnection;
import com.osstelecom.db.inventory.topology.connection.impact.IImpactManager;
import com.osstelecom.db.inventory.topology.node.INetworkNode;
import java.util.ArrayList;

/**
 *
 * @author Nishisan
 */
public interface ITopology {

    public INetworkNode addNode(INetworkNode node);

    public INetworkNode removeNode(INetworkNode node);

    public INetworkConnection addConnection(INetworkNode source, INetworkNode target, String name);

    public INetworkConnection addConnection(INetworkNode source, INetworkNode target);

    public INetworkConnection removeConnection(INetworkNode source, INetworkNode target, String name);

    public ArrayList<INetworkConnection> getConnections();

    public void setConnections(ArrayList<INetworkConnection> connections);

    public ArrayList<INetworkNode> getEndPoints();

    public ArrayList<INetworkNode> getNodes();

    public IImpactManager getImpactManager();

    public void setImpactManager(IImpactManager impactManager);

    public INetworkNode getNodeByName(String name);

    public INetworkConnection getConnectionByName(String name);

    public void autoArrange();

    public void setScaleFactor(Integer factor);

    public Integer getScaleFactor();

    public String getUuid();

    public ArrayList<INetworkNode> getTopOut(int count);

    public void resetDynamicValues();
    
    public void destroyTopology();
}
