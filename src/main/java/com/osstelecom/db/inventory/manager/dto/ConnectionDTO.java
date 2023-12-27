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

import com.osstelecom.db.inventory.manager.resources.ServiceResource;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Representa o DTO de uma conexão
 *
 * @author Lucas Nishimura
 * @created 16.12.2021
 */
public class ConnectionDTO {

    @Schema(description = "Nome do recurso de origem da conexão")
    private String fromName;

    @Schema(description = "Endereço do nó do recurso de origem da conexão")
    private String fromNodeAddress;

    @Schema(description = "Endereço do nó do recurso de destino da conexão")
    private String toNodeAddress;

    @Schema(description = "Nome da classe do recurso de origem da conexão")
    private String fromClassName;

    @Schema(description = "ID do recurso de origem da conexão")
    private String fromId;

    @Schema(description = "Chave do documento do recurso de origem da conexão na coleção")
    private String fromKey;

    @Schema(description = "ID do recurso de destino da conexão")
    private String toId;

    @Schema(description = "Chave do documento do recurso de destino da conexão na coleção")
    private String toKey;

    @Schema(description = "Nome do recurso de destino da conexão")
    private String toName;

    @Schema(description = "Nome da classe do recurso de destino da conexão")
    private String toClassName;

    @Schema(description = "Nome da conexão")
    private String connectionName;

    @Schema(description = "Endereço do nó da conexão")
    private String nodeAddress;

    @Schema(description = "Classe da conexão")
    private String connectionClass = "connection.default";

    @Schema(description = "Nome da classe")
    private String className = "connection.default";

    @Schema(description = "Nome do schema de atributos")
    private String attributeSchemaName = "connection.default";

    @Schema(description = "Indica se o status operacional deve ser propagado")
    private Boolean propagateOperStatus;

    @Schema(description = "Status operacional da conexão")
    private String operationalStatus = "Up";

    @Schema(description = "Descrição da conexão")
    private String description;

    @Schema(description = "Categoria da conexão")
    private String category;

    @Schema(description = "Status administrativo da conexão")
    private String adminStatus = "Up";

    @Schema(description = "Status de negócio da conexão")
    private String businessStatus = "Up";

    @Schema(description = "Valores dos atributos da conexão")
    private Map<String, Object> attributes = new ConcurrentHashMap<>();

    @Schema(description = "Serviço dependente associado à conexão")
    private ServiceResource dependentService;

    @Schema(description = "Chave da Collection, é o ID da conexão")
    private String key;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

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

    /**
     * @param denpendsOnService the denpendsOnService to set
     */
    public void setDependentService(ServiceResource denpendsOnService) {
        this.dependentService = denpendsOnService;
    }

    public ServiceResource getDependentService() {
        return dependentService;
    }

    /**
     * @return the fromKey
     */
    public String getFromKey() {
        return fromKey;
    }

    /**
     * @param fromKey the fromKey to set
     */
    public void setFromKey(String fromKey) {
        this.fromKey = fromKey;
    }

    /**
     * @return the toKey
     */
    public String getToKey() {
        return toKey;
    }

    /**
     * @param toKey the toKey to set
     */
    public void setToKey(String toKey) {
        this.toKey = toKey;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getAdminStatus() {
        return adminStatus;
    }

    public void setAdminStatus(String adminStatus) {
        this.adminStatus = adminStatus;
    }

    public String getBusinessStatus() {
        return businessStatus;
    }

    public void setBusinessStatus(String businessStatus) {
        this.businessStatus = businessStatus;
    }

}
