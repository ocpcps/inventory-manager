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
import com.arangodb.entity.DocumentCreateEntity;
import com.arangodb.entity.DocumentDeleteEntity;
import com.arangodb.entity.DocumentUpdateEntity;
import com.arangodb.entity.MultiDocumentEntity;
import com.arangodb.model.DocumentCreateOptions;
import com.arangodb.model.DocumentDeleteOptions;
import com.arangodb.model.DocumentUpdateOptions;
import com.arangodb.model.OverwriteMode;
import com.osstelecom.db.inventory.manager.dto.FilterDTO;
import com.osstelecom.db.inventory.manager.exception.ArangoDaoException;
import com.osstelecom.db.inventory.manager.exception.ResourceNotFoundException;
import com.osstelecom.db.inventory.manager.resources.CircuitResource;
import com.osstelecom.db.inventory.manager.resources.Domain;
import com.osstelecom.db.inventory.manager.resources.GraphList;
import com.osstelecom.db.inventory.manager.resources.ManagedResource;
import java.io.IOException;

/**
 *
 * @author Lucas Nishimura <lucas.nishimura@gmail.com>
 * @created 31.08.2022
 */
@Component
public class CircuitResourceDao extends AbstractArangoDao<CircuitResource> {

    @Override
    public CircuitResource findResource(CircuitResource resource) throws ArangoDaoException, ResourceNotFoundException {
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

            String aql = " for doc in `" + resource.getDomain().getCircuits() + "` filter ";

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
                    resource.setAttributeSchemaName("circuit.default");
                }
                bindVars.put("attributeSchemaName", resource.getAttributeSchemaName());
            }

