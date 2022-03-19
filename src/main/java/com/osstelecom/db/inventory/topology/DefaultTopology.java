/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.osstelecom.db.inventory.topology;

import com.osstelecom.db.inventory.topology.listeners.DefaultTopologyListener;

/**
 *
 * @author Nishisan
 */
public class DefaultTopology extends Topology {

    public DefaultTopology() {
        super(new DefaultTopologyListener());
    }

    public DefaultTopology(Integer scaleFactor) {
        super(scaleFactor);
    }

}
