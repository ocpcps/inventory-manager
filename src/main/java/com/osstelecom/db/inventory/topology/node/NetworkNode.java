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
package com.osstelecom.db.inventory.topology.node;

import com.osstelecom.db.inventory.topology.connection.INetworkConnection;
import com.osstelecom.db.inventory.topology.ITopology;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;


/*geom @auPoint3hor Niimport org.graphstream.ui.geom.Point3;
shisan
 */
public abstract class NetworkNode implements INetworkNode {

    private ITopology topology;
    private Integer id = -1;
    private Object payLoad = null;
    private Boolean active = true;
    private Integer connectionCount = 0;
    private Integer incommingConnectionCount = 0;
    private Integer outCommingConnectionCount = 0;
    private Double weight;
    private Boolean endPoint = false;
    private String name;
    private String uuid;
    private Boolean isOnRisk = false;

    private ConcurrentHashMap<String, INetworkConnection> endpointConnections = new ConcurrentHashMap<>();
    private ReentrantLock lock = new ReentrantLock(true);
    private ConcurrentHashMap<String, Boolean> visitedThreads = new ConcurrentHashMap<>();
    private ArrayList<INetworkConnection> connections = new ArrayList<>();
    private ArrayList<INetworkConnection> probedConnections = new ArrayList<>();
    private List<ArrayList<INetworkNode>> solutions = Collections.synchronizedList(new ArrayList<ArrayList<INetworkNode>>());
    private ConcurrentHashMap<String, Object> attributes = new ConcurrentHashMap<>();

    @Override
    public void addAttribute(String key, Object value) {
        this.attributes.put(key, value);
    }

    @Override
    public Object getAttribute(String key) {
        return attributes.get(key);
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 47 * hash + Objects.hashCode(this.uuid);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final NetworkNode other = (NetworkNode) obj;
        if (!Objects.equals(this.uuid, other.uuid)) {
            return false;
        }
        return true;
    }

    //
    // Elementos que são Impactados por este
    //
    private ConcurrentHashMap<String, INetworkNode> impactedNodes = new ConcurrentHashMap<>();

    private Integer width;
    private Integer heigth;

    @Override
    public List<ArrayList<INetworkNode>> getSolutions() {
        return this.solutions;
    }

    @Override
    public void addSolution(ArrayList<INetworkNode> solution) {

        this.solutions.add(new ArrayList<>(solution));

    }

    @Override
    public ArrayList<ArrayList<INetworkNode>> getSolutionsExcept(INetworkNode node) {

        ArrayList<ArrayList<INetworkNode>> result = new ArrayList<>();
        this.solutions.forEach(l -> {
            if (!l.contains(node)) {
                result.add(l);
            }
        });

        return result;
    }

    @Override
    public Integer getProbedConnectionsCount() {
        return this.probedConnections.size();
    }

    @Override
    public void markConnectionAsProbed(INetworkConnection connection) {
        if (!this.probedConnections.contains(connection)) {
//            connection.setVisited();
            this.probedConnections.add(connection);
        }
    }

    @Override
    public List<INetworkConnection> getUnprobedConnections() {
        return this.connections.parallelStream().filter(c -> !this.probedConnections.contains(c)).collect(Collectors.toList());
    }

    @Override
    public List<INetworkConnection> getVisitedConnections(String uid) {
        return this.connections.parallelStream().filter(c -> c.isVisited(uid)).collect(Collectors.toList());
    }

    @Override
    public List<INetworkConnection> getUnVisitedConnections(String uid) {
        return this.connections.parallelStream().filter(c -> !c.isVisited(uid) && c.getActive()).collect(Collectors.toList());
    }

    @Override
    public void addImpactList(INetworkNode node) {
        this.impactedNodes.put(node.getUuid(), node);
    }

    @Override
    public ConcurrentHashMap<String, INetworkNode> getImpactedNodes() {
        return this.impactedNodes;
    }

