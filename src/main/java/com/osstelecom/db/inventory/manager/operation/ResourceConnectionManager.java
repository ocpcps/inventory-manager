package com.osstelecom.db.inventory.manager.operation;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.arangodb.entity.DocumentCreateEntity;
import com.arangodb.entity.DocumentDeleteEntity;
import com.arangodb.entity.DocumentUpdateEntity;
import com.arangodb.entity.MultiDocumentEntity;
import com.google.common.eventbus.Subscribe;
import com.osstelecom.db.inventory.manager.dao.ResourceConnectionDao;
import com.osstelecom.db.inventory.manager.dto.FilterDTO;
import com.osstelecom.db.inventory.manager.events.ManagedResourceUpdatedEvent;
import com.osstelecom.db.inventory.manager.events.ResourceConnectionCreatedEvent;
import com.osstelecom.db.inventory.manager.events.ResourceConnectionUpdatedEvent;
import com.osstelecom.db.inventory.manager.events.ServiceStateTransionedEvent;
import com.osstelecom.db.inventory.manager.exception.ArangoDaoException;
import com.osstelecom.db.inventory.manager.exception.DomainNotFoundException;
import com.osstelecom.db.inventory.manager.exception.GenericException;
import com.osstelecom.db.inventory.manager.exception.InvalidRequestException;
import com.osstelecom.db.inventory.manager.exception.ResourceNotFoundException;
import com.osstelecom.db.inventory.manager.exception.SchemaNotFoundException;
import com.osstelecom.db.inventory.manager.exception.ScriptRuleException;
import com.osstelecom.db.inventory.manager.listeners.EventManagerListener;
import com.osstelecom.db.inventory.manager.resources.BasicResource;
import com.osstelecom.db.inventory.manager.resources.Domain;
import com.osstelecom.db.inventory.manager.resources.GraphList;
import com.osstelecom.db.inventory.manager.resources.ManagedResource;
import com.osstelecom.db.inventory.manager.resources.ResourceConnection;
import com.osstelecom.db.inventory.manager.resources.ServiceResource;
import com.osstelecom.db.inventory.manager.resources.exception.AttributeConstraintViolationException;
import com.osstelecom.db.inventory.manager.resources.model.ResourceSchemaModel;
import com.osstelecom.db.inventory.manager.session.DynamicRuleSession;
import com.osstelecom.db.inventory.manager.session.SchemaSession;
import java.io.IOException;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

@Service
public class ResourceConnectionManager extends Manager {
    
    @Autowired
    private EventManagerListener eventManager;
    
    @Autowired
    private LockManager lockManager;
    
    @Autowired
    private SchemaSession schemaSession;
    
    @Autowired
    private DomainManager domainManager;
    
    @Autowired
    private ResourceConnectionDao resourceConnectionDao;
    
    @Autowired
    private DynamicRuleSession dynamicRuleSession;
    
    @Autowired
    private ServiceManager serviceManager;
    
    private Logger logger = LoggerFactory.getLogger(ResourceConnectionManager.class);
    
    @EventListener(ApplicationReadyEvent.class)
    private void onStartUp() {
        this.eventManager.registerListener(this);
    }
    
    public ResourceConnection deleteResourceConnection(ResourceConnection connection) throws ArangoDaoException {
        DocumentDeleteEntity<ResourceConnection> result = this.resourceConnectionDao.deleteResource(connection);
        return result.getOld();
    }

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
    
