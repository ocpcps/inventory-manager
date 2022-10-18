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
import com.osstelecom.db.inventory.manager.exception.ArangoDaoException;
import com.osstelecom.db.inventory.manager.exception.BasicException;
import com.osstelecom.db.inventory.manager.exception.ResourceNotFoundException;
import com.osstelecom.db.inventory.manager.resources.Domain;
import com.osstelecom.db.inventory.manager.resources.GraphList;
import com.osstelecom.db.inventory.manager.resources.ManagedResource;
import java.io.IOException;

/**
 *
 * @author Lucas Nishimura <lucas.nishimura@gmail.com>
 * @created 30.08.2022
 */
@Component
public class ManagedResourceDao extends AbstractArangoDao<ManagedResource> {

    @Override
    public ManagedResource findResource(ManagedResource resource) throws ArangoDaoException, ResourceNotFoundException {
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

            String aql = " for doc in `" + resource.getDomain().getNodes() + "` filter ";

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
                    resource.setAttributeSchemaName("resource.default");
                }
                bindVars.put("attributeSchemaName", resource.getAttributeSchemaName());
            }

            aql = this.buildAqlFromBindings(aql, bindVars, true);

            GraphList<ManagedResource> result = this.query(aql, bindVars, ManagedResource.class, this.getDb());

            return result.getOne();
        } catch (BasicException ex) {
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
            // Liberar o Lock manager Aqui,  ou subir para o manager
            //
        }

    }

    @Override
    public DocumentCreateEntity<ManagedResource> insertResource(ManagedResource resource) throws ArangoDaoException {
        //
        // A complexidade de validação dos requistos do dado deve ter sido feita na dao antes de chegar aqui.
        //
        try {
            return this.getDb().collection(resource.getDomain().getNodes()).insertDocument(resource, new DocumentCreateOptions().returnNew(true).returnOld(true));
        } catch (Exception ex) {
            throw new ArangoDaoException(ex.getMessage(), ex);
        } finally {
            //
            // Liberar o Lock manager Aqui, ou subir para o manager
            //
        }
    }

    @Override
    public DocumentUpdateEntity<ManagedResource> updateResource(ManagedResource resource) throws ArangoDaoException {
        //
        // A complexidade de validação dos requistos do dado deve ter sido feita na dao antes de chegar aqui.
        //
        try {
            return this.getDb().collection(resource.getDomain().getNodes()).updateDocument(resource.getKey(), resource, new DocumentUpdateOptions().returnNew(true).returnOld(true).keepNull(false).waitForSync(false), ManagedResource.class);
        } catch (Exception ex) {
            throw new ArangoDaoException(ex);
        } finally {
            //
            // Liberar o Lock manager Aqui, ou subir para o manager
            //
        }
    }

    @Override
    public DocumentCreateEntity<ManagedResource> upsertResource(ManagedResource resource) throws ArangoDaoException {
        //
        // A complexidade de validação dos requistos do dado deve ter sido feita na dao antes de chegar aqui.
        //

        try {
            return this.getDb().collection(resource.getDomain().getNodes()).insertDocument(resource, new DocumentCreateOptions().overwriteMode(OverwriteMode.update).mergeObjects(true).returnNew(true).returnOld(true));
        } catch (Exception ex) {
            throw new ArangoDaoException(ex);
        } finally {
            //
            // Liberar o Lock manager Aqui, ou subir para o manager
            //
        }
    }

    @Override
    public DocumentDeleteEntity<ManagedResource> deleteResource(ManagedResource resource) throws ArangoDaoException {
        try {
            return this.getDb().collection(resource.getDomain().getNodes()).deleteDocument(resource.getKey(), ManagedResource.class, new DocumentDeleteOptions().returnOld(true));
        } catch (Exception ex) {
            throw new ArangoDaoException(ex);
        } finally {
            //
            // Liberar o Lock manager Aqui, ou subir para o manager
            //
        }
    }

    @Override
    public GraphList<ManagedResource> findResourcesBySchemaName(String attributeSchemaName, Domain domain) throws ResourceNotFoundException, ArangoDaoException {
        try {
            String aql = "for doc in `" + domain.getNodes() + "` filter doc.attributeSchemaName = @attributeSchemaName return doc";
            Map<String, Object> bindVars = new HashMap<>();

            bindVars.put("attributeSchemaName", attributeSchemaName);
            return this.query(aql, bindVars, ManagedResource.class, this.getDb());
        } catch (Exception ex) {
            throw new ArangoDaoException(ex);
        }
    }

    @Override
    public GraphList<ManagedResource> findResourcesByClassName(String className, Domain domain) throws ResourceNotFoundException, ArangoDaoException {
        try {
            String aql = "for doc in `" + domain.getNodes() + "` filter doc.className = @className return doc";
            Map<String, Object> bindVars = new HashMap<>();
            bindVars.put("attributeSchemaName", className);
            return this.query(aql, bindVars, ManagedResource.class, this.getDb());
        } catch (Exception ex) {
            throw new ArangoDaoException(ex);
        }
    }

    @Override
    public GraphList<ManagedResource> findResourceByFilter(String filter, Map<String, Object> bindVars, Domain domain) throws ArangoDaoException {
        try {
            String aql = " for doc in   `" + domain.getNodes() + "`";
            aql += " filter doc.domainName == @domainName ";
            bindVars.put("domainName", domain.getDomainName());

            if (filter != null && !filter.trim().equals("")) {
                aql += " and " + filter;
            }
            aql += " return doc";
            return this.query(aql, bindVars, ManagedResource.class, this.getDb());
        } catch (Exception ex) {
            throw new ArangoDaoException(ex);
        }
    }

    @Override
    public MultiDocumentEntity<DocumentUpdateEntity<ManagedResource>> updateResources(List<ManagedResource> resources, Domain domain) throws ArangoDaoException {
        try {
            ArangoCollection connectionCollection = this.getDb().collection(domain.getNodes());
            return connectionCollection.updateDocuments(resources, new DocumentUpdateOptions().returnNew(true).returnOld(true).keepNull(false).mergeObjects(false), ManagedResource.class);
        } catch (Exception ex) {
            throw new ArangoDaoException(ex);
        }
    }

    @Override
    public int getCount(Domain domain) throws ResourceNotFoundException, IOException {
        String aql = "for doc in `" + domain.getNodes() + "` return doc";
        GraphList<ManagedResource> result = this.query(aql, null, ManagedResource.class, this.getDb());
        Integer longValue = result.size();
        result.close();
        return longValue;
    }
}
