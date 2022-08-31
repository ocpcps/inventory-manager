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

import com.arangodb.ArangoCollection;
import com.arangodb.entity.DocumentCreateEntity;
import com.arangodb.entity.DocumentDeleteEntity;
import com.arangodb.entity.DocumentUpdateEntity;
import com.arangodb.entity.MultiDocumentEntity;
import com.arangodb.model.DocumentCreateOptions;
import com.arangodb.model.DocumentDeleteOptions;
import com.arangodb.model.DocumentUpdateOptions;
import com.arangodb.model.OverwriteMode;
import com.osstelecom.db.inventory.graph.arango.GraphList;
import com.osstelecom.db.inventory.manager.dto.DomainDTO;
import com.osstelecom.db.inventory.manager.exception.ArangoDaoException;
import com.osstelecom.db.inventory.manager.exception.ArangoDaoException;
import com.osstelecom.db.inventory.manager.exception.ResourceNotFoundException;
import com.osstelecom.db.inventory.manager.resources.CircuitResource;
import com.osstelecom.db.inventory.manager.resources.ManagedResource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 *
 * @author Lucas Nishimura <lucas.nishimura@gmail.com>
 * @created 31.08.2022
 */
@Service
public class CircuitResourceDao extends AbstractArangoDao<CircuitResource> {

    @Autowired
    private ArangoDao arangoDao;

    @Override
    public CircuitResource findResource(CircuitResource resource) throws ArangoDaoException,ResourceNotFoundException {
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

            String aql = " for doc in " + resource.getDomain().getCircuits() + " filter ";

            Map<String, Object> bindVars = new HashMap<>();
            aql += " doc.domainName == @domainName";

            bindVars.put("domainName", resource.getDomain().getDomainName());

            if (resource.getId() != null) {
                bindVars.put("_id", resource.getId());
            }
            if (resource.getUid() != null) {
                bindVars.put("_key", resource.getUid());
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
                    resource.setAttributeSchemaName("resource.default");
                }
                bindVars.put("attributeSchemaName", resource.getAttributeSchemaName());
            }

            if (resource.getOperationalStatus() != null) {
                bindVars.put("operationalStatus", resource.getOperationalStatus());
            }

            //
            // Creates AQL
            //
            aql = this.buildAqlFromBindings(aql, bindVars, true);

            GraphList<CircuitResource> result = this.query(aql, bindVars, CircuitResource.class, this.arangoDao.getDb());
            if (result.isEmpty()){
                throw new ResourceNotFoundException("Resource Not Found");
            }
            return result.getOne();
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
            return this.arangoDao.getDb().collection(resource.getDomain().getCircuits()).insertDocument(resource, new DocumentCreateOptions().returnNew(true).returnOld(true));
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
            return this.arangoDao.getDb().collection(resource.getDomain().getCircuits()).insertDocument(resource, new DocumentCreateOptions().overwriteMode(OverwriteMode.update).mergeObjects(true).returnNew(true).returnOld(true));
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
            return this.arangoDao.getDb().collection(resource.getDomain().getCircuits()).updateDocument(resource.getUid(), resource, new DocumentUpdateOptions().returnNew(true).returnOld(true).keepNull(false).waitForSync(false), CircuitResource.class);
        } catch (Exception ex) {
            throw new ArangoDaoException(ex);
        } finally {
            //
            // Liberar o Lock manager Aqui, ou subir para o manager
            //
        }
    }

    @Override
    public MultiDocumentEntity<DocumentUpdateEntity<CircuitResource>> updateResources(List<CircuitResource> resources, DomainDTO domain) throws ArangoDaoException {
        try {
            ArangoCollection connectionCollection = this.arangoDao.getDb().collection(domain.getCircuits());
            MultiDocumentEntity<DocumentUpdateEntity<CircuitResource>> results = connectionCollection.updateDocuments(resources, new DocumentUpdateOptions().returnNew(true).returnOld(true).keepNull(false).mergeObjects(false), CircuitResource.class);
            return results;
        } catch (Exception ex) {
            throw new ArangoDaoException(ex);
        }
    }

    @Override
    public DocumentDeleteEntity<ManagedResource> deleteResource(CircuitResource resource) throws ArangoDaoException {
        try {
            return this.arangoDao.getDb().collection(resource.getDomain().getCircuits()).deleteDocument(resource.getId(), ManagedResource.class, new DocumentDeleteOptions().returnOld(true));
        } catch (Exception ex) {
            throw new ArangoDaoException(ex);
        } finally {
            //
            // Liberar o Lock manager Aqui, ou subir para o manager
            //
        }
    }

    @Override
    public GraphList<CircuitResource> findResourcesBySchemaName(String attributeSchemaName, DomainDTO domain) throws ArangoDaoException {
        try {
            String aql = "for doc in " + domain.getCircuits() + "filter doc.attributeSchemaName = @attributeSchemaName return doc";
            Map<String, Object> bindVars = new HashMap<>();

            bindVars.put("attributeSchemaName", attributeSchemaName);
            GraphList<CircuitResource> result = this.query(aql, bindVars, CircuitResource.class, this.arangoDao.getDb());
            return result;
        } catch (Exception ex) {
            throw new ArangoDaoException(ex);
        }
    }

    @Override
    public GraphList<CircuitResource> findResourcesByClassName(String className, DomainDTO domain) throws ArangoDaoException {
        try {
            String aql = "for doc in " + domain.getCircuits() + " filter doc.className = @className return doc";
            Map<String, Object> bindVars = new HashMap<>();
//            bindVars.put("collection", domain.getNodes());
            bindVars.put("attributeSchemaName", className);
            GraphList<CircuitResource> result = this.query(aql, bindVars, CircuitResource.class, this.arangoDao.getDb());
            return result;
        } catch (Exception ex) {
            throw new ArangoDaoException(ex);
        }
    }

    @Override
    public GraphList<CircuitResource> findResourceByFilter(String filter, Map<String, Object> bindVars, DomainDTO domain) throws ArangoDaoException {
        try {
            String aql = " for doc in   " + domain.getCircuits();
            aql += " filter doc.domainName == @domainName ";
            bindVars.put("domainName", domain.getDomainName());

            if (filter != null) {
                aql += " and " + filter;
            }
            aql += " return doc";
            GraphList<CircuitResource> result = this.query(aql, bindVars, CircuitResource.class, this.arangoDao.getDb());
            return result;
        } catch (Exception ex) {
            throw new ArangoDaoException(ex);
        }
    }

}
