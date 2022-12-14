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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.arangodb.ArangoCollection;
import com.arangodb.ArangoCursor;
import com.arangodb.entity.DocumentCreateEntity;
import com.arangodb.entity.DocumentDeleteEntity;
import com.arangodb.entity.DocumentUpdateEntity;
import com.arangodb.entity.MultiDocumentEntity;
import com.arangodb.model.AqlQueryOptions;
import com.arangodb.model.DocumentCreateOptions;
import com.arangodb.model.DocumentDeleteOptions;
import com.arangodb.model.DocumentUpdateOptions;
import com.arangodb.model.OverwriteMode;
import com.osstelecom.db.inventory.manager.dto.FilterDTO;
import com.osstelecom.db.inventory.manager.exception.ArangoDaoException;
import com.osstelecom.db.inventory.manager.exception.InvalidRequestException;
import com.osstelecom.db.inventory.manager.exception.ResourceNotFoundException;
import com.osstelecom.db.inventory.manager.resources.CircuitResource;
import com.osstelecom.db.inventory.manager.resources.Domain;
import com.osstelecom.db.inventory.manager.resources.GraphList;
import com.osstelecom.db.inventory.manager.resources.ResourceConnection;
import java.io.IOException;

/**
 *
 * @author Lucas Nishimura <lucas.nishimura@gmail.com>
 * @created 31.08.2022
 */
@Component
public class ResourceConnectionDao extends AbstractArangoDao<ResourceConnection> {

    @Override
    public ResourceConnection findResource(ResourceConnection resource)
            throws ArangoDaoException, ResourceNotFoundException, InvalidRequestException {
        try {
            //
            // Pensar no Lock Manager aqui, ou subir para o manager
            //
            //
            // Domain is mandatory! make sure you set the domain before handling to dao
            //

            if (resource.getDomain() == null) {
                throw new ArangoDaoException("Missing Domain Information for Resource");
            }

            String aql = " for doc in `" + resource.getDomain().getConnections() + "` filter ";

            Map<String, Object> bindVars = new HashMap<>();
            aql += " doc.domainName == @domainName";

            bindVars.put("domainName", resource.getDomain().getDomainName());

            if (resource.getId() != null) {
                bindVars.put("_id", resource.getId());
            }
            if (resource.getKey() != null) {
                bindVars.put("_key", resource.getKey());
            }

            if (resource.getName() != null) {
                bindVars.put("name", resource.getName());
            }

            if (resource.getNodeAddress() != null) {
                bindVars.put("nodeAddress", resource.getNodeAddress());
            }

            if (resource.getClassName() != null) {
                bindVars.put("className", resource.getClassName());
            }

            if (resource.getAttributeSchemaName() != null) {
                //
                // Ugly fix.
                //
                if (resource.getAttributeSchemaName().equalsIgnoreCase("default")) {
                    resource.setAttributeSchemaName("connection.default");
                }
                bindVars.put("attributeSchemaName", resource.getAttributeSchemaName());
            }
//
//            if (resource.getOperationalStatus() != null) {
//                bindVars.put("operationalStatus", resource.getOperationalStatus());
//            }

            //
            // Creates AQL
            //
            aql = this.buildAqlFromBindings(aql, bindVars, false);

            //
            // Lida com o FROM:
            //
            if (resource.getFrom() != null && resource.getFrom().getNodeAddress() != null
                    && resource.getFrom().getClassName() != null
                    && resource.getFrom().getDomainName() != null) {
                aql += " and  doc.fromResource.nodeAddress == @fromNodeAddress ";
                aql += " and  doc.fromResource.className   == @fromClassName ";
                aql += " and  doc.fromResource.domainName  == @fromDomainName ";

                bindVars.put("fromNodeAddress", resource.getFrom().getNodeAddress());
                bindVars.put("fromClassName", resource.getFrom().getClassName());
                bindVars.put("fromDomainName", resource.getFrom().getDomainName());

            }

            if (resource.getTo() != null
                    && resource.getTo().getNodeAddress() != null
                    && resource.getTo().getClassName() != null
                    && resource.getTo().getDomainName() != null) {
                aql += " and  doc.toResource.nodeAddress == @toNodeAddress ";
                aql += " and  doc.toResource.className   == @toClassName ";
                aql += " and  doc.toResource.domainName  == @toDomainName ";

                bindVars.put("toNodeAddress", resource.getTo().getNodeAddress());
                bindVars.put("toClassName", resource.getTo().getClassName());
                bindVars.put("toDomainName", resource.getTo().getDomainName());
            }

//            aql += " return doc";
            FilterDTO filter = new FilterDTO(aql, bindVars);
            GraphList<ResourceConnection> result = this.query(filter, ResourceConnection.class, this.getDb());

            return result.getOne();
        } catch (ResourceNotFoundException | InvalidRequestException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ArangoDaoException(ex);
        } finally {
            //
            // Liberar o Lock manager Aqui, ou subir para o manager
            //
        }
    }

