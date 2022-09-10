package com.osstelecom.db.inventory.manager.operation;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.arangodb.entity.DocumentCreateEntity;
import com.arangodb.entity.DocumentUpdateEntity;
import com.arangodb.entity.MultiDocumentEntity;
import com.osstelecom.db.inventory.graph.arango.GraphList;
import com.osstelecom.db.inventory.manager.dao.ResourceConnectionDao;
import com.osstelecom.db.inventory.manager.dto.FilterDTO;
import com.osstelecom.db.inventory.manager.events.ResourceConnectionCreatedEvent;
import com.osstelecom.db.inventory.manager.exception.ArangoDaoException;
import com.osstelecom.db.inventory.manager.exception.BasicException;
import com.osstelecom.db.inventory.manager.exception.DomainNotFoundException;
import com.osstelecom.db.inventory.manager.exception.GenericException;
import com.osstelecom.db.inventory.manager.exception.InvalidRequestException;
import com.osstelecom.db.inventory.manager.exception.ResourceNotFoundException;
import com.osstelecom.db.inventory.manager.exception.SchemaNotFoundException;
import com.osstelecom.db.inventory.manager.exception.ScriptRuleException;
import com.osstelecom.db.inventory.manager.listeners.EventManagerListener;
import com.osstelecom.db.inventory.manager.resources.BasicResource;
import com.osstelecom.db.inventory.manager.resources.Domain;
import com.osstelecom.db.inventory.manager.resources.ResourceConnection;
import com.osstelecom.db.inventory.manager.resources.exception.AttributeConstraintViolationException;
import com.osstelecom.db.inventory.manager.resources.model.ResourceSchemaModel;
import com.osstelecom.db.inventory.manager.session.DynamicRuleSession;
import com.osstelecom.db.inventory.manager.session.SchemaSession;

@Service
public class ResourceConnectionManager extends Manager {

    @Autowired
    private EventManagerListener eventManager;

    @Autowired
    private ReentrantLock lockManager;

    @Autowired
    private SchemaSession schemaSession;

    @Autowired
    private DomainManager domainManager;

    @Autowired
    private ResourceConnectionDao resourceConnectionDao;

    @Autowired
    private DynamicRuleSession dynamicRuleSession;

    /**
     * Cria uma nova Conexão entre dois elementos, Note que a ordem é importante
     * para garantir o Dê -> Para
     *
     * @param from
     * @param to
     * @return
     */
    public ResourceConnection createResourceConnection(BasicResource from, BasicResource to, String domainName) throws DomainNotFoundException, ArangoDaoException {
        String timerId = startTimer("createResourceConnection");
        try {
            lockManager.lock();
            Domain domain = this.domainManager.getDomain(domainName);
            return this.createResourceConnection(from, to, domain); // <-- Event Handled Here
        } finally {
            if (lockManager.isLocked()) {
                lockManager.unlock();
            }
            endTimer(timerId);

        }
    }

    public ResourceConnection createResourceConnection(ResourceConnection connection) throws GenericException, SchemaNotFoundException, AttributeConstraintViolationException, ScriptRuleException, ArangoDaoException {
        String timerId = startTimer("createResourceConnection");
        try {
            lockManager.lock();
            connection.setKey(this.getUUID());

            ResourceSchemaModel schemaModel = schemaSession.loadSchema(connection.getAttributeSchemaName());
            connection.setSchemaModel(schemaModel);
            schemaSession.validateResourceSchema(connection);
            dynamicRuleSession.evalResource(connection, "I", this);
            //
            // Creates the connection on DB
            //
            DocumentCreateEntity<ResourceConnection> result = resourceConnectionDao.insertResource(connection);
            connection.setKey(result.getId());
            connection.setRevisionId(result.getRev());
            //
            // Update Edges
            //

            ResourceConnectionCreatedEvent event = new ResourceConnectionCreatedEvent(connection);
            eventManager.notifyEvent(event);
            return connection;
        } finally {
            if (lockManager.isLocked()) {
                lockManager.unlock();
            }
            endTimer(timerId);
        }
    }

