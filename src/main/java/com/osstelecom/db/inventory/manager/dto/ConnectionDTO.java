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
package com.osstelecom.db.inventory.manager.dto;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * @author Lucas Nishimura <lucas.nishimura@gmail.com>
 * @created 16.12.2021
 */
public class ConnectionDTO {

    private String fromName;
    private String fromNodeAddress;
    private String toNodeAddress;
    private String fromClassName;
    private String fromId;
    private String toId;
    private String toName;
    private String toClassName;
    private String connectionName;
    private String nodeAddress;
    private String connectionClass = "connection.default";
    private String attributeSchemaName = "connection.default";
    private Boolean propagateOperStatus;
    private String operationalStatus = "UP";
    private Map<String, Object> attributes = new ConcurrentHashMap<>();

    /**
     * @return the attributes
     */
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    /**
     * @param attributes the attributes to set
     */
    public void setAttributes(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    /**
     * @return the fromName
     */
    public String getFromName() {
        return fromName;
    }

    /**
     * @param fromName the fromName to set
     */
    public void setFromName(String fromName) {
        this.fromName = fromName;
    }

    /**
     * @return the fromClassName
     */
    public String getFromClassName() {
        return fromClassName;
    }

    /**
     * @param fromClassName the fromClassName to set
     */
    public void setFromClassName(String fromClassName) {
        this.fromClassName = fromClassName;
    }

    /**
     * @return the toName
     */
    public String getToName() {
        return toName;
    }

    /**
     * @param toName the toName to set
     */
    public void setToName(String toName) {
        this.toName = toName;
    }

    /**
     * @return the toClassName
     */
    public String getToClassName() {
        return toClassName;
    }

    /**
     * @param toClassName the toClassName to set
     */
    public void setToClassName(String toClassName) {
        this.toClassName = toClassName;
    }

    /**
     * @return the connectionName
     */
    public String getConnectionName() {
        return connectionName;
    }

    /**
     * @param connectionName the connectionName to set
     */
    public void setConnectionName(String connectionName) {
        this.connectionName = connectionName;
    }

    /**
     * @return the connectionClass
     */
    public String getConnectionClass() {
        return connectionClass;
    }

    /**
     * @param connectionClass the connectionClass to set
     */
    public void setConnectionClass(String connectionClass) {
        this.connectionClass = connectionClass;
    }

    /**
     * @return the attributeSchemaName
     */
    public String getAttributeSchemaName() {
        return attributeSchemaName;
    }

    /**
     * @param attributeSchemaName the attributeSchemaName to set
     */
    public void setAttributeSchemaName(String attributeSchemaName) {
        this.attributeSchemaName = attributeSchemaName;
    }

    /**
     * @return the propagateOperStatus
     */
    public Boolean getPropagateOperStatus() {
        return propagateOperStatus;
    }

    /**
     * @param propagateOperStatus the propagateOperStatus to set
     */
    public void setPropagateOperStatus(Boolean propagateOperStatus) {
        this.propagateOperStatus = propagateOperStatus;
    }

    /**
     * @return the operationalStatus
     */
    public String getOperationalStatus() {
        return operationalStatus;
    }

    /**
     * @param operationalStatus the operationalStatus to set
     */
    public void setOperationalStatus(String operationalStatus) {
        this.operationalStatus = operationalStatus;
    }

    /**
     * @return the fromNodeAddress
     */
    public String getFromNodeAddress() {
        return fromNodeAddress;
    }

    /**
     * @param fromNodeAddress the fromNodeAddress to set
     */
    public void setFromNodeAddress(String fromNodeAddress) {
        this.fromNodeAddress = fromNodeAddress;
    }

    /**
     * @return the toNodeAddress
     */
    public String getToNodeAddress() {
        return toNodeAddress;
    }

    /**
     * @param toNodeAddress the toNodeAddress to set
     */
    public void setToNodeAddress(String toNodeAddress) {
        this.toNodeAddress = toNodeAddress;
    }

    /**
     * @return the nodeAddress
     */
    public String getNodeAddress() {
        return nodeAddress;
    }

    /**
     * @param nodeAddress the nodeAddress to set
     */
    public void setNodeAddress(String nodeAddress) {
        this.nodeAddress = nodeAddress;
    }

    /**
     * @return the fromId
     */
    public String getFromId() {
        return fromId;
    }

    /**
     * @param fromId the fromId to set
     */
    public void setFromId(String fromId) {
        this.fromId = fromId;
    }

    /**
     * @return the toId
     */
    public String getToId() {
        return toId;
    }

    /**
     * @param toId the toId to set
     */
    public void setToId(String toId) {
        this.toId = toId;
    }
}
