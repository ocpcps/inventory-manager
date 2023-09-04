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

import com.osstelecom.db.inventory.manager.request.BasicRequest;
import com.osstelecom.db.inventory.manager.request.IRequest;
import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * @author Lucas Nishimura
 * @created 15.12.2021
 */
public abstract class BasicException extends Exception implements Serializable, IBasicException {

    protected IRequest<? extends BasicRequest> request;
    protected Integer statusCode = 500;
    private Map<String, Object> details;

    protected BasicException(Throwable thrwbl) {
        super(thrwbl);
        this.handleDetails(thrwbl);
    }

    private void handleDetails(Throwable ex) {
        if (ex instanceof BasicException) {
            BasicException a = (BasicException) ex;
            this.details = a.getDetails();
        }
    }

    protected void addDetailMap(String key, Object obj) {
        if (this.details == null) {
            this.details = new ConcurrentHashMap<>();
        }
        this.details.put(key, obj);
    }

    protected BasicException() {
    }

    protected BasicException(String msg) {
        super(msg);
    }

    protected BasicException(IRequest<? extends BasicRequest> request) {
        this.request = request;
    }

    protected BasicException(IRequest<? extends BasicRequest> request, String message) {
        super(message);
        this.request = request;
    }

    protected BasicException(String msg, Throwable cause) {
        super(msg, cause);
        this.handleDetails(cause);
    }

    protected BasicException(IRequest<? extends BasicRequest> request, String message, Throwable cause) {
        super(message, cause);
        this.request = request;
        this.handleDetails(cause);
    }

    protected BasicException(IRequest<? extends BasicRequest> request, Throwable cause) {
        super(cause);
        this.request = request;
        this.handleDetails(cause);

    }

    protected BasicException(IRequest<? extends BasicRequest> request, String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
        this.request = request;
        this.handleDetails(cause);
    }

    /**
     * @return the request
     */
    public IRequest<? extends BasicRequest> getRequest() {
        return request;
    }

    /**
     * @param request the request to set
     */
    public void setRequest(IRequest<? extends BasicRequest> request) {
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

//    public void setDetails(Object... details) {
//        this.details = Arrays.asList(details);
//    }
    /**
     * @return the details
     */
    public Map<String, Object> getDetails() {
        return details;
    }

    @Override
    public void printStackTrace() {
        if (this.details != null) {
//            this.details.forEach((k, v) -> {
//                System.err.println("k: -> " + v);
//            });

        }
        super.printStackTrace(); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/OverriddenMethodBody
    }

}
