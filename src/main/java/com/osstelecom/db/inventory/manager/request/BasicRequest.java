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
package com.osstelecom.db.inventory.manager.request;

/**
 *
 * @author Lucas Nishimura <lucas.nishimura@gmail.com>
 * @param <T>
 * @created 15.12.2021
 */
public abstract class BasicRequest<T> implements IRequest<T> {

    private String requestToken;
    private String requestTarget;
    private String requestDomain;
    private String className;
    private String userId;
    private String userEmail;
    private String userName;
    private String userLogin;
    private T payLoad;

    public String getClassName() {
        if (this.className == null) {
            this.className = this.getClass().getName();
        }
        return className;
    }

    @Override
    public String getRequestToken() {
        return this.requestToken;
    }

    @Override
    public String getRequestTarget() {
        return this.requestTarget;
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
     * @return the requestDomain
     */
    @Override
    public String getRequestDomain() {
        return requestDomain;
    }

    /**
     * @param requestDomain the requestDomain to set
     */
    @Override
    public void setRequestDomain(String requestDomain) {
        this.requestDomain = requestDomain;
    }

    /**
     * @return the userId
     */
    public String getUserId() {
        return userId;
    }

    /**
     * @param userId the userId to set
     */
    public void setUserId(String userId) {
        this.userId = userId;
    }

    /**
     * @return the userEmail
     */
    public String getUserEmail() {
        return userEmail;
    }

    /**
     * @param userEmail the userEmail to set
     */
    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    /**
     * @return the userName
     */
    public String getUserName() {
        return userName;
    }

    /**
     * @param userName the userName to set
     */
    public void setUserName(String userName) {
        this.userName = userName;
    }

    /**
     * @return the userLogin
     */
    public String getUserLogin() {
        return userLogin;
    }

    /**
     * @param userLogin the userLogin to set
     */
    public void setUserLogin(String userLogin) {
        this.userLogin = userLogin;
    }

}
