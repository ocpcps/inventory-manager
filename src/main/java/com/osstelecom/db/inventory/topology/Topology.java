/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.osstelecom.db.inventory.topology;

import com.osstelecom.db.inventory.topology.connection.DefaultNetworkConnection;
import com.osstelecom.db.inventory.topology.connection.INetworkConnection;
import com.osstelecom.db.inventory.topology.connection.impact.DefaultImpactManager;
import com.osstelecom.db.inventory.topology.connection.impact.IImpactManager;
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
    private IImpactManager impactManager;

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
        DefaultNetworkConnection networkConnection = new DefaultNetworkConnection(source, target, name, this);
        connectionNames.put(name, networkConnection);
        getConnections().add(networkConnection);
        return networkConnection;
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
    public IImpactManager getImpactManager() {
        if (this.impactManager == null) {
            this.impactManager = new DefaultImpactManager(this);
        }
        return this.impactManager;
    }

    @Override
    public void setImpactManager(IImpactManager impactManager) {
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
    public Integer getScaleFactor() {
        return scaleFactor;
    }

    /**
     * @param scaleFactor the scaleFactor to set
     */
    public void setScaleFactor(Integer scaleFactor) {
        this.scaleFactor = scaleFactor;
    }

    public String getUuid() {
        return uuid;
    }
}
