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
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.osstelecom.db.inventory.manager.resources.exception.MetricConstraintException;

/**
 * Classe que representa uma métrica
 *
 * @author Lucas Nishimura
 */
@JsonInclude(Include.NON_NULL)
public class ConsumableMetric {

    @DocumentField(DocumentField.Type.ID)
    private String id;
    @DocumentField(DocumentField.Type.KEY)
    private String key;

    private Domain domain;    
    private String domainName;

    private String metricName;
    private String metricShort;
    private String metricDescription;
    private Double metricValue = 0D;
    private Double minValue;
    private Double maxValue;
    private Double unitValue;
    private String category;

    /**
     * @return the _id
     */
    public String getId() {
        return id;
    }

    /**
     * @param id the _id to set
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * @param _key the _id to set
     */
    public void setKey(String key) {
        this.key = key;
    }

    /**
     * @return the _key
     */
    public String getKey() {
        return this.key;
    }

    /**
     * @param domain the domain to set
     */
    public void setDomain(Domain domain) {
        this.domain = domain;
        if(domain != null){
            domainName = domain.getDomainName();
        }
    }

    /**
     * @return the domain
     */
    public Domain getDomain() {
        return domain;
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
     * @return the metricName
     */
    public String getMetricName() {
        return metricName;
    }

    /**
     * @param metricName the metricName to set
     */
    public void setMetricName(String metricName) {
        this.metricName = metricName;
    }

    /**
     * @return the metricShort
     */
    public String getMetricShort() {
        return metricShort;
    }

    /**
     * @param metricShort the metricShort to set
     */
    public void setMetricShort(String metricShort) {
        this.metricShort = metricShort;
    }

    /**
     * @return the metricDescription
     */
    public String getMetricDescription() {
        return metricDescription;
    }

    /**
     * @param metricDescription the metricDescription to set
     */
    public void setMetricDescription(String metricDescription) {
        this.metricDescription = metricDescription;
    }

    /**
     * @return the metricValue
     */
    public Double getMetricValue() {
        return metricValue;
    }

    /**
     * @param metricValue the metricValue to set
     * @throws
     * com.osstelecom.db.inventory.manager.resources.exception.MetricConstraintException
     */
    public void setMetricValue(Double metricValue) throws MetricConstraintException {
        if (minValue != null && metricValue < minValue) {
            throw new MetricConstraintException("Metric is Less than Acceptable: Proposed: [" + metricValue + "] Current: [" + this.metricValue + "] Min: [" + this.minValue + "]");
        }

        if (maxValue != null && metricValue > maxValue) {
            throw new MetricConstraintException("Metric Value Exceeds Acceptable: Proposed: [" + metricValue + "] Current: [" + this.metricValue + "] Max:[" + this.maxValue + "]");
        }

        this.metricValue = metricValue;
    }

    /**
     * @return the minValue
     */
    public Double getMinValue() {
        return minValue;
    }

    /**
     * @param minValue the minValue to set
     */
    public void setMinValue(Double minValue) {
        this.minValue = minValue;
    }

    /**
     * @return the maxValue
     */
    public Double getMaxValue() {
        return maxValue;
    }

    /**
     * @param maxValue the maxValue to set
     */
    public void setMaxValue(Double maxValue) {
        this.maxValue = maxValue;
    }

    /**
     * @return the unitValue
     */
    public Double getUnitValue() {
        return unitValue;
    }

    /**
     * @param unitValue the unitValue to set
     */
    public void setUnitValue(Double unitValue) {
        this.unitValue = unitValue;
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
}
