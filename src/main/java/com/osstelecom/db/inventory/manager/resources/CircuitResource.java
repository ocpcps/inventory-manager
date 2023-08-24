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
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Lucas Nishimura
 * @created 04.01.2022
 */
@JsonInclude(Include.NON_NULL)
public class CircuitResource extends BasicResource {

    private boolean degrated = false;
    private boolean broken = false;
    private List<String> brokenResources;

    private ManagedResource aPoint;

    private ManagedResource zPoint;

    /**
     * Services (IDS) Carried By this Circuit
     */
    private List<String> services = new ArrayList<>();

    /**
     * Later Will be used by the impact manager to check if the circuit is
     * reliable
     */
    private Integer minRedundancyCount = 3;

    private List<String> circuitPath = new ArrayList<>();

    /**
     * @return the services
     */
    public List<String> getServices() {
        return services;
    }

    /**
     * @param services the services to set
     */
    public void setServices(List<String> services) {
        this.services = services;
    }

    public boolean getDegrated() {
        return degrated;
    }

    public void setDegrated(Boolean degrated) {
        this.degrated = degrated;
    }

    public boolean getBroken() {
        return broken;
    }

    public void setBroken(boolean broken) {
        this.broken = broken;
    }

    /**
     * @return the circuitPath
     */
    public List<String> getCircuitPath() {
        return circuitPath;
    }

    /**
     * @param circuitPath the circuitPath to set
     */
    public void setCircuitPath(List<String> circuitPath) {
        this.circuitPath = circuitPath;
    }

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

    public CircuitResource(String attributeSchema, Domain domain) {
        super(attributeSchema, domain);
    }

    public CircuitResource(Domain domain) {
        super(domain);
    }

    public CircuitResource(Domain domain, String id) {
        super(domain, id);
    }

    public CircuitResource(String domainName, String id) {
        super(domainName, id);
    }

    
    public CircuitResource() {
    }

    /**
     * @return the brokenResources
     */
    public List<String> getBrokenResources() {
        return brokenResources;
    }

    /**
     * @param brokenResources the brokenResources to set
     */
    public void setBrokenResources(List<String> brokenResources) {
        this.brokenResources = brokenResources;
    }
}
