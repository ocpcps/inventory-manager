/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.osstelecom.db.inventory.topology.node;

import com.osstelecom.db.inventory.topology.ITopology;

/**
 *
 * @author Nishisan
 */
public class DefaultNode extends NetworkNode {

    public DefaultNode(Integer id, ITopology topology) {
        super(id, topology);
    }

    public DefaultNode(String name, Integer id, ITopology topology) {
        super(name, id, topology);
    }

    public DefaultNode(ITopology topology) {
        super(topology);
    }
}
