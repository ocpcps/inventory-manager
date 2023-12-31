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
import com.arangodb.ArangoDBException;
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
import com.osstelecom.db.inventory.manager.exception.InvalidRequestException;
import com.osstelecom.db.inventory.manager.exception.ResourceNotFoundException;
import com.osstelecom.db.inventory.manager.resources.Domain;
import com.osstelecom.db.inventory.manager.resources.GraphList;
import com.osstelecom.db.inventory.manager.resources.ServiceResource;
import java.io.IOException;

/**
 * Representa a dao de um service
 *
 * @author Lucas Nishimura
 * @created 31.08.2022
 */
@Component
public class ServiceResourceDao extends AbstractArangoDao<ServiceResource> {

    public GraphList<ServiceResource> findUpperResources(ServiceResource resource) throws ArangoDaoException, ResourceNotFoundException, InvalidRequestException {
        try {
            Map<String, Object> bindVars = new HashMap<>();
            String aql = " for doc in   " + resource.getDomain().getServices();
            aql += " filter @id in  doc.dependencies[*]._id";
            aql += " for dep in doc.dependencies ";
//            aql += " return doc";
            bindVars.put("id", resource.getId());
            FilterDTO filter = new FilterDTO(aql, bindVars);
            return this.query(filter, ServiceResource.class);
        } catch (ResourceNotFoundException | InvalidRequestException ex) {
            //
            // Neste caso queremos saber se não existe nada
            //
            throw ex;
        } catch (Exception ex) {
            throw new ArangoDaoException(ex);
        }
    }

    @Override
    public ServiceResource findResource(ServiceResource resource) throws ArangoDaoException, ResourceNotFoundException {
        String aql = " for doc in `" + resource.getDomain().getServices() + "` filter ";
        Map<String, Object> bindVars = new HashMap<>();
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

            aql += " doc.domainName == @domainName ";

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
                    /**
                     * não vou colocar o schema, se ele tiver id ou key, por ser
                     * mais especifico
                     */
                    if (resource.getId() == null && resource.getKey() == null) {
                        resource.setAttributeSchemaName("service.default");
                        bindVars.put("attributeSchemaName", resource.getAttributeSchemaName());
                    }
                }

            }

