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
package com.osstelecom.db.inventory.manager.exception;

import com.osstelecom.db.inventory.manager.request.IRequest;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * @author Lucas Nishimura <lucas.nishimura@gmail.com>
 * @created 15.12.2021
 */
public abstract class BasicException extends Exception implements Serializable {

    protected IRequest<?> request;
    protected Integer statusCode = 500;
    private Object details;

    public void addDetails(String key, Object obj) {
        if (this.details == null) {
            Map<String, Object> map = new ConcurrentHashMap<String, Object>();
            map.put(key, obj);
            this.details = map;
        } else if (this.details instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) this.details;
            map.put(key, obj);
        } else {
            this.details = obj;
        }
    }

    public BasicException() {
    }

    public BasicException(String msg) {
        super(msg);
    }

    public BasicException(IRequest<?> request) {
        this.request = request;
    }

    public BasicException(IRequest<?> request, String message) {
        super(message);
        this.request = request;
    }

    public BasicException(String msg, Throwable cause) {
        super(msg, cause);
    }

    public BasicException(IRequest<?> request, String message, Throwable cause) {
        super(message, cause);
        this.request = request;
    }

    public BasicException(IRequest<?> request, Throwable cause) {
        super(cause);
        this.request = request;
    }

    public BasicException(IRequest<?> request, String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
        this.request = request;
    }

    /**
     * @return the request
     */
    public IRequest getRequest() {
        return request;
    }

    /**
     * @param request the request to set
     */
    public void setRequest(IRequest<?> request) {
        this.request = request;
    }

    /**
     * @return the statusCode
     */
    public Integer getStatusCode() {
        return statusCode;
    }

    /**
     * @param statusCode the statusCode to set
     */
    public void setStatusCode(Integer statusCode) {
        this.statusCode = statusCode;
    }

    /**
     * @param details the details to set
     */
    public void setDetails(Object details) {
        this.details = details;
    }

    public void setDetails(Object... details) {
        this.details = Arrays.asList(details);
    }

    /**
     * @return the details
     */
    public Object getDetails() {
        return details;
    }
}
