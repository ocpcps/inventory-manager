package com.osstelecom.db.inventory.manager.operation;

import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import com.arangodb.entity.DocumentCreateEntity;
import com.arangodb.entity.DocumentDeleteEntity;
import com.arangodb.entity.DocumentUpdateEntity;
import com.google.common.eventbus.Subscribe;
import com.osstelecom.db.inventory.manager.dao.CircuitResourceDao;
import com.osstelecom.db.inventory.manager.dao.GraphDao;
import com.osstelecom.db.inventory.manager.dao.ResourceConnectionDao;
import com.osstelecom.db.inventory.manager.dto.FilterDTO;
import com.osstelecom.db.inventory.manager.events.CircuitPathUpdatedEvent;
import com.osstelecom.db.inventory.manager.events.CircuitResourceCreatedEvent;
import com.osstelecom.db.inventory.manager.events.CircuitResourceUpdatedEvent;
import com.osstelecom.db.inventory.manager.events.ManagedResourceUpdatedEvent;
import com.osstelecom.db.inventory.manager.events.ResourceConnectionUpdatedEvent;
import com.osstelecom.db.inventory.manager.exception.ArangoDaoException;
import com.osstelecom.db.inventory.manager.exception.DomainNotFoundException;
import com.osstelecom.db.inventory.manager.exception.GenericException;
import com.osstelecom.db.inventory.manager.exception.InvalidRequestException;
import com.osstelecom.db.inventory.manager.exception.ResourceNotFoundException;
import com.osstelecom.db.inventory.manager.exception.SchemaNotFoundException;
import com.osstelecom.db.inventory.manager.exception.ScriptRuleException;
import com.osstelecom.db.inventory.manager.listeners.EventManagerListener;
import com.osstelecom.db.inventory.manager.resources.CircuitResource;
import com.osstelecom.db.inventory.manager.resources.Domain;
import com.osstelecom.db.inventory.manager.resources.GraphList;
import com.osstelecom.db.inventory.manager.resources.ManagedResource;
import com.osstelecom.db.inventory.manager.resources.ResourceConnection;
import com.osstelecom.db.inventory.manager.resources.exception.AttributeConstraintViolationException;
import com.osstelecom.db.inventory.manager.resources.model.ResourceSchemaModel;
import com.osstelecom.db.inventory.manager.session.DynamicRuleSession;
import com.osstelecom.db.inventory.manager.session.SchemaSession;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Service
public class CircuitResourceManager extends Manager {

    @Autowired
    private EventManagerListener eventManager;

    @Autowired
    private LockManager lockManager;

    @Autowired
    private SchemaSession schemaSession;

    @Autowired
    private CircuitResourceDao circuitResourceDao;

    @Autowired
    private DynamicRuleSession dynamicRuleSession;

    @Autowired
    private ResourceConnectionDao resourceConnectionDao;

    @Autowired
    private DomainManager domainManager;

    @Autowired
    private GraphDao graphDao;

    private Logger logger = LoggerFactory.getLogger(CircuitResourceManager.class);

    public GraphList<CircuitResource> findAll(Domain domain) throws ArangoDaoException, ResourceNotFoundException, InvalidRequestException {
        return this.circuitResourceDao.findAll(domain);
    }

    public CircuitResource deleteCircuitResource(CircuitResource circuitResource) throws ArangoDaoException {
        DocumentDeleteEntity<CircuitResource> result = this.circuitResourceDao.deleteResource(circuitResource);
        return result.getOld();
    }

