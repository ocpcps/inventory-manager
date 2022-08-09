/*
 * Copyright (C) 2021 Lucas Nishimura <lucas.nishimura@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.osstelecom.db.inventory.manager.resources;

import com.arangodb.entity.DocumentField;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.osstelecom.db.inventory.manager.dto.DomainDTO;
import com.osstelecom.db.inventory.manager.resources.exception.ConnectionAlreadyExistsException;
import com.osstelecom.db.inventory.manager.resources.exception.ConnectionNotFoundException;
import com.osstelecom.db.inventory.manager.resources.exception.MetricConstraintException;
import com.osstelecom.db.inventory.manager.resources.exception.NoResourcesAvailableException;
import com.osstelecom.db.inventory.manager.resources.model.ResourceSchemaModel;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import org.bson.codecs.pojo.annotations.BsonIgnore;

/**
 * Classe que representa um elemento básico
 *
 * @author Lucas Nishimura <lucas.nishimura@gmail.com>
 */
public class BasicResource {

    private DomainDTO domain;
    private String domainName;
    private Date installationDate;
    private Date activationDate;
    private Date inactivationDate;
    private Date lastModifiedDate;
    private Date insertedDate;
    private Boolean deleted;
    private Boolean readOnly;
    private Boolean isLeaf;
    private Boolean isConsumable;
    private Boolean isConsumer = false; //revisar
    private ConsumableMetric consumableMetric;
    private ConsumableMetric consumerMetric;

    private String name;
    private String description;
    private String nodeAddress;
    private String vendor;
    private String version;
    private String owner;
    private String author;
    private String resourceType;
    private String attributeSchemaName = "default";
    private String category;
    private String adminStatus;
    private String operationalStatus;
    private String businessStatus;
    private String node;
    private ArrayList<String> tags;
    @DocumentField(DocumentField.Type.KEY)
    private String uid;
    @DocumentField(DocumentField.Type.ID)
    private String id;
    private String className = "Default";
    private ConcurrentHashMap<String, Object> attributes = new ConcurrentHashMap<>();
    /**
     * Cuidado com os campos a seguir!
     */
    @JsonIgnore
    @BsonIgnore
    private ConcurrentHashMap<String, ResourceConnection> connections = new ConcurrentHashMap<>();
    @JsonIgnore
    @BsonIgnore
    private ConcurrentHashMap<String, ResourceConnection> connectionCache = new ConcurrentHashMap<>();
    @JsonIgnore
    @BsonIgnore
    private ConcurrentHashMap<String, Object> attachments = new ConcurrentHashMap<>();

    private Long atomId = 0L;
    private ResourceSchemaModel schemaModel;
    @DocumentField(DocumentField.Type.REV)
    private String revisionId;

    public void addTag(String tag) {
        if (this.tags == null) {
            this.tags = new ArrayList<>();
        }
        if (!this.tags.contains(tag)) {
            this.tags.add(tag);
        }
    }

    public void removeTag(String tag) {
        if (this.tags.contains(tag)) {
            this.tags.remove(tag);
        }
        if (this.tags.isEmpty()){
            this.tags = null;
        }
    }

    /**
     * @param className the className to set
     */
    public void setClassName(String className) {
        this.className = className;
    }

    /**
     * @param domain the domain to set
     */
    public void setDomain(DomainDTO domain) {
        this.domain = domain;
        if (domain != null) {
            if (this.domainName == null) {
                this.domainName = domain.getDomainName();
            } else if (!this.domainName.equals(domain.getDomainName())) {
                this.domainName = domain.getDomainName();
            }
        }
    }

    /**
     * @return the domain
     */
    public DomainDTO getDomain() {
        return domain;
    }

    /**
     * @return the adminStatus
     */
    public String getAdminStatus() {
        return adminStatus;
    }

