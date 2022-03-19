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
import com.arangodb.entity.CollectionEntity;
import com.arangodb.entity.CollectionType;
import com.arangodb.entity.DocumentCreateEntity;
import com.arangodb.entity.DocumentUpdateEntity;
import com.arangodb.entity.EdgeDefinition;
import com.arangodb.entity.GraphEntity;
import com.arangodb.model.CollectionCreateOptions;
import com.arangodb.model.DocumentCreateOptions;
import com.arangodb.model.DocumentUpdateOptions;
import com.arangodb.model.PersistentIndexOptions;
import com.osstelecom.db.inventory.manager.configuration.ArangoDBConfiguration;
import com.osstelecom.db.inventory.manager.configuration.ConfigurationManager;
import com.osstelecom.db.inventory.manager.configuration.InventoryConfiguration;
import com.osstelecom.db.inventory.manager.dto.DomainDTO;
import com.osstelecom.db.inventory.manager.dto.FilterDTO;
import com.osstelecom.db.inventory.manager.exception.DomainAlreadyExistsException;
import com.osstelecom.db.inventory.manager.exception.GenericException;
import com.osstelecom.db.inventory.manager.exception.ResourceNotFoundException;
import com.osstelecom.db.inventory.manager.resources.BasicResource;
import com.osstelecom.db.inventory.manager.resources.CircuitResource;
import com.osstelecom.db.inventory.manager.resources.ManagedResource;
import com.osstelecom.db.inventory.manager.resources.ResourceConnection;
import com.osstelecom.db.inventory.manager.resources.ResourceLocation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
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
    private String dbName;
    private ArangoCollection domainsCollection;
    
    @Autowired
    private ConfigurationManager configurationManager;

    /**
     * Inicializa a conexão com o ArandoDB
     *
     * @todo: Migrar os dados de conexão para a configuração
     */
    @EventListener(ApplicationReadyEvent.class)
    private void start() {
        this.inventoryConfiguration = this.configurationManager.loadConfiguration();
        
        ArangoDBConfiguration arangoDbConfiguration = inventoryConfiguration.getGraphDbConfiguration();
        this.graphDb = new ArangoDB.Builder()
                .host(arangoDbConfiguration.getHost(), arangoDbConfiguration.getPort())
                .user(arangoDbConfiguration.getUser())
                .password(arangoDbConfiguration.getPassword()).build();
        this.database = this.graphDb.db(arangoDbConfiguration.getDatabaseName());
        
        if (!this.database.exists()) {
            logger.error("ERROR DB DOES NOT EXISTS... TRYING TO CREATE IT...");
            this.database.create();
        }
        this.domainsCollection = this.database.collection(arangoDbConfiguration.getDomainsCollection());
        if (!domainsCollection.exists()) {
            domainsCollection.create(new CollectionCreateOptions().type(CollectionType.DOCUMENT));
        }
        this.getDomains();
        try {
            logger.info(".........................................");
            logger.info("Graph DB Connected to: [" + this.graphDb.getVersion().getServer() + " " + this.graphDb.getVersion().getVersion() + "]");
            logger.info("Listing Databases:....");
            for (String arangoDbName : this.graphDb.getDatabases()) {
                if (arangoDbConfiguration.getDatabaseName().equals(arangoDbName)) {
                    logger.info("\tDB:.....: " + arangoDbName + "\t <---- USING THIS ONE :)");
                    this.dbName = arangoDbName;
                } else {
                    logger.info("\tDB:.....: " + arangoDbName);
                }
                
            }
            logger.info(".........................................");
        } catch (ArangoDBException ex) {
            logger.error("Failed GraphDB:", ex);
        }
    }
    
    public ArangoCollection getCollectionByName(String name) {
        return this.graphDb.db(this.dbName).collection(name);
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
        
        logger.debug("Creating New Domain: '" + domainName);
        
        if (!this.domainsCollection.documentExists(domainName)) {
            CollectionEntity nodes = this.graphDb.db(this.dbName)
                    .createCollection(domainName
                            + this.inventoryConfiguration.getGraphDbConfiguration().getNodeSufix(), new CollectionCreateOptions().type(CollectionType.DOCUMENT));
            
            this.graphDb.db(this.dbName).collection(domainName
                    + this.inventoryConfiguration.getGraphDbConfiguration().getNodeSufix()).ensurePersistentIndex(Arrays.asList("name", "nodeAddress", "className", "domain._key"), new PersistentIndexOptions().unique(true).name("NodeUNIQIDX"));
            
            CollectionEntity connections = this.graphDb.db(this.dbName)
                    .createCollection(domainName
                            + this.inventoryConfiguration.getGraphDbConfiguration().getNodeConnectionSufix(), new CollectionCreateOptions().type(CollectionType.EDGES));
            
            this.graphDb.db(this.dbName).collection(connections.getName()
            ).ensurePersistentIndex(Arrays.asList("name", "nodeAddress", "className", "domain._key"), new PersistentIndexOptions().unique(true).name("ConnectionUNIQIDX"));
            
            CollectionEntity services = this.graphDb.db(this.dbName)
                    .createCollection(domainName
                            + this.inventoryConfiguration.getGraphDbConfiguration().getServiceSufix(), new CollectionCreateOptions().type(CollectionType.DOCUMENT));
            
            CollectionEntity circuits = this.graphDb.db(this.dbName)
                    .createCollection(domainName
                            + this.inventoryConfiguration.getGraphDbConfiguration().getCircuitsSufix(), new CollectionCreateOptions().type(CollectionType.DOCUMENT));
            
            this.graphDb.db(this.dbName).collection(circuits.getName()
            ).ensurePersistentIndex(Arrays.asList("name", "aPoint.nodeAddress", "aPoint.className", "aPoint.domain._key", "zPoint.nodeAddress", "zPoint.className", "zPoint.domain._key", "className", "domain._key"), new PersistentIndexOptions().unique(true).name("CircuitUNIQIDX"));
            
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
                    + " GRAPH: " + domainName + this.inventoryConfiguration.getGraphDbConfiguration().getConnectionLayerSufix());
            
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
    public ArrayList<DomainDTO> getDomains() {
        ArrayList<DomainDTO> result = new ArrayList<>();
        logger.debug("Domains Size is: " + this.domainsCollection.count().getCount());
        ArangoCursor<DomainDTO> cursor = this.database.query("FOR doc IN domains    RETURN doc", DomainDTO.class);
        cursor.forEachRemaining(domain -> {
            logger.info("Found Domain: [" + domain.getDomainName() + "]");
            result.add(domain);
        });
        return result;
    }

    /**
     * Atualiza o Domain
     *
     * @param domain
     */
    public void updateDomain(DomainDTO domain) {
        logger.debug("Persinting Domain info...:" + domain.getDomainName());
        this.domainsCollection.updateDocument(domain.getDomainName(), domain);
    }
    
    public ResourceConnection updateResourceConnection(ResourceConnection connection) {
        DocumentUpdateEntity<ResourceConnection> result = this.database.collection(connection.getDomain().getConnections()).updateDocument(connection.getUid(), connection, new DocumentUpdateOptions().returnNew(true).returnOld(true), ResourceConnection.class);
        return result.getNew();
    }
    
    public CircuitResource updateCircuitResource(CircuitResource resource) {
        DocumentUpdateEntity<CircuitResource> result = this.database.collection(resource.getDomain().getCircuits()).updateDocument(resource.getUid(), resource, new DocumentUpdateOptions().returnNew(true).returnOld(true), CircuitResource.class);
        return result.getNew();
    }

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
//        this.graphDb.db(this.dbName).graph("").addEdgeDefinition(edgeDefiniton);
        GraphEntity graph = this.graphDb.db(this.dbName).createGraph(graphName, edgeDefinitions);
        return graph;
    }

    /**
     * Cria um elemento Comum
     *
     * @param resource
     * @return
     * @throws GenericException
     */
    public DocumentCreateEntity<ManagedResource> createManagedResource(ManagedResource resource) throws GenericException {
        try {
            DocumentCreateEntity<ManagedResource> result = this.database
                    .collection(resource.getDomain().getNodes()).insertDocument(resource, new DocumentCreateOptions().returnNew(true).returnOld(true));
            return result;
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new GenericException(ex.getMessage());
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
            DocumentCreateEntity<ResourceLocation> result = this.database.collection(resource.getDomain().getNodes()).insertDocument(resource);
            
            return result;
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new GenericException(ex.getMessage());
        }
    }
    
    public DocumentCreateEntity<ResourceConnection> createConnection(ResourceConnection connection) throws GenericException {
        try {
            DocumentCreateEntity<ResourceConnection> result = this.database.collection(connection.getDomain().getConnections()).insertDocument(connection);
            return result;
        } catch (Exception ex) {
            throw new GenericException(ex.getMessage());
        }
    }
    
    public DocumentCreateEntity<CircuitResource> createCircuitResource(CircuitResource circuitResource) throws GenericException {
        try {
            DocumentCreateEntity<CircuitResource> result = this.database.collection(circuitResource.getDomain().getCircuits()).insertDocument(circuitResource);
            return result;
        } catch (Exception ex) {
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
    public ResourceLocation findResourceLocation(String name, String nodeAddress, String className, DomainDTO domain) throws ResourceNotFoundException {
        HashMap<String, Object> bindVars = new HashMap<>();
        bindVars.put("name", name);
        bindVars.put("className", className);
//        this.database.collection("").getd
        String aql = "FOR doc IN "
                + domain.getNodes() + " FILTER ";
        
        if (nodeAddress != null) {
            if (!nodeAddress.equals("")) {
                bindVars.remove("name");
                bindVars.put("nodeAddress", nodeAddress);
                aql += "  doc.nodeAddress == @nodeAddress ";
            }
        }
        
        if (bindVars.containsKey("name")) {
            aql += "  doc.name == @name ";
        }
        
        aql += " and doc.className == @className ";
        
        if (domain.getDomainName() != null) {
            bindVars.put("domainName", domain.getDomainName());
            aql += "  and doc.domainName == @domainName ";
        }
        
        aql += "  RETURN doc ";
        logger.info("RUNNING: AQL:[" + aql + "]");
        logger.info("\tBindings:");
        bindVars.forEach((k, v) -> {
            logger.info("\t  [@" + k + "]=[" + v + "]");
            
        });
        ArangoCursor<ResourceLocation> cursor = this.database.query(aql, bindVars, ResourceLocation.class);
//        if (cursor.getCount() > 0) {
        ArrayList<ResourceLocation> locations = new ArrayList<>();
        
        cursor.forEachRemaining(location -> {
            locations.add(location);
        });
        if (!locations.isEmpty()) {
            return locations.get(0);
        }
//        }
        if (bindVars.containsKey("name")) {
            logger.warn("Resource with name:[" + name + "] nodeAddress:[" + nodeAddress + "] className:[" + className + "] was not found..");
            
            throw new ResourceNotFoundException("Resource With Name:[" + name + "] and Class: [" + className + "] Not Found in Domain:" + domain.getDomainName());
        } else {
            logger.warn("Resource with name:[" + name + "] nodeAddress:[" + nodeAddress + "] className:[" + className + "] was not found..");
            
            throw new ResourceNotFoundException("Resource With Node Address:[" + nodeAddress + "] and Class: [" + className + "] Not Found in Domain:" + domain.getDomainName());
        }
    }
    
    public ManagedResource findManagedResource(String name, String nodeAddress, String className, DomainDTO domain) throws ResourceNotFoundException {
        HashMap<String, Object> bindVars = new HashMap<>();
        bindVars.put("name", name);
        bindVars.put("className", className);
        
        String aql = "FOR doc IN "
                + domain.getNodes() + " FILTER ";
        
        if (nodeAddress != null) {
            if (!nodeAddress.equals("")) {
                bindVars.remove("name");
                bindVars.put("nodeAddress", nodeAddress);
                aql += "  doc.nodeAddress == @nodeAddress ";
            }
        }
        
        if (bindVars.containsKey("name")) {
            aql += "  doc.name == @name ";
        }
        
        aql += " and doc.className == @className ";
        
        if (domain.getDomainName() != null) {
            bindVars.put("domainName", domain.getDomainName());
            aql += "  and doc.domainName == @domainName ";
        }
        aql += "  RETURN doc ";
        logger.info("RUNNING: AQL:[" + aql + "]");
        logger.info("\tBindings:");
        bindVars.forEach((k, v) -> {
            logger.info("\t  [@" + k + "]=[" + v + "]");
            
        });
        ArangoCursor<ManagedResource> cursor = this.database.query(aql, bindVars, ManagedResource.class);
//        if (cursor.getCount() > 0) {
        ArrayList<ManagedResource> locations = new ArrayList<>();
        
        cursor.forEachRemaining(location -> {
            locations.add(location);
        });
        if (!locations.isEmpty()) {
            return locations.get(0);
        }
//        }
        logger.warn("Resource with name:[" + name + "] nodeAddress:[" + nodeAddress + "] className:[" + className + "] was not found..");
        throw new ResourceNotFoundException("Resource With Name:[" + name + "] and Class: [" + className + "] Not Found in Domain:" + domain.getDomainName());
    }

    /**
     * Encontra apenas 1 Resultado..
     *
     * @param connection
     * @return
     */
    public ResourceConnection findResourceConnection(ResourceConnection connection) throws ResourceNotFoundException {
        ArrayList<ResourceConnection> resultList = new ArrayList<>();
        String aql = "for doc in " + connection.getDomain().getConnections() + " FILTER ";
        HashMap<String, Object> bindVars = new HashMap<>();
        if (connection.getName() != null) {
            bindVars.put("name", connection.getName());
        }
        
        aql += " doc.className == @className";
        bindVars.put("className", connection.getClassName());
        
        if (connection.getDomainName() != null) {
            bindVars.put("domainName", connection.getDomainName());
            aql += "  and doc.domainName == @domainName ";
        }
        
        if (connection.getNodeAddress() != null) {
            if (!connection.getNodeAddress().equals("")) {
                bindVars.remove("name");
                bindVars.put("nodeAddress", connection.getNodeAddress());
                aql += "  and doc.nodeAddress == @nodeAddress ";
            }
        }
        
        if (connection.getFrom() != null) {
            if (connection.getFrom().getNodeAddress() != null
                    && connection.getFrom().getClassName() != null
                    && connection.getFrom().getDomainName() != null) {
                aql += " and  doc.from.nodeAddress == @fromNodeAddress ";
                aql += " and  doc.from.className   == @fromClassName ";
                aql += " and  doc.from.domainName  == @fromDomainName ";
                
                bindVars.put("fromNodeAddress", connection.getFrom().getNodeAddress());
                bindVars.put("fromClassName", connection.getFrom().getClassName());
                bindVars.put("fromDomainName", connection.getFrom().getDomainName());
                
            }
        }
        
        if (connection.getTo() != null) {
            if (connection.getTo().getNodeAddress() != null
                    && connection.getTo().getClassName() != null
                    && connection.getTo().getDomainName() != null) {
                aql += " and  doc.to.nodeAddress == @toNodeAddress ";
                aql += " and  doc.to.className   == @toClassName ";
                aql += " and  doc.to.domainName  == @toDomainName ";
                
                bindVars.put("toNodeAddress", connection.getTo().getNodeAddress());
                bindVars.put("toClassName", connection.getTo().getClassName());
                bindVars.put("toDomainName", connection.getTo().getDomainName());
                
            }
        }
        
        if (bindVars.containsKey("name")) {
            aql += " and doc.name == @name ";
        }
        
        aql += " RETURN doc";
        logger.debug("RUNNING: AQL:[" + aql + "]");
        logger.debug("\tBindings:");
        bindVars.forEach((k, v) -> {
            logger.debug("\t  [@" + k + "]=[" + v + "]");
            
        });
        ArangoCursor<ResourceConnection> cursor = this.database.query(aql, bindVars, ResourceConnection.class);
//        ArangoCursor<ResourceConnection> cursor = this.database.query("for doc in vivo_connections return doc", ResourceConnection.class);
//        logger.debug("Found: " + cursor.getCount() + " Connections");
//        logger.debug("Cursor ID: " + cursor.getId());
//        logger.debug("Cursor ID: " + cursor.getWarnings().isEmpty());

        cursor.forEachRemaining(resConnection -> {
            logger.debug("Found Connection :" + resConnection.getId());
            resultList.add(resConnection);
        });
        
        if (!resultList.isEmpty()) {
            return resultList.get(0);
        }
//        }
        logger.warn("Resource with name:[" + connection.getName() + "] nodeAddress:[" + connection.getNodeAddress() + "] className:[" + connection.getClassName() + "] was not found..");
        throw new ResourceNotFoundException("Resource With Name:[" + connection.getName() + "] and Class: [" + connection.getClassName() + "] Not Found in Domain:" + connection.getDomainName());
        
    }
    
    public CircuitResource findCircuitResource(CircuitResource resource) throws ResourceNotFoundException {
        HashMap<String, Object> bindVars = new HashMap<>();
        bindVars.put("name", resource.getName());
        bindVars.put("className", resource.getClassName());
        
        String aql = "FOR doc IN `"
                + resource.getDomain().getCircuits() + "` FILTER ";
        
        if (resource.getNodeAddress() != null) {
            if (!resource.getNodeAddress().equals("")) {
                bindVars.remove("name");
                bindVars.put("nodeAddress", resource.getNodeAddress());
                aql += "  doc.nodeAddress == @nodeAddress ";
            }
        }
        
        if (bindVars.containsKey("name")) {
            aql += "  doc.name == @name ";
        }
        
        aql += " and doc.className == @className   RETURN doc";
        
        logger.info("RUNNING: AQL:[" + aql + "]");
        logger.info("\tBindings:");
        bindVars.forEach((k, v) -> {
            logger.info("\t  [@" + k + "]=[" + v + "]");
            
        });
        ArangoCursor<CircuitResource> cursor = this.database.query(aql, bindVars, CircuitResource.class);
//        if (cursor.getCount() > 0) {
        ArrayList<CircuitResource> circuits = new ArrayList<>();
        
        cursor.forEachRemaining(location -> {
            circuits.add(location);
        });
        if (!circuits.isEmpty()) {
            return circuits.get(0);
        }
//        }
        logger.warn("Resource with name:[" + resource.getName() + "] nodeAddress:[" + resource.getNodeAddress() + "] className:[" + resource.getClassName() + "] was not found..");
        throw new ResourceNotFoundException("Resource With Name:[" + resource.getName() + "] and Class: [" + resource.getClassName() + "] Not Found in Domain:" + resource.getDomainName());
    }
    
    public ArrayList<ResourceConnection> findCircuitPath(CircuitResource circuit) {
        ArrayList<ResourceConnection> resultList = new ArrayList<>();
        String aql = "FOR doc IN " + circuit.getDomain().getConnections() + " \n"
                + "   filter @circuitId in doc.circuits[*] \n"
                + " return doc";
        HashMap<String, Object> bindVars = new HashMap<>();
        bindVars.put("circuitId", circuit.getId());
        ArangoCursor<ResourceConnection> cursor = this.database.query(aql, bindVars, ResourceConnection.class);
        cursor.forEachRemaining(connection -> {
            resultList.add(connection);
        });
        
        return resultList;
    }
    
    public ArangoCursor<ResourceConnection> filterConnectionByAQL(String aql, HashMap<String, Object> bindings) {
        ArangoCursor<ResourceConnection> cursor = this.database.query(aql, bindings, ResourceConnection.class);
        
        return cursor;
    }
    
    public ArrayList<BasicResource> getNodesByFilter(FilterDTO filter, DomainDTO domain) throws ResourceNotFoundException {
        ArrayList<BasicResource> resultList = new ArrayList<>();
        String aql = "FOR doc IN " + domain.getNodes() + " \n"
                + "   filter   doc.className  in @classes \n";
        
        if (filter.getFilter() != null) {
            if (!filter.getFilter().trim().equals("")) {
                //
                //
                //

                aql += " and " + filter.getFilter();
            }
        }
        aql += " return doc";
        HashMap<String, Object> bindVars = new HashMap<>();
        bindVars.put("classes", filter.getClasses());
        ArangoCursor<BasicResource> cursor = this.database.query(aql, bindVars, BasicResource.class);
        
        cursor.forEachRemaining(connection -> {
            resultList.add(connection);
        });
        if (resultList.isEmpty()) {
            throw new ResourceNotFoundException("No Resource Found for Filter: [" + aql + "]");
        }
        logger.debug("Found:" + resultList.size());
        return resultList;
    }
    
    public ArrayList<ResourceConnection> getConnectionsByFilter(FilterDTO filter, DomainDTO domain) throws ResourceNotFoundException {
        ArrayList<ResourceConnection> resultList = new ArrayList<>();
        String aql = "FOR doc IN " + domain.getConnections() + " \n"
                + "   filter   doc.className  in @classes \n";
        
        if (filter.getFilter() != null) {
            if (!filter.getFilter().trim().equals("")) {
                //
                //
                //

                aql += " and " + filter.getFilter();
            }
        }
        aql += " return doc";
        HashMap<String, Object> bindVars = new HashMap<>();
        bindVars.put("classes", filter.getClasses());
        ArangoCursor<ResourceConnection> cursor = this.database.query(aql, bindVars, ResourceConnection.class);
        
        Long start = System.currentTimeMillis();
        cursor.forEachRemaining(connection -> {
            resultList.add(connection);
        });
        Long end = System.currentTimeMillis();
        Long took = end - start;
        logger.debug("Query Took: " + took + " ms Total Result Found was:" + resultList.size());
        logger.debug("AQL: [" + aql + "]");
        if (resultList.isEmpty()) {
            throw new ResourceNotFoundException("No Resource Found for Filter: [" + aql + "]");
        }
        logger.debug("Found:" + resultList.size());
        return resultList;
        
    }
    
};