//            if (resource.getOperationalStatus() != null) {
//                bindVars.put("operationalStatus", resource.getOperationalStatus());
//            }
            //
            // Creates AQL
            //
            aql = this.buildAqlFromBindings(aql, bindVars, true);

            GraphList<CircuitResource> result = this.query(aql, bindVars, CircuitResource.class, this.getDb());

            return result.getOne();
        } catch (ResourceNotFoundException ex) {
            //
            // Sobe essa excpetion
            //
            throw ex;
        } catch (Exception ex) {
            throw new ArangoDaoException(ex);
        } finally {
            //
            // Liberar o Lock manager Aqui,  ou subir para o manager
            //
        }
    }

    @Override
    public DocumentCreateEntity<CircuitResource> insertResource(CircuitResource resource) throws ArangoDaoException {
        //
        // A complexidade de validação dos requistos do dado deve ter sido feita na dao antes de chegar aqui.
        //
        try {
            return this.getDb().collection(resource.getDomain().getCircuits()).insertDocument(resource, new DocumentCreateOptions().returnNew(true).returnOld(true));
        } catch (Exception ex) {
            throw new ArangoDaoException(ex);
        } finally {
            //
            // Liberar o Lock manager Aqui, ou subir para o manager
            //
        }
    }

    @Override
    public DocumentCreateEntity<CircuitResource> upsertResource(CircuitResource resource) throws ArangoDaoException {
        //
        // A complexidade de validação dos requistos do dado deve ter sido feita na dao antes de chegar aqui.
        //

        try {
            return this.getDb().collection(resource.getDomain().getCircuits()).insertDocument(resource, new DocumentCreateOptions().overwriteMode(OverwriteMode.update).mergeObjects(true).returnNew(true).returnOld(true));
        } catch (Exception ex) {
            throw new ArangoDaoException(ex);
        } finally {
            //
            // Liberar o Lock manager Aqui, ou subir para o manager
            //
        }
    }

    @Override
    public DocumentUpdateEntity<CircuitResource> updateResource(CircuitResource resource) throws ArangoDaoException {
        //
        // A complexidade de validação dos requistos do dado deve ter sido feita na dao antes de chegar aqui.
        //
        try {
            return this.getDb().collection(resource.getDomain().getCircuits()).updateDocument(resource.getKey(), resource, new DocumentUpdateOptions().returnNew(true).returnOld(true).keepNull(false).waitForSync(false), CircuitResource.class);
        } catch (Exception ex) {
            throw new ArangoDaoException(ex);
        } finally {
            //
            // Liberar o Lock manager Aqui, ou subir para o manager
            //
        }
    }

    @Override
    public MultiDocumentEntity<DocumentUpdateEntity<CircuitResource>> updateResources(List<CircuitResource> resources, Domain domain) throws ArangoDaoException {
        try {
            ArangoCollection connectionCollection = this.getDb().collection(domain.getCircuits());
            return connectionCollection.updateDocuments(resources, new DocumentUpdateOptions().returnNew(true).returnOld(true).keepNull(false).mergeObjects(false), CircuitResource.class);
        } catch (Exception ex) {
            throw new ArangoDaoException(ex);
        }
    }

    @Override
    public DocumentDeleteEntity<CircuitResource> deleteResource(CircuitResource resource) throws ArangoDaoException {
        try {
            return this.getDb().collection(resource.getDomain().getCircuits()).deleteDocument(resource.getId(), CircuitResource.class, new DocumentDeleteOptions().returnOld(true));
        } catch (Exception ex) {
            throw new ArangoDaoException(ex);
        } finally {
            //
            // Liberar o Lock manager Aqui, ou subir para o manager
            //
        }
    }

    @Override
    public GraphList<CircuitResource> findResourcesBySchemaName(String attributeSchemaName, Domain domain) throws ArangoDaoException {
        try {
            String aql = "for doc in `" + domain.getCircuits() + "` filter doc.attributeSchemaName == @attributeSchemaName return doc";
            Map<String, Object> bindVars = new HashMap<>();

            bindVars.put("attributeSchemaName", attributeSchemaName);
            return this.query(aql, bindVars, CircuitResource.class, this.getDb());
        } catch (Exception ex) {
            throw new ArangoDaoException(ex);
        }
    }

    @Override
    public GraphList<CircuitResource> findResourcesByClassName(String className, Domain domain) throws ArangoDaoException {
        try {
            String aql = "for doc in `" + domain.getCircuits() + "` filter doc.className == @className return doc";
            Map<String, Object> bindVars = new HashMap<>();
            bindVars.put("attributeSchemaName", className);
            return this.query(aql, bindVars, CircuitResource.class, this.getDb());
        } catch (Exception ex) {
            throw new ArangoDaoException(ex);
        }
    }

    @Override
    public GraphList<CircuitResource> findResourceByFilter(FilterDTO filter, Map<String, Object> bindVars, Domain domain) throws ArangoDaoException, ResourceNotFoundException {
        try {
            String aql = " for doc in   `" + domain.getCircuits() + "`";
            aql += " filter doc.domainName == @domainName ";
            bindVars.put("domainName", domain.getDomainName());

            if (filter.getAqlFilter() != null && !filter.getAqlFilter().trim().equals("")) {
                aql += " and " + filter.getAqlFilter();
            }

            if (filter.getSortCondition() != null) {
                aql += " " + filter.getSortCondition();
            }

            aql += " return doc";
            return this.query(aql, bindVars, CircuitResource.class, this.getDb());
        } catch (ResourceNotFoundException ex) {
            throw ex;
        } catch (Exception ex) {
            ex.printStackTrace();
            ArangoDaoException a = new ArangoDaoException(ex);
            if (bindVars != null) {
                a.addDetails("filter", bindVars);
            }
            throw a;
        }
    }

    // /**
    // * Update Circuit Resource
    // *
    // * @param resource
    // * @return
    // */
    // public DocumentUpdateEntity<CircuitResource>
    // updateCircuitResource(CircuitResource resource) {
    // return
    // this.database.collection(resource.getDomain().getCircuits()).updateDocument(resource.getUid(),
    // resource,
    // new
    // DocumentUpdateOptions().returnNew(true).keepNull(false).returnOld(true).mergeObjects(false),
    // CircuitResource.class);
    // }
    // /**
    // * Creates a Circuit Resource
    // *
    // * @param circuitResource
    // * @return
    // * @throws GenericException
    // */
    // public DocumentCreateEntity<CircuitResource>
    // createCircuitResource(CircuitResource circuitResource) throws
    // GenericException {
    // try {
    // DocumentCreateEntity<CircuitResource> result =
    // this.database.collection(circuitResource.getDomain().getCircuits()).insertDocument(circuitResource);
    // return result;
    // } catch (Exception ex) {
    // throw new GenericException(ex.getMessage());
    // }
    // }
    // /**
    // * Find Circuit Resource
    // *
    // * @param resource
    // * @return
    // * @throws ResourceNotFoundException
    // * @throws ArangoDaoException
    // */
    // public CircuitResource findCircuitResource(CircuitResource resource) throws
    // ResourceNotFoundException, ArangoDaoException {
    //
    // if (resource.getId() != null) {
    // return findCircuitResourceById(resource.getId(), resource.getDomain());
    // }
    //
    // HashMap<String, Object> bindVars = new HashMap<>();
    // bindVars.put("name", resource.getName());
    // bindVars.put("className", resource.getClassName());
    //
    // String aql = "FOR doc IN `"
    // + resource.getDomain().getCircuits() + "` FILTER ";
    //
    // if (resource.getNodeAddress() != null) {
    // if (!resource.getNodeAddress().equals("")) {
    // bindVars.remove("name");
    // bindVars.put("nodeAddress", resource.getNodeAddress());
    // aql += " doc.nodeAddress == @nodeAddress ";
    // }
    // }
    //
    // if (bindVars.containsKey("name")) {
    // aql += " doc.name == @name ";
    // }
    //
    // aql += " and doc.className == @className RETURN doc";
    //
    // logger.info("(findCircuitResource) RUNNING: AQL:[{}]", aql);
    // logger.info("\tBindings:");
    // bindVars.forEach((k, v) -> {
    // logger.info("\t [@{}]=[{}]", k, v);
    //
    // });
    // ArangoCursor<CircuitResource> cursor = this.database.query(aql, bindVars,
    // CircuitResource.class);
    // ArrayList<CircuitResource> circuits = new ArrayList<>();
    // circuits.addAll(getListFromCursorType(cursor));
    // if (!circuits.isEmpty()) {
    // return circuits.get(0);
    // }
    //
    // logger.warn("Resource with name:[{}] nodeAddress:[{}] className:[{}] was not
    // found..",
    // resource.getName(), resource.getNodeAddress(), resource.getClassName());
    // throw new ResourceNotFoundException("4 Resource With Name:[" +
    // resource.getName() + "] and Class: [" + resource.getClassName() + "] Not
    // Found in Domain:" + resource.getDomainName());
    // }
    // /**
    // * Find Circuit Resource by ID
    // *
    // * @param id
    // * @param domain
    // * @return
    // * @throws ResourceNotFoundException
    // * @throws ArangoDaoException
    // */
    // public CircuitResource findCircuitResourceById(String id, Domain domain)
    // throws ResourceNotFoundException, ArangoDaoException {
    // HashMap<String, Object> bindVars = new HashMap<>();
    // bindVars.put("id", id);
    //
    // String aql = "FOR doc IN `"
    // + domain.getCircuits() + "` FILTER ";
    //
    // if (bindVars.containsKey("id")) {
    // aql += " doc._id == @id ";
    // }
    //
    // aql += " RETURN doc";
    //
    // logger.info("(findCircuitResourceById) RUNNING: AQL:{}]", aql);
    // logger.info("\tBindings:");
    // bindVars.forEach((k, v) -> {
    // logger.info("\t [@{}]=[{}]", k, v);
    //
    // });
    // ArangoCursor<CircuitResource> cursor = this.database.query(aql, bindVars,
    // CircuitResource.class);
    // ArrayList<CircuitResource> circuits = new ArrayList<>();
    // circuits.addAll(getListFromCursorType(cursor));
    // if (!circuits.isEmpty()) {
    // return circuits.get(0);
    // }
    //
    // logger.warn("Resource with ID:{}] was not found..", id);
    // throw new ResourceNotFoundException("4 Resource With ID:[" + id + "] Not
    // Found in Domain:" + domain.getDomainName());
    // }
    @Override
    public int getCount(Domain domain) throws ResourceNotFoundException, IOException {
        String aql = "for doc in `" + domain.getCircuits() + "` return doc";
        GraphList<CircuitResource> result = this.query(aql, null, CircuitResource.class, this.getDb());
        Integer longValue = result.size();
        result.close();
        return longValue;
    }
}
