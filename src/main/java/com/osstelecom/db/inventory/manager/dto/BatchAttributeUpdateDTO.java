/*
 * Copyright (C) 2023 Lucas Nishimura <lucas.nishimura@gmail.com>
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

import java.util.HashMap;

/**
 * DTO para solicitação de Atualizações Massivas
 *
 * @author Lucas Nishimura
 * @created 26.07.2023
 */
public class BatchAttributeUpdateDTO {

    private FilterDTO filter;
    private HashMap<String, Object> attributes;
    private Long updatedObjectCound = 0L;
    private Long errorsCounter = 0L;
    private Long totalObjects = 0L;

    public Long getTotalObjects() {
        return totalObjects;
    }

    public void setTotalObjects(Long totalObjects) {
        this.totalObjects = totalObjects;
    }

    public Long addError() {
        return this.errorsCounter++;
    }

    public Long addUpdatedObject() {
        return this.updatedObjectCound++;
    }

    public Long getUpdatedObjectCound() {
        return updatedObjectCound;
    }

    public void setUpdatedObjectCound(Long updatedObjectCound) {
        this.updatedObjectCound = updatedObjectCound;
    }

    public Long getErrorsCounter() {
        return errorsCounter;
    }

    public void setErrorsCounter(Long errorsCounter) {
        this.errorsCounter = errorsCounter;
    }

    public FilterDTO getFilter() {
        return filter;
    }

    public void setFilter(FilterDTO filter) {
        this.filter = filter;
    }

    public HashMap<String, Object> getAttributes() {
        return attributes;
    }

    public void setAttributes(HashMap<String, Object> attributes) {
        this.attributes = attributes;
    }

}