    @Override
    public DocumentCreateEntity<ResourceConnection> insertResource(ResourceConnection resource)
            throws ArangoDaoException {
        //
        // A complexidade de validação dos requistos do dado deve ter sido feita na dao
        // antes de chegar aqui.
        //
        try {
            return this.getDb().collection(resource.getDomain().getConnections()).insertDocument(resource,
                    new DocumentCreateOptions().returnNew(true).returnOld(true));
        } catch (Exception ex) {
            throw new ArangoDaoException(ex);
        } finally {
            //
            // Liberar o Lock manager Aqui, ou subir para o manager
            //
        }
    }

    @Override
    public DocumentCreateEntity<ResourceConnection> upsertResource(ResourceConnection resource)
            throws ArangoDaoException {
        //
        // A complexidade de validação dos requistos do dado deve ter sido feita na dao
        // antes de chegar aqui.
        //
        try {
            return this.getDb().collection(resource.getDomain().getConnections()).insertDocument(resource,
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

    @Override
    public DocumentUpdateEntity<ResourceConnection> updateResource(ResourceConnection resource)
            throws ArangoDaoException {
        //
        // A complexidade de validação dos requistos do dado deve ter sido feita na dao
        // antes de chegar aqui.
        //
        try {
            return this.getDb().collection(resource.getDomain().getConnections()).updateDocument(resource.getKey(),
                    resource,
                    new DocumentUpdateOptions().returnNew(true).mergeObjects(true).returnOld(true).keepNull(false).waitForSync(false),
                    ResourceConnection.class);
        } catch (Exception ex) {
            throw new ArangoDaoException(ex);
        } finally {
            //
            // Liberar o Lock manager Aqui, ou subir para o manager
            //
        }
    }

    @Override
    public DocumentDeleteEntity<ResourceConnection> deleteResource(ResourceConnection resource) throws ArangoDaoException {
        try {
            return this.getDb().collection(resource.getDomain().getConnections()).deleteDocument(resource.getKey(),
                    ResourceConnection.class, new DocumentDeleteOptions().returnOld(true));
        } catch (Exception ex) {
            throw new ArangoDaoException(ex);
        } finally {
            //
            // Liberar o Lock manager Aqui, ou subir para o manager
            //
        }
    }

    @Override
    public GraphList<ResourceConnection> findResourcesBySchemaName(String attributeSchemaName, Domain domain)
            throws ArangoDaoException, InvalidRequestException, ResourceNotFoundException {
        try {
            String aql = "for doc in `" + domain.getConnections() + "`"
                    + "filter doc.attributeSchemaName == @attributeSchemaName ";
            Map<String, Object> bindVars = new HashMap<>();
            bindVars.put("attributeSchemaName", attributeSchemaName);
            FilterDTO filter = new FilterDTO(aql, bindVars);

            return this.query(filter, ResourceConnection.class, this.getDb());
        } catch (InvalidRequestException | ResourceNotFoundException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ArangoDaoException(ex);
        }
    }

    @Override
    public GraphList<ResourceConnection> findResourcesByClassName(String className, Domain domain)
            throws ArangoDaoException, ResourceNotFoundException, InvalidRequestException {
        try {
            String aql = "for doc in `" + domain.getConnections() + "` filter doc.className == @className ";
            Map<String, Object> bindVars = new HashMap<>();
            bindVars.put("attributeSchemaName", className);
            FilterDTO filter = new FilterDTO(aql, bindVars);
            return this.query(filter, ResourceConnection.class, this.getDb());
        } catch (InvalidRequestException | ResourceNotFoundException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ArangoDaoException(ex);
        }
    }

    @Override
    public GraphList<ResourceConnection> findResourceByFilter(FilterDTO filter,
            Domain domain) throws ArangoDaoException, ResourceNotFoundException, InvalidRequestException {
        try {
            String aql = " for doc in   `" + domain.getConnections() + "` ";
            aql += " filter doc.domainName == @domainName ";
            filter.getBindings().put("domainName", domain.getDomainName());

            if (filter.getAqlFilter() != null && !filter.getAqlFilter().trim().equals("")) {
                aql += " and " + filter.getAqlFilter();
            }

            if (filter.getSortCondition() != null) {
                aql += " " + filter.getSortCondition();
            }

//            aql += " return doc";
            filter.setAqlFilter(aql);
            return this.query(filter, ResourceConnection.class, this.getDb());
        } catch (InvalidRequestException | ResourceNotFoundException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ArangoDaoException(ex);
        }
    }

    @Override
    public MultiDocumentEntity<DocumentUpdateEntity<ResourceConnection>> updateResources(
            List<ResourceConnection> resources, Domain domain) throws ArangoDaoException {
        try {
            ArangoCollection connectionCollection = this.getDb().collection(domain.getConnections());
            return connectionCollection.updateDocuments(resources, new DocumentUpdateOptions().returnNew(true)
                    .returnOld(true).keepNull(false).mergeObjects(false).waitForSync(false), ResourceConnection.class);
        } catch (Exception ex) {
            throw new ArangoDaoException(ex);
        }
    }

    /**
     * Obtem os vertices relacionados ao circuito Muito especifica para
     * generalizar
     *
     * @param circuit
     * @return
     */
    public GraphList<ResourceConnection> findCircuitPaths(CircuitResource circuit) {
        // String aql = "FOR v, e, p IN 1..@dLimit ANY @aPoint " +
        // circuit.getDomain().getConnections() + "\n"
        // + " FILTER v._id == @zPoint "
        // + " AND @circuitId in e.circuits[*] "
        // + " AND e.operationalStatus ==@operStatus "
        // + " for a in p.edges[*] return distinct a";
        String aql = "FOR path\n"
                + "  IN 1..@dLimit ANY k_paths\n"
                + "  @aPoint TO @zPoint\n"
                + "  GRAPH '" + circuit.getDomain().getConnectionLayer() + "'\n"
                + "    for v in  path.edges\n"
                + "      filter @circuitId  in v.circuits[*] \n"
                + "        \n"
                + "       return v";

        HashMap<String, Object> bindVars = new HashMap<>();
        bindVars.put("dLimit", circuit.getCircuitPath().size() + 1);
        bindVars.put("aPoint", circuit.getaPoint().getId());
        bindVars.put("zPoint", circuit.getzPoint().getId());
        bindVars.put("circuitId", circuit.getId());
        logger.info("(query) RUNNING: AQL:[{}]", aql);
        bindVars.forEach((k, v) -> {
            logger.info("\t  [@{}]=[{}]", k, v);

        });
        ArangoCursor<ResourceConnection> cursor = this.getDb().query(aql, bindVars,
                new AqlQueryOptions().count(true).batchSize(5000), ResourceConnection.class);
        return new GraphList<>(cursor);
    }

    @Override
    public Long getCount(Domain domain) throws  IOException, InvalidRequestException {
        String aql = "for doc in `" + domain.getConnections() + "` ";
        FilterDTO filter = new FilterDTO(aql);
        try {
            GraphList<ResourceConnection> result = this.query(filter, ResourceConnection.class, this.getDb());
            Long longValue = result.size();
            result.close();
            return longValue;
        } catch (ResourceNotFoundException ex) {
            return 0L;
        }
    }
}
