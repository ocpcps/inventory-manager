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

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.arangodb.ArangoCollection;
import com.arangodb.ArangoCursor;
import com.arangodb.ArangoDatabase;
import com.arangodb.entity.CollectionEntity;
import com.arangodb.entity.CollectionType;
import com.arangodb.entity.DocumentUpdateEntity;
import com.arangodb.entity.EdgeDefinition;
import com.arangodb.entity.GraphEntity;
import com.arangodb.model.CollectionCreateOptions;
import com.arangodb.model.DocumentUpdateOptions;
import com.arangodb.model.PersistentIndexOptions;
import com.osstelecom.db.inventory.manager.configuration.ArangoDBConfiguration;
import com.osstelecom.db.inventory.manager.configuration.ConfigurationManager;
import com.osstelecom.db.inventory.manager.configuration.InventoryConfiguration;
import com.osstelecom.db.inventory.manager.exception.ArangoDaoException;
import com.osstelecom.db.inventory.manager.exception.DomainAlreadyExistsException;
import com.osstelecom.db.inventory.manager.exception.DomainNotFoundException;
import com.osstelecom.db.inventory.manager.resources.Domain;
import java.io.IOException;

/**
 * Manages The ArangoDB Connection
 *
 * @todo, avaliar se os cursores precisam ser fechados depois de consumidos!
 *
 * @author Lucas Nishimura <lucas.nishimura@gmail.com>
 * @created 14.12.2021
 */
@Component
public class DomainDao {

    protected Logger logger = LoggerFactory.getLogger(DomainDao.class);

    @Autowired
    private ConfigurationManager configurationManager;

    private ArangoCollection domainsCollection;

    @Autowired
    private ArangoDatabase arangoDatabase;

    @EventListener(ApplicationReadyEvent.class)
    private void start() {
        InventoryConfiguration inventoryConfiguration = configurationManager.loadConfiguration();
        ArangoDBConfiguration arangoDbConfiguration = inventoryConfiguration.getGraphDbConfiguration();

        this.domainsCollection = arangoDatabase.collection(arangoDbConfiguration.getDomainsCollection());
        if (!domainsCollection.exists()) {
            domainsCollection.create(new CollectionCreateOptions().type(CollectionType.DOCUMENT));
        }
    }

