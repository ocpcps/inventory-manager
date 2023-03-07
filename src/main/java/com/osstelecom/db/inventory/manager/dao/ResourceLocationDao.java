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
import com.osstelecom.db.inventory.manager.exception.BasicException;
import com.osstelecom.db.inventory.manager.exception.GenericException;
import com.osstelecom.db.inventory.manager.exception.InvalidRequestException;
import com.osstelecom.db.inventory.manager.exception.ResourceNotFoundException;
import com.osstelecom.db.inventory.manager.resources.Domain;
import com.osstelecom.db.inventory.manager.resources.GraphList;
import com.osstelecom.db.inventory.manager.resources.ResourceLocation;
import java.io.IOException;

/**
 * Needs To be created
 *
 * @author Lucas Nishimura <lucas.nishimura@gmail.com>
 * @created 03.09.2022
 */
@Component
public class ResourceLocationDao extends AbstractArangoDao<ResourceLocation> {

    @Override
    public ResourceLocation findResource(ResourceLocation resource) throws ArangoDaoException, ResourceNotFoundException, InvalidRequestException {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public DocumentCreateEntity<ResourceLocation> insertResource(ResourceLocation resource) throws ArangoDaoException {
//        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody

        //
        // A complexidade de validação dos requistos do dado deve ter sido feita na dao antes de chegar aqui.
        //
        try {
            return this.getDb().collection(resource.getDomain().getNodes()).insertDocument(resource, new DocumentCreateOptions().returnNew(true).returnOld(true));
        } catch (Exception ex) {
            throw new ArangoDaoException(ex);
        } finally {
            //
            // Liberar o Lock manager Aqui, ou subir para o manager
            //
        }
    }

    @Override
    public DocumentCreateEntity<ResourceLocation> upsertResource(ResourceLocation resource) throws ArangoDaoException {
//        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
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
    public DocumentUpdateEntity<ResourceLocation> updateResource(ResourceLocation resource) throws ArangoDaoException, ResourceNotFoundException, InvalidRequestException {
//        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
        //
        // A complexidade de validação dos requistos do dado deve ter sido feita na dao antes de chegar aqui.
        //
        try {
            return this.getDb().collection(resource.getDomain().getNodes()).updateDocument(resource.getKey(), resource, new DocumentUpdateOptions().returnNew(true).returnOld(true).mergeObjects(false).keepNull(false).waitForSync(false), ResourceLocation.class);
        } catch (Exception ex) {
            throw new ArangoDaoException(ex);
        } finally {
            //
            // Liberar o Lock manager Aqui, ou subir para o manager
            //
        }
    }

    @Override
    public MultiDocumentEntity<DocumentUpdateEntity<ResourceLocation>> updateResources(List<ResourceLocation> resources, Domain domain) throws BasicException {
//        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
        try {
            ArangoCollection connectionCollection = this.getDb().collection(domain.getNodes());
            return connectionCollection.updateDocuments(resources, new DocumentUpdateOptions().returnNew(true).returnOld(true).keepNull(false).mergeObjects(false), ResourceLocation.class);
        } catch (Exception ex) {
            throw new ArangoDaoException(ex);
        }
    }

    @Override
    public DocumentDeleteEntity<ResourceLocation> deleteResource(ResourceLocation resource) throws BasicException {
//        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
        try {
            return this.getDb().collection(resource.getDomain().getNodes()).deleteDocument(resource.getId(), ResourceLocation.class, new DocumentDeleteOptions().returnOld(true));
        } catch (Exception ex) {
            throw new ArangoDaoException(ex);
        } finally {
            //
            // Liberar o Lock manager Aqui, ou subir para o manager
            //
        }
    }

    @Override
    public GraphList<ResourceLocation> findResourcesBySchemaName(String attributeSchemaName, Domain domain) throws ArangoDaoException, ResourceNotFoundException, InvalidRequestException {
//        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
        try {
            String aql = "for doc in `" + domain.getNodes() + "` filter doc.attributeSchemaName == @attributeSchemaName ";
            Map<String, Object> bindVars = new HashMap<>();

            bindVars.put("attributeSchemaName", attributeSchemaName);
            FilterDTO filter = new FilterDTO(aql, bindVars);
            return this.query(filter, ResourceLocation.class, this.getDb());
        } catch (ResourceNotFoundException | InvalidRequestException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ArangoDaoException(ex);
        }
    }

    @Override
    public GraphList<ResourceLocation> findResourcesByClassName(String className, Domain domain) throws ArangoDaoException, ResourceNotFoundException, InvalidRequestException {
//        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
        try {
            String aql = "for doc in `" + domain.getNodes() + "` filter doc.className == @className ";
            Map<String, Object> bindVars = new HashMap<>();
            bindVars.put("attributeSchemaName", className);
            FilterDTO filter = new FilterDTO(aql, bindVars);
            return this.query(filter, ResourceLocation.class, this.getDb());
        } catch (ResourceNotFoundException | InvalidRequestException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ArangoDaoException(ex);
        }
    }

    @Override
    public GraphList<ResourceLocation> findResourceByFilter(FilterDTO filter, Domain domain) throws ArangoDaoException, ResourceNotFoundException, InvalidRequestException {
//        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
        try {
            String aql = " for doc in   `" + domain.getNodes() + "`";
            aql += " filter doc.domainName == @domainName ";
            filter.getBindings().put("domainName", domain.getDomainName());

            if (filter.getAqlFilter() != null && !filter.getAqlFilter().trim().equals("")) {
                aql += " and " + filter.getAqlFilter();
            }

            if (filter.getSortCondition() != null) {
                aql += " " + filter.getSortCondition();
            }

//            aql += " return doc";
            return this.query(filter, ResourceLocation.class, this.getDb());
        } catch (ResourceNotFoundException | InvalidRequestException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ArangoDaoException(ex);
        }
    }

    /**
     * Cria um elemento Comum
     *
     * @param resource
     * @return
     * @throws GenericException
     */
    public DocumentCreateEntity<ResourceLocation> createResourceLocation(ResourceLocation resource) throws GenericException {
        try {
            return this.getDb().collection(resource.getDomain().getNodes()).insertDocument(resource);
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new GenericException(ex.getMessage());
        }
    }

    /**
     * Pesquisa o recurso com base no name, e nodeAddress, dando sempre
     * preferencia para o nodeaddress ao nome.
     *
     * @param name
     * @param nodeAddress
     * @param className
     * @param domain
     * @return
     * @throws ResourceNotFoundException
     */
    public ResourceLocation findResourceLocation(String name, String nodeAddress, String className, Domain domain) throws ResourceNotFoundException, InvalidRequestException {
        HashMap<String, Object> bindVars = new HashMap<>();
        if (name != null && !name.equals("null")) {
            bindVars.put("name", name);
        }
        bindVars.put("className", className);
//        this.database.collection("").getd
        String aql = "FOR doc IN `"
                + domain.getNodes() + "` FILTER ";

        if (nodeAddress != null && !nodeAddress.equals("")) {
            bindVars.remove("name");
            bindVars.put("nodeAddress", nodeAddress);
            aql += " doc.nodeAddress == @nodeAddress ";
        }

        if (bindVars.containsKey("name")) {
            aql += " doc.name == @name ";
        }

        aql += " and doc.className == @className ";

        if (domain.getDomainName() != null) {
            bindVars.put("domainName", domain.getDomainName());
            aql += " and doc.domainName == @domainName ";
        }

//        aql += " RETURN doc ";
        logger.info("(findResourceLocation) RUNNING: AQL:[{}]", aql);
        logger.info("\tBindings:");
        bindVars.forEach((k, v) -> {
            logger.info("\t  [@{}]=[{}]", k, v);

        });

        FilterDTO filter = new FilterDTO(aql, bindVars);
        GraphList<ResourceLocation> locations = this.query(filter, ResourceLocation.class, this.getDb());
//        List<ResourceLocation> locations = cursor.asListRemaining();

        if (!locations.isEmpty()) {
            return locations.getOne();
        }

        logger.warn("Resource with name:[{}] nodeAddress:[{}] className:[{}] was not found..", name, nodeAddress, className);
        if (bindVars.containsKey("name") && name != null) {
            throw new ResourceNotFoundException("1 Resource With Name:[" + name + "] and Class: [" + className + "] Not Found in Domain:" + domain.getDomainName());
        }
        throw new ResourceNotFoundException("2 Resource With Node Address:[" + nodeAddress + "] and Class: [" + className + "] Not Found in Domain:" + domain.getDomainName());
    }

    @Override
    public Long getCount(Domain domain) throws IOException, InvalidRequestException {
        String aql = "for doc in `" + domain.getConnections() + "` ";
        FilterDTO filter = new FilterDTO(aql);
        try {
            GraphList<ResourceLocation> result = this.query(filter, ResourceLocation.class, this.getDb());
            Long longValue = result.size();
            result.close();
            return longValue;
        } catch (ResourceNotFoundException ex) {
            return 0L;
        }
    }
}
