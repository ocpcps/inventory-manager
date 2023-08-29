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
import com.osstelecom.db.inventory.manager.dao.HistoryDao;
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
import com.osstelecom.db.inventory.manager.resources.History;
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
public class HistoryManager extends Manager {

    @Autowired
    private LockManager lockManager;

    @Autowired
    private HistoryDao historyDao;

    /**
     * Search for a History Manager
     *
     * @param history
     * @return
     * @throws ResourceNotFoundException
     * @throws ArangoDaoException
     */
    public History getHistoryResourceById(History history)
            throws ResourceNotFoundException, ArangoDaoException, InvalidRequestException {
        if (history.getId() != null) {
            if (!history.getId().contains("/")) {
                history.setId(history.getId());
            }
        }

        return this.historyDao.findResource(history);
    }

    public History getHistoryConnectionById(History history)
            throws ResourceNotFoundException, ArangoDaoException, InvalidRequestException {
        if (history.getId() != null) {
            if (!history.getId().contains("/")) {
                history.setId(history.getId());
            }
        }

        return this.historyDao.findConnection(history);
    }

    public History getHistoryCircuitById(History history)
            throws ResourceNotFoundException, ArangoDaoException, InvalidRequestException {
        if (history.getId() != null) {
            if (!history.getId().contains("/")) {
                history.setId(history.getId());
            }
        }

        return this.historyDao.findCircuit(history);
    }

    public History getHistoryServiceById(History history)
            throws ResourceNotFoundException, ArangoDaoException, InvalidRequestException {
        if (history.getId() != null) {
            if (!history.getId().contains("/")) {
                history.setId(history.getId());
            }
        }

        return this.historyDao.findService(history);
    }

}
