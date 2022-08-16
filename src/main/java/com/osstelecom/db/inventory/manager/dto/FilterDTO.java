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

import java.util.ArrayList;
import java.util.HashMap;

/**
 *
 * @author Lucas Nishimura <lucas.nishimura@gmail.com>
 * @created 26.01.2022
 */
public class FilterDTO {

    private ArrayList<String> classes;
    private ArrayList<String> objects;
    private String aqlFilter;
    private HashMap<String, Object> bindings;
    private String targetRegex;
    private Boolean computeWeakLinks = false;
    private Integer computeThreads = 8;
    private Integer minCuts = 1;

    /**
     * @return the classes
     */
    public ArrayList<String> getClasses() {
        return classes;
    }

    /**
     * @param classes the classes to set
     */
    public void setClasses(ArrayList<String> classes) {
        this.classes = classes;
    }

    /**
     * @return the objects
     */
    public ArrayList<String> getObjects() {
        return objects;
    }

    /**
     * @param objects the objects to set
     */
    public void setObjects(ArrayList<String> objects) {
        this.objects = objects;
    }

    /**
     * @return the targetRegex
     */
    public String getTargetRegex() {
        return targetRegex;
    }

    /**
     * @param targetRegex the targetRegex to set
     */
    public void setTargetRegex(String targetRegex) {
        this.targetRegex = targetRegex;
    }

    /**
     * @return the computeWeakLinks
     */
    public Boolean getComputeWeakLinks() {
        return computeWeakLinks;
    }

    /**
     * @param computeWeakLinks the computeWeakLinks to set
     */
    public void setComputeWeakLinks(Boolean computeWeakLinks) {
        this.computeWeakLinks = computeWeakLinks;
    }

    /**
     * @return the computeThreads
     */
    public Integer getComputeThreads() {
        return computeThreads;
    }

    /**
     * @param computeThreads the computeThreads to set
     */
    public void setComputeThreads(Integer computeThreads) {
        this.computeThreads = computeThreads;
    }

    /**
     * @return the minCuts
     */
    public Integer getMinCuts() {
        return minCuts;
    }

    /**
     * @param minCuts the minCuts to set
     */
    public void setMinCuts(Integer minCuts) {
        this.minCuts = minCuts;
    }

    /**
     * @return the aqlFilter
     */
    public String getAqlFilter() {
        return aqlFilter;
    }

    /**
     * @param aqlFilter the aqlFilter to set
     */
    public void setAqlFilter(String aqlFilter) {
        this.aqlFilter = aqlFilter;
    }

    /**
     * @return the bindings
     */
    public HashMap<String, Object> getBindings() {
        return bindings;
    }

    /**
     * @param bindings the bindings to set
     */
    public void setBindings(HashMap<String, Object> bindings) {
        this.bindings = bindings;
    }
}
