/*
 * Copyright (C) 2022 Lucas Nishimura <lucas.nishimura@gmail.com>
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
package com.osstelecom.db.inventory.manager.dto;

import com.osstelecom.db.inventory.manager.resources.CircuitResource;
import com.osstelecom.db.inventory.manager.resources.ResourceConnection;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Lucas Nishimura <lucas.nishimura@gmail.com>
 * @created 04.01.2022
 */
public class CircuitPathDTO implements Serializable {

    private CircuitResource circuit;
    private List<ResourceConnection> paths = new ArrayList<>();

    /**
     * @return the circuit
     */
    public CircuitResource getCircuit() {
        return circuit;
    }

    /**
     * @param circuit the circuit to set
     */
    public void setCircuit(CircuitResource circuit) {
        this.circuit = circuit;
    }

    /**
     * @return the paths
     */
    public List<ResourceConnection> getPaths() {
        return paths;
    }

    /**
     * @param paths the paths to set
     */
    public void setPaths(List<ResourceConnection> paths) {
        this.paths = paths;
    }

//    /**
//     * @return the degrated
//     */
//    public Boolean getDegrated() {
//        return degrated;
//    }
//    /**
//     * @param degrated the degrated to set
//     */
//    public void setDegrated(Boolean degrated) {
//        this.degrated = degrated;
//    }
//
//    /**
//     * @return the broken
//     */
//    public Boolean getBroken() {
//        return broken;
//    }
//
//    /**
//     * @param broken the broken to set
//     */
//    public void setBroken(Boolean broken) {
//        this.broken = broken;
//    }
//    /**
//     * @return the brokenResources
//     */
//    public ArrayList<String> getBrokenResources() {
//        return brokenResources;
//    }
//
//    /**
//     * @param brokenConnections the brokenResources to set
//     */
//    public void setBrokenResources(ArrayList<String> brokenConnections) {
//        this.brokenResources = brokenConnections;
//    }
}
