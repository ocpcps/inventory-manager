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

/**
 * Representa um local Fisico
 *
 * @author Lucas Nishimura
 */
public class ResourceLocation extends BasicResource {

    private String latLong;
    private String city;
    private String state;
    private String country = "BR";

    public ResourceLocation(String attributeSchema, Domain domain) {
        super(attributeSchema, domain);
    }

    public ResourceLocation(Domain domain) {
        super(domain);
    }

    public ResourceLocation() {
    }

    /**
     * @return the latLong
     */
    public String getLatLong() {
        return latLong;
    }

    /**
     * @param latLong the latLong to set
     */
    public void setLatLong(String latLong) {
        this.latLong = latLong;
    }

    /**
     * @return the city
     */
    public String getCity() {
        return city;
    }

    /**
     * @param city the city to set
     */
    public void setCity(String city) {
        this.city = city;
    }

    /**
     * @return the state
     */
    public String getState() {
        return state;
    }

    /**
     * @param state the state to set
     */
    public void setState(String state) {
        this.state = state;
    }

    /**
     * @return the country
     */
    public String getCountry() {
        return country;
    }

    /**
     * @param country the country to set
     */
    public void setCountry(String country) {
        this.country = country;
    }

}
