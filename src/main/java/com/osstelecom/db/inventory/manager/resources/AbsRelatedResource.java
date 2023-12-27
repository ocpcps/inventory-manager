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

/**
 *
 * @author Lucas Nishimura
 * @created 20.09.2022
 */
public abstract class AbsRelatedResource extends BasicResource {

    //
    // ID Do Serviço que ele depende.
    //
    private ServiceResource dependentService;

    public AbsRelatedResource(String domainName, String id) {
        super(domainName, id);
    }

    public AbsRelatedResource(String attributeSchema, Domain domain) {
        super(attributeSchema, domain);
    }

    public AbsRelatedResource(Domain domain) {
        super(domain);
    }

    public AbsRelatedResource(Domain domain, String key, String id) {
        super(domain, key, id);
    }

    public AbsRelatedResource(Domain domain, String name, String nodeAddress, String className) {
        super(domain, name, nodeAddress, className);
    }

    public AbsRelatedResource(Domain domain, String id) {
        super(domain, id);
    }

    public AbsRelatedResource() {
    }

    /**
     * @return the dependentService
     */
    public ServiceResource getDependentService() {
        return dependentService;
    }

    /**
     * @param dependentService the dependentService to set
     */
    public void setDependentService(ServiceResource dependentService) {
        this.dependentService = dependentService;
    }

}
