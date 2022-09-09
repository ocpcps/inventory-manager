/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.osstelecom.db.inventory.topology.connection;

import com.osstelecom.db.inventory.topology.node.INetworkNode;
import com.osstelecom.db.inventory.topology.ITopology;

/**
 *
 * @author Nishisan
 */
public class DefaultNetworkConnection extends NetworkConnection {

    public DefaultNetworkConnection() {
    }

    public DefaultNetworkConnection(Integer id) {
        this.setId(id);
    }

    public DefaultNetworkConnection(INetworkNode source, INetworkNode target, ITopology topology) {
        super();
        this.setTopology(topology);
        this.setSource(source);
        this.setTarget(target);

        source.addConnection(this);

        target.addConnection(this);

        String autoName = source.getName() + "." + target.getName();
        if (this.getTopology().getConnectionByName(autoName) == null) {
            this.setName(autoName);
        }

    }

    public DefaultNetworkConnection(INetworkNode source, INetworkNode target, String name, ITopology topology) {
        super();
        this.setSource(source);

        this.setTarget(target);
        source.addConnection(this);
        target.addConnection(this);
        this.setName(name);
        this.setTopology(topology);
    }

    public DefaultNetworkConnection(INetworkNode source, INetworkNode target, String name, Object payLoad, ITopology topology) {
        super();
        this.setSource(source);
        source.addConnection(this);
        this.setTarget(target);
        target.addConnection(this);
        this.setPayLoad(payLoad);
        this.setName(name);
        this.setTopology(topology);
    }

    public DefaultNetworkConnection(INetworkNode source, INetworkNode target, Object payLoad, Integer id, String name, ITopology topology) {
        super();
        this.setSource(source);
        source.addConnection(this);
        this.setTarget(target);
        target.addConnection(this);
        this.setPayLoad(payLoad);
        this.setId(id);
        this.setName(name);
        this.setTopology(topology);
    }

}
