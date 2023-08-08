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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.arangodb.ArangoDatabase;
import com.arangodb.entity.DocumentCreateEntity;
import com.arangodb.entity.DocumentDeleteEntity;
import com.arangodb.entity.DocumentUpdateEntity;
import com.arangodb.model.AqlQueryOptions;
import com.arangodb.model.DocumentCreateOptions;
import com.arangodb.model.DocumentDeleteOptions;
import com.arangodb.model.DocumentUpdateOptions;
import com.arangodb.model.OverwriteMode;
import com.osstelecom.db.inventory.manager.dto.FilterDTO;
import com.osstelecom.db.inventory.manager.exception.ArangoDaoException;
import com.osstelecom.db.inventory.manager.exception.InvalidRequestException;
import com.osstelecom.db.inventory.manager.exception.ResourceNotFoundException;
import com.osstelecom.db.inventory.manager.resources.BasicResource;
import com.osstelecom.db.inventory.manager.resources.ConsumableMetric;
import com.osstelecom.db.inventory.manager.resources.Domain;
import com.osstelecom.db.inventory.manager.resources.GraphList;

/**
 * Representa a dao do Consumable Metric
 *
 * @author Lucas Nishimura
 * @created 30.08.2022
 */
@Component
public class ConsumableMetricDao {

    @Autowired
    private ArangoDatabase arangoDatabase;

    protected Logger logger = LoggerFactory.getLogger(ConsumableMetricDao.class);

    public ConsumableMetric findConsumableMetric(ConsumableMetric consumableMetric)
            throws ArangoDaoException, ResourceNotFoundException, InvalidRequestException {
        try {
            //
            // Pensar no Lock Manager aqui, ou subir para o manager
            //
            //
            // Domain is mandatory! make sure you set the domain before handling to dao
            //

            if (consumableMetric.getDomain() == null) {
                throw new ArangoDaoException("Missing Domain Information for Resource");
            }

            String aql = " for doc in `" + consumableMetric.getDomain().getMetrics() + "` filter ";

            Map<String, Object> bindVars = new HashMap<>();
            aql += " doc.domainName == @domainName";

            bindVars.put("domainName", consumableMetric.getDomain().getDomainName());

            if (consumableMetric.getMetricName() != null) {
                bindVars.put("metricName", consumableMetric.getMetricName());
            }

            if (consumableMetric.getCategory() != null) {
                bindVars.put("category", consumableMetric.getCategory());
            }

            if (consumableMetric.getMetricShort() != null) {
                bindVars.put("metricShort", consumableMetric.getMetricShort());
            }

            aql = this.buildAqlFromBindings(aql, bindVars, false);
            FilterDTO filter = new FilterDTO(aql, bindVars);
            GraphList<ConsumableMetric> result = this.query(filter, this.getDb());

            return result.getOne();
        } catch (ResourceNotFoundException | InvalidRequestException ex) {
            //
            // Sobe as exceptions conhecidass
            //
            throw ex;
        } catch (Exception ex) {
            //
            // Encapsula as não conhecidas.
            //

            throw new ArangoDaoException(ex);
        } finally {
            //
            // Liberar o Lock manager Aqui, ou subir para o manager
            //
        }

    }

    public DocumentCreateEntity<ConsumableMetric> insertConsumableMetric(ConsumableMetric consumableMetric)
            throws ArangoDaoException {
        //
        // A complexidade de validação dos requistos do dado deve ter sido feita na dao
        // antes de chegar aqui.
        //
        try {
            return this.getDb().collection(consumableMetric.getDomain().getMetrics()).insertDocument(consumableMetric,
                    new DocumentCreateOptions().returnNew(true).returnOld(true));
        } catch (Exception ex) {
            throw new ArangoDaoException(ex.getMessage(), ex);
        } finally {
            //
            // Liberar o Lock manager Aqui, ou subir para o manager
            //
        }
    }

    public DocumentUpdateEntity<ConsumableMetric> updateConsumableMetric(ConsumableMetric consumableMetric)
            throws ArangoDaoException {
        //
        // A complexidade de validação dos requistos do dado deve ter sido feita na dao
        // antes de chegar aqui.
        //
        try {
            return this.getDb().collection(consumableMetric.getDomain().getMetrics())
                    .updateDocument(consumableMetric.getMetricName(), consumableMetric, new DocumentUpdateOptions()
                            .returnNew(true).returnOld(true).mergeObjects(false).keepNull(false).waitForSync(false),
                            ConsumableMetric.class);
        } catch (Exception ex) {
            throw new ArangoDaoException(ex);
        } finally {
            //
            // Liberar o Lock manager Aqui, ou subir para o manager
            //
        }
    }

    public DocumentCreateEntity<ConsumableMetric> upsertConsumableMetric(ConsumableMetric consumableMetric)
            throws ArangoDaoException {
        //
        // A complexidade de validação dos requistos do dado deve ter sido feita na dao
        // antes de chegar aqui.
        //

        try {
            return this.getDb().collection(consumableMetric.getDomain().getMetrics()).insertDocument(consumableMetric,
                    new DocumentCreateOptions().overwriteMode(OverwriteMode.update).mergeObjects(true).returnNew(true)
                            .returnOld(true));
        } catch (Exception ex) {
            throw new ArangoDaoException(ex);
        } finally {
            //
            // Liberar o Lock manager Aqui, ou subir para o manager
            //
        }
    }

