/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.osstelecom.db.inventory.topology.node;

/**
 *
 * @author Lucas Nishimura <lucas.nishimura@gmail.com>
 * @created 06.01.2022
 */
public class SourceTargetWrapper {

    private INetworkNode source;
    private INetworkNode target;
    private Integer limit;
    private Boolean useCache;

    /**
     * @return the source
     */
    public INetworkNode getSource() {
        return source;
    }

    /**
     * @param source the source to set
     */
    public void setSource(INetworkNode source) {
        this.source = source;
    }

    /**
     * @return the target
     */
    public INetworkNode getTarget() {
        return target;
    }

    /**
     * @param target the target to set
     */
    public void setTarget(INetworkNode target) {
        this.target = target;
    }

    /**
     * @return the limit
     */
    public Integer getLimit() {
        return limit;
    }

    /**
     * @param limit the limit to set
     */
    public void setLimit(Integer limit) {
        this.limit = limit;
    }

    /**
     * @return the useCache
     */
    public Boolean getUseCache() {
        return useCache;
    }

    /**
     * @param useCache the useCache to set
     */
    public void setUseCache(Boolean useCache) {
        this.useCache = useCache;
    }

}
