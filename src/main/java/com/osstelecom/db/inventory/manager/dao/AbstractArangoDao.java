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
package com.osstelecom.db.inventory.manager.dao;

import com.arangodb.ArangoDatabase;
import com.arangodb.entity.DocumentCreateEntity;
import com.arangodb.entity.DocumentDeleteEntity;
import com.arangodb.entity.DocumentUpdateEntity;
import com.arangodb.model.AqlQueryOptions;
import com.osstelecom.db.inventory.graph.arango.GraphList;
import com.osstelecom.db.inventory.manager.dto.DomainDTO;
import com.osstelecom.db.inventory.manager.exception.BasicException;
import com.osstelecom.db.inventory.manager.exception.ResourceNotFoundException;
import com.osstelecom.db.inventory.manager.resources.BasicResource;
import com.osstelecom.db.inventory.manager.resources.ManagedResource;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Lucas Nishimura <lucas.nishimura@gmail.com>
 * @param <T>
 * @created 30.08.2022
 */
public abstract class AbstractArangoDao<T extends BasicResource> {

    private Logger logger = LoggerFactory.getLogger(AbstractArangoDao.class);

    public abstract T findResource(T resource) throws BasicException;

    public abstract DocumentCreateEntity<T> insertResource(T resource) throws BasicException;

    public abstract DocumentCreateEntity<T> upsertResource(T resource) throws BasicException;

    public abstract DocumentUpdateEntity<T> updateResource(T resource) throws BasicException;

    public abstract DocumentDeleteEntity<ManagedResource> deleteResource(T resource) throws BasicException;

    public abstract GraphList<T> findResourcesBySchemaName(String schemaName, DomainDTO domain) throws BasicException;

    public abstract GraphList<T> findResourcesByClassName(String className, DomainDTO domain) throws BasicException;

    public abstract GraphList<T> findResourceByFilter(String aql, Map<String, Object> bindVars, DomainDTO domain) throws BasicException;

    protected String buildAqlFromBindings(String aql, Map<String, Object> bindVars) {
        StringBuffer buffer = new StringBuffer(aql);
        bindVars.forEach((k, v) -> {
            if (!k.equalsIgnoreCase("domainName")) {
                //
                // Deal with list types
                //
                if (v instanceof List || v.getClass().isArray()) {
                    buffer.append(" and doc." + k + " in @" + k);
                } else {
                    buffer.append(" and doc." + k + " == @" + k);
                }
            }
        });
        buffer.append(" return doc");
        return buffer.toString();
    }

    /**
     * Queria deixar o Type como Generics....
     *
     * @param aql
     * @param bindVars
     * @param type
     * @param db
     * @return
     */
    public GraphList<T> query(String aql, Map<String, Object> bindVars, Class<T> type, ArangoDatabase db) throws ResourceNotFoundException {
        logger.info("(query) RUNNING: AQL:[{}]", aql);
        bindVars.forEach((k, v) -> {
            logger.info("\t  [@{}]=[{}]", k, v);

        });
        GraphList<T> result = new GraphList<T>(db.query(aql, bindVars, new AqlQueryOptions().fullCount(true).count(true), type));
        if (result.isEmpty()) {
            ResourceNotFoundException ex = new ResourceNotFoundException();
            //
            // Create a Detail map to the user
            //
            ex.addDetails("AQL", aql);
            ex.addDetails("params", bindVars);
            throw ex;
        }
        return result;
    }
}
