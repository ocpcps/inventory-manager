/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.osstelecom.db.inventory.topology.connection.impact;

import com.osstelecom.db.inventory.topology.ITopology;

/**
 *
 * @author Nishisan
 */
public abstract class ImpactManager implements IImpactManager {

    private final ITopology topology;
    
    public ImpactManager(ITopology topology) {
        this.topology = topology;
        this.topology.setImpactManager(this);
    }

    @Override
    public ITopology getTopology() {
        return this.topology;
    }
    
    
    
}
