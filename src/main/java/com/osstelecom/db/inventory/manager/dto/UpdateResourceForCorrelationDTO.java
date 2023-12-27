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
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Utilizado para Correlação com TEMS
 *
 * @author Lucas Nishimura
 * @created 30.08.2022
 */
public class UpdateResourceForCorrelationDTO {

    private String eventName;
    private String eventCategory;
    private String federatedId;
    private Long eventTimeStamp;
    /**
     * UP or Down
     */
    private String operation;
    /**
     * Se maior que zero rever a situação anterior quando vencer;
     */
    private Integer ttl;
    private List<String> correlationIds = new ArrayList<>();

    private Map<String, Object> detail = new ConcurrentHashMap<>();

    public String getEventName() {
        return eventName;
    }

    public void setEventName(String eventName) {
        this.eventName = eventName;
    }

    public String getEventCategory() {
        return eventCategory;
    }

    public void setEventCategory(String eventCategory) {
        this.eventCategory = eventCategory;
    }

    public String getFederatedId() {
        return federatedId;
    }

    public void setFederatedId(String federatedId) {
        this.federatedId = federatedId;
    }

    public Long getEventTimeStamp() {
        return eventTimeStamp;
    }

    public void setEventTimeStamp(Long eventTimeStamp) {
        this.eventTimeStamp = eventTimeStamp;
    }

    public Map<String, Object> getDetail() {
        return detail;
    }

    public void setDetail(Map<String, Object> detail) {
        this.detail = detail;
    }

    /**
     * @return the correlationIds
     */
    public List<String> getCorrelationIds() {
        return correlationIds;
    }

    /**
     * @param correlationIds the correlationIds to set
     */
    public void setCorrelationIds(List<String> correlationIds) {
        this.correlationIds = correlationIds;
    }

    /**
     * @return the operation
     */
    public String getOperation() {
        return operation;
    }

    /**
     * @param operation the operation to set
     */
    public void setOperation(String operation) {
        this.operation = operation;
    }

    /**
     * @return the ttl
     */
    public Integer getTtl() {
        return ttl;
    }

    /**
     * @param ttl the ttl to set
     */
    public void setTtl(Integer ttl) {
        this.ttl = ttl;
    }

}