    public ResourceConnection createResourceConnection(ResourceConnection connection) throws GenericException, SchemaNotFoundException, AttributeConstraintViolationException, ScriptRuleException, ArangoDaoException, ResourceNotFoundException, InvalidRequestException, DomainNotFoundException {
        String timerId = startTimer("createResourceConnection");
        try {
            lockManager.lock();
            
            Boolean useUpsert = false;
            if (connection.getKey() == null) {
                connection.setKey(this.getUUID());
            } else {
                useUpsert = true;
            }
            
            if (connection.getDependentService() != null) {
                connection.setDependentService(connection.getDependentService());
            }
            
            if (connection.getDependentService() != null) {
                //
                // Valida se o serviço existe
                //
                ServiceResource service = this.serviceManager.getService(connection.getDependentService());
                //
                // Arruma com a referencia do DB
                //
                connection.setDependentService(service);
                //
                // Agora vamos ver se o serviço é de um dominio diferente do recurso... não podem ser do mesmo
                //

                if (service.getDomain().getDomainName().equals(connection.getDomain().getDomainName())) {
                    throw new InvalidRequestException("Resource and Parent Service cannot be in the same domain.");
                }
            }
            ResourceSchemaModel schemaModel = schemaSession.loadSchema(connection.getAttributeSchemaName());
            connection.setSchemaModel(schemaModel);
            schemaSession.validateResourceSchema(connection);
            dynamicRuleSession.evalResource(connection, "I", this);
            
            DocumentCreateEntity<ResourceConnection> result;
            
            if (useUpsert) {
                //
                // Will Try to update or insert the connection
                //
                result = this.resourceConnectionDao.upsertResource(connection);
            } else {
                //
                // Creates the connection on DB
                //
                result = resourceConnectionDao.insertResource(connection);
                
            }
            
            connection.setKey(result.getId());
            connection.setRevisionId(result.getRev());
            //
            // Update Edges
            //

            ResourceConnectionCreatedEvent event = new ResourceConnectionCreatedEvent(result);
            eventManager.notifyResourceEvent(event);
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
            this.eventManager.notifyResourceEvent(event);
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
    public ResourceConnection findResourceConnection(ResourceConnection connection) throws ResourceNotFoundException, ArangoDaoException, InvalidRequestException {
        String timerId = startTimer("findResourceConnection");
        try {
            
            if (connection.getId() != null) {
                if (!connection.getId().contains("/")) {
                    connection.setId(connection.getDomain().getConnections() + "/" + connection.getId());
                }
            }
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
    public DocumentUpdateEntity<ResourceConnection> updateResourceConnection(ResourceConnection connection) throws ArangoDaoException, AttributeConstraintViolationException {
        String timerId = startTimer("updateResourceConnection");
        try {
            lockManager.lock();
            connection.setLastModifiedDate(new Date());
            
            schemaSession.validateResourceSchema(connection);
            
            DocumentUpdateEntity<ResourceConnection> result = this.resourceConnectionDao.updateResource(connection);
            ResourceConnectionUpdatedEvent updateEvent = new ResourceConnectionUpdatedEvent(result);
            this.eventManager.notifyResourceEvent(updateEvent);
            return result;
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
    
    public GraphList<ResourceConnection> getConnectionsByFilter(FilterDTO filter, String domainName) throws ArangoDaoException, DomainNotFoundException, InvalidRequestException, ResourceNotFoundException {
        Domain domain = domainManager.getDomain(domainName);
        if (filter.getObjects().contains("connections")) {
//            HashMap<String, Object> bindVars = new HashMap<>();

            if (filter.getClasses() != null && !filter.getClasses().isEmpty()) {
                filter.getBindings().put("classes", filter.getClasses());
            }
            
            if (filter.getBindings() != null && !filter.getBindings().isEmpty()) {
                filter.getBindings().putAll(filter.getBindings());
            }
            return this.resourceConnectionDao.findResourceByFilter(filter, domain);
        }
        throw new InvalidRequestException("getConnectionsByFilter() can only retrieve connections objects");
    }

    /**
     * Recebi uma notificação de que um recurso foi atualizado, vou procurar as
     * conexões que ele possui e atualizar as referencias.
     *
     * @param updateEvent
     */
    @Subscribe
    public void onManagedResourceUpdatedEvent(ManagedResourceUpdatedEvent updateEvent) {
        ManagedResource updatedResource = updateEvent.getNewResource();
        // Update the related dependencies
        //
        try {
            List<String> relatedCircuits = new ArrayList<>();
            //
            // Transcrever para um filtro, que busca os recursos relacionados
            // Eventualmente irá se tornar um método no CircuitManager
            //
            String filter = "@resourceId in  doc.relatedNodes[*]";
            Map<String, Object> bindVars = new HashMap<>();
            bindVars.put("resourceId", updatedResource.getId());
            //
            // Procura as conexões relacionadas no mesmo dominio
            //
            try {
                this.resourceConnectionDao.findResourceByFilter(new FilterDTO(filter, bindVars), updatedResource.getDomain()).forEach((connection) -> {
                    
                    if (connection.getFrom().getKey().equals(updatedResource.getKey())) {
                        //
                        // Update from
                        //

                        connection.setFrom(updatedResource);
                        //
                        // validando 
                        //

                    } else if (connection.getTo().getKey().equals(updatedResource.getKey())) {
                        //
                        // Update to
                        //
                        connection.setTo(updatedResource);
                        
                    }

                    //
                    // Reflete o estado da conexão
                    //
                    if (!connection.getOperationalStatus().equals(updatedResource.getOperationalStatus())) {
                        connection.setOperationalStatus(updatedResource.getOperationalStatus());
                    }
                    
                    try {
                        //
                        // Atualiza a conexão
                        //
                        this.updateResourceConnection(connection); // <- Atualizou a conexão no banco

                    } catch (ArangoDaoException | AttributeConstraintViolationException ex) {
                        logger.error("Failed to Update Circuit: [{}]", connection.getId(), ex);
                    }
                });
            } catch (ResourceNotFoundException ex) {
                //
                // This is expected
                //
            }
            
        } catch (IOException | IllegalStateException | InvalidRequestException | ArangoDaoException ex) {
            logger.error("Failed to Update Resource Connection Relation", ex);
            
        }
    }

    /**
     * Recebe as notificações de Serviços
     *
     * @param serviceUpdateEvent
     */
    @Subscribe
    public void onServiceStateTransionedEvent(ServiceStateTransionedEvent serviceStateTransitionedEvent) throws DomainNotFoundException, ResourceNotFoundException, ArangoDaoException, InvalidRequestException, AttributeConstraintViolationException {
        if (serviceStateTransitionedEvent.getNewResource().getRelatedResourceConnections() != null
                && !serviceStateTransitionedEvent.getNewResource().getRelatedResourceConnections().isEmpty()) {
            
            for (String connectionId : serviceStateTransitionedEvent.getNewResource().getRelatedResourceConnections()) {
                String domainName = this.domainManager.getDomainNameFromId(connectionId);
                if (!domainName.equals(serviceStateTransitionedEvent.getNewResource().getDomainName())) {
                    //
                    // Só propaga se o serviço e o recurso estiverem em dominios diferentes!
                    // Isto é para "tentar" evitar uma referencia circular, é para tentar, pois não garante.
                    //

                    //
                    // Então sabemos que o Serviço em questão afeta recursos de outros dominios!
                    //
                    Domain domain = this.domainManager.getDomain(domainName);
                    ResourceConnection connection = new ResourceConnection(domain, connectionId);
                    
                    connection.setAttributeSchemaName(null);

                    //
                    // Obtem a referencia do DB
                    //
                    connection = this.findResourceConnection(connection);

                    //
                    // Replica o estado do Serviço no Recurso.
                    // 
                    connection.setOperationalStatus(serviceStateTransitionedEvent.getNewResource().getOperationalStatus());
                    connection.setDependentService(serviceStateTransitionedEvent.getNewResource());
                    //
                    // Atualiza tudo, retrigando todo ciclo novamente
                    //
                    this.updateResourceConnection(connection);
                }
            }
        }
    }
}
