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
package com.osstelecom.db.inventory.manager.events;

import com.arangodb.entity.DocumentCreateEntity;
import com.arangodb.entity.DocumentDeleteEntity;
import com.arangodb.entity.DocumentUpdateEntity;
import com.osstelecom.db.inventory.manager.jobs.DBJobInstance;
import com.osstelecom.db.inventory.manager.resources.BasicResource;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Lucas Nishimura
 * @param <T>
 * @created 10.04.2022
 */
public abstract class BasicResourceEvent<T extends BasicResource> {

    private Date eventDate;
    private String sourceEventId;
    private String sourceEventDescription;
    private DBJobInstance relatedJob;

    private T oldResource;
    private T newResource;
    private Map<String, Object> details = new HashMap<>();

    public BasicResourceEvent(T resource) {
        this.newResource = resource;
    }

    public BasicResourceEvent(DocumentUpdateEntity<T> entity) {
        this.oldResource = entity.getOld();
        this.newResource = entity.getNew();
        this.eventDate = new Date();
    }

    public BasicResourceEvent(DocumentCreateEntity<T> entity) {
        this.oldResource = entity.getOld();
        this.newResource = entity.getNew();
        this.eventDate = new Date();
    }

    public BasicResourceEvent(DocumentDeleteEntity<T> entity) {
        this.oldResource = entity.getOld();
        this.eventDate = new Date();
    }

    public BasicResourceEvent(T oldResource, T newResource) {
        this.oldResource = oldResource;
        this.newResource = newResource;
        this.eventDate = new Date();
    }

    public Date getEventDate() {
        return eventDate;
    }

    public void setEventDate(Date eventDate) {
        this.eventDate = eventDate;
    }

    public String getSourceEventId() {
        return sourceEventId;
    }

    public void setSourceEventId(String sourceEventId) {
        this.sourceEventId = sourceEventId;
    }

    public String getSourceEventDescription() {
        return sourceEventDescription;
    }

    public void setSourceEventDescription(String sourceEventDescription) {
        this.sourceEventDescription = sourceEventDescription;
    }

    protected void setEventDate() {
        this.eventDate = new Date();
    }

    public void addEventDetail(String key, Object value) {
        this.details.put(key, value);
    }

    public DBJobInstance getRelatedJob() {
        return relatedJob;
    }

    public void setRelatedJob(DBJobInstance relatedJob) {
        this.relatedJob = relatedJob;
    }

    /**
     * @return the oldResource
     */
    public T getOldResource() {
        return oldResource;
    }

    /**
     * @param oldResource the oldResource to set
     */
    public void setOldResource(T oldResource) {
        this.oldResource = oldResource;
    }

    /**
     * @return the newResource
     */
    public T getNewResource() {
        return newResource;
    }

    /**
     * @param newResource the newResource to set
     */
    public void setNewResource(T newResource) {
        this.newResource = newResource;
    }

    /**
     * @return the details
     */
    public Map<String, Object> getDetails() {
        return details;
    }

    /**
     * @param details the details to set
     */
    public void setDetails(Map<String, Object> details) {
        this.details = details;
    }

}
