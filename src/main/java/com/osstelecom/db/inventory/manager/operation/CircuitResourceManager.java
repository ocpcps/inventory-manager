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
import com.arangodb.entity.DocumentUpdateEntity;
import com.google.common.eventbus.Subscribe;
import com.osstelecom.db.inventory.graph.arango.GraphList;
import com.osstelecom.db.inventory.manager.dao.CircuitResourceDao;
import com.osstelecom.db.inventory.manager.dao.ResourceConnectionDao;
import com.osstelecom.db.inventory.manager.events.CircuitResourceCreatedEvent;
import com.osstelecom.db.inventory.manager.events.CircuitResourceUpdatedEvent;
import com.osstelecom.db.inventory.manager.events.CircuitStateTransionedEvent;
import com.osstelecom.db.inventory.manager.events.ProcessCircuityIntegrityEvent;
import com.osstelecom.db.inventory.manager.exception.ArangoDaoException;
import com.osstelecom.db.inventory.manager.exception.GenericException;
import com.osstelecom.db.inventory.manager.exception.ResourceNotFoundException;
import com.osstelecom.db.inventory.manager.exception.SchemaNotFoundException;
import com.osstelecom.db.inventory.manager.exception.ScriptRuleException;
import com.osstelecom.db.inventory.manager.listeners.EventManagerListener;
import com.osstelecom.db.inventory.manager.resources.CircuitResource;
import com.osstelecom.db.inventory.manager.resources.Domain;
import com.osstelecom.db.inventory.manager.resources.ResourceConnection;
import com.osstelecom.db.inventory.manager.resources.exception.AttributeConstraintViolationException;
import com.osstelecom.db.inventory.manager.resources.model.ResourceSchemaModel;
import com.osstelecom.db.inventory.manager.session.DynamicRuleSession;
import com.osstelecom.db.inventory.manager.session.SchemaSession;

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

    private Logger logger = LoggerFactory.getLogger(CircuitResourceManager.class);

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
        try {
            lockManager.lock();
            Domain domain = circuit.getDomain();
            circuit.setKey(this.getUUID());
            circuit.setAtomId(domain.addAndGetId());
            ResourceSchemaModel schemaModel = schemaSession.loadSchema(circuit.getAttributeSchemaName());
            circuit.setSchemaModel(schemaModel);
            schemaSession.validateResourceSchema(circuit);
            dynamicRuleSession.evalResource(circuit, "I", this);
            DocumentCreateEntity<CircuitResource> result = this.circuitResourceDao.insertResource(circuit);
            circuit.setKey(result.getId());
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
            throws ResourceNotFoundException, ArangoDaoException {
        String timerId = startTimer("findCircuitResource");
        try {
            lockManager.lock();
            return this.circuitResourceDao.findResource(circuit);
        } finally {
            if (lockManager.isLocked()) {
                lockManager.unlock();
            }
            endTimer(timerId);
        }
    }

    /**
     * Update a circuit
     *
     * @param resource
     * @return
     */
    public CircuitResource updateCircuitResource(CircuitResource resource) throws ArangoDaoException {
        String timerId = startTimer("updateCircuitResource");
        try {
            lockManager.lock();
            resource.setLastModifiedDate(new Date());
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
        return this.resourceConnectionDao.findCircuitPaths(circuit);
    }

    @EventListener(ApplicationReadyEvent.class)
    private void onStartUp() {
        eventManager.registerListener(this);
    }

    @Subscribe
    public void onProcessCircuityIntegrityEvent(ProcessCircuityIntegrityEvent processEvent) throws ArangoDaoException {
        logger.debug("A Circuit[{}] Dependency has been updated ,Integrity Needs to be recalculated",
                processEvent.getNewResource().getId());
        this.computeCircuitIntegrity(processEvent.getNewResource());
    }

    /**
     * Check if a circuit is OK or Not, if the transition happen, we should
     * update it
     *
     * @param circuit
     * @param connections
     * @param target
     */
    private void computeCircuitIntegrity(CircuitResource circuit) throws ArangoDaoException {
        //
        // Forks with the logic of checking integrity
        //
        Long start = System.currentTimeMillis();
        boolean stateChanged = false;
        List<ResourceConnection> connections = this.findCircuitPaths(circuit).toList();
        boolean degratedFlag = false;
        for (ResourceConnection connection : connections) {

            //
            // get current node status
            //
            if (!connection.getOperationalStatus().equalsIgnoreCase("UP") && !circuit.getDegrated()) {
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
            circuit.setOperationalStatus("DOWN");
        } else {
            circuit.setOperationalStatus("UP");
        }

        if (!circuit.getBroken() && !circuit.getBrokenResources().isEmpty()) {
            stateChanged = true;
            circuit.getBrokenResources().clear();
        }

        Long end = System.currentTimeMillis();
        Long took = end - start;
        logger.debug("Check Circuit Integrity for [{}] Took: {} ms State Changed: {} Broken Count:[{}]",
                circuit.getId(), took, stateChanged, circuit.getBrokenResources().size());
        if (stateChanged) {
            this.updateCircuitResource(circuit);
        }
    }

    /**
     * Recebe as notificações para Circuitos Criados
     *
     * @param circuit
     */
    @Subscribe
    public void onCircuitResourceCreatedEvent(CircuitResourceCreatedEvent circuit) {
        logger.debug("Resource Connection[{}] Created ID:[{}]]", circuit.getOldResource().getId(),
                circuit.getNewResource().getId());
    }

    /**
     * Recebe as notificações para Circuitos Atualizados, note que ele trata a
     * transição de dependencia do circuito,
     *
     * @param updateEvent
     */
    @Subscribe
    public void onCircuitResourceUpdatedEvent(CircuitResourceUpdatedEvent updateEvent) {
        //
        // flag para dizer se houve transição relevante para o circuito
        //
        boolean circuitTransioned = false;
        if (!updateEvent.getOldResource().getOperationalStatus()
                .equals(updateEvent.getNewResource().getOperationalStatus())) {
            logger.debug("Resource Connection[{}] Updated FOM:[{}] TO:[{}]", updateEvent.getOldResource().getId(),
                    updateEvent.getOldResource().getOperationalStatus(),
                    updateEvent.getNewResource().getOperationalStatus());
            //
            // Transitou de status de UP->DOWN ou DOWN-> UP
            // Produzir evento de notificação, agora devemos processar os serviços.
            //
            circuitTransioned = true;
        } else {
            //
            // O serviço não ficou quebrado, mas pode ter transitado para degradado, ou mesmo retornado
            //
            if (updateEvent.getOldResource().getDegrated() != updateEvent.getNewResource().getDegrated()) {
                //
                // Trabalha a propagação do Evento do Recurso -> Conexão -> Circuito -> Serviço
                //
                if (updateEvent.getNewResource().getDegrated()) {
                    //
                    // Saiu de OK para Degradado
                    //
                    circuitTransioned = true;
                } else {
                    //
                    // Saiu de Degrado para OK
                    //
                    circuitTransioned = true;
                }
            }
        }

        if (circuitTransioned) {
            //
            // Gera o Evento de Notificação de transição para o serviço
            //
            CircuitStateTransionedEvent circuitStateTransitionedEvent = new CircuitStateTransionedEvent(updateEvent.getOldResource(), updateEvent.getNewResource());
            //
            // Deverá ser consumido lá do ServiceManager
            //
            this.eventManager.notifyResourceEvent(circuitStateTransitionedEvent);
        }
    }

}