//            if (resource.getOperationalStatus() != null) {
//                bindVars.put("operationalStatus", resource.getOperationalStatus());
//            }
            //
            // Creates AQL
            //
            aql = this.buildAqlFromBindings(aql, bindVars, false);
            FilterDTO filter = new FilterDTO(aql, bindVars);
            GraphList<ServiceResource> result = this.query(filter, ServiceResource.class);
            if (result.isEmpty()) {
                ResourceNotFoundException ex = new ResourceNotFoundException("Resource Not Found");
                //
                // Melhor detalhe para o cliente, podemos melhorar no construtor depois
                //
                ex.addDetails("aql", aql);
                ex.addDetails("bindings", bindVars);
                throw ex;
            }
            return result.getOne();
        } catch (Exception ex) {
            ArangoDaoException aex = new ArangoDaoException(ex);
            aex.addDetails("aql", aql);
            aex.addDetails("bindings", bindVars);
            throw aex;
        } finally {
            //
            // Liberar o Lock manager Aqui,  ou subir para o manager
            //
        }
    }

    @Override
    public DocumentCreateEntity<ServiceResource> insertResource(ServiceResource resource) throws ArangoDaoException {
        //
        // A complexidade de validação dos requistos do dado deve ter sido feita na dao antes de chegar aqui.
        //
        try {
            return this.getDb().collection(resource.getDomain().getServices()).insertDocument(resource, new DocumentCreateOptions().returnNew(true).returnOld(true));
        } catch (Exception ex) {
            throw new ArangoDaoException(ex);
        } finally {
            //
            // Liberar o Lock manager Aqui, ou subir para o manager
            //
        }
    }

    @Override
    public DocumentCreateEntity<ServiceResource> upsertResource(ServiceResource resource) throws ArangoDaoException {
        //
        // A complexidade de validação dos requistos do dado deve ter sido feita na dao antes de chegar aqui.
        //

        try {
            return this.getDb().collection(resource.getDomain().getServices()).insertDocument(resource, new DocumentCreateOptions().overwriteMode(OverwriteMode.update).mergeObjects(true).returnNew(true).returnOld(true));
        } catch (Exception ex) {
            throw new ArangoDaoException(ex);
        } finally {
            //
            // Liberar o Lock manager Aqui, ou subir para o manager
            //
        }
    }

    @Override
    public DocumentUpdateEntity<ServiceResource> updateResource(ServiceResource resource) throws ArangoDaoException {
        //
        // A complexidade de validação dos requistos do dado deve ter sido feita na dao antes de chegar aqui.
        //
        try {
            return this.getDb().collection(resource.getDomain().getServices()).updateDocument(resource.getKey(), resource, new DocumentUpdateOptions().returnNew(true).mergeObjects(false).returnOld(true).keepNull(false).waitForSync(false), ServiceResource.class);
        } catch (Exception ex) {
            throw new ArangoDaoException(ex);
        } finally {
            //
            // Liberar o Lock manager Aqui, ou subir para o manager
            //
        }
    }

    @Override
    public MultiDocumentEntity<DocumentUpdateEntity<ServiceResource>> updateResources(List<ServiceResource> resources, Domain domain) throws ArangoDaoException {
        try {
            ArangoCollection connectionCollection = this.getDb().collection(domain.getServices());
            return connectionCollection.updateDocuments(resources, new DocumentUpdateOptions().returnNew(true).returnOld(true).keepNull(false).mergeObjects(false), ServiceResource.class);
        } catch (Exception ex) {
            throw new ArangoDaoException(ex);
        }
    }

    @Override
    public DocumentDeleteEntity<ServiceResource> deleteResource(ServiceResource resource) throws ArangoDaoException {
        try {
            return this.getDb().collection(resource.getDomain().getServices()).deleteDocument(resource.getId(), ServiceResource.class, new DocumentDeleteOptions().returnOld(true));
        } catch (Exception ex) {
            throw new ArangoDaoException(ex);
        }
    }

    @Override
    public GraphList<ServiceResource> findResourcesBySchemaName(String attributeSchemaName, Domain domain) throws ArangoDaoException, ResourceNotFoundException, InvalidRequestException {
        try {
            String aql = "for doc in `" + domain.getServices() + "` filter doc.attributeSchemaName == @attributeSchemaName ";
            Map<String, Object> bindVars = new HashMap<>();

            bindVars.put("attributeSchemaName", attributeSchemaName);
            FilterDTO filter = new FilterDTO(aql, bindVars);
            return this.query(filter, ServiceResource.class);
        } catch (ResourceNotFoundException | InvalidRequestException ex) {
            //
            // Neste caso queremos saber se não existe nada
            //
            throw ex;
        } catch (Exception ex) {
            throw new ArangoDaoException(ex);
        }
    }

    @Override
    public GraphList<ServiceResource> findResourcesByClassName(String className, Domain domain) throws ArangoDaoException, InvalidRequestException, ResourceNotFoundException {
        try {
            String aql = "for doc in `" + domain.getServices() + "` filter doc.className == @className";
            Map<String, Object> bindVars = new HashMap<>();
            bindVars.put("attributeSchemaName", className);
            FilterDTO filter = new FilterDTO(aql, bindVars);
            return this.query(filter, ServiceResource.class);
        } catch (ResourceNotFoundException | InvalidRequestException ex) {
            //
            // Neste caso queremos saber se não existe nada
            //
            throw ex;
        } catch (Exception ex) {
            throw new ArangoDaoException(ex);
        }
    }

    @Override
    public GraphList<ServiceResource> findResourceByFilter(FilterDTO filter, Domain domain) throws ArangoDaoException, ResourceNotFoundException {
        String aql = "";
        try {
            aql += " for doc in   `" + domain.getServices() + "`";
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
            return this.query(filter, ServiceResource.class);
        } catch (ResourceNotFoundException ex) {
            //
            // Sobe essa excpetion
            //
            throw ex;
        } catch (Exception ex) {
            ArangoDaoException exa = new ArangoDaoException(ex);
            exa.addDetails("aql", aql);
            exa.addDetails("binds", filter.getBindings());
            throw exa;
        }
    }

    @Override
    public Long getCount(Domain domain) throws IOException, InvalidRequestException {
        String aql = "RETURN COLLECTION_COUNT(@d) ";
        FilterDTO filter = new FilterDTO(aql);
        filter.getBindings().put("d", domain.getServices());
        try (ArangoCursor<Long> cursor = this.getDb().query(aql, filter.getBindings(), Long.class)) {
            Long longValue;
            try (GraphList<Long> result = new GraphList<>(cursor)) {
                longValue = result.getOne();
            }
            return longValue;
        } catch (ArangoDBException | IOException ex) {
            logger.error("Failed to Get Service Count:[{}]", ex.getMessage(), ex);
            return -1L;
        }
    }

    @Override
    public GraphList<ServiceResource> findAll(Domain domain) throws ArangoDaoException, ResourceNotFoundException, InvalidRequestException {
        return this.query(FilterDTO.findAllServices(domain), ServiceResource.class);
    }
}
