/*
 * Copyright (C) 2021 Lucas Nishimura <lucas.nishimura@gmail.com>
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
import com.arangodb.ArangoCursor;
import com.arangodb.ArangoDB;
import com.arangodb.ArangoDBException;
import com.arangodb.ArangoDatabase;
import com.arangodb.DbName;
import com.arangodb.entity.CollectionEntity;
import com.arangodb.entity.CollectionType;
import com.arangodb.entity.DocumentCreateEntity;
import com.arangodb.entity.DocumentUpdateEntity;
import com.arangodb.entity.EdgeDefinition;
import com.arangodb.entity.GraphEntity;
import com.arangodb.model.CollectionCreateOptions;
import com.arangodb.model.DocumentCreateOptions;
import com.arangodb.model.DocumentUpdateOptions;
import com.arangodb.model.OverwriteMode;
import com.arangodb.model.PersistentIndexOptions;
import com.osstelecom.db.inventory.manager.configuration.ArangoDBConfiguration;
import com.osstelecom.db.inventory.manager.configuration.ConfigurationManager;
import com.osstelecom.db.inventory.manager.configuration.InventoryConfiguration;
import com.osstelecom.db.inventory.manager.dto.DomainDTO;
import com.osstelecom.db.inventory.manager.exception.ArangoDaoException;
import com.osstelecom.db.inventory.manager.exception.DomainAlreadyExistsException;
import com.osstelecom.db.inventory.manager.exception.DomainNotFoundException;
import com.osstelecom.db.inventory.manager.exception.GenericException;
import com.osstelecom.db.inventory.manager.exception.ResourceNotFoundException;
import com.osstelecom.db.inventory.manager.resources.ResourceLocation;
import com.osstelecom.db.inventory.manager.resources.ServiceResource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Manages The ArangoDB Connection
 *
 * @todo, avaliar se os cursores precisam ser fechados depois de consumidos!
 * @author Lucas Nishimura <lucas.nishimura@gmail.com>
 * @created 14.12.2021
 */
@Component
public class ArangoDao {

    private ArangoDB graphDb;
    private ArangoDatabase database;
    private Logger logger = LoggerFactory.getLogger(ArangoDao.class);
    private InventoryConfiguration inventoryConfiguration;
    private DbName dbName;
    private ArangoCollection domainsCollection;

    @Autowired
    private ConfigurationManager configurationManager;

    public ArangoDatabase getDb() {
        return this.database;
    }

    /**
     * Inicializa a conexão com o ArangoDB
     *
     */
    @EventListener(ApplicationReadyEvent.class)
    private void start() {
        this.inventoryConfiguration = this.configurationManager.loadConfiguration();

        ArangoDBConfiguration arangoDbConfiguration = inventoryConfiguration.getGraphDbConfiguration();
        this.graphDb = new ArangoDB.Builder()
                .host(arangoDbConfiguration.getHost(), arangoDbConfiguration.getPort())
                .user(arangoDbConfiguration.getUser())
                .password(arangoDbConfiguration.getPassword()).build();
        this.database = this.graphDb.db(DbName.of(arangoDbConfiguration.getDatabaseName()));

        if (!this.database.exists()) {
            logger.warn("ERROR DB DOES NOT EXISTS... TRYING TO CREATE IT...");
            this.database.create();
        }
        this.domainsCollection = this.database.collection(arangoDbConfiguration.getDomainsCollection());
        if (!domainsCollection.exists()) {
            domainsCollection.create(new CollectionCreateOptions().type(CollectionType.DOCUMENT));
        }

        try {
            this.getDomains();
            logger.info(".........................................");
            logger.info("Graph DB Connected to: [{} {}]", this.graphDb.getVersion().getServer(), this.graphDb.getVersion().getVersion());
            logger.info("Listing Databases:....");
            for (String arangoDbName : this.graphDb.getDatabases()) {
                if (arangoDbConfiguration.getDatabaseName().equals(arangoDbName)) {
                    logger.info("\tDB:.....: {}\t <---- USING THIS ONE :)", arangoDbName);
                    this.dbName = DbName.of(arangoDbName);
                } else {
                    logger.info("\tDB:.....: {}", arangoDbName);
                }

            }
            logger.info(".........................................");
        } catch (ArangoDBException ex) {
            logger.error("Failed GraphDB:", ex);
        } catch (ArangoDaoException ex) {
            logger.error("Failed GraphDB IO:", ex);

        }
    }