    /**
     * Creates a Circuit Resource
     *
     * @param circuit
     * @return
     * @throws GenericException
     * @throws SchemaNotFoundException
     * @throws AttributeConstraintViolationException
     * @throws ScriptRuleException
     */
    public CircuitResource createCircuitResource(CircuitResource circuit) throws GenericException,
            SchemaNotFoundException, AttributeConstraintViolationException, ScriptRuleException, ArangoDaoException {
        String timerId = startTimer("createCircuitResource");
        Boolean useUpsert = false;
        try {
            lockManager.lock();
            Domain domain = circuit.getDomain();
            //
            // START - Subir as validações para session
            //
            if (circuit.getKey() == null) {
                circuit.setKey(getUUID());
            } else {
                //
                // Teve um ID declarado pelo usuário ou solicitante, podemos converter isso para um upsert
                //
                useUpsert = true;
            }

            circuit.setAtomId(domain.addAndGetId());
            ResourceSchemaModel schemaModel = schemaSession.loadSchema(circuit.getAttributeSchemaName());            
            circuit.setSchemaModel(schemaModel);
            
            schemaSession.validateResourceSchema(circuit);
            
          
            dynamicRuleSession.evalResource(circuit, "I", this);
            DocumentCreateEntity<CircuitResource> result;
            if (useUpsert) {
                result = this.circuitResourceDao.upsertResource(circuit);
            } else {
                result = this.circuitResourceDao.insertResource(circuit);

            }
            /**
             * Fix
             */
            circuit.setKey(result.getKey());
            circuit.setRevisionId(result.getRev());
            //
            // Aqui criou o circuito
            //
            CircuitResourceCreatedEvent event = new CircuitResourceCreatedEvent(circuit);
            this.eventManager.notifyResourceEvent(event);
            return circuit;
        } finally {
            if (lockManager.isLocked()) {
                lockManager.unlock();
            }
            endTimer(timerId);
        }
    }

    /**
     * Find a Circuit Instance
     *
     * @param circuit
     * @return
     * @throws ResourceNotFoundException
     * @throws ArangoDaoException
     */
    public CircuitResource findCircuitResource(CircuitResource circuit)
            throws ResourceNotFoundException, ArangoDaoException, InvalidRequestException {
        String timerId = startTimer("findCircuitResource");
        try {
            lockManager.lock();

            //
            // Arruma o ID :)
            //
            if (circuit.getId() != null) {
                if (!circuit.getId().contains("/")) {
                    circuit.setId(circuit.getDomain().getCircuits() + "/" + circuit.getId());
                }
            }

            return this.circuitResourceDao.findResource(circuit);
        } finally {
            if (lockManager.isLocked()) {
                lockManager.unlock();
            }
            endTimer(timerId);
        }
    }

    /**
     * Criado para diferenciar o motivo da atualização
     *
     * @param resource
     * @return
     * @throws ArangoDaoException
     * @throws SchemaNotFoundException
     * @throws GenericException
     * @throws AttributeConstraintViolationException
     * @throws ScriptRuleException
     */
    public CircuitResource updateCircuitPath(CircuitResource resource) throws ArangoDaoException, SchemaNotFoundException, GenericException, AttributeConstraintViolationException, ScriptRuleException {

        /**
         * então atualizou os paths. vamos verificar depois se o
         */
        CircuitResource updatedResource = this.updateCircuitResource(resource);
        CircuitPathUpdatedEvent event = new CircuitPathUpdatedEvent(updatedResource);
        this.eventManager.notifyResourceEvent(event);
        return updatedResource;
    }

    /**
     * Update a circuit
     *
     * @param resource
     * @return
     */
    public CircuitResource updateCircuitResource(CircuitResource resource) throws ArangoDaoException, SchemaNotFoundException, GenericException, AttributeConstraintViolationException, ScriptRuleException {
        String timerId = startTimer("updateCircuitResource");
        try {
            lockManager.lock();
            resource.setLastModifiedDate(new Date());

            ResourceSchemaModel schemaModel = schemaSession.loadSchema(resource.getAttributeSchemaName());
            resource.setSchemaModel(schemaModel);
            schemaSession.validateResourceSchema(resource);
            dynamicRuleSession.evalResource(resource, "U", this);

            DocumentUpdateEntity<CircuitResource> result = circuitResourceDao.updateResource(resource);
            CircuitResource newResource = result.getNew();
            CircuitResource oldResource = result.getOld();

            CircuitResourceUpdatedEvent event = new CircuitResourceUpdatedEvent(oldResource, newResource);
            //
            // Emits the transitional event
            //
            this.eventManager.notifyResourceEvent(event);
            return newResource;
        } finally {
            if (lockManager.isLocked()) {
                lockManager.unlock();
            }
            endTimer(timerId);
        }
    }

