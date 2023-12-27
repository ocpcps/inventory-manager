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
package com.osstelecom.db.inventory.topology;

import com.osstelecom.db.inventory.topology.connection.DefaultNetworkConnection;
import com.osstelecom.db.inventory.topology.connection.INetworkConnection;
import com.osstelecom.db.inventory.topology.impact.DefaultImpactManagerImpl;
import com.osstelecom.db.inventory.topology.listeners.DefaultTopologyListener;
import com.osstelecom.db.inventory.topology.listeners.TopologyListener;
import com.osstelecom.db.inventory.topology.node.INetworkNode;
import java.awt.Point;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import com.osstelecom.db.inventory.topology.impact.ImpactManagerIf;

/**
 *
 * @author Nishisan
 */
public abstract class Topology implements ITopology {
    
    private TopologyListener topologyListener;
    private ArrayList<INetworkNode> nodes = new ArrayList<>();
    private ArrayList<INetworkNode> endPoints = new ArrayList<>();
    private ArrayList<INetworkConnection> connections = new ArrayList<>();
    private ConcurrentHashMap<String, INetworkConnection> connectionNames = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, INetworkNode> nodeNames = new ConcurrentHashMap<>();
    private Integer width = 1600;
    private Integer heigth = 900;
    private ImpactManagerIf impactManager;
    
    private ArrayList<INetworkNode> topOut = new ArrayList<>();
    private Point2D minPoint;
    private Point2D maxPoint;
    private Point middle;
    private Integer scaleFactor = 1;
    private String uuid;
    
    @Override
    public void destroyTopology() {
        //
        // Make Sure all Resources are Freed
        //

        //
        // Destroy all Connections..
        //
//        nodes.forEach(n -> {
//            n.getConnections().forEach(c -> {
//                c.disconnect(n);
//            });
//
//        });
        nodes.clear();
        
        connections.clear();
        
        nodeNames.clear();
        
        connectionNames.clear();
        
    }
    
