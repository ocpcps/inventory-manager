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
package com.osstelecom.db.inventory.manager.resources.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Date;
import java.util.List;

/**
 * Classe que representa um Atributo
 *
 * @author Lucas Nishimura
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ResourceAttributeModel {

    private String _id;
    private String name;
    //
    // Nome do campo a ser mostrado no front
    //
    private String displayName;
    //
    // Diz se o campo vai ser exibido ou não
    //
    private boolean displayable = true;
    private boolean readOnly = false;
    private String variableType;
    private String description; //Updateble
    private String defaultValue;
    private List<String> allowedValues;
    private Boolean required;
    private Boolean isList = false;
    private Boolean trackChanges;
    private Integer minOccurrences;
    private Integer maxOccurrences;
    private String validationRegex;
    private String validationScript;
    private String validationPluginClass;
    private Boolean validate;
    private String itemHash;
    private Boolean doRemove;
    private Date creationDate;
    private Date lastUpdate;
    //
    //  Diz se é um atributo fornecido pelo discovery
    //
    private Boolean isDiscovery = false;

    //
    // Diz se é um atributo fornecido pelo usuário ou mesmo federado
    //
    private Boolean isUserAttribute = true;

    public String getVariableType() {
        return variableType;
    }

    public void setVariableType(String variableType) {
        this.variableType = variableType;
    }

    public Boolean getIsList() {
        return isList;
    }

    public void setIsList(Boolean isList) {
        this.isList = isList;
    }

    public Integer getMinOccurrences() {
        return minOccurrences;
    }

    public void setMinOccurrences(Integer minOccurrences) {
        this.minOccurrences = minOccurrences;
    }

    public Integer getMaxOccurrences() {
        return maxOccurrences;
    }

    public void setMaxOccurrences(Integer maxOccurrences) {
        this.maxOccurrences = maxOccurrences;
    }

    /**
     * @return the _id
     */
    public String getId() {
        return _id;
    }

    /**
     * @param _id the _id to set
     */
    public void setId(String _id) {
        this._id = _id;
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
     * @return the required
     */
    public Boolean getRequired() {
        return required;
    }

    /**
     * @param required the required to set
     */
    public void setRequired(Boolean required) {
        this.required = required;
    }

    /**
     * @return the validationRegex
     */
    public String getValidationRegex() {
        return validationRegex;
    }

    /**
     * @param validationRegex the validationRegex to set
     */
    public void setValidationRegex(String validationRegex) {
        this.validationRegex = validationRegex;
    }

    /**
     * @return the validationScript
     */
    public String getValidationScript() {
        return validationScript;
    }

    /**
     * @param validationScript the validationScript to set
     */
    public void setValidationScript(String validationScript) {
        this.validationScript = validationScript;
    }

    /**
     * @return the defaultValue
     */
    public String getDefaultValue() {
        return defaultValue;
    }

    /**
     * @param defaultValue the defaultValue to set
     */
    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    /**
     * @return the allowedValues
     */
    public List<String> getAllowedValues() {
        return allowedValues;
    }

    /**
     * @param allowedValues the allowedValues to set
     */
    public void setAllowedValues(List<String> allowedValues) {
        this.allowedValues = allowedValues;
    }

    /**
     * @return the trackChanges
     */
    public Boolean getTrackChanges() {
        return trackChanges;
    }

    /**
     * @param trackChanges the trackChanges to set
     */
    public void setTrackChanges(Boolean trackChanges) {
        this.trackChanges = trackChanges;
    }

    /**
     * @return the validationPluginClass
     */
    public String getValidationPluginClass() {
        return validationPluginClass;
    }

    /**
     * @param validationPluginClass the validationPluginClass to set
     */
    public void setValidationPluginClass(String validationPluginClass) {
        this.validationPluginClass = validationPluginClass;
    }

    /**
     * @return the validate
     */
    public Boolean getValidate() {
        return validate;
    }

    /**
     * @param validate the validate to set
     */
    public void setValidate(Boolean validate) {
        this.validate = validate;
    }

    /**
     * @return the itemHash
     */
    public String getItemHash() {
        return itemHash;
    }

    /**
     * @param itemHash the itemHash to set
     */
    public void setItemHash(String itemHash) {
        this.itemHash = itemHash;
    }

    /**
     * @return the doRemove
     */
    public Boolean getDoRemove() {
        return doRemove;
    }

    /**
     * @param doRemove the doRemove to set
     */
    public void setDoRemove(Boolean doRemove) {
        this.doRemove = doRemove;
    }

    /**
     * @return the isDiscovery
     */
    public Boolean getIsDiscovery() {
        return isDiscovery;
    }

    /**
     * @param isDiscovery the isDiscovery to set
     */
    public void setIsDiscovery(Boolean isDiscovery) {
        this.isDiscovery = isDiscovery;
    }

    /**
     * @return the displayName
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * @param displayName the displayName to set
     */
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    /**
     * @return the displayable
     */
    public boolean isDisplayable() {
        return displayable;
    }

    /**
     * @param displayable the displayable to set
     */
    public void setDisplayable(boolean displayable) {
        this.displayable = displayable;
    }

    /**
     * @return the creationDate
     */
    public Date getCreationDate() {
        return creationDate;
    }

    /**
     * @param creationDate the creationDate to set
     */
    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }

    /**
     * @return the lastUpdate
     */
    public Date getLastUpdate() {
        return lastUpdate;
    }

    /**
     * @param lastUpdate the lastUpdate to set
     */
    public void setLastUpdate(Date lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

    /**
     * @return the readOnly
     */
    public boolean isReadOnly() {
        return readOnly;
    }

    /**
     * @param readOnly the readOnly to set
     */
    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
    }

}
