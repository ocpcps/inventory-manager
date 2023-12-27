/*
 * Copyright (C) 2021 Lucas Nishimura <lucas.nishimura@gmail.com>
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
package com.osstelecom.db.inventory.topology.node;

import com.osstelecom.db.inventory.topology.connection.INetworkConnection;
import java.util.List;

/**
 *
 * @author Lucas Nishimura
 * @created 06.01.2022
 */
public class SourceTargetWrapper {

    private INetworkNode source;
    private INetworkNode target;
    private INetworkConnection connection;
    private Integer limit;
    private Boolean useCache;
    private List<String> pathList;

    public SourceTargetWrapper() {
    }

    

    public SourceTargetWrapper(INetworkNode source, INetworkNode target, INetworkConnection connection) {
        this.source = source;
        this.target = target;
        this.connection = connection;
    }

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

    /**
     * @return the pathList
     */
    public List<String> getPathList() {
        return pathList;
    }

    /**
     * @param pathList the pathList to set
     */
    public void setPathList(List<String> pathList) {
        this.pathList = pathList;
    }

}
