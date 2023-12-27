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
package com.osstelecom.db.inventory.manager.request;

import com.osstelecom.db.inventory.manager.dto.FilterDTO;
import com.osstelecom.db.inventory.manager.resources.Domain;

/**
 *
 * @author Lucas Nishimura
 * @created 26.01.2022
 */
public class FilterRequest extends BasicRequest<FilterDTO> {

    public FilterRequest() {
    }

    public FilterRequest(FilterDTO filter) {
        this.setPayLoad(filter);
    }

    public FilterRequest(FilterDTO filter, Domain domain) {
        this.setPayLoad(filter);
        this.setRequestDomain(domain.getDomainName());
    }

    public FilterRequest(FilterDTO filter, Domain domain, String... objects) {
        this.setPayLoad(filter);
        this.setRequestDomain(domain.getDomainName());
        for (String object:objects){
            this.getPayLoad().getObjects().add(object);
        }
    }

}
