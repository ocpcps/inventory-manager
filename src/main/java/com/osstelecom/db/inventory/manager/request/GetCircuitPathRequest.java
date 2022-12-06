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
package com.osstelecom.db.inventory.manager.request;

/**
 *
 * @author Lucas Nishimura <lucas.nishimura@gmail.com>
 * @created 05.01.2022
 */
public class GetCircuitPathRequest extends CreateCircuitPathRequest {

    private String circuitId;
    private String domainName;

    public GetCircuitPathRequest(String circuitId, String domainName) {
        this.circuitId = circuitId;
        this.domainName = domainName;
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
     * @return the domainName
     */
    public String getDomainName() {
        return domainName;
    }

    /**
     * @param domainName the domainName to set
     */
    public void setDomainName(String domainName) {
        this.domainName = domainName;
    }
}
