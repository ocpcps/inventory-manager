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
package com.osstelecom.db.inventory.manager.response;

import com.arangodb.entity.CursorEntity.Stats;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.osstelecom.db.inventory.manager.resources.GraphList;
import com.osstelecom.db.inventory.visualization.dto.ThreeJSViewDTO;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Lucas Nishimura
 * @created 15.12.2021
 */
@JsonInclude(Include.NON_NULL)
public abstract class BasicResponse<T> implements IResponse<T> {

    private int statusCode = 200;
    private T payLoad;
    private Long size;
    private String className;
    private Stats arangoStats;

    public String getClassName() {
        if (this.className == null) {
            this.className = this.getClass().getName();
        }
        return className;
    }

    public BasicResponse(T obj) {
        this.setPayLoad(obj);
        if (this.payLoad instanceof List) {
            this.size = ((List<?>) this.payLoad).size() + 0L; // Implicit Type Cast...be carefull
        } else if (this.payLoad instanceof Map) {
            this.size = ((Map<?, ?>) this.payLoad).size() + 0L;
        } else if (this.payLoad instanceof GraphList) {
            this.size = ((GraphList<?>) this.payLoad).size();
        } else if (this.payLoad instanceof ThreeJSViewDTO) {
            ThreeJSViewDTO view = (ThreeJSViewDTO) this.payLoad;
            this.size = view.getLinkCount() + view.getNodeCount();
        }
    }

    @Override
    public void setStatusCode(int status) {
        this.statusCode = status;
    }

    @Override
    public int getStatusCode() {
        return this.statusCode;
    }

    @Override
    public T getPayLoad() {
        return payLoad;
    }

    @Override
    public void setPayLoad(T t) {
        this.payLoad = t;
    }

    /**
     * @return the size
     */
    public Long getSize() {
        return size;
    }

    /**
     * @param size the size to set
     */
    public void setSize(Long size) {
        this.size = size;
    }

    public Stats getArangoStats() {
        return arangoStats;
    }

    public void setArangoStats(Stats arangoStats) {
        this.arangoStats = arangoStats;
    }

}