    public ArangoCollection getCollectionByName(String name) {
        return this.graphDb.db(this.dbName).collection(name);
    }

    /**
     * Deletes the domain from the system
     *
     * @param domain
     * @return
     * @throws DomainNotFoundException
     */
    public DomainDTO deleteDomain(DomainDTO domain) throws DomainNotFoundException {
        if (Boolean.TRUE.equals(this.domainsCollection.documentExists(domain.getDomainName()))) {
            /**
             * Now we know that the domain exists. Lets delete it all
             */

            this.graphDb.db(this.dbName).collection(domain.getNodes()).drop();
            this.graphDb.db(this.dbName).collection(domain.getCircuits()).drop();
            this.graphDb.db(this.dbName).collection(domain.getConnections()).drop();
            this.graphDb.db(this.dbName).collection(domain.getServices()).drop();
            this.graphDb.db(this.dbName).graph(domain.getConnectionLayer()).drop();
            this.domainsCollection.deleteDocument(domain.getDomainName());
            logger.debug("Domain :[{}] Deleted", domain.getDomainName());
            return domain;
        } else {
            throw new DomainNotFoundException("Domain with name:[" + domain.getDomainName() + "] not found");
        }
    }

    /**
     * Cria um Novo Dominio xD e suas collections
     *
     * @param domainRequestDTO
     * @return
     * @throws
     * com.osstelecom.db.inventory.manager.exception.DomainAlreadyExistsException
     */
    @SuppressWarnings("empty-statement")
    public DomainDTO createDomain(DomainDTO domainRequestDTO) throws DomainAlreadyExistsException {
        String domainName = domainRequestDTO.getDomainName();

        logger.debug("Creating New Domain: {}", domainName);

        if (!this.domainsCollection.documentExists(domainName).booleanValue()) {
            CollectionEntity nodes = this.graphDb.db(this.dbName)
                    .createCollection(domainName
                            + this.inventoryConfiguration.getGraphDbConfiguration().getNodeSufix(), new CollectionCreateOptions().type(CollectionType.DOCUMENT));

            this.graphDb.db(this.dbName).collection(domainName
                    + this.inventoryConfiguration.getGraphDbConfiguration().getNodeSufix()).ensurePersistentIndex(Arrays.asList("name", "nodeAddress", "className", "domain._key"), new PersistentIndexOptions().unique(true).name("NodeUNIQIDX"));

            //
            // Performance IDX
            //
            this.graphDb.db(this.dbName).collection(domainName
                    + this.inventoryConfiguration.getGraphDbConfiguration()
                            .getNodeSufix()).ensurePersistentIndex(Arrays.asList("nodeAddress", "className", "domainName"), new PersistentIndexOptions().name("NodeSEARCHIDX"));

            CollectionEntity connections = this.graphDb.db(this.dbName)
                    .createCollection(domainName
                            + this.inventoryConfiguration.getGraphDbConfiguration().getNodeConnectionSufix(), new CollectionCreateOptions().type(CollectionType.EDGES));

            this.graphDb.db(this.dbName).collection(connections.getName()
            ).ensurePersistentIndex(Arrays.asList("name", "nodeAddress", "className", "domain._key"), new PersistentIndexOptions().unique(true).name("ConnectionUNIQIDX"));

            this.graphDb.db(this.dbName).collection(connections.getName()
            ).ensurePersistentIndex(Arrays.asList("circuits[*]"), new PersistentIndexOptions().unique(false).name("circuitsIDX"));

            this.graphDb.db(this.dbName).collection(connections.getName()
            ).ensurePersistentIndex(Arrays.asList("className", "domainName", "fromResource.nodeAddress", "fromResource.className", "fromResource.domainName", "toResource.nodeAddress", "toResource.className", "toResource.domainName"), new PersistentIndexOptions().unique(false).name("searchIDX"));

            CollectionEntity services = this.graphDb.db(this.dbName)
                    .createCollection(domainName
                            + this.inventoryConfiguration.getGraphDbConfiguration().getServiceSufix(), new CollectionCreateOptions().type(CollectionType.DOCUMENT));

            CollectionEntity circuits = this.graphDb.db(this.dbName)
                    .createCollection(domainName
                            + this.inventoryConfiguration.getGraphDbConfiguration().getCircuitsSufix(), new CollectionCreateOptions().type(CollectionType.DOCUMENT));

            this.graphDb.db(this.dbName).collection(circuits.getName()
            ).ensurePersistentIndex(Arrays.asList("name", "aPoint.nodeAddress", "aPoint.className", "aPoint.domain._key", "zPoint.nodeAddress", "zPoint.className", "zPoint.domain._key", "className", "domain._key"), new PersistentIndexOptions().unique(true).name("CircuitUNIQIDX"));

            this.graphDb.db(this.dbName).collection(circuits.getName()
            ).ensurePersistentIndex(Arrays.asList("nodeAddress", "className", "domainName"), new PersistentIndexOptions().unique(false).name("searchIDX1"));

            GraphEntity connectionLayer = createGraph(domainName + this.inventoryConfiguration.getGraphDbConfiguration().getConnectionLayerSufix(), connections.getName(), nodes.getName(), services.getName(), circuits.getName());

            domainRequestDTO.setServices(services.getName());
            domainRequestDTO.setConnectionLayer(connectionLayer.getName());
            domainRequestDTO.setConnections(connections.getName());
            domainRequestDTO.setNodes(nodes.getName());
            domainRequestDTO.setCircuits(circuits.getName());
            domainRequestDTO.setDomainName(domainName);

            DocumentCreateEntity<DomainDTO> result = this.domainsCollection.insertDocument(domainRequestDTO);

            logger.debug("Created Domain: " + domainName + " With:"
                    + " NODES:" + domainName + this.inventoryConfiguration.getGraphDbConfiguration().getNodeSufix()
                    + " EDGES:" + domainName + this.inventoryConfiguration.getGraphDbConfiguration().getNodeConnectionSufix()
                    + " GRAPH:" + domainName + this.inventoryConfiguration.getGraphDbConfiguration().getConnectionLayerSufix());

            return domainRequestDTO;
        } else {
            throw new DomainAlreadyExistsException("Domain with Name: [" + domainName + "] already exists");
        }
    }

