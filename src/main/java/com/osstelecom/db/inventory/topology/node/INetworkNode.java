/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.osstelecom.db.inventory.topology.node;

import com.osstelecom.db.inventory.topology.connection.INetworkConnection;
import com.osstelecom.db.nventory.topology.object.DefaultNetworkObject;
import com.osstelecom.db.inventory.topology.ITopology;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * @author Nishimport org.graphstream.ui.geom.Point3; isan
 */
public interface INetworkNode extends DefaultNetworkObject {

    /**
     *Adiciona uma nova conexão ao node
     * @param connection
     * @return
     */
    public INetworkConnection addConnection(INetworkConnection connection);

    public INetworkConnection removeConnection(INetworkConnection connection);

    public ArrayList<INetworkConnection> getConnections();

    public Boolean endPoint();

    public void setEndPoint(Boolean endPoint);

    public String getName();

    public void setName(String name);

    public void setOnRisk();

    public void clearAlamrs();

    public void enable();

    public void disable();

    public ITopology getTopology();

    public void setTopology(ITopology topology);

    public Integer getIncommingConnectionCount();

    public Integer getOutcommingConnectionCount();

    public Integer getEndpointConnectionsCount();

    public Long getEndpointConnectionsCount(INetworkNode node);

    public void resetEndPointConnectionsCount();

    public void addEndPointConnection(INetworkConnection connection);

    public INetworkNode getOtherSide(INetworkConnection connection);

    public ArrayList<INetworkConnection> getConnectionRelated(INetworkNode node);

    public void lock();

    public void unlock();

    public ArrayList<INetworkConnection> getDivergentConnectionsFrom(INetworkNode node);

    public void addImpactList(INetworkNode node);

    public ConcurrentHashMap<String, INetworkNode> getImpactedNodes();

    public List<INetworkConnection> getVisitedConnections();

    public List<INetworkConnection> getUnVisitedConnections();

    public Long getActiveConnnectionsCount();

    public Long getActiveConnectionsCount(INetworkNode node);

    public Integer getProbedConnectionsCount();

    public void markConnectionAsProbed(INetworkConnection connection);

    public List<INetworkConnection> getUnprobedConnections();
    
    public  List<ArrayList<INetworkNode>> getSolutions();
    
    public void addSolution(ArrayList<INetworkNode> solution);
    
    public List<ArrayList<INetworkNode>> getSolutionsExcept(INetworkNode node);
}