    /**
     * Return the circuit connections( Paths )
     *
     * @param circuit
     * @return
     * @throws ArangoDaoException
     */
    public GraphList<ResourceConnection> findCircuitPaths(CircuitResource circuit) {
        String timerId = startTimer("findCircuitPaths");
        try {
            return this.graphDao.findCircuitPaths(circuit, true);
        } finally {
            endTimer(timerId);
        }
    }

    public GraphList<ResourceConnection> findCircuitPaths(CircuitResource circuit, Boolean useTraversal) {
        String timerId = startTimer("findCircuitPaths:useTraversal[" + useTraversal + "]");
        try {
            return this.graphDao.findCircuitPaths(circuit, useTraversal);
        } finally {
            endTimer(timerId);
        }
    }

    public GraphList<CircuitResource> findCircuitsByFilter(FilterDTO filter, Domain domain) throws ArangoDaoException, ResourceNotFoundException, InvalidRequestException {
        String timerId = startTimer("findCircuitsByFilter");
        try {
            return this.circuitResourceDao.findResourceByFilter(filter, domain);
        } finally {
            endTimer(timerId);
        }
    }

    public GraphList<CircuitResource> findCircuitsByFilter(FilterDTO filter, String domainName) throws ArangoDaoException, ResourceNotFoundException, InvalidRequestException, DomainNotFoundException {
        String timerId = startTimer("findCircuitsByFilter");
        try {
            Domain domain = this.domainManager.getDomain(domainName);
            return this.circuitResourceDao.findResourceByFilter(filter, domain);
        } finally {
            endTimer(timerId);
        }
    }

    @EventListener(ApplicationReadyEvent.class)
    private void onStartUp() {
        eventManager.registerListener(this);
    }

    @Subscribe
    public void onCircuitPathUpdatedEvent(CircuitPathUpdatedEvent event) throws ResourceNotFoundException, InvalidRequestException, SchemaNotFoundException, GenericException, AttributeConstraintViolationException, ScriptRuleException {
        try {
            CircuitResource circuit = this.findCircuitResource(event.getNewResource());
            this.computeCircuitIntegrity(circuit);
        } catch (ResourceNotFoundException ex) {
            logger.warn("Inconsistent Database Detetected on computeCircuitIntegrity()");
        } catch (ArangoDaoException ex) {
            logger.warn("Generic Error on computeCircuitIntegrity()", ex);
        }
    }

    /**
     * Recebemos a notificação de que uma conexão foi atualizada
     *
     * @param updatedEvent
     */
    @Subscribe
    public void onResourceConnectionUpdatedEvent(ResourceConnectionUpdatedEvent updatedEvent) throws InvalidRequestException, SchemaNotFoundException, GenericException, AttributeConstraintViolationException, ScriptRuleException {
        String timerId = startTimer("CircuitResourceManager.onResourceConnectionUpdatedEvent");
        try {
            ResourceConnection newConnection = updatedEvent.getNewResource();
            ResourceConnection oldConnection = updatedEvent.getOldResource();
            if (newConnection.getCircuits() != null) {
                if (!newConnection.getCircuits().isEmpty()) {
                    //
                    // Uma Conexão foi atualizada.
                    //
                    if (!newConnection.getOperationalStatus().equals(oldConnection.getOperationalStatus())) {
                        //
                        // Trasicionou o estado da conexão.
                        //
                        logger.debug("Connection:[{}] carries:[{}] circuits", newConnection.getId(), newConnection.getCircuits().size());
                        for (String circuitId : newConnection.getCircuits()) {
                            try {
                                CircuitResource circuit = this.findCircuitResource(new CircuitResource(newConnection.getDomain(), circuitId));
                                this.computeCircuitIntegrity(circuit);
                            } catch (ResourceNotFoundException ex) {
                                logger.warn("Inconsistent Database Detetected");
                            } catch (ArangoDaoException ex) {
                                logger.warn("Generic Error", ex);
                            }
                        }

                    }
                } else {
                    logger.debug("Connection ID[{}] Has No Circuits Empty", newConnection.getKey());
                }
            } else {
                logger.debug("Connection ID[{}] Has No Circuits Null", newConnection.getKey());
            }
        } finally {
            endTimer(timerId);
        }

    }

