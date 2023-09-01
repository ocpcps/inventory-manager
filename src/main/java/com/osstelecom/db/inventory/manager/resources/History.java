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
package com.osstelecom.db.inventory.manager.resources;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 *
 * @author Lucas Nishimura
 * @created 04.01.2022
 */
@JsonInclude(Include.NON_NULL)
public class History {

    private String id;
    private String key;
    private String reference;
    private LocalDateTime time;
    private Long sequency;
    private Domain domain;
    private String type;
    private BasicResource content;

    public History() {
    }

    public History(BasicResource content) {
        this.reference = content.getId();
        this.time = LocalDateTime.now();
        this.domain = content.getDomain();
        this.type = content.getClass().getSimpleName();
        this.content = content;
    }

    public History(String reference, String type, Domain domain) {
        this.reference = reference;
        this.domain = domain;
        this.type = type;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public LocalDateTime getTime() {
        return time;
    }

    public void setTime(LocalDateTime time) {
        this.time = time;
    }

    public Long getSequency() {
        return sequency;
    }

    public void setSequency(Long sequency) {
        this.sequency = sequency;
    }

    public Domain getDomain() {
        return domain;
    }

    public void setDomain(Domain domain) {
        this.domain = domain;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public BasicResource getContent() {
        return content;
    }

    public void setContent(BasicResource content) {
        this.content = content;
    }

}
