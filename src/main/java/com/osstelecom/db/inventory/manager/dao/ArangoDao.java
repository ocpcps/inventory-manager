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
import com.arangodb.entity.MultiDocumentEntity;
import com.arangodb.model.AqlQueryOptions;
import com.arangodb.model.CollectionCreateOptions;
import com.arangodb.model.DocumentCreateOptions;
import com.arangodb.model.DocumentUpdateOptions;
import com.arangodb.model.PersistentIndexOptions;
import com.osstelecom.db.inventory.graph.arango.GraphList;
import com.osstelecom.db.inventory.manager.configuration.ArangoDBConfiguration;
import com.osstelecom.db.inventory.manager.configuration.ConfigurationManager;
import com.osstelecom.db.inventory.manager.configuration.InventoryConfiguration;
import com.osstelecom.db.inventory.manager.dto.DomainDTO;
import com.osstelecom.db.inventory.manager.dto.FilterDTO;
import com.osstelecom.db.inventory.manager.exception.ArangoDaoException;
import com.osstelecom.db.inventory.manager.exception.DomainAlreadyExistsException;
import com.osstelecom.db.inventory.manager.exception.DomainNotFoundException;
import com.osstelecom.db.inventory.manager.exception.GenericException;
import com.osstelecom.db.inventory.manager.exception.ResourceNotFoundException;
import com.osstelecom.db.inventory.manager.exception.ServiceNotFoundException;
import com.osstelecom.db.inventory.manager.resources.BasicResource;
import com.osstelecom.db.inventory.manager.resources.CircuitResource;
import com.osstelecom.db.inventory.manager.resources.ManagedResource;
import com.osstelecom.db.inventory.manager.resources.ResourceConnection;
import com.osstelecom.db.inventory.manager.resources.ResourceLocation;
import com.osstelecom.db.inventory.manager.resources.ServiceResource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        this.database = this.graphDb.db(DbName.of(arangoDbConfiguration.getDatabaseName()));

        if (!this.database.exists()) {
            logger.error("ERROR DB DOES NOT EXISTS... TRYING TO CREATE IT...");
            this.database.create();
        }
        this.domainsCollection = this.database.collection(arangoDbConfiguration.getDomainsCollection());
        if (!domainsCollection.exists()) {
            domainsCollection.create(new CollectionCreateOptions().type(CollectionType.DOCUMENT));
        }

        try {
            this.getDomains();
            logger.info(".........................................");
            logger.info("Graph DB Connected to: [{} {}]", this.graphDb.getVersion().getServer(),  this.graphDb.getVersion().getVersion());
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
            logger.debug("Domain :[{}] Deleted",domain.getDomainName());
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

    public ResourceConnection updateResourceConnection(ResourceConnection connection) {
        connection.setLastModifiedDate(new Date());
        DocumentUpdateEntity<ResourceConnection> result = this.database
                .collection(connection.getDomain().getConnections())
                .updateDocument(connection.getUid(), connection,
                        new DocumentUpdateOptions().returnNew(true).returnOld(true).keepNull(false).mergeObjects(false), ResourceConnection.class);
        return result.getNew();
    }

    public List<ResourceConnection> updateResourceConnections(List<ResourceConnection> connections) {
        List<ResourceConnection> resultDocs = new ArrayList<>();
        ArangoCollection connectionCollection = this.database
                .collection(connections.get(0).getDomain().getConnections());
        MultiDocumentEntity<DocumentUpdateEntity<ResourceConnection>> results = connectionCollection.updateDocuments(connections, new DocumentUpdateOptions().returnNew(true).returnOld(true).keepNull(false).mergeObjects(false), ResourceConnection.class);
        results.getDocuments().forEach(connection -> {
            //
            // Adiciona a conexão criada no documento
            //
            resultDocs.add(connection.getNew());
        });
        return resultDocs;
    }

    /**
     * Update Circuit Resource
     *
     * @param resource
     * @return
     */
    public DocumentUpdateEntity<CircuitResource> updateCircuitResource(CircuitResource resource) {
        return this.database.collection(resource.getDomain().getCircuits()).updateDocument(resource.getUid(), resource,
                new DocumentUpdateOptions().returnNew(true).keepNull(false).returnOld(true).mergeObjects(false), CircuitResource.class);
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

        return this.graphDb.db(this.dbName).createGraph(graphName, edgeDefinitions);
    }

    /**
     * Cria um elemento Comum
     *
     * @param resource
     * @return
     * @throws GenericException
     */
    public DocumentCreateEntity<ManagedResource> createManagedResource(ManagedResource resource) throws GenericException, ArangoDaoException {
        try {
            return this.database
                    .collection(resource.getDomain()
                            .getNodes())
                    .insertDocument(resource, new DocumentCreateOptions().returnNew(true).returnOld(true));
        } catch (ArangoDBException ex) {
            throw new ArangoDaoException(ex.getErrorMessage());
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
            GenericException easd = new GenericException(ex.getMessage());
            easd.setParentExceptionClass(ex.getClass().getName());
            throw easd;
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

    /**
     * Create a connection
     *
     * @param connection
     * @return
     * @throws GenericException
     */
    public DocumentCreateEntity<ResourceConnection> createConnection(ResourceConnection connection) throws GenericException {
        try {
            DocumentCreateEntity<ResourceConnection> result = this.database.collection(connection.getDomain().getConnections()).insertDocument(connection);
            return result;
        } catch (Exception ex) {
            throw new GenericException(ex.getMessage());
        }
    }

    /**
     * Creates a Circuit Resource
     *
     * @param circuitResource
     * @return
     * @throws GenericException
     */
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

    /**
     * Find Managed Resource
     *
     * @param name
     * @param nodeAddress
     * @param className
     * @param domain
     * @return
     * @throws ResourceNotFoundException
     * @throws ArangoDaoException
     */
    public ManagedResource findManagedResource(String name, String nodeAddress, String className, DomainDTO domain) throws ResourceNotFoundException, ArangoDaoException {
        HashMap<String, Object> bindVars = new HashMap<>();
        if (name != null) {
            bindVars.put("name", name);
        }
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
        aql += " RETURN doc ";
        logger.info("(findManagedResource) RUNNING: AQL:[{}]", aql);
        logger.info("\tBindings:");
        bindVars.forEach((k, v) -> {  
            logger.info("\t  [@{}]=[{}]", k, v);

        });
        ArangoCursor<ManagedResource> cursor = this.database.query(aql, bindVars, ManagedResource.class);
        ArrayList<ManagedResource> resources = new ArrayList<>();

        resources.addAll(getListFromCursorType(cursor));
        // cursor.close();
        if (!resources.isEmpty()) {
            return resources.get(0);
        }
        logger.warn("Resource with name:[{}] nodeAddress:[{}] className:[{}] was not found..", name, nodeAddress, className);
        throw new ResourceNotFoundException("5 Resource With Name:[" + name == null ? nodeAddress : name + "] and Class: [" + className + "] Not Found in Domain:" + domain.getDomainName());
    }

    /**
     * Find a managed resource by ID
     *
     * @param resourceId
     * @param domain
     * @return
     * @throws ResourceNotFoundException
     * @throws ArangoDaoException
     */
    public ManagedResource findManagedResourceById(String resourceId, DomainDTO domain) throws ResourceNotFoundException, ArangoDaoException {
        String aql = "FOR doc IN "
                + domain.getNodes() + " FILTER ";
        aql += "doc._id == @id or doc._key == @id";
        aql += "  RETURN doc ";
        HashMap<String, Object> bindVars = new HashMap<>();
        bindVars.put("id", resourceId);
        ArangoCursor<ManagedResource> cursor = this.database.query(aql, bindVars, ManagedResource.class);
        ArrayList<ManagedResource> resources = new ArrayList<>();
        logger.info("(findManagedResourceById) RUNNING: AQL:[{}]",aql);
        logger.info("\tBindings:");
        bindVars.forEach((k, v) -> {
            logger.info("\t  [@{}]=[{}]", k, v);

        });
        resources.addAll(getListFromCursorType(cursor));
        if (!resources.isEmpty()) {
            return resources.get(0);
        } else {
            throw new ResourceNotFoundException("Resource WITH id[" + resourceId + "] not found in domain:" + domain.getDomainName());
        }
    }

    /**
     * Find a service by ID
     *
     * @param serviceId
     * @param domain
     * @return
     * @throws ResourceNotFoundException
     * @throws ArangoDaoException
     */
    public ServiceResource findServiceById(ServiceResource service) throws ServiceNotFoundException, ArangoDaoException {
        String aql = "FOR doc IN "
                + service.getDomain().getServices() + " FILTER ";
        aql += "doc._id == @id or doc._key == @id";
        aql += "  RETURN doc ";
        HashMap<String, Object> bindVars = new HashMap<>();
        bindVars.put("id", service.getId());
        ArangoCursor<ServiceResource> cursor = this.database.query(aql, bindVars, ServiceResource.class);
        ArrayList<ServiceResource> resources = new ArrayList<>();
        logger.info("(findServiceById) RUNNING: AQL:[{}]",aql);
        logger.info("\tBindings:");
        bindVars.forEach((k, v) -> {
            logger.info("\t  [@{}]=[{}]", k, v);

        });
        resources.addAll(getListFromCursorType(cursor));
        if (!resources.isEmpty()) {
            return resources.get(0);
        } else {
            throw new ServiceNotFoundException("Service WITH id[" + service.getId() + "] not found in domain:" + service.getDomain().getDomainName());
        }
    }

    /**
     * Deletes the domain from the system
     *
     * @param service
     * @return
     * @throws DomainNotFoundException
     */
    public ServiceResource deleteService(ServiceResource service) {
        DomainDTO domain = service.getDomain();                  
        this.graphDb.db(this.dbName).collection(domain.getServices()).deleteDocument(service.getId());
        logger.debug("Service :[{}] Deleted", service.getId());
        return service;        
    }

    /**
     * 
     * @param service
     * @return
     * @throws GenericException
     * @throws ArangoDaoException
     */
    public DocumentCreateEntity<ServiceResource> createService(ServiceResource service) throws GenericException, ArangoDaoException {
        try {
            return this.database
                    .collection(service.getDomain()
                            .getServices())
                    .insertDocument(service, new DocumentCreateOptions().returnNew(true).returnOld(true));
        } catch (ArangoDBException ex) {
            throw new ArangoDaoException(ex.getErrorMessage());
        } catch (Exception ex) {
            GenericException easd = new GenericException(ex.getMessage());
            easd.setParentExceptionClass(ex.getClass().getName());
            throw easd;
        }
    }
    
    public DocumentUpdateEntity<ServiceResource> updateService(ServiceResource service) {
        return this.database.collection(service.getDomain().getServices()).updateDocument(service.getId(), service,
                    new DocumentUpdateOptions().returnNew(true).keepNull(false).returnOld(true).mergeObjects(false), ServiceResource.class);
    }

    /**
     * Find the node related connections
     *
     * @param resource
     * @return
     */
    public GraphList<ResourceConnection> findRelatedConnections(BasicResource resource) {
        Long start = System.currentTimeMillis();
        String aql = "for doc in " + resource.getDomain().getConnections() + " FILTER";
        aql += " @resourceId in  doc.relatedNodes[*] return doc";

        HashMap<String, Object> bindVars = new HashMap<>();
        bindVars.put("resourceId", resource.getId());

        ArangoCursor<ResourceConnection> cursor = this.database.query(aql,
                bindVars, new AqlQueryOptions().count(true).batchSize(5000), ResourceConnection.class);
        GraphList<ResourceConnection> result = new GraphList<>(cursor);
        Long end = System.currentTimeMillis();
        Long took = end - start;
        if (took > 100) {
            logger.warn("TOOK [{}] ms to get Related Resources",took);
        }
        return result;
    }

    /**
     * Encontra apenas 1 Resultado..de conexão
     *
     * @param connection
     * @return
     */
    public ResourceConnection findResourceConnection(ResourceConnection connection) throws ResourceNotFoundException, ArangoDaoException {
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
                aql += " and  doc.fromResource.nodeAddress == @fromNodeAddress ";
                aql += " and  doc.fromResource.className   == @fromClassName ";
                aql += " and  doc.fromResource.domainName  == @fromDomainName ";

                bindVars.put("fromNodeAddress", connection.getFrom().getNodeAddress());
                bindVars.put("fromClassName", connection.getFrom().getClassName());
                bindVars.put("fromDomainName", connection.getFrom().getDomainName());

            }
        } else {
            logger.warn("(findResourceConnection) From is NULL");
        }

        if (connection.getTo() != null) {
            if (connection.getTo().getNodeAddress() != null
                    && connection.getTo().getClassName() != null
                    && connection.getTo().getDomainName() != null) {
                aql += " and  doc.toResource.nodeAddress == @toNodeAddress ";
                aql += " and  doc.toResource.className   == @toClassName ";
                aql += " and  doc.toResource.domainName  == @toDomainName ";

                bindVars.put("toNodeAddress", connection.getTo().getNodeAddress());
                bindVars.put("toClassName", connection.getTo().getClassName());
                bindVars.put("toDomainName", connection.getTo().getDomainName());

            }
        } else {
            logger.warn("(findResourceConnection) To is NULL");
        }

        if (bindVars.containsKey("name")) {
            aql += " and doc.name == @name ";
        }

        aql += " RETURN doc";
        logger.debug("(findResourceConnection) RUNNING: AQL:[{}]",aql);
        logger.debug("\tBindings:");
        bindVars.forEach((k, v) -> {
            logger.debug("\t  [@{}]=[{}]", k, v);

        });
        ArangoCursor<ResourceConnection> cursor = this.database.query(aql, bindVars, ResourceConnection.class);

        resultList.addAll(getListFromCursorType(cursor));

        //cursor.close();
        if (!resultList.isEmpty()) {
            return resultList.get(0);
        }
        logger.warn("Resource with name:[{}] nodeAddress:[{}] className:[{}] was not found..",
        connection.getName(),connection.getNodeAddress(),connection.getClassName());
        throw new ResourceNotFoundException("3 Resource With Name:[" + connection.getName() + "] and Class: [" + connection.getClassName() + "] Not Found in Domain:" + connection.getDomainName());

    }

    /**
     * Find Circuit Resource
     *
     * @param resource
     * @return
     * @throws ResourceNotFoundException
     * @throws ArangoDaoException
     */
    public CircuitResource findCircuitResource(CircuitResource resource) throws ResourceNotFoundException, ArangoDaoException {
        
        if(resource.getId()!= null){
            return findCircuitResourceById(resource.getId(), resource.getDomain());
        }

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

        logger.info("(findCircuitResource) RUNNING: AQL:[{}]",aql);
        logger.info("\tBindings:");
        bindVars.forEach((k, v) -> {
            logger.info("\t  [@{}]=[{}]", k, v);

        });
        ArangoCursor<CircuitResource> cursor = this.database.query(aql, bindVars, CircuitResource.class);
        ArrayList<CircuitResource> circuits = new ArrayList<>();
        circuits.addAll(getListFromCursorType(cursor));
        if (!circuits.isEmpty()) {
            return circuits.get(0);
        }

        logger.warn("Resource with name:[{}] nodeAddress:[{}] className:[{}] was not found..",
        resource.getName(), resource.getNodeAddress(), resource.getClassName());
        throw new ResourceNotFoundException("4 Resource With Name:[" + resource.getName() + "] and Class: [" + resource.getClassName() + "] Not Found in Domain:" + resource.getDomainName());
    }

    /**
     * Find Circuit Resource by ID
     *
     * @param id
     * @param domain
     * @return
     * @throws ResourceNotFoundException
     * @throws ArangoDaoException
     */
    public CircuitResource findCircuitResourceById(String id, DomainDTO domain) throws ResourceNotFoundException, ArangoDaoException {
        HashMap<String, Object> bindVars = new HashMap<>();
        bindVars.put("id", id);

        String aql = "FOR doc IN `"
                + domain.getCircuits() + "` FILTER ";

        if (bindVars.containsKey("id")) {
            aql += "  doc._id == @id ";
        }

        aql += " RETURN doc";

        logger.info("(findCircuitResourceById) RUNNING: AQL:{}]", aql);
        logger.info("\tBindings:");
        bindVars.forEach((k, v) -> {
            logger.info("\t  [@{}]=[{}]", k, v);

        });
        ArangoCursor<CircuitResource> cursor = this.database.query(aql, bindVars, CircuitResource.class);
        ArrayList<CircuitResource> circuits = new ArrayList<>();
        circuits.addAll(getListFromCursorType(cursor));
        if (!circuits.isEmpty()) {
            return circuits.get(0);
        }

        logger.warn("Resource with ID:{}] was not found..", id);
        throw new ResourceNotFoundException("4 Resource With ID:[" + id + "]  Not Found in Domain:" + domain.getDomainName());
    }
//
//    /**
//     * Find Circuit Path
//     *
//     * @param circuit
//     * @return
//     * @throws ArangoDaoException
//     */
//    public ArrayList<ResourceConnection> findCircuitPath(CircuitResource circuit) throws ArangoDaoException {
//        ArrayList<ResourceConnection> resultList = new ArrayList<>();
//        String aql = "FOR doc IN " + circuit.getDomain().getConnections() + " \n"
//                + "   filter @circuitId in doc.circuits[*] \n"
//                + " return doc";
//        HashMap<String, Object> bindVars = new HashMap<>();
//        bindVars.put("circuitId", circuit.getId());
//        ArangoCursor<ResourceConnection> cursor = this.database.query(aql, bindVars, ResourceConnection.class);
//        resultList.addAll(getListFromCursorType(cursor));
//
//        return resultList;
//    }

    //
    // Validar quão perigoso é isso
    //
//    public ArangoCursor<ResourceConnection> filterConnectionByAQL(String aql, HashMap<String, Object> bindings) {
//        ArangoCursor<ResourceConnection> cursor = this.database.query(aql, bindings, ResourceConnection.class);
//        return cursor;
//    }
    /**
     * Get Nodes By Filter
     *
     * @param filter
     * @param domain
     * @return
     * @throws ResourceNotFoundException
     * @throws ArangoDaoException
     */
    public List<ManagedResource> getNodesByFilter(FilterDTO filter, DomainDTO domain) throws ResourceNotFoundException, ArangoDaoException {
        ArrayList<ManagedResource> resultList = new ArrayList<>();
        HashMap<String, Object> bindVars = new HashMap<>();

        String aql = "FOR doc IN " + domain.getNodes() + " \n"
                + "   filter   1==1 ";

        if (filter.getClasses() != null && !filter.getClasses().isEmpty()) {
            aql += "and doc.className  in @classes \n";
            bindVars.put("classes", filter.getClasses());
        }
        if (filter.getAqlFilter() != null && !filter.getAqlFilter().trim().equals("")) {
            aql += " and " + filter.getAqlFilter();
        }
        aql += " return doc";

        if (filter.getBindings() != null && !filter.getBindings().isEmpty()) {
            bindVars.putAll(filter.getBindings());
        }

        logger.info("(getNodesByFilter) RUNNING: AQL:[{}]", aql);
        logger.info("\tBindings:");

        bindVars.forEach((k, v) -> {
            logger.info("\t  [@{}]=[{}]", k, v);
        });

        ArangoCursor<ManagedResource> cursor = this.database.query(aql, bindVars, new AqlQueryOptions(), ManagedResource.class);

        resultList.addAll(getListFromCursorType(cursor));

        if (resultList.isEmpty()) {
            throw new ResourceNotFoundException("No Resource Found for Filter: [" + aql + "]");
        }
        logger.debug("Found:{}", resultList.size());
        return resultList;
    }

    /**
     * Get Connections By Filter
     *
     * @param filter
     * @param domain
     * @return
     * @throws ResourceNotFoundException
     * @throws ArangoDaoException
     */
    public GraphList<ResourceConnection> getConnectionsByFilter(FilterDTO filter, DomainDTO domain) throws ResourceNotFoundException, ArangoDaoException {
        //ArrayList<ResourceConnection> resultList = new ArrayList<>();
        Map<String, Object> bindVars = new HashMap<>();
        String aql = "FOR doc IN " + domain.getConnections() + " \n"
                + "   filter   1==1 ";

        if (filter.getClasses() != null) {
            if (!filter.getClasses().isEmpty()) {
                aql += "doc.className  in @classes \n";
                bindVars.put("classes", filter.getClasses());
            }
        }

        if (filter.getAqlFilter() != null) {
            if (!filter.getAqlFilter().trim().equals("")) {
                //
                //
                //
                aql += " and " + filter.getAqlFilter();
            }
        }
        aql += " return doc";

        if (filter.getBindings() != null && !filter.getBindings().isEmpty()) {
            bindVars.putAll(filter.getBindings());
        }

        ArangoCursor<ResourceConnection> cursor = this.database.query(aql, bindVars, new AqlQueryOptions().fullCount(true).count(true), ResourceConnection.class);
        GraphList<ResourceConnection> resultList = new GraphList<>(cursor);
        Long start = System.currentTimeMillis();
        //resultList.addAll(getListFromCursorType(cursor));

        Long end = System.currentTimeMillis();
        Long took = end - start;
        logger.debug("Query Took: {} ms Total Result Found was:{}",took ,resultList.size());
        logger.debug("AQL: [{}]",aql);
        if (resultList.size() < 1) {
            throw new ResourceNotFoundException("No Resource Found for Filter: [" + aql + "] Total:[" + resultList.size() + "]");
        }
        logger.debug("Found:{}", resultList.size());
        return resultList;

    }

    /**
     * Aqui vamos fazer a busca retorna um stream de dados
     *
     * @param schemaName
     */
    public GraphList<ManagedResource> findManagedResourcesBySchemaName(String schemaName, DomainDTO domain) {
        //
        // Dynamic apply the query for the nodes
        //
        String aql = "FOR doc IN " + domain.getNodes() + " \n"
                + "   filter   doc.attributeSchemaName  == @attributeSchemaName "
                + "return doc \n";

        HashMap<String, Object> bindVars = new HashMap<>();
        bindVars.put("attributeSchemaName", schemaName);
        ArangoCursor<ManagedResource> cursor = this.database.query(aql, bindVars, new AqlQueryOptions().count(true).batchSize(5000), ManagedResource.class);
        return new GraphList<>(cursor);
    }

    /**
     * Obtem o caminho de um circuito no grafo
     *
     * @param circuit
     * @return
     */
    public GraphList<ResourceConnection> findCircuitPaths(CircuitResource circuit) {
//        String aql = "FOR v, e, p IN 1..@dLimit ANY @aPoint " + circuit.getDomain().getConnections() + "\n"
//                + " FILTER v._id ==  @zPoint "
//                + " AND @circuitId in e.circuits[*] "
//                + " AND   e.operationalStatus ==@operStatus "
//                + "  for a in  p.edges[*] return distinct a";
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
        //bindVars.put("operStatus", operStatus);
        ArangoCursor<ResourceConnection> cursor = this.database.query(aql, bindVars, new AqlQueryOptions().count(true).batchSize(5000), ResourceConnection.class);
        GraphList<ResourceConnection> result = new GraphList<>(cursor);

        logger.info("(getCircuitPath) RUNNING: AQL:[" + aql + "]");
        logger.info("\tBindings:");

        bindVars.forEach((k, v) -> {
            logger.info("\t  [@" + k + "]=[" + v + "]");

        });
        logger.info("(getCircuitPath) Size: " + result.size());
        return result;
    }

    /**
     * Update the Basic Resource, and fires the update of the related
     * connections and circuits
     *
     * @param resource
     * @return
     */
    public DocumentUpdateEntity<ManagedResource> updateManagedResource(ManagedResource resource) {
        resource.setLastModifiedDate(new Date());
        DocumentUpdateEntity<ManagedResource> result = this.database.collection(resource.getDomain().getNodes()).updateDocument(resource.getUid(), resource, new DocumentUpdateOptions().returnNew(true).returnOld(true).keepNull(false).waitForSync(false), ManagedResource.class);

        ManagedResource updatedResource = result.getNew();
        // ManagedResource oldResource = result.getOld();
        return result;
    }

    /**
     * Update the Basic Resource back
     *
     * @param resource
     * @return
     */
//    public ResourceConnection updateResourceConnection(ResourceConnection resource) {
//        resource.setLastModifiedDate(new Date());
//        DocumentUpdateEntity<BasicResource> result = this.database.collection(resource.getDomain().getNodes()).updateDocument(resource.getUid(), resource, new DocumentUpdateOptions().returnNew(true).returnOld(true).keepNull(false).waitForSync(false), BasicResource.class);
//        return result.getNew();
//    }
};