    /**
     * @param adminStatus the adminStatus to set
     */
    public void setAdminStatus(String adminStatus) {
        this.adminStatus = adminStatus;
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
     * @return the connectionCache
     */
    public ConcurrentHashMap<String, ResourceConnection> getConnectionCache() {
        return connectionCache;
    }

    /**
     * @param connectionCache the connectionCache to set
     */
    public void setConnectionCache(ConcurrentHashMap<String, ResourceConnection> connectionCache) {
        this.connectionCache = connectionCache;
    }

    /**
     * @return the atomId
     */
    public Long getAtomId() {
        return atomId;
    }

    public void setAtomId(Long id) {
        this.atomId = id;
    }

    public BasicResource(String attributeSchema, DomainDTO domain) {
        this.attributeSchemaName = attributeSchemaName;
        this.domain = domain;
    }

    public BasicResource(DomainDTO domain) {
        this.attributeSchemaName = "default";
        this.domain = domain;
    }

    public BasicResource() {

    }

    /**
     * Retorna a data de instalação do elemento
     *
     * @return the installationDate
     */
    public Date getInstallationDate() {
        return installationDate;
    }

    /**
     * Notifica o elemento que uma nova conexão foi criada.
     *
     * @param connection
     * @throws ConnectionAlreadyExistsException
     */
    public void notifyConnection(ResourceConnection connection) throws ConnectionAlreadyExistsException, MetricConstraintException, NoResourcesAvailableException {
        String connectionCacheKey = connection.getFrom() + "." + connection.getTo();

        if (getConnectionCache().containsKey(connectionCacheKey)) {
            ResourceConnection previousConnection = getConnectionCache().get(connectionCacheKey);
            throw new ConnectionAlreadyExistsException("This Object: [" + this.getUid() + "] already know connection with id: [" + previousConnection.getUid() + "] From: [" + previousConnection.getFrom() + "] To: [" + previousConnection.getTo() + "]");
        } else {
            getConnectionCache().put(connectionCacheKey, connection);
        }

        if (!connections.containsKey(connection.getUid())) {
            //
            // Varre se a conexão já existe..
            //
            this.getConnections().put(connection.getUid(), connection);
        } else {
            throw new ConnectionAlreadyExistsException("This Object: [" + this.getUid() + "] is Alredy Knows connection with id: [" + connection.getUid() + "]");
        }

        //
        // Temos um consumer :? 
        //
        if (connection.getTo() != null) {
            if (connection.getTo().getIsConsumer()) {
                if (connection.getFrom().getIsConsumable()) {

                    if (connection.getFrom().getConsumableMetric().getMetricValue() - connection.getTo().getConsumerMetric().getUnitValue() < connection.getFrom().getConsumableMetric().getMinValue()) {
                        throw new NoResourcesAvailableException("No Resouces Available on: " + connection.getFrom().getUid() + " Current: [" + connection.getFrom().getConsumableMetric().getMetricValue() + "] Needed: [" + connection.getTo().getConsumerMetric().getUnitValue() + "]");
                    }

                    //
                    // Temos recursos  para Consumir
                    //
                    connection.getFrom().getConsumableMetric().setMetricValue(connection.getFrom().getConsumableMetric().getMetricValue() - connection.getTo().getConsumerMetric().getUnitValue());
                }
            }
        }
    }

    /**
     * Notifica o Elemento que uma conexão foi desfeita
     *
     * @param connection
     * @throws ConnectionNotFoundException
     */
    public void notifyDisconnection(ResourceConnection connection) throws ConnectionNotFoundException {
        if (getConnections().containsKey(connection.getUid())) {
            getConnections().remove(connection.getUid());
        } else {
            throw new ConnectionNotFoundException("This Object: [" + this.getUid() + "] Does not know connection with id: [" + connection.getUid() + "]");
        }
    }

    /**
     *
     * @param installationDate the installationDate to set
     */
    public void setInstallationDate(Date installationDate) {
        this.installationDate = installationDate;
    }

    /**
     * @return the activationDate
     */
    public Date getActivationDate() {
        return activationDate;
    }

    /**
     * @param activationDate the activationDate to set
     */
    public void setActivationDate(Date activationDate) {
        this.activationDate = activationDate;
    }

    /**
     * @return the inactivationDate
     */
    public Date getInactivationDate() {
        return inactivationDate;
    }

    /**
     * @param inactivationDate the inactivationDate to set
     */
    public void setInactivationDate(Date inactivationDate) {
        this.inactivationDate = inactivationDate;
    }

    /**
     * @return the lastModifiedDate
     */
    public Date getLastModifiedDate() {
        return lastModifiedDate;
    }

    /**
     * @param lastModifiedDate the lastModifiedDate to set
     */
    public void setLastModifiedDate(Date lastModifiedDate) {
        this.lastModifiedDate = lastModifiedDate;
    }

    /**
     * @return the insertedDate
     */
    public Date getInsertedDate() {
        return insertedDate;
    }

    /**
     * @param insertedDate the insertedDate to set
     */
    public void setInsertedDate(Date insertedDate) {
        this.insertedDate = insertedDate;
    }

    /**
     * @return the deleted
     */
    public Boolean getDeleted() {
        return deleted;
    }

    /**
     * @param deleted the deleted to set
     */
    public void setDeleted(Boolean deleted) {
        this.deleted = deleted;
    }

    /**
     * @return the readOnly
     */
    public Boolean getReadOnly() {
        return readOnly;
    }

    /**
     * @param readOnly the readOnly to set
     */
    public void setReadOnly(Boolean readOnly) {
        this.readOnly = readOnly;
    }

    /**
     * @return the isLeaf
     */
    public Boolean getIsLeaf() {
        return isLeaf;
    }

    /**
     * @param isLeaf the isLeaf to set
     */
    public void setIsLeaf(Boolean isLeaf) {
        this.isLeaf = isLeaf;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the vendor
     */
    public String getVendor() {
        return vendor;
    }

    /**
     * @param vendor the vendor to set
     */
    public void setVendor(String vendor) {
        this.vendor = vendor;
    }

    /**
     * @return the version
     */
    public String getVersion() {
        return version;
    }

    /**
     * @param version the version to set
     */
    public void setVersion(String version) {
        this.version = version;
    }

    /**
     * @return the owner
     */
    public String getOwner() {
        return owner;
    }

    /**
     * @param owner the owner to set
     */
    public void setOwner(String owner) {
        this.owner = owner;
    }

    /**
     * @return the author
     */
    public String getAuthor() {
        return author;
    }

    /**
     * @param author the author to set
     */
    public void setAuthor(String author) {
        this.author = author;
    }

    /**
     * @return the resourceType
     */
    public String getResourceType() {
        return resourceType;
    }

    /**
     * @param resourceType the resourceType to set
     */
    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    /**
     * @return the _id
     */
    public String getUid() {
        return this.uid;
    }

    /**
     * @param _id the _id to set
     */
    public void setUid(String id) {
        this.uid = id;
//        this.id = id;
    }

    /**
     * @return the attributes
     */
    public ConcurrentHashMap<String, Object> getAttributes() {
        return attributes;
    }

    /**
     * @param attributes the attributes to set
     */
    public void setAttributes(ConcurrentHashMap<String, Object> attributes) {
        this.attributes = attributes;
    }

    /**
     * @return the connections
     */
    public ConcurrentHashMap<String, ResourceConnection> getConnections() {
        return connections;
    }

    /**
     * @param connections the connections to set
     */
    public void setConnections(ConcurrentHashMap<String, ResourceConnection> connections) {
        this.connections = connections;
    }

    /**
     * @return the isConsumable
     */
    public Boolean getIsConsumable() {
        return isConsumable;
    }

    /**
     * @param isConsumable the isConsumable to set
     */
    public void setIsConsumable(Boolean isConsumable) {
        this.isConsumable = isConsumable;
    }

    /**
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * @param description the description to set
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * @return the attributeSchema
     */
    public String getAttributeSchemaName() {
        return attributeSchemaName;
    }

    /**
     * @param attributeSchema the attributeSchema to set
     */
    public void setAttributeSchemaName(String attributeSchemaName) {
        this.attributeSchemaName = attributeSchemaName;
    }

    /**
     * @return the category
     */
    public String getCategory() {
        return category;
    }

    /**
     * @param category the category to set
     */
    public void setCategory(String category) {
        this.category = category;
    }

    /**
     * @return the noce
     */
    public String getNode() {
        return node;
    }

    /**
     * @param noce the noce to set
     */
    public void setNoce(String node) {
        this.setNode(node);
    }

    /**
     * @return the isConsumer
     */
    public Boolean getIsConsumer() {
        return isConsumer;
    }

    /**
     * @param isConsumer the isConsumer to set
     */
    public void setIsConsumer(Boolean isConsumer) {
        this.isConsumer = isConsumer;
    }

    /**
     * @return the consumableMetric
     */
    public ConsumableMetric getConsumableMetric() {
        return consumableMetric;
    }

    /**
     * @param consumableMetric the consumableMetric to set
     */
    public void setConsumableMetric(ConsumableMetric consumableMetric) {
        this.consumableMetric = consumableMetric;
    }

    /**
     * @return the consumerMetric
     */
    public ConsumableMetric getConsumerMetric() {
        return consumerMetric;
    }

    /**
     * @param consumerMetric the consumerMetric to set
     */
    public void setConsumerMetric(ConsumableMetric consumerMetric) {
        this.consumerMetric = consumerMetric;
    }

    /**
     * @return the attachments
     */
    public ConcurrentHashMap<String, Object> getAttachments() {
        return attachments;
    }

    /**
     * @param attachments the attachments to set
     */
    public void setAttachments(ConcurrentHashMap<String, Object> attachments) {
        this.attachments = attachments;
    }

    /**
     * @return the businessStatus
     */
    public String getBusinessStatus() {
        return businessStatus;
    }

    /**
     * @param businessStatus the businessStatus to set
     */
    public void setBusinessStatus(String businessStatus) {
        this.businessStatus = businessStatus;
    }

    /**
     * @return the className
     */
    public String getClassName() {
        return className;
    }

    /**
     * @return the schemaModel
     */
    public ResourceSchemaModel getSchemaModel() {
        return schemaModel;
    }

    /**
     * @param schemaModel the schemaModel to set
     */
    public void setSchemaModel(ResourceSchemaModel schemaModel) {
        this.schemaModel = schemaModel;
    }

    public String getObjectClass() {
        return this.getClass().getSimpleName();
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
     * @return the revisionId
     */
    public String getRevisionId() {
        return revisionId;
    }

    /**
     * @param revisionId the revisionId to set
     */
    public void setRevisionId(String revisionId) {
        this.revisionId = revisionId;
    }

    /**
     * @return the domainName
     */
    public String getDomainName() {
        return domainName;
    }

    /**
     * @param domainName the domainName to set
     */
    public void setDomainName(String domainName) {
        this.domainName = domainName;
    }

    /**
     * @param node the node to set
     */
    public void setNode(String node) {
        this.node = node;
    }

    /**
     * @return the id
     */
    public String getId() {
        return id;
    }

    /**
     * @param id the id to set
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Muito experimental, não mexer!!!
     */
//    @Override
//    public int hashCode() {
//        int hash = 5;
//        hash = 97 * hash + Objects.hashCode(this.domainName);
//        hash = 97 * hash + Objects.hashCode(this.nodeAddress);
//        hash = 97 * hash + Objects.hashCode(this.className);
//        return hash;
//    }
//
//    @Override
//    public boolean equals(Object obj) {
//        if (this == obj) {
//            return true;
//        }
//        if (obj == null) {
//            return false;
//        }
//        if (getClass() != obj.getClass()) {
//            return false;
//        }
//        final BasicResource other = (BasicResource) obj;
//        if (!Objects.equals(this.domainName, other.domainName)) {
//            return false;
//        }
//        if (!Objects.equals(this.nodeAddress, other.nodeAddress)) {
//            return false;
//        }
//        if (!Objects.equals(this.className, other.className)) {
//            return false;
//        }
//        return true;
//    }
}