    @Override
    public void resetDynamicValues() {
//        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        this.nodeNames.entrySet().parallelStream().forEach(e -> {
            e.getValue().resetEndPointConnectionsCount();
        });
    }
    
    @Override
    public ArrayList<INetworkNode> getTopOut(int count) {
        if (topOut.isEmpty()) {
            nodeNames.entrySet().parallelStream().sorted(new Comparator<Map.Entry<String, INetworkNode>>() {
                @Override
                public int compare(Map.Entry<String, INetworkNode> o1, Map.Entry<String, INetworkNode> o2) {
                    return o1.getValue().getIncommingConnectionCount() - o2.getValue().getIncommingConnectionCount();
                }
            }).forEachOrdered((t) -> {
                if (topOut.size() < count) {
                    topOut.add(t.getValue());
                }
                
            });

//            for (INetworkNode t : topOut) {
//                System.out.println("Name:" + t.getName() + " T: " + t.getIncommingConnectionCount());
//            }
        }
        return topOut;
    }
    
    public Topology() {
        
        this.uuid = UUID.randomUUID().toString();
        
    }
    
    public Topology(Integer scaleFactor) {
        
        this.topologyListener = new DefaultTopologyListener();
        this.setScaleFactor(scaleFactor);
        
    }
    
    public Topology(TopologyListener listener) {
        this.topologyListener = listener;
        
    }
    
    public Topology(TopologyListener listener, Boolean useGraph) {
        this.topologyListener = listener;
    }
    
    public Topology(ImpactManagerIf impactManager) {
        this.setImpactManager(impactManager);
        this.setScaleFactor(100);
    }
    
    @Override
    public INetworkNode addNode(INetworkNode node) {
        this.topologyListener.onNodeAdded(node);
        if (node.endPoint()) {
            this.endPoints.add(node);
            
        } else {
            nodes.add(node);
            nodeNames.put(node.getName(), node);
        }
        
        return node;
    }
    
    @Override
    public INetworkNode removeNode(INetworkNode node) {
        this.topologyListener.onNodeRemoved(node);
        
        if (node.endPoint()) {
            this.endPoints.remove(node);
        } else {
            nodes.remove(node);
            nodeNames.remove(node.getName());
        }
//        graph.removeNode(node.getUuid());
        return node;
    }
    
    @Override
    public INetworkConnection addConnection(INetworkNode source, INetworkNode target, String name) {

        //
        // Prevents duplicated Connections
        //
        if (!connectionNames.containsKey(name)) {
            DefaultNetworkConnection networkConnection = new DefaultNetworkConnection(source, target, name, this);
            connectionNames.put(name, networkConnection);
            getConnections().add(networkConnection);
            return networkConnection;
        } else {
            return connectionNames.get(name);
        }
        
    }
    
    @Override
    public INetworkConnection addConnection(INetworkNode source, INetworkNode target) {
        DefaultNetworkConnection networkConnection = new DefaultNetworkConnection(source, target, this);
        connectionNames.put(networkConnection.getName(), networkConnection);
        getConnections().add(networkConnection);
        return networkConnection;
    }
    
    @Override
    public INetworkConnection removeConnection(INetworkNode source, INetworkNode target, String name) {
        for (INetworkConnection c : source.getConnections()) {
            if (c.getTarget().equals(target) && c.getName().equals(name)) {
                getConnections().remove(c);
                connectionNames.remove(c.getName());
                return c;
            }
        }
        
        return null;
    }

    /**
     * @return the connections
     */
    @Override
    public ArrayList<INetworkConnection> getConnections() {
        return connections;
    }

    /**
     * @param connections the connections to set
     */
    @Override
    public void setConnections(ArrayList<INetworkConnection> connections) {
        this.connections = connections;
    }

    /**
     * @return the width
     */
    public Integer getWidth() {
        return width;
    }

    /**
     * @param width the width to set
     */
    public void setWidth(Integer width) {
        this.width = width;
    }

    /**
     * @return the heigth
     */
    public Integer getHeigth() {
        return heigth;
    }

    /**
     * @param heigth the heigth to set
     */
    public void setHeigth(Integer heigth) {
        this.heigth = heigth;
    }
    
    @Override
    public ArrayList<INetworkNode> getEndPoints() {
        return this.endPoints;
    }
    
    @Override
    public ImpactManagerIf getImpactManager() {
        if (this.impactManager == null) {
            this.impactManager = new DefaultImpactManagerImpl(this);
        }
        return this.impactManager;
    }
    
    @Override
    public void setImpactManager(ImpactManagerIf impactManager) {
        if (impactManager.getTopology()==null){
            impactManager.setTopology(this);
        }
        this.impactManager = impactManager;
    }
    
    @Override
    public ArrayList<INetworkNode> getNodes() {
        return this.nodes;
    }
    
    @Override
    public INetworkNode getNodeByName(String name) {
        
        if (nodeNames.containsKey(name)) {
            return nodeNames.get(name);
        }
        
        return null;
    }
    
    public void printConnections() {
        this.getConnections().stream().forEach((c) -> {
            System.out.println(":" + c.getName() + " From: " + c.getSource().getName() + " Target: " + c.getTarget().getName());
        });
        
    }
    
    @Override
    public INetworkConnection getConnectionByName(String name) {
        if (connectionNames.containsKey(name)) {
            return connectionNames.get(name);
        }
        return null;
    }
    
    @Override
    public void autoArrange() {
        
    }

    /**
     * @return the scaleFactor
     */
    @Override
    public Integer getScaleFactor() {
        return scaleFactor;
    }

    /**
     * @param scaleFactor the scaleFactor to set
     */
    @Override
    public void setScaleFactor(Integer scaleFactor) {
        this.scaleFactor = scaleFactor;
    }
    
    @Override
    public String getUuid() {
        return uuid;
    }
}
