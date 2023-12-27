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
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.arangodb.ArangoCursor;
import com.arangodb.ArangoDatabase;
import com.arangodb.model.AqlQueryOptions;
import com.osstelecom.db.inventory.manager.resources.CircuitResource;
import com.osstelecom.db.inventory.manager.resources.Domain;
import com.osstelecom.db.inventory.manager.resources.History;
import com.osstelecom.db.inventory.manager.resources.ManagedResource;
import com.osstelecom.db.inventory.manager.resources.ResourceConnection;
import com.osstelecom.db.inventory.manager.resources.ServiceResource;

/**
 *
 * @author Leonardo Rodrigues
 * @created 28.08.2023
 */
@Component
public class HistoryDao {

    @Autowired
    private ArangoDatabase arangoDatabase;

    protected Logger logger = LoggerFactory.getLogger(HistoryDao.class);

    public ArangoDatabase getDb() {
        return this.arangoDatabase;
    }

    public List<History> list(History history) {
        String collectionName = this.getCollectionName(history);
        String reference = history.getReference();

        String aql = "FOR doc IN `" + collectionName + "` FILTER ";
        aql += " doc.reference == '" + reference + "' sort doc.sequence desc ";

        return this.query(aql, null);
    }

    private String getCollectionName(History history) {

        String type = history.getType();
        Domain domain = history.getDomain();

        if (type.contentEquals(ManagedResource.class.getSimpleName())) {
            return domain.getDomainName() + "_nodes_hist";
        } else if (type.contentEquals(ResourceConnection.class.getSimpleName())) {
            return domain.getDomainName() + "_connections_hist";
        } else if (type.contentEquals(CircuitResource.class.getSimpleName())) {
            return domain.getDomainName() + "_circuits_hist";
        } else if (type.contentEquals(ServiceResource.class.getSimpleName())) {
            return domain.getDomainName() + "_services_hist";
        }

        return null;
    }

    public List<History> query(String aql, Map<String, Object> bindVars) {
        Long start = System.currentTimeMillis();
        String uid = UUID.randomUUID().toString();
        List<History> result = new ArrayList<>();

        aql = aql + " return doc";

        ArangoCursor<History> cursor = null;
        if (bindVars != null) {
            bindVars.forEach((k, v) -> {
                logger.info("\t  [@{}]=[{}]", k, v);
            });

            logger.info("(query) - [{}] - RUNNING: AQL:[{}]", uid, aql);
            bindVars.forEach((k, v) -> {
                logger.info("\t  [@{}]=[{}]", k, v);
            });
            cursor = this.getDb().query(aql, bindVars,
                    new AqlQueryOptions().fullCount(true).count(true), History.class);

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

        } else {
            cursor = this.getDb().query(aql, new AqlQueryOptions().fullCount(true).count(true), History.class);
        }

        if (cursor != null && cursor.hasNext()) {
            result = cursor.asListRemaining();
        }

        try {
            if (cursor != null)
                cursor.close();
        } catch (IOException ex) {
            logger.error("Failed to close Cursor");
        }

        return result;
    }

}
