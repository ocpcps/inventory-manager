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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.arangodb.ArangoCollection;
import com.arangodb.ArangoCursor;
import com.arangodb.ArangoDatabase;
import com.arangodb.entity.DocumentCreateEntity;
import com.arangodb.entity.DocumentDeleteEntity;
import com.arangodb.entity.DocumentUpdateEntity;
import com.arangodb.entity.MultiDocumentEntity;
import com.arangodb.model.AqlQueryOptions;
import com.osstelecom.db.inventory.manager.dto.FilterDTO;
import com.osstelecom.db.inventory.manager.exception.ArangoDaoException;
import com.osstelecom.db.inventory.manager.exception.BasicException;
import com.osstelecom.db.inventory.manager.exception.InvalidRequestException;
import com.osstelecom.db.inventory.manager.exception.ResourceNotFoundException;
import com.osstelecom.db.inventory.manager.resources.BasicResource;
import com.osstelecom.db.inventory.manager.resources.Domain;
import com.osstelecom.db.inventory.manager.resources.GraphList;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Dao Abstrata do Arango DB
 *
 * @author Lucas Nishimura
 * @param <T>
 * @created 30.08.2022
 */
public abstract class AbstractArangoDao<T extends BasicResource> {

    @Autowired
    private ArangoDatabase arangoDatabase;

    protected Logger logger = LoggerFactory.getLogger(AbstractArangoDao.class);

    public abstract T findResource(T resource)
            throws ArangoDaoException, ResourceNotFoundException, InvalidRequestException;

    public abstract DocumentCreateEntity<T> insertResource(T resource)
            throws ArangoDaoException, ResourceNotFoundException, InvalidRequestException;

    public abstract DocumentCreateEntity<T> upsertResource(T resource)
            throws ArangoDaoException, ResourceNotFoundException, InvalidRequestException;

    public abstract DocumentUpdateEntity<T> updateResource(T resource)
            throws ArangoDaoException, ResourceNotFoundException, InvalidRequestException;

    public abstract MultiDocumentEntity<DocumentUpdateEntity<T>> updateResources(List<T> resources, Domain domain)
            throws BasicException;

    public abstract DocumentDeleteEntity<T> deleteResource(T resource) throws BasicException;

    public abstract GraphList<T> findResourcesBySchemaName(String schemaName, Domain domain)
            throws ArangoDaoException, ResourceNotFoundException, InvalidRequestException;

    public abstract GraphList<T> findResourcesByClassName(String className, Domain domain)
            throws ArangoDaoException, ResourceNotFoundException, InvalidRequestException;

    public abstract GraphList<T> findAll(Domain domain)
            throws ArangoDaoException, ResourceNotFoundException, InvalidRequestException;

    public abstract GraphList<T> findResourceByFilter(FilterDTO filter, Domain domain)
            throws ArangoDaoException, ResourceNotFoundException, InvalidRequestException;