    /**
     * Recebi uma notificação de que um recurso foi atualizado
     *
     * @param updatedEvent
     */
    @Subscribe
    public void onManagedResourceUpdatedEvent(ManagedResourceUpdatedEvent updatedEvent) throws ArangoDaoException, IOException, InvalidRequestException {
        String timerId = startTimer("onManagedResourceUpdatedEvent");
        //
        // Vamos filtrar se algum circuito usa  a gente
        //
        String filter = "(doc.aPoint._id == @resourceId or doc.zPoint._id == @resourceId) ";
        Map<String, Object> bindVars = new HashMap<>();
        bindVars.put("resourceId", updatedEvent.getNewResource().getId());
        try {
            this.circuitResourceDao.findResourceByFilter(new FilterDTO(filter, "sort doc.nodeAddress", bindVars), updatedEvent.getNewResource().getDomain()).forEach(circuit -> {
                try {
                    if (circuit.getaPoint().getId().equals(updatedEvent.getNewResource().getId())) {

                        circuit.setaPoint(updatedEvent.getNewResource());
                        logger.debug("Updated A Point in Circuit [{}]", circuit.getId());
                    } else if (circuit.getzPoint().getId().equals(updatedEvent.getNewResource().getId())) {

                        circuit.setzPoint(updatedEvent.getNewResource());
                        logger.debug("Updated Z Point in Circuit [{}]", circuit.getId());
                    } else {
                        logger.warn("Inconsistent Database Data Detected");

                    }
                    this.updateCircuitResource(circuit);
                } catch (ArangoDaoException | AttributeConstraintViolationException | GenericException | SchemaNotFoundException | ScriptRuleException ex) {
                    logger.error("Failed to Update Resources on circuit", ex);
                }
            });
        } catch (ResourceNotFoundException ex) {
            //
            // This is expetected
            //
        } finally {
            endTimer(timerId);
        }
    }