    /**
     * Obtem a lista de domain
     *
     * @return
     */
    public List<DomainDTO> getDomains() throws ArangoDaoException {
        logger.debug("Domains Size is: {}", this.domainsCollection.count().getCount());
        ArangoCursor<DomainDTO> cursor = this.database.query("FOR doc IN domains RETURN doc", DomainDTO.class);
        return getListFromCursorType(cursor);
    }

    private <T> ArrayList<T> getListFromCursorType(ArangoCursor<T> cursor) throws ArangoDaoException {
        ArrayList<T> result = new ArrayList<>();
        //result.forEach(action);
        cursor.forEachRemaining(data -> {
            result.add(data);
        });
        try {
            cursor.close();
        } catch (IOException ex) {
            throw new ArangoDaoException("Failed to Close Cursor:", ex);
        }
        return result;
    }

    /**
     * Atualiza o Domain
     *
     * @param domain
     */
    public void updateDomain(DomainDTO domain) {
        logger.debug("Persinting Domain info...:{}", domain.getDomainName());
        this.domainsCollection.updateDocument(domain.getDomainName(), domain);
    }

//    /**
//     * Update Circuit Resource
//     *
//     * @param resource
//     * @return
//     */
//    public DocumentUpdateEntity<CircuitResource> updateCircuitResource(CircuitResource resource) {
//        return this.database.collection(resource.getDomain().getCircuits()).updateDocument(resource.getUid(), resource,
//                new DocumentUpdateOptions().returnNew(true).keepNull(false).returnOld(true).mergeObjects(false), CircuitResource.class);
//    }

