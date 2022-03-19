/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.osstelecom.db.inventory.topology.server;

import com.osstelecom.db.inventory.topology.ITopology;
import java.rmi.Remote;

/**
 *
 * @author Lucas
 */
public interface ITopologyServer extends Remote {

    public void addTopology(ITopology topology);
}