    /**
     * Check if a circuit is OK or Not, if the transition happen, we should
     * update it
     *
     * @param circuit
     * @param connections
     * @param target
     */
    private void computeCircuitIntegrity(CircuitResource circuit) throws ArangoDaoException, SchemaNotFoundException, GenericException, AttributeConstraintViolationException, ScriptRuleException {
        String timerId = startTimer("computeCircuitIntegrity");
        try {

            if (circuit.getaPoint().equals(circuit.getzPoint()) || circuit.getaPoint().getId().equals(circuit.getzPoint().getId())) {
                //
                // This is very wrong... Never we can reach an end here
                //
                logger.warn("Invalid Circuit Detected [{}], aPoint and zPoint are The same, this generates a circular reference", circuit.getId());
                circuit.setBroken(true);
                circuit.setOperationalStatus("Down");
                this.updateCircuitResource(circuit);
                return;
            }

            //
            // Forks with the logic of checking integrity
            //
            Long start = System.currentTimeMillis();
            if (circuit.getCircuitPath() != null) {
                logger.debug("Starting Circuit Computing on:[{}] With:[{}] Paths", circuit.getId(), circuit.getCircuitPath().size());
            } else {
                logger.debug("Starting Circuit Computing on:[{}]", circuit.getId());
            }
            boolean stateChanged = false;
            /**
             * Não precisa ser uma travessia!, nos conhecemos o caminhos podemos
             * usar uma simples query!
             */
            List<ResourceConnection> connections = this.findCircuitPaths(circuit, false).toList();
            boolean degratedFlag = false;
            /**
             * Varre as conexões para ver se tem alguma down, se tiver já é
             * degradado
             */
            for (ResourceConnection connection : connections) {
                logger.debug("Connection [{}] Status:[{}]", connection.getId(), connection.getOperationalStatus());
                //
                // get current node status
                //
                if (!connection.getOperationalStatus().equalsIgnoreCase("UP")) {
                    //
                    // Transitou de normal para degradado
                    //
                    degratedFlag = true;
                }

            }

            if (circuit.getDegrated()) {
                if (!degratedFlag) {
                    //
                    // Estava degradado e agora normalizou
                    //
                    circuit.setDegrated(false);
                    stateChanged = true;
                }
            } else {
                if (degratedFlag) {
                    //
                    // Estava bom e agora degradou
                    //
                    circuit.setDegrated(true);
                    stateChanged = true;
                }
            }

            //
            // Checks the current state of the circuit
            //
            List<String> brokenNodes = this.domainManager.checkBrokenGraph(connections, circuit.getaPoint());

            //
            //
            //
            if (!brokenNodes.isEmpty()) {
                //
                // Check if the broken nodes has the zPoint or aPoint,
                // If so it means that the circuit is broken!
                //
                if (brokenNodes.contains(circuit.getzPoint().getId())
                        || brokenNodes.contains(circuit.getaPoint().getId())) {
                    if (!circuit.getBroken()) {
                        //
                        // Transitou para Broken..
                        //
                        circuit.setBroken(true);
                        stateChanged = true;
                    }
                } else if (circuit.getBroken()) {
                    //
                    // Normalizou
                    //
                    circuit.setBroken(false);
                    stateChanged = true;

                }

                circuit.setBrokenResources(brokenNodes);
            } else if (circuit.getBroken()) {
                //
                // Normalizou
                //
                circuit.setBroken(false);
                stateChanged = true;

            }

            if (circuit.getBroken()) {
                circuit.setOperationalStatus("Down");
                circuit.setDegrated(true);
            } else {
                circuit.setOperationalStatus("Up");
            }
            if (circuit.getBrokenResources() != null) {
                if (!circuit.getBroken() && !circuit.getBrokenResources().isEmpty()) {
                    stateChanged = true;
                    circuit.getBrokenResources().clear();
                }
            }

            Long end = System.currentTimeMillis();
            Long took = end - start;
            if (circuit.getBrokenResources() != null) {
                logger.debug("Check Circuit Integrity for [{}] Took: {} ms State Changed: {} Broken Count:[{}] Total Connections:[{}]",
                        circuit.getId(), took, stateChanged, circuit.getBrokenResources().size(), connections.size());
            } else {
                logger.debug("Check Circuit Integrity for [{}] Took: {} ms State Changed: {}  Total Connections:[{}]",
                        circuit.getId(), took, stateChanged, connections.size());
            }
            if (stateChanged) {
                this.updateCircuitResource(circuit);
            }
        } finally {
            logger.debug("Done Circuit Computing on:[{}]", circuit.getId());
            endTimer(timerId);
        }
    }

    /**
     * Recebe as notificações para Circuitos Criados
     *
     * @param circuit
     */
    @Subscribe
    public void onCircuitResourceCreatedEvent(CircuitResourceCreatedEvent circuit) {
        logger.debug("Resource Created ID:[{}]]", circuit.getNewResource().getId());
    }

}