    /**
     * Cria um Graph
     *
     * @param graphName
     * @param connectionCollection
     * @param nodesDocument
     * @param serviceDocument
     * @return
     */
    private GraphEntity createGraph(String graphName, String connectionCollection, String nodesDocument, String serviceDocument, String circuitDocument) {
        EdgeDefinition edgeDefiniton = new EdgeDefinition()
                .collection(connectionCollection)
                .from(nodesDocument, serviceDocument, circuitDocument)
                .to(nodesDocument, serviceDocument, circuitDocument);
        Collection<EdgeDefinition> edgeDefinitions = Arrays.asList(edgeDefiniton);

        return this.graphDb.db(this.dbName).createGraph(graphName, edgeDefinitions);
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
            DocumentCreateEntity<ResourceLocation> result = this.database.collection(resource.getDomain().getNodes()).insertDocument(resource);

            return result;
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new GenericException(ex.getMessage());
        }
    }

//    /**
//     * Creates a Circuit Resource
//     *
//     * @param circuitResource
//     * @return
//     * @throws GenericException
//     */
//    public DocumentCreateEntity<CircuitResource> createCircuitResource(CircuitResource circuitResource) throws GenericException {
//        try {
//            DocumentCreateEntity<CircuitResource> result = this.database.collection(circuitResource.getDomain().getCircuits()).insertDocument(circuitResource);
//            return result;
//        } catch (Exception ex) {
//            throw new GenericException(ex.getMessage());
//        }
//    }

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
    public ResourceLocation findResourceLocation(String name, String nodeAddress, String className, DomainDTO domain) throws ResourceNotFoundException, ArangoDaoException {
        HashMap<String, Object> bindVars = new HashMap<>();
        if (name != null) {
            if (!name.equals("null")) {
                bindVars.put("name", name);
            }
        }
        bindVars.put("className", className);
//        this.database.collection("").getd
        String aql = "FOR doc IN "
                + domain.getNodes() + " FILTER ";

        if (nodeAddress != null) {
            if (!nodeAddress.equals("")) {
                bindVars.remove("name");
                bindVars.put("nodeAddress", nodeAddress);
                aql += " doc.nodeAddress == @nodeAddress ";
            }
        }

        if (bindVars.containsKey("name")) {
            aql += " doc.name == @name ";
        }

        aql += " and doc.className == @className ";

        if (domain.getDomainName() != null) {
            bindVars.put("domainName", domain.getDomainName());
            aql += " and doc.domainName == @domainName ";
        }

        aql += " RETURN doc ";
        logger.info("(findResourceLocation) RUNNING: AQL:[" + aql + "]");
        logger.info("\tBindings:");
        bindVars.forEach((k, v) -> {
            logger.info("\t  [@" + k + "]=[" + v + "]");

        });
        ArangoCursor<ResourceLocation> cursor = this.database.query(aql, bindVars, ResourceLocation.class);

        ArrayList<ResourceLocation> locations = new ArrayList<>();

        locations.addAll(getListFromCursorType(cursor));

        if (!locations.isEmpty()) {
            return locations.get(0);
        }

        logger.warn("Resource with name:[{}] nodeAddress:[{}] className:[{}] was not found..", name, nodeAddress, className);
        if (bindVars.containsKey("name") && name != null) {
            throw new ResourceNotFoundException("1 Resource With Name:[" + name + "] and Class: [" + className + "] Not Found in Domain:" + domain.getDomainName());
        }
        throw new ResourceNotFoundException("2 Resource With Node Address:[" + nodeAddress + "] and Class: [" + className + "] Not Found in Domain:" + domain.getDomainName());
    }
    
