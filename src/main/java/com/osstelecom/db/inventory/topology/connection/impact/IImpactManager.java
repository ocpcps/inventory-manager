/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.osstelecom.db.inventory.topology.connection.impact;

import com.osstelecom.db.inventory.topology.connection.INetworkConnection;
import com.osstelecom.db.inventory.topology.node.INetworkNode;
import com.osstelecom.db.inventory.topology.ITopology;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Nishisan
 */
public interface IImpactManager {

    public ArrayList<INetworkNode> getUnreacheableNodes();

    public List<INetworkNode> getWeakNodes(Integer connLimit, Boolean all, Integer threadCount, Boolean useCache);

    public List<INetworkNode> getWeakNodes(Integer connLimit,ArrayList<INetworkNode> nodes);

    public ITopology getTopology();

    public ArrayList<INetworkConnection> getUnreachableConnections();

}