    @Override
    public synchronized void lock() {
        lock.lock();
    }

    @Override
    public synchronized void unlock() {
        lock.unlock();
    }

    /**
     * Retorna uma lista de conexões divergentes do node A
     *
     * @param node
     * @return
     */
    @Override
    public ArrayList<INetworkConnection> getDivergentConnectionsFrom(INetworkNode node) {
        ArrayList<INetworkConnection> result = new ArrayList<>();
        for (INetworkConnection connection : this.getConnections()) {
            if (!connection.isRelatedToNode(node)) {
                result.add(connection);
            }
        }
        return result;
    }

    @Override
    public ArrayList<INetworkConnection> getConnectionRelated(INetworkNode node) {
        ArrayList<INetworkConnection> result = new ArrayList<>();
        for (INetworkConnection connection : this.connections) {
            if (connection.getTarget().equals(node) || connection.getSource().equals(node)) {
                if (!result.contains(connection)) {
                    result.add(connection);
                }
            }
        }
        return result;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public Boolean isVisited(String uid) {
        return visitedThreads.containsKey(uid);
    }

    @Override
    public void setVisited(String uid) {
        visitedThreads.put(uid, true);
    }

    @Override
    public void setUnvisited(String uid) {
        visitedThreads.remove(uid);
    }

    @Override
    public INetworkNode getOtherSide(INetworkConnection connection) {

        if (connections.contains(connection)) {
            if (connection.getTarget().equals(this)) {
                return connection.getSource();
            } else {
                return connection.getTarget();
            }
        } else {
            return null;
        }
    }

    @Override
    public void resetEndPointConnectionsCount() {
        endpointConnections.clear();
    }

    @Override
    public Integer getEndpointConnectionsCount() {
        return endpointConnections.size();
    }

    /**
     * Precisa Melhorar, não funciona!
     *
     * @param node
     * @deprecated 
     * @return
     */
    @Override
    public Long getEndpointConnectionsCount(INetworkNode node) {
        return endpointConnections.entrySet().parallelStream().filter(k -> !k.getValue().isRelatedToNode(node)).count();
    }

    @Override
    public void addEndPointConnection(INetworkConnection connection) {
//        this.lock();
        if (!endpointConnections.containsKey(connection.getUuid())) {
            if (!connection.leadsToEndpoint()) {
                connection.leadsToEndPoint(true);
            }
            endpointConnections.put(connection.getUuid(), connection);
        } else {
            endpointConnections.replace(connection.getUuid(), connection);
        }
//        this.unlock();
    }

    @Override
    public Integer getIncommingConnectionCount() {
        return this.incommingConnectionCount;
    }

    @Override
    public Integer getOutcommingConnectionCount() {
        return this.outCommingConnectionCount;
    }

    public NetworkNode(ITopology topology) {
        this.topology = topology;
        setUuid();

        this.topology.addNode(this);
    }

    public NetworkNode(String name, ITopology topology) {
        this.setName(name);
        setUuid();
        this.topology = topology;
        this.topology.addNode(this);
    }

    public NetworkNode(String name, Integer id, ITopology topology) {
        this.setName(name);
        this.setId(id);
        setUuid();
        this.topology = topology;
        this.topology.addNode(this);
    }

    public NetworkNode(Integer id, ITopology topology) {
        this.setId(id);
        setUuid();
        this.topology = topology;
        this.topology.addNode(this);
    }

    /**
     * @return the id
     */
    @Override
    public Integer getId() {
        return id;
    }

    /**
     * @param id the id to set
     */
    @Override
    public void setId(Integer id) {
        this.id = id;
    }

    /**
     * @return the payLoad
     */
    @Override
    public Object getPayLoad() {
        return payLoad;
    }

    /**
     * @param payLoad the payLoad to set
     */
    @Override
    public void setPayLoad(Object payLoad) {
        this.payLoad = payLoad;
    }

    /**
     * @return the active
     */
    @Override
    public Boolean getActive() {
        return active;
    }

    /**
     * @param active the active to set
     */
    @Override
    public void setActive(Boolean active) {
        this.active = active;

    }

    /**
     * @return the connectionCount
     */
    @Override
    public Integer getConnectionCount() {
        return connectionCount;
    }

    /**
     * @param connectionCount the connectionCount to set
     */
    @Override
    public void setConnectionCount(Integer connectionCount) {
        this.connectionCount = connectionCount;
    }

    /**
     * @return the weight
     */
    @Override
    public Double getWeight() {
        return weight;
    }

    /**
     * @param weight the weight to set
     */
    @Override
    public void setWeight(Double weight) {
        this.weight = weight;
    }

    @Override
    public INetworkConnection addConnection(INetworkConnection connection) {
        if (connection.getTarget().equals(this)) {
            this.incommingConnectionCount++;
        } else {
            this.outCommingConnectionCount++;
        }
        this.getConnections().add(connection);
        this.setConnectionCount((Integer) (this.getConnectionCount() + 1));
        this.setWeight(calculateWeight());
        return connection;
    }

    @Override
    public INetworkConnection removeConnection(INetworkConnection connection) {
        if (connection.getTarget().equals(this)) {
            this.incommingConnectionCount--;
        } else {
            this.outCommingConnectionCount--;
        }
        this.getConnections().remove(connection);
        this.setConnectionCount((Integer) (this.getConnectionCount() - 1));
        this.setWeight(calculateWeight());
        return connection;
    }

    @Override
    public Double calculateWeight() {
        double d = 1;
        if (this.getConnectionCount() > 0) {
            double r = d / (double) this.getConnectionCount();
            return r;
        }
        return new Double("1");
    }

    @Override
    public Boolean endPoint() {
        return this.endPoint;
    }

    @Override
    public void setEndPoint(Boolean endPoint) {
        if (!this.endPoint && endPoint) {
            this.topology.getNodes().remove(endPoint);
            this.topology.getEndPoints().add(this);
        } else if (!this.topology.getNodes().contains(this)) {
            this.topology.getNodes().add(this);
        }

        this.endPoint = endPoint;

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
    public void setConnections(ArrayList<INetworkConnection> connections) {
        this.connections = connections;
    }

    /**
     * @return the name
     */
    @Override
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    @Override
    public void setName(String name) {
        this.name = name;
    }

    private void setUuid() {
        this.uuid = UUID.randomUUID().toString();
    }

    @Override
    public String getUuid() {
        return this.uuid;
    }

    @Override
    public void disable() {
        this.setActive(false);
    }

    @Override
    public void enable() {
        this.setActive(true);
    }

    @Override
    public void setOnRisk() {
        this.isOnRisk = true;
    }

    @Override
    public void clearAlamrs() {
        this.isOnRisk = false;
        this.enable();
    }

    /**
     * @return the topology
     */
    @Override
    public ITopology getTopology() {
        return topology;
    }

    /**
     * @param topology the topology to set
     */
    @Override
    public void setTopology(ITopology topology) {
        this.topology = topology;
    }

    /**
     * @return the width
     */
    @Override
    public Integer getWidth() {
        return width;
    }

    /**
     * @param width the width to set
     */
    @Override
    public void setWidth(Integer width) {
        this.width = width;
    }

    /**
     * @return the heigth
     */
    @Override
    public Integer getHeigth() {
        return heigth;
    }

    /**
     * @param heigth the heigth to set
     */
    @Override
    public void setHeigth(Integer heigth) {
        this.heigth = heigth;
    }

    @Override
    public Long getActiveConnnectionsCount() {
        return this.connections.parallelStream().filter(c -> c.getActive()).count();
    }

    @Override
    public Long getActiveConnectionsCount(INetworkNode node) {
        return this.connections.parallelStream().filter(c -> c.getActive() && !c.isRelatedToNode(node)).count();
    }

}
