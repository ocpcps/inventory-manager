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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.osstelecom.db.inventory.manager.operation.DomainManager;
import com.osstelecom.db.inventory.manager.resources.exception.MetricConstraintException;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Classe que representa uma métrica
 *
 * @author Lucas Nishimura <lucas.nishimura@gmail.com>
 */
public class ConsumableMetric {

    private final DomainManager domain;
    @Schema(description = "The Consumable Metric Name", example = "Megabytes,Gigabytes,Meters,Units,Kilemeters")
    private String metricName;
    @Schema(description = "The Consumable Metric String code, or abreviation", example = "MB,GB,M,U,KM")
    private String metricShort;
    @Schema(description = "The Consumable Metric Description", example = "The amount of Megabytes configured")
    private String metricDescription;
    @Schema(description = "The Consumable Metric Value", example = "1024")
    private Double metricValue = 0D;
    @Schema(description = "The Consumable Metric Minimum Value", example = "1")
    private Double minValue;
    @Schema(description = "The Consumable Metric Maximun Value", example = "10240")
    private Double maxValue;
    @Schema(description = "The Consumable Metric Unity value", example = "1")
    private Double unitValue;
     @Schema(description = "The Consumable Metric Category", example = "Interface Speed")
    private String category;

    public ConsumableMetric(DomainManager domain) {
        this.domain = domain;
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
        if (minValue != null) {
            if (metricValue < minValue) {
                throw new MetricConstraintException("Metric is Less than Acceptable: Proposed: [" + metricValue + "] Current: [" + this.metricValue + "] Min: [" + this.minValue + "]");
            }
        }

        if (maxValue != null) {
            if (metricValue > maxValue) {
                throw new MetricConstraintException("Metric Value Exceeds Acceptable: Proposed: [" + metricValue + "] Current: [" + this.metricValue + "] Max:[" + this.maxValue + "]");
            }
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
