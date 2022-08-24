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
package com.osstelecom.db.inventory.manager.resources;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.osstelecom.db.inventory.manager.dto.DomainDTO;
import java.util.ArrayList;

/**
 *
 * @author Lucas Nishimura <lucas.nishimura@gmail.com>
 * @created 04.01.2022
 */
@JsonInclude(Include.NON_NULL)
public class CircuitResource extends BasicResource {

    /**
     * @return the circuitPath
     */
    public ArrayList<String> getCircuitPath() {
        return circuitPath;
    }

    /**
     * @param circuitPath the circuitPath to set
     */
    public void setCircuitPath(ArrayList<String> circuitPath) {
        this.circuitPath = circuitPath;
    }

    private ManagedResource aPoint;

    private ManagedResource zPoint;

    /**
     * Services (IDS) Carried By this Circuit
     */
    private ArrayList<String> services = new ArrayList<>();

    /**
     * Will be used by the impact manager to check if the circuit is reliable
     */
    private Integer minRedundancyCount = 1;

    private ArrayList<String> circuitPath = new ArrayList<>();

    /**
     * @return the aPoint
     */
    public ManagedResource getaPoint() {
        return aPoint;
    }

    /**
     * @param aPoint the aPoint to set
     */
    public void setaPoint(ManagedResource aPoint) {
        this.aPoint = aPoint;
    }

    /**
     * @return the zPoint
     */
    public ManagedResource getzPoint() {
        return zPoint;
    }

    /**
     * @param zPoint the zPoint to set
     */
    public void setzPoint(ManagedResource zPoint) {
        this.zPoint = zPoint;
    }

    public CircuitResource(String attributeSchema, DomainDTO domain) {
        super(attributeSchema, domain);
    }

    public CircuitResource(DomainDTO domain) {
        super(domain);
    }

    public CircuitResource() {
    }
}