    public DocumentDeleteEntity<ConsumableMetric> deleteConsumableMetric(ConsumableMetric consumableMetric)
            throws ArangoDaoException {
        try {
            return this.getDb().collection(consumableMetric.getDomain().getMetrics()).deleteDocument(
                    consumableMetric.getMetricName(), ConsumableMetric.class,
                    new DocumentDeleteOptions().returnOld(true));
        } catch (Exception ex) {
            throw new ArangoDaoException(ex);
        } finally {
            //
            // Liberar o Lock manager Aqui, ou subir para o manager
            //
        }
    }

    public GraphList<ConsumableMetric> findConsumableMetricByFilter(FilterDTO filter, Domain domain)
            throws ArangoDaoException, ResourceNotFoundException, InvalidRequestException {
        try {
            String aql = " for doc in   `" + domain.getMetrics() + "`";
            aql += " filter doc.domainName == @domainName ";
            filter.getBindings().put("domainName", domain.getDomainName());

            if (filter.getAqlFilter() != null && !filter.getAqlFilter().trim().equals("")) {
                aql += " and " + filter.getAqlFilter();
            }

            if (filter.getSortCondition() != null) {
                aql += " " + filter.getSortCondition();
            }

            filter.setAqlFilter(aql);
            return this.query(filter, this.getDb());
        } catch (InvalidRequestException | ResourceNotFoundException ex) {
            throw ex;

        } catch (Exception ex) {
            throw new ArangoDaoException(ex);
        }
    }

    public Long getCount(Domain domain) throws IOException, InvalidRequestException {
        String aql = "for doc in `" + domain.getMetrics() + "` ";
        FilterDTO filter = new FilterDTO(aql);
        try {
            GraphList<ConsumableMetric> result = this.query(filter, this.getDb());
            Long longValue = result.size();
            result.close();
            return longValue;
        } catch (ResourceNotFoundException ex) {
            return 0L;
        }
    }

    public GraphList<BasicResource> findParentsWithMetrics(BasicResource from) {
        String aql = "FOR v, e, p IN 1..16 INBOUND '" + from.getId() + "' GRAPH '" + from.getDomainName() + "_connections_layer' ";
        aql += "FILTER v.consumableMetric != null ";
        aql += "RETURN distinct v ";
        return new GraphList<>(
                getDb().query(aql, new HashMap<>(), new AqlQueryOptions().fullCount(true).count(true), BasicResource.class));
    }

    public GraphList<BasicResource> findChildsWithMetrics(BasicResource to) {
        String aql = "FOR v, e, p IN 1..16 OUTBOUND '" + to.getId() + "' GRAPH '" + to.getDomainName() + "_connections_layer' ";
        aql += "FILTER v.consumerMetric != null ";
        aql += "RETURN distinct v ";
        return new GraphList<>(
                getDb().query(aql, new HashMap<>(), new AqlQueryOptions().fullCount(true).count(true), BasicResource.class));
    }

    /**
     *
     * @param filter
     * @param type
     * @param db
     * @return
     * @throws ResourceNotFoundException
     */
    private GraphList<ConsumableMetric> query(FilterDTO filter, ArangoDatabase db)
            throws ResourceNotFoundException, InvalidRequestException {
        Long start = System.currentTimeMillis();
        String uid = UUID.randomUUID().toString();

        if (filter.getBindings() != null) {
            filter.getBindings().forEach((k, v) -> {
                logger.info("\t  [@{}]=[{}]", k, v);
            });

            if (filter.getAqlFilter().toLowerCase().contains("return")) {
                throw new InvalidRequestException("Please remove Return statement from your filter.");
            }

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

            filter.setAqlFilter(filter.getAqlFilter() + " return doc");
            logger.info("(query) - [{}] - RUNNING: AQL:[{}]", uid, filter.getAqlFilter());
            filter.getBindings().forEach((k, v) -> {
                logger.info("\t  [@{}]=[{}]", k, v);
            });
            GraphList<ConsumableMetric> result = new GraphList<>(
                    db.query(filter.getAqlFilter(), filter.getBindings(),
                            new AqlQueryOptions().fullCount(true).count(true), ConsumableMetric.class));

            if (result.isEmpty()) {
                ResourceNotFoundException ex = new ResourceNotFoundException();
                //
                // Create a Detail map to the user
                //
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
            return new GraphList<>(
                    db.query(filter.getAqlFilter(), new AqlQueryOptions().fullCount(true).count(true), ConsumableMetric.class));
        }
    }

    private String buildAqlFromBindings(String aql, Map<String, Object> bindVars, boolean appendReturn) {
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

    private ArangoDatabase getDb() {
        return this.arangoDatabase;
    }
}
