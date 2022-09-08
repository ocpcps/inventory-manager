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

import java.util.List;
import java.util.Map;

/**
 *
 * @author Lucas Nishimura <lucas.nishimura@gmail.com>
 * @created 15.12.2021
 */
public abstract class BasicResponse<T> implements IResponse<T> {

    private int statusCode = 200;
    private T payLoad;
    private int size;
    private String className;

    public String getClassName() {
        if (this.className==null){
            this.className = this.getClass().getName();
        }
        return className;
    }

    public BasicResponse(T obj) {
        this.setPayLoad(obj);
        if (this.payLoad instanceof List) {
            this.size = ((List<?>) this.payLoad).size();
        } else if (this.payLoad instanceof Map) {
            this.size = ((Map<?,?>) this.payLoad).size();
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

}
