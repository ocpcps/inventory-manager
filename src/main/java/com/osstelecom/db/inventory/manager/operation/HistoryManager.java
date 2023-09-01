package com.osstelecom.db.inventory.manager.operation;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.osstelecom.db.inventory.manager.configuration.KafkaConfiguration;
import com.osstelecom.db.inventory.manager.dao.HistoryDao;
import com.osstelecom.db.inventory.manager.exception.ArangoDaoException;
import com.osstelecom.db.inventory.manager.exception.InvalidRequestException;
import com.osstelecom.db.inventory.manager.exception.ResourceNotFoundException;
import com.osstelecom.db.inventory.manager.resources.BasicResource;
import com.osstelecom.db.inventory.manager.resources.History;

@Service
public class HistoryManager extends Manager {

    @Autowired
    private LockManager lockManager;

    @Autowired
    private KafkaTemplate<String, History> kafkaTemplate;

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
    public List<History> getHistoryResourceById(History history)
            throws ResourceNotFoundException, ArangoDaoException, InvalidRequestException {
        return this.historyDao.list(history);
    }

    public List<History> getHistoryConnectionById(History history)
            throws ResourceNotFoundException, ArangoDaoException, InvalidRequestException {
        return this.historyDao.list(history);
    }

    public List<History> getHistoryCircuitById(History history)
            throws ResourceNotFoundException, ArangoDaoException, InvalidRequestException {
        return this.historyDao.list(history);
    }

    public List<History> getHistoryServiceById(History history)
            throws ResourceNotFoundException, ArangoDaoException, InvalidRequestException {
        return this.historyDao.list(history);
    }


    public void saveHistory(BasicResource resource) {
        History history = new History(resource);
        kafkaTemplate.send(KafkaConfiguration.TOPIC_ID, history);
    }

}
