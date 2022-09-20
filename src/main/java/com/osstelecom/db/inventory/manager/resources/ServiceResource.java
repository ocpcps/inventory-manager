/*
 * Copyright (C) 2021 Lucas Nishimura <lucas.nishimura@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.osstelecom.db.inventory.manager.resources;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 *
 * @author Lucas Nishimura <lucas.nishimura@gmail.com>
 */
@JsonInclude(Include.NON_NULL)
public class ServiceResource extends BasicResource {

    /**
     *
     * IDS of Services that this services depends on, like parent service.
     */
    private List<ServiceResource> dependencies;

    /**
     * IDS of Circuits that support this service
     */
    private List<CircuitResource> circuits;

    //
    // The next three realated stuff will bring hell on earth to maintain D:
    // God Help Our Souls!
    // @By Nishisan
    //
    
    /**
     * IDS of Services that this services is parent of This aproach is easier to
     * maintain.Can be read as the "list of children Services" of this service
     */
    private List<String> relatedServices;

    /**
     * IDS of Managed Resources that this services is parent of This aproach is
     * easier to maintain.Can be read as the "list of children Services" of this
     * service
     */
    private List<String> relatedManagedResources;

    /**
     * IDS of Resources Connections that this services is parent of This aproach
     * is easier to maintain.Can be read as the "list of children Services" of
     * this service
     */
    private List<String> relatedResourceConnections;

    @Schema(description = "If true, indicates that the Service has some broken connections")
    private boolean degrated = false;

    @Schema(description = "If true, indicates that the Service has some broken connections, and it is broken")
    private boolean broken = false;

    @Schema(description = "The ID List of the broken connections")
    private List<String> brokenResources;

    public ServiceResource(String attributeSchema, Domain domain) {
        super(attributeSchema, domain);
    }

    public ServiceResource(Domain domain) {
        super(domain);
    }

    public ServiceResource() {
    }

    public ServiceResource(String id) {
        this.setId(id);
    }

    public List<ServiceResource> getDependencies() {
        return dependencies;
    }

    public void setDependencies(List<ServiceResource> dependencies) {
        this.dependencies = dependencies;
    }

    public List<CircuitResource> getCircuits() {
        return circuits;
    }

    public void setCircuits(List<CircuitResource> circuits) {
        this.circuits = circuits;
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

    /**
     * @return the relatedServices
     */
    public List<String> getRelatedServices() {
        return relatedServices;
    }

    /**
     * @param relatedServices the relatedServices to set
     */
    public void setRelatedServices(List<String> relatedServices) {
        this.relatedServices = relatedServices;
    }
}