    /**
     * Cria uma nova Conexão entre dois elementos, Note que a ordem é importante
     * para garantir o Dê -> Para
     *
     * @param from
     * @param to
     * @return
     */
    public ResourceConnection createResourceConnection(BasicResource from, BasicResource to, Domain domain) throws ArangoDaoException {

        String timerId = startTimer("createResourceConnection");
        try {
            lockManager.lock();
            ResourceConnection connection = new ResourceConnection(domain);
            connection.setKey(this.getUUID());
            connection.setFrom(from);
            connection.setTo(to);

            // connection.setAtomId(this.getAtomId());
            connection.setAtomId(connection.getDomain().addAndGetId());
            //
            // Notifica o Elemento Origem para Computar o Consumo de recursos se necessário
            //
            // from.notifyConnection(connection);
            DocumentCreateEntity<ResourceConnection> result = resourceConnectionDao.insertResource(connection);
            connection.setKey(result.getId());
            connection.setRevisionId(result.getRev());

            ResourceConnectionCreatedEvent event = new ResourceConnectionCreatedEvent(connection);
            this.eventManager.notifyEvent(event);
            return connection;
        } finally {
            if (lockManager.isLocked()) {
                lockManager.unlock();
                endTimer(timerId);
            }
        }
    }

    /**
     * Search for a resource connection
     *
     * @param connection
     * @return
     * @throws ResourceNotFoundException
     * @throws ArangoDaoException
     */
    public ResourceConnection findResourceConnection(ResourceConnection connection) throws ResourceNotFoundException, ArangoDaoException {
        String timerId = startTimer("findResourceConnection");
        try {
            lockManager.lock();

            return this.resourceConnectionDao.findResource(connection);
        } finally {
            if (lockManager.isLocked()) {
                lockManager.unlock();
            }
            endTimer(timerId);
        }
    }

    /**
     * Update a Resource Connection
     *
     * @param connection
     * @return
     */
    public DocumentUpdateEntity<ResourceConnection> updateResourceConnection(ResourceConnection connection) throws ArangoDaoException {
        String timerId = startTimer("updateResourceConnection");
        try {
            lockManager.lock();
            connection.setLastModifiedDate(new Date());
            return this.resourceConnectionDao.updateResource(connection);
        } finally {
            if (lockManager.isLocked()) {
                lockManager.unlock();
            }
            endTimer(timerId);
        }
    }

    /**
     * Update a Resource Connection
     *
     * @param connection
     * @return
     */
    public List<ResourceConnection> updateResourceConnections(List<ResourceConnection> connections, Domain domain) throws ArangoDaoException {
        String timerId = startTimer("updateResourceConnections:[" + connections.size() + "]");
        try {
            lockManager.lock();
            connections.forEach(connection -> {
                connection.setLastModifiedDate(new Date());
            });
            MultiDocumentEntity<DocumentUpdateEntity<ResourceConnection>> results = this.resourceConnectionDao.updateResources(connections, domain);
            List<ResourceConnection> resultDocs = new ArrayList<>();
            results.getDocuments().forEach(connection -> {
                //
                // Adiciona a conexão criada no documento
                //
                resultDocs.add(connection.getNew());
            });
            return resultDocs;
        } finally {
            if (lockManager.isLocked()) {
                lockManager.unlock();
            }
            endTimer(timerId);
        }
    }

    public GraphList<ResourceConnection> getConnectionsByFilter(FilterDTO filter, String domainName) throws ArangoDaoException, DomainNotFoundException, InvalidRequestException {
        Domain domain = domainManager.getDomain(domainName);
        if (filter.getObjects().contains("connections")) {
            HashMap<String, Object> bindVars = new HashMap<>();

            if (filter.getClasses() != null && !filter.getClasses().isEmpty()) {
                bindVars.put("classes", filter.getClasses());
            }

            if (filter.getBindings() != null && !filter.getBindings().isEmpty()) {
                bindVars.putAll(filter.getBindings());
            }
            return this.resourceConnectionDao.findResourceByFilter(filter.getAqlFilter(), bindVars, domain);
        }
        throw new InvalidRequestException("getConnectionsByFilter() can only retrieve connections objects");
    }
}