//    /**
//     * Find Circuit Resource
//     *
//     * @param resource
//     * @return
//     * @throws ResourceNotFoundException
//     * @throws ArangoDaoException
//     */
//    public CircuitResource findCircuitResource(CircuitResource resource) throws ResourceNotFoundException, ArangoDaoException {
//
//        if (resource.getId() != null) {
//            return findCircuitResourceById(resource.getId(), resource.getDomain());
//        }
//
//        HashMap<String, Object> bindVars = new HashMap<>();
//        bindVars.put("name", resource.getName());
//        bindVars.put("className", resource.getClassName());
//
//        String aql = "FOR doc IN `"
//                + resource.getDomain().getCircuits() + "` FILTER ";
//
//        if (resource.getNodeAddress() != null) {
//            if (!resource.getNodeAddress().equals("")) {
//                bindVars.remove("name");
//                bindVars.put("nodeAddress", resource.getNodeAddress());
//                aql += "  doc.nodeAddress == @nodeAddress ";
//            }
//        }
//
//        if (bindVars.containsKey("name")) {
//            aql += "  doc.name == @name ";
//        }
//
//        aql += " and doc.className == @className   RETURN doc";
//
//        logger.info("(findCircuitResource) RUNNING: AQL:[{}]", aql);
//        logger.info("\tBindings:");
//        bindVars.forEach((k, v) -> {
//            logger.info("\t  [@{}]=[{}]", k, v);
//
//        });
//        ArangoCursor<CircuitResource> cursor = this.database.query(aql, bindVars, CircuitResource.class);
//        ArrayList<CircuitResource> circuits = new ArrayList<>();
//        circuits.addAll(getListFromCursorType(cursor));
//        if (!circuits.isEmpty()) {
//            return circuits.get(0);
//        }
//
//        logger.warn("Resource with name:[{}] nodeAddress:[{}] className:[{}] was not found..",
//                resource.getName(), resource.getNodeAddress(), resource.getClassName());
//        throw new ResourceNotFoundException("4 Resource With Name:[" + resource.getName() + "] and Class: [" + resource.getClassName() + "] Not Found in Domain:" + resource.getDomainName());
//    }

//    /**
//     * Find Circuit Resource by ID
//     *
//     * @param id
//     * @param domain
//     * @return
//     * @throws ResourceNotFoundException
//     * @throws ArangoDaoException
//     */
//    public CircuitResource findCircuitResourceById(String id, DomainDTO domain) throws ResourceNotFoundException, ArangoDaoException {
//        HashMap<String, Object> bindVars = new HashMap<>();
//        bindVars.put("id", id);
//
//        String aql = "FOR doc IN `"
//                + domain.getCircuits() + "` FILTER ";
//
//        if (bindVars.containsKey("id")) {
//            aql += "  doc._id == @id ";
//        }
//
//        aql += " RETURN doc";
//
//        logger.info("(findCircuitResourceById) RUNNING: AQL:{}]", aql);
//        logger.info("\tBindings:");
//        bindVars.forEach((k, v) -> {
//            logger.info("\t  [@{}]=[{}]", k, v);
//
//        });
//        ArangoCursor<CircuitResource> cursor = this.database.query(aql, bindVars, CircuitResource.class);
//        ArrayList<CircuitResource> circuits = new ArrayList<>();
//        circuits.addAll(getListFromCursorType(cursor));
//        if (!circuits.isEmpty()) {
//            return circuits.get(0);
//        }
//
//        logger.warn("Resource with ID:{}] was not found..", id);
//        throw new ResourceNotFoundException("4 Resource With ID:[" + id + "]  Not Found in Domain:" + domain.getDomainName());
//    }

};
