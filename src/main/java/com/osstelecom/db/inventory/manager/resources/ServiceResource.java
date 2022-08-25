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

import com.osstelecom.db.inventory.manager.dto.DomainDTO;
import java.util.List;

/**
 *
 * @author Lucas Nishimura <lucas.nishimura@gmail.com>
 */
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
    
    public ServiceResource(String id){
        this.setId(id);
    }

    public ServiceResource(){}

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

}
