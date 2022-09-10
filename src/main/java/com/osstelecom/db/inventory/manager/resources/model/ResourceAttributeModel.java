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
import java.util.ArrayList;

/**
 * Classe que representa um Atributo
 *
 * @author Lucas Nishimura <lucas.nishimura@gmail.com>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ResourceAttributeModel {

    private String _id;
    private String name;
    private String variableType;
    private String description; //Updateble
    private String defaultValue;
    private ArrayList<String> allowedValues;
    private Boolean required;
    private Boolean isList;
    private Boolean trackChanges;
    private Integer minOccurrences;
    private Integer maxOccurrences;
    private String validationRegex;
    private String validationScript;
    private String validationPluginClass;
    private Boolean validate;
    private String itemHash;
    private Boolean doRemove;

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
    public ArrayList<String> getAllowedValues() {
        return allowedValues;
    }

    /**
     * @param allowedValues the allowedValues to set
     */
    public void setAllowedValues(ArrayList<String> allowedValues) {
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

}