    protected String buildAqlFromBindings(String aql, Map<String, Object> bindVars, boolean appendReturn) {
        StringBuilder buffer = new StringBuilder(aql);
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
        if (appendReturn) {
            buffer.append(" return doc");
        }
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
     * @throws
     * com.osstelecom.db.inventory.manager.exception.ResourceNotFoundException
     */
    public GraphList<T> runNativeQuery(String aql, Map<String, Object> bindVars, Class<T> type, ArangoDatabase db)
            throws ResourceNotFoundException {
        Long start = System.currentTimeMillis();
        String uid = UUID.randomUUID().toString();
        logger.info("(native-query) - [{}] - RUNNING: AQL:[{}]", uid, aql);
        if (bindVars != null) {
            bindVars.forEach((k, v) -> {
                logger.info("\t  [@{}]=[{}]", k, v);
            });

            GraphList<T> result = new GraphList<>(
                    db.query(aql, bindVars, new AqlQueryOptions().fullCount(true).count(true), type));

            if (result.isEmpty()) {
                ResourceNotFoundException ex = new ResourceNotFoundException();
                //
                // Create a Detail map to the user
                //
                try {
                    result.close();
                } catch (Exception e) {
                    logger.error("close cursos error when empty response", e);
                }
                ex.addDetails("AQL", aql);
                ex.addDetails("params", bindVars);
                throw ex;
            }
            Long end = System.currentTimeMillis();
            Long took = end - start;
            //
            // Se for maior que 100ms, avaliar
            //
            if (took > 100) {
                logger.warn("(query) - [{}] - Took: [{}] ms", uid, took);
            } else {
                logger.info("(query) - [{}] - Took: [{}] ms", uid, took);
            }
            return result;
        } else {
            GraphList<T> result = new GraphList<>(
                    db.query(aql, new AqlQueryOptions().fullCount(true).count(true), type));
            if (result.isEmpty()) {
                ResourceNotFoundException ex = new ResourceNotFoundException();
                //
                // Create a Detail map to the user
                //
                ex.addDetails("AQL", aql);
                ex.addDetails("params", "NO_PARAMS");
                throw ex;
            }
            return result;
        }

    }

    public String runNativeQuery(FilterDTO filter) {
        String aql = filter.getAqlFilter();
        Map<String, Object> bindVars = filter.getBindings();

        Long start = System.currentTimeMillis();
        String uid = UUID.randomUUID().toString();
        String buffer = "[";

        if (filter.getOffSet() >= 0L) {
            filter.getBindings().put("offset", filter.getOffSet());
        }
        if (filter.getLimit() >= 0L) {
            filter.getBindings().put("limit", filter.getLimit());
        }

        logger.info("(native-query) - [{}] - RUNNING: AQL:[{}]", uid, aql);
        if (bindVars != null) {
            bindVars.forEach((k, v) -> {
                logger.info("\t  [@{}]=[{}]", k, v);
            });
        }

        ArangoCursor<String> result = this.getDb().query(aql, bindVars,
                new AqlQueryOptions().fullCount(true).count(true), String.class);
        if (result.getCount() > 0) {
            buffer = buffer.concat(result.stream().collect(Collectors.joining(", ")));
            try {
                result.close();
            } catch (Exception e) {
                logger.error("close cursor error when empty response", e);
            }
        }

        Long end = System.currentTimeMillis();
        Long took = end - start;
        //
        // Se for maior que 100ms, avaliar
        //
        if (took > 100) {
            logger.warn("(query) - [{}] - Took: [{}] ms", uid, took);
        } else {
            logger.info("(query) - [{}] - Took: [{}] ms", uid, took);
        }

        buffer = buffer.concat("]");
        return buffer;
    }

    /**
     *
     * @param filter
     * @param type
     * @param db
     * @return
     * @throws ResourceNotFoundException
     */
    public GraphList<T> query(FilterDTO filter, Class<T> type)
            throws ResourceNotFoundException, InvalidRequestException {
        Long start = System.currentTimeMillis();
        String uid = UUID.randomUUID().toString();

        if (filter.getBindings() != null) {
            filter.getBindings().forEach((k, v) -> {
                logger.info("\t  [@{}]=[{}]", k, v);
            });

            //
            // Trata a paginação
            //
            if (filter.getOffSet() != null && filter.getLimit() != null) {
                if (filter.getOffSet() >= 0L && filter.getLimit() >= 0L) {
                    filter.setAqlFilter(filter.getAqlFilter() + " limit @offset,@limit");
                    filter.getBindings().put("offset", filter.getOffSet());
                    filter.getBindings().put("limit", filter.getLimit());
                } else {
                    //
                    // Negative value means pagination is disabled
                    //
                }
            }

            logger.info("(query) - [{}] - RUNNING: AQL:[{}]", uid, filter.getAqlFilter() + " [return doc] < -- Only for logging,");
            if (filter.getAqlFilter().toLowerCase().contains("return")) {
                throw new InvalidRequestException("Please remove Return statement from your filter.");
            }
            filter.setAqlFilter(filter.getAqlFilter() + " return doc");

            filter.getBindings().forEach((k, v) -> {
                logger.info("\t  [@{}]=[{}]", k, v);
            });
            GraphList<T> result = new GraphList<>(
                    this.arangoDatabase.query(filter.getAqlFilter(), filter.getBindings(),
                            new AqlQueryOptions().fullCount(true).count(true).ttl(1800), type));

            if (result.isEmpty()) {
                ResourceNotFoundException ex = new ResourceNotFoundException();
                //
                // Create a Detail map to the user
                //
                try {
                    result.close();
                } catch (Exception e) {
                    logger.error("close cursos error when empty response", e);
                }

                ex.addDetails("AQL", filter.getAqlFilter());
                ex.addDetails("params", filter.getBindings());

                throw ex;
            }
            Long end = System.currentTimeMillis();
            Long took = end - start;
            //
            // Se for maior que 100ms, avaliar
            //
            if (took > 100) {
                logger.warn("(query) - [{}] - Took: [{}] ms", uid, took);
            } else {
                logger.info("(query) - [{}] - Took: [{}] ms", uid, took);
            }
            return result;
        } else {
            /**
             * Alterado em 21/07/2023 para fechar o cursor em caso de não
             * encontrar nada e também para lançar a exception
             * ResourceNotFoundException
             */
            GraphList<T> result = new GraphList<>(
                    this.arangoDatabase.query(filter.getAqlFilter(), new AqlQueryOptions().fullCount(true).count(true), type));
            if (result.isEmpty()) {
                result.close();
                throw new ResourceNotFoundException().addDetails("AQL", filter.getAqlFilter());
            }
            return result;
        }
    }

    public abstract Long getCount(Domain domain) throws IOException, InvalidRequestException;

    public ArangoDatabase getDb() {
        return this.arangoDatabase;
    }

    public ArangoCollection getCollectionByName(String name) {
        return arangoDatabase.collection(name);
    }

    public List<T> getListFromCursorType(ArangoCursor<T> cursor) throws ArangoDaoException {
        List<T> result = new ArrayList<>();
        cursor.forEachRemaining(result::add);
        try {
            cursor.close();
        } catch (IOException ex) {
            throw new ArangoDaoException("Failed to Close Cursor:", ex);
        }
        return result;
    }
}
