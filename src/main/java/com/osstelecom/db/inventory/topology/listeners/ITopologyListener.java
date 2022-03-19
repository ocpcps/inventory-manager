/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
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
