/*
 * Copyright (C) 2023 Lucas Nishimura <lucas.nishimura@gmail.com>
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
package com.osstelecom.db.inventory.visualization.dto;

import com.osstelecom.db.inventory.manager.resources.CircuitResource;

/**
 *
 * @author Lucas Nishimura
 * @created 23.02.2023
 */
public class ThreeJSCircuitDTO extends BaseGraphDTO {

    private String aPointName;
    private String aPointId;
    private String zPointName;
    private String zPontId;
    private String circuitId;
    private String nodeAdress;

    public ThreeJSCircuitDTO(CircuitResource circuit) {
        if (circuit.getaPoint().getName() != null) {
            this.setaPointName(circuit.getaPoint().getName());
        } else if (circuit.getaPoint().getNodeAddress() != null) {
            this.setaPointName(circuit.getaPoint().getNodeAddress());
        }
        this.aPointId = circuit.getaPoint().getKey();

        if (circuit.getzPoint().getName() != null) {
            this.setzPointName(circuit.getzPoint().getName());
        } else if (circuit.getzPoint().getNodeAddress() != null) {
            this.setzPointName(circuit.getzPoint().getNodeAddress());
        }
        this.zPontId = circuit.getzPoint().getKey();

        this.circuitId = circuit.getKey();
        this.nodeAdress = circuit.getNodeAddress();

    }

    /**
     * @return the aPointName
     */
    public String getaPointName() {
        return aPointName;
    }

    /**
     * @param aPointName the aPointName to set
     */
    public void setaPointName(String aPointName) {
        this.aPointName = aPointName;
    }

    /**
     * @return the aPointId
     */
    public String getaPointId() {
        return aPointId;
    }

    /**
     * @param aPointId the aPointId to set
     */
    public void setaPointId(String aPointId) {
        this.aPointId = aPointId;
    }

    /**
     * @return the zPointName
     */
    public String getzPointName() {
        return zPointName;
    }

    /**
     * @param zPointName the zPointName to set
     */
    public void setzPointName(String zPointName) {
        this.zPointName = zPointName;
    }

    /**
     * @return the zPontId
     */
    public String getzPontId() {
        return zPontId;
    }

    /**
     * @param zPontId the zPontId to set
     */
    public void setzPontId(String zPontId) {
        this.zPontId = zPontId;
    }

    /**
     * @return the circuitId
     */
    public String getCircuitId() {
        return circuitId;
    }

    /**
     * @param circuitId the circuitId to set
     */
    public void setCircuitId(String circuitId) {
        this.circuitId = circuitId;
    }

    /**
     * @return the nodeAdress
     */
    public String getNodeAdress() {
        return nodeAdress;
    }

    /**
     * @param nodeAdress the nodeAdress to set
     */
    public void setNodeAdress(String nodeAdress) {
        this.nodeAdress = nodeAdress;
    }
}
