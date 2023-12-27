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
import java.util.ArrayList;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * @author Nishisan
 */
public abstract class NetworkConnection implements INetworkConnection {

    private INetworkNode source;
    private INetworkNode target;
    private Integer id = -1;
    private Object payLoad = null;
    private Boolean active = true;
    private Integer connectionCount = 0;
    private Double weight = 1.0;
    private String uuid = "";
    private String name = "";
    private ITopology topology;
    private Integer width;
    private Integer heigth;
    private Boolean leadsToEndPoint = false;
    private ArrayList<ArrayList<INetworkNode>> pathList = new ArrayList<ArrayList<INetworkNode>>();
    private ConcurrentHashMap<String, Boolean> visitedThreads = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, Object> attributes = new ConcurrentHashMap<>();

    @Override
    public void disconnect(INetworkNode node) {
        if (this.source.equals(node)) {
            node.removeConnection(this);
        } else if (this.target.equals(node)) {
            node.removeConnection(this);
        }
    }

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
        int hash = 7;
        hash = 89 * hash + Objects.hashCode(this.uuid);
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
        final NetworkConnection other = (NetworkConnection) obj;
        if (!Objects.equals(this.uuid, other.uuid)) {
            return false;
        }
        return true;
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
    public void printPathList() {
        int listIdx = 0;
        if (!pathList.isEmpty()) {
            for (ArrayList<INetworkNode> nodes : pathList) {
                System.out.println("L:[" + listIdx + "]");
                listIdx++;
                for (INetworkNode p : nodes) {
                    System.out.print(p.getName() + ".");
                }
                System.out.println("");
            }
        }
    }

    @Override
    public Boolean isRelatedToNode(INetworkNode node) {
//        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        if (this.source.equals(node) || this.target.equals(node)) {
            return true;
        } else {
            return false;
        }
    }

    public synchronized ArrayList<ArrayList<INetworkNode>> addPathList(ArrayList<INetworkNode> list) {
        pathList.add(list);
        return pathList;
    }

    @Override
    public void leadsToEndPoint(Boolean lead) {
        this.leadsToEndPoint = lead;
    }

    @Override
    public Boolean leadsToEndpoint() {
        return this.leadsToEndPoint;
    }

    public NetworkConnection() {
        setUuid();
    }

    @Override
    public void setSource(INetworkNode source) {
        this.source = source;
    }

    @Override
    public INetworkNode getSource() {
        return this.source;
    }

    @Override
    public void setTarget(INetworkNode target) {
        this.target = target;
    }

    @Override
    public INetworkNode getTarget() {
        return this.target;
    }

    @Override
    public Integer getId() {
        return this.id;
    }

    @Override
    public void setId(Integer id) {
        this.id = id;
    }

    @Override
    public void setPayLoad(Object payLoad) {
        this.payLoad = payLoad;
    }

    @Override
    public Object getPayLoad() {
        return this.payLoad;
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
        propagate(this.target);
        propagate(this.source);
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
    public Double calculateWeight() {
        //implementar
        return null;
    }

    private void setUuid() {
        this.uuid = UUID.randomUUID().toString();
    }

    @Override
    public String getUuid() {
        return this.uuid;
    }

    @Override
    public void enable() {
        this.setActive(true);

    }

    @Override
    public void disable() {
        this.setActive(false);
    }

    protected void propagate(INetworkNode node) {
        Double outage = 0.0;
        for (INetworkConnection tc : node.getConnections()) {
            if (!tc.getActive()) {
                outage = outage + node.getWeight();
            }
        }
        if (outage >= 1) {
            node.setActive(false);
        } else {
            node.setActive(true);
        }

        if (outage == node.getWeight()) {
            node.setOnRisk();
        }
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

    /**
     * @return the topology
     */
    @Override
    public ITopology getTopology() {
        return topology;
    }

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
}
