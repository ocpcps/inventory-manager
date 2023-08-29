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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import io.swagger.v3.oas.annotations.media.Schema;

import java.sql.Date;

/**
 *
 * @author Lucas Nishimura
 * @created 04.01.2022
 */
@JsonInclude(Include.NON_NULL)
public class History {

    @Schema(example = "id do metodo")
    private String id;

    @Schema(example = "dominio")
    private String domain;

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Schema(example = "data e hora da modificacao")
    private Date time;

    public Date getTime() {
        return time;
    }

    public void setTime(Date time) {
        this.time = time;
    }

    @Schema(example = "quantidade de registros")
    private Long sequency;

    public Long getSequency() {
        return sequency;
    }

    public void setSequency(Long sequency) {
        this.sequency = sequency;
    }

    @Schema(example = "conteudo do resource/conenctions/circuit/service")
    private BasicResource content;

    public BasicResource getContent() {
        return content;
    }

    public void setContent(BasicResource content) {
        this.content = content;
    }

    public History(String id, String domain, Date time, Long sequency, BasicResource content) {
        this.id = id;
        this.domain = domain;
        this.time = time;
        this.sequency = sequency;
        this.content = content;
    }

    public History(Domain domain2, Object object) {
    }


}
