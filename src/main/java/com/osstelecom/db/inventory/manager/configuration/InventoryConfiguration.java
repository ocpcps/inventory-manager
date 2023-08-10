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
package com.osstelecom.db.inventory.manager.configuration;

/**
 * Representa a configuração do microserviço
 *
 * @author Lucas Nishimura
 * @created 14.12.2021
 */
public class InventoryConfiguration {

    private MongoDBConfiguration mongoDbConfiguration = new MongoDBConfiguration();
    private ArangoDBConfiguration graphDbConfiguration = new ArangoDBConfiguration();

    private String schemaDir = "samples/schema";
    private String iconsDir = "icons/";
    private String rulesDir = "rules/";
    private String dateFormat = "dd-MM-yyyy";
    private String dateTimeFormat = "dd-MM-yyyy hh:MM:ss";
    private Boolean consumableMetricsEnabled = false;
    /**
     * Diz se vamos avaliar as regras dinamicas do groovy, Habilitar reduz a
     * performance
     */
    private Boolean dynamicRulesEnabled = false;
    /**
     * Tempo em segundos do Cache
     */
    private Integer schemaCacheTTL = 300;
    private boolean trackTimers = true;

    /**
     * @return the mongoDbConfiguration
     */
    public MongoDBConfiguration getMongoDbConfiguration() {
        return mongoDbConfiguration;
    }

    /**
     * @param mongoDbConfiguration the mongoDbConfiguration to set
     */
    public void setMongoDbConfiguration(MongoDBConfiguration mongoDbConfiguration) {
        this.mongoDbConfiguration = mongoDbConfiguration;
    }

    /**
     * @return the graphDbConfiguration
     */
    public ArangoDBConfiguration getGraphDbConfiguration() {
        return graphDbConfiguration;
    }

    /**
     * @param graphDbConfiguration the graphDbConfiguration to set
     */
    public void setGraphDbConfiguration(ArangoDBConfiguration graphDbConfiguration) {
        this.graphDbConfiguration = graphDbConfiguration;
    }

    /**
     * @return the schemaDir
     */
    public String getSchemaDir() {
        return schemaDir;
    }

    /**
     * @param schemaDir the schemaDir to set
     */
    public void setSchemaDir(String schemaDir) {
        this.schemaDir = schemaDir;
    }

    /**
     * @return the iconsDir
     */
    public String getIconsDir() {
        return iconsDir;
    }

    /**
     * @param iconsDir the iconsDir to set
     */
    public void setIconsDir(String iconsDir) {
        this.iconsDir = iconsDir;
    }

    /**
     * @return the rulesDir
     */
    public String getRulesDir() {
        return rulesDir;
    }

    /**
     * @param rulesDir the rulesDir to set
     */
    public void setRulesDir(String rulesDir) {
        this.rulesDir = rulesDir;
    }

    /**
     * @return the dateFormat
     */
    public String getDateFormat() {
        return dateFormat;
    }

    /**
     * @param dateFormat the dateFormat to set
     */
    public void setDateFormat(String dateFormat) {
        this.dateFormat = dateFormat;
    }

    /**
     * @return the dateTimeFormat
     */
    public String getDateTimeFormat() {
        return dateTimeFormat;
    }

    /**
     * @param dateTimeFormat the dateTimeFormat to set
     */
    public void setDateTimeFormat(String dateTimeFormat) {
        this.dateTimeFormat = dateTimeFormat;
    }

    /**
     * @return the trackTimers
     */
    public boolean getTrackTimers() {
        return trackTimers;
    }

    /**
     * @param trackTimers the trackTimers to set
     */
    public void setTrackTimers(boolean trackTimers) {
        this.trackTimers = trackTimers;
    }

    /**
     * @return the dynamicRulesEnabled
     */
    public Boolean getDynamicRulesEnabled() {
        return dynamicRulesEnabled;
    }

    /**
     * @param dynamicRulesEnabled the dynamicRulesEnabled to set
     */
    public void setDynamicRulesEnabled(Boolean dynamicRulesEnabled) {
        this.dynamicRulesEnabled = dynamicRulesEnabled;
    }

    /**
     * @return the schemaCacheTTL
     */
    public Integer getSchemaCacheTTL() {
        return schemaCacheTTL;
    }

    /**
     * @return the consumableMetricsEnabled
     */
    public Boolean getConsumableMetricsEnabled() {
        return consumableMetricsEnabled;
    }

    /**
     * @param consumableMetricsEnabled the consumableMetricsEnabled to set
     */
    public void setConsumableMetricsEnabled(Boolean consumableMetricsEnabled) {
        this.consumableMetricsEnabled = consumableMetricsEnabled;
    }
}
