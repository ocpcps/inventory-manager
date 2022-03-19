/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.osstelecom.db.inventory.topology.server;

import com.osstelecom.db.inventory.topology.ITopology;

/**
 *
 * @author Lucas
 */
public class TopologyServer implements ITopologyServer {

    private TopologyDao topologyDao = new TopologyDao();

    @Override
    public void addTopology(ITopology topology) {

    }

}