    /**
     * Deletes the domain from the system
     *
     * @param domain
     * @return
     * @throws DomainNotFoundException
     */
    public Domain deleteDomain(Domain domain) throws DomainNotFoundException {
        if (Boolean.TRUE.equals(this.domainsCollection.documentExists(domain.getDomainName()))) {
            /**
             * Now we know that the domain exists. Lets delete it all
             */
            arangoDatabase.collection(domain.getNodes()).drop();
            arangoDatabase.collection(domain.getCircuits()).drop();
            arangoDatabase.collection(domain.getConnections()).drop();
            arangoDatabase.collection(domain.getServices()).drop();
            arangoDatabase.collection(domain.getMetrics()).drop();
            arangoDatabase.graph(domain.getConnectionLayer()).drop();
            this.domainsCollection.deleteDocument(domain.getDomainName());
            logger.debug("Domain :[{}] Deleted", domain.getDomainName());
            return domain;
        } else {
            throw new DomainNotFoundException(
                    "Domain with name:[" + domain.getDomainName() + "] not found");
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
    public Domain createDomain(Domain domainRequestDTO) throws DomainAlreadyExistsException {
        InventoryConfiguration inventoryConfiguration = configurationManager.loadConfiguration();
        ArangoDBConfiguration arangoDbConfiguration = inventoryConfiguration.getGraphDbConfiguration();

        String domainName = domainRequestDTO.getDomainName();

        logger.debug("Creating New Domain: {}", domainName);

        if (!this.domainsCollection.documentExists(domainName).booleanValue()) {
            CollectionEntity nodes = arangoDatabase
                    .createCollection(domainName
                            + arangoDbConfiguration.getNodeSufix(),
                            new CollectionCreateOptions().type(CollectionType.DOCUMENT));

            arangoDatabase.collection(domainName
                    + arangoDbConfiguration.getNodeSufix())
                    .ensurePersistentIndex(
                            Arrays.asList("name", "nodeAddress", "className",
                                    "domain._key"),
                            new PersistentIndexOptions().unique(true).name("NodeUNIQIDX"));

            //
            // Performance IDX
            //
            arangoDatabase.collection(domainName
                    + arangoDbConfiguration
                            .getNodeSufix())
                    .ensurePersistentIndex(Arrays.asList("nodeAddress", "className", "domainName"),
                            new PersistentIndexOptions().name("NodeSEARCHIDX"));

            CollectionEntity connections = arangoDatabase
                    .createCollection(domainName
                            + inventoryConfiguration.getGraphDbConfiguration()
                                    .getNodeConnectionSufix(),
                            new CollectionCreateOptions().type(CollectionType.EDGES));

            arangoDatabase.collection(connections.getName()).ensurePersistentIndex(
                    Arrays.asList("name", "nodeAddress", "className", "domain._key"),
                    new PersistentIndexOptions().unique(true).name("ConnectionUNIQIDX"));

            arangoDatabase.collection(connections.getName()).ensurePersistentIndex(
                    Arrays.asList("circuits[*]"),
                    new PersistentIndexOptions().unique(false).name("circuitsIDX"));

            arangoDatabase.collection(connections.getName())
                    .ensurePersistentIndex(
                            Arrays.asList("className", "domainName",
                                    "fromResource.nodeAddress",
                                    "fromResource.className",
                                    "fromResource.domainName",
                                    "toResource.nodeAddress",
                                    "toResource.className",
                                    "toResource.domainName"),
                            new PersistentIndexOptions().unique(false).name("searchIDX"));

            CollectionEntity services = arangoDatabase
                    .createCollection(domainName
                            + arangoDbConfiguration.getServiceSufix(),
                            new CollectionCreateOptions().type(CollectionType.DOCUMENT));

            arangoDatabase.collection(services.getName()).ensurePersistentIndex(
                    Arrays.asList("name", "nodeAddress", "className", "domain._key"),
                    new PersistentIndexOptions().unique(true).name("ServiceUNIQIDX"));

            CollectionEntity circuits = arangoDatabase
                    .createCollection(domainName
                            + arangoDbConfiguration.getCircuitsSufix(),
                            new CollectionCreateOptions().type(CollectionType.DOCUMENT));

            arangoDatabase.collection(circuits.getName()).ensurePersistentIndex(
                    Arrays.asList("name", "aPoint.nodeAddress", "aPoint.className",
                            "aPoint.domain._key",
                            "zPoint.nodeAddress", "zPoint.className", "zPoint.domain._key",
                            "className", "domain._key"),
                    new PersistentIndexOptions().unique(true).name("CircuitUNIQIDX"));

            arangoDatabase.collection(circuits.getName()).ensurePersistentIndex(
                    Arrays.asList("nodeAddress", "className", "domainName"),
                    new PersistentIndexOptions().unique(false).name("searchIDX1"));

            CollectionEntity metrics = arangoDatabase
                    .createCollection(domainName
                            + arangoDbConfiguration.getMetricSufix(),
                            new CollectionCreateOptions().type(CollectionType.DOCUMENT));

            arangoDatabase.collection(metrics.getName()).ensurePersistentIndex(
                    Arrays.asList("metricName", "domain._key"),
                    new PersistentIndexOptions().unique(true).name("MetricUNIQIDX"));

            GraphEntity connectionLayer = createGraph(
                    domainName + arangoDbConfiguration.getConnectionLayerSufix(),
                    connections.getName(), nodes.getName(), services.getName(), circuits.getName());

            domainRequestDTO.setServices(services.getName());
            domainRequestDTO.setConnectionLayer(connectionLayer.getName());
            domainRequestDTO.setConnections(connections.getName());
            domainRequestDTO.setNodes(nodes.getName());
            domainRequestDTO.setCircuits(circuits.getName());
            domainRequestDTO.setMetrics(metrics.getName());
            domainRequestDTO.setDomainName(domainName);

            this.domainsCollection.insertDocument(domainRequestDTO);

            logger.debug("Created Domain: {} With: NODES: {} EDGES: {} GRAPH: {}", domainName,
                    domainName + arangoDbConfiguration.getNodeSufix(), domainName
                    + arangoDbConfiguration.getNodeConnectionSufix(),
                    domainName
                    + arangoDbConfiguration.getConnectionLayerSufix());

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
    public List<Domain> getDomains() throws ArangoDaoException {
        try {
            logger.debug("Domains Size is: {}", this.domainsCollection.count().getCount());
            ArangoCursor<Domain> cursor = arangoDatabase.query("FOR doc IN domains filter doc.valid == true RETURN doc", Domain.class);
            List<Domain> result = cursor.asListRemaining();
            cursor.close();
            return result;
        } catch (IOException ex) {
            throw new ArangoDaoException(ex);
        }
    }

    public Boolean checkIfCollectionExists(String collectionName) {
        return this.arangoDatabase.collection(collectionName).exists();
    }

    /**
     * Atualiza o Domain
     *
     * @param domain
     */
    public DocumentUpdateEntity<Domain> updateDomain(Domain domain) {
        logger.debug("Persinting Domain info...:{}", domain.getDomainName());
        DocumentUpdateEntity<Domain> result = this.domainsCollection.updateDocument(domain.getDomainName(), domain, new DocumentUpdateOptions().mergeObjects(true).returnNew(true).returnOld(true));

        return result;
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
    private GraphEntity createGraph(String graphName, String connectionCollection, String nodesDocument,
            String serviceDocument, String circuitDocument) {
        EdgeDefinition edgeDefiniton = new EdgeDefinition()
                .collection(connectionCollection)
                .from(nodesDocument, serviceDocument, circuitDocument)
                .to(nodesDocument, serviceDocument, circuitDocument);
        Collection<EdgeDefinition> edgeDefinitions = Arrays.asList(edgeDefiniton);

        return arangoDatabase.createGraph(graphName, edgeDefinitions);
    }

};
