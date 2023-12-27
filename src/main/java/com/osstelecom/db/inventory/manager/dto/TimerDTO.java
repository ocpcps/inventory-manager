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

/**
 *
 * @author Lucas Nishimura
 * @created 04.01.2022
 */
public class TimerDTO {

    private String key;
    private String operation;
    private Long startTimer;
    private String objectId;

    public TimerDTO(String key, String operation, Long startTimer) {
        this.key = key;
        this.operation = operation;
        this.startTimer = startTimer;
    }

    public TimerDTO(String key, String objectId, String operation, Long startTimer) {
        this.key = key;
        this.operation = operation;
        this.startTimer = startTimer;
        this.objectId = objectId;
    }

    /**
     * @return the uid
     */
    public String getKey() {
        return key;
    }

    /**
     * @param uid the uid to set
     */
    public void setKey(String key) {
        this.key = key;
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
     * @return the startTimer
     */
    public Long getStartTimer() {
        return startTimer;
    }

    /**
     * @param startTimer the startTimer to set
     */
    public void setStartTimer(Long startTimer) {
        this.startTimer = startTimer;
    }

    /**
     * @return the objectId
     */
    public String getObjectId() {
        return objectId;
    }

    /**
     * @param objectId the objectId to set
     */
    public void setObjectId(String objectId) {
        this.objectId = objectId;
    }
}
