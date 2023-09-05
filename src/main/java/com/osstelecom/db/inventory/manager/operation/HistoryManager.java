package com.osstelecom.db.inventory.manager.operation;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.google.common.eventbus.Subscribe;
import com.osstelecom.db.inventory.manager.configuration.KafkaConfiguration;
import com.osstelecom.db.inventory.manager.dao.HistoryDao;
import com.osstelecom.db.inventory.manager.events.ManagedResourceCreatedEvent;
import com.osstelecom.db.inventory.manager.events.ManagedResourceUpdatedEvent;
import com.osstelecom.db.inventory.manager.exception.ArangoDaoException;
import com.osstelecom.db.inventory.manager.exception.ResourceNotFoundException;
import com.osstelecom.db.inventory.manager.listeners.EventManagerListener;
import com.osstelecom.db.inventory.manager.resources.BasicResource;
import com.osstelecom.db.inventory.manager.resources.History;
import com.osstelecom.db.inventory.manager.resources.ManagedResource;

@Service
public class HistoryManager extends Manager {

    @Autowired
    private EventManagerListener eventManager;

    @Autowired
    private KafkaTemplate<String, History> kafkaTemplate;

    @Autowired
    private HistoryDao historyDao;

    @EventListener(ApplicationReadyEvent.class)
    private void onStartUp() {
        eventManager.registerListener(this);
    }

    /**
     * Search for a History Manager
     *
     * @param history
     * @return
     * @throws ResourceNotFoundException
     * @throws ArangoDaoException
     */
    public List<History> getHistoryResourceById(History history) {
        return this.historyDao.list(history);
    }

    public List<History> getHistoryConnectionById(History history) {
        return this.historyDao.list(history);
    }

    public List<History> getHistoryCircuitById(History history) {
        return this.historyDao.list(history);
    }

    public List<History> getHistoryServiceById(History history) {
        return this.historyDao.list(history);
    }

    public void sendHistory(BasicResource resource) {
        History history = new History(resource);
        kafkaTemplate.send(KafkaConfiguration.TOPIC_ID, history);
    }

    @Subscribe
    public void onManagedResourceCreatedEvent(ManagedResourceCreatedEvent createEvent) {
        ManagedResource newResource = createEvent.getNewResource();
        sendHistory(newResource);
    }

    @Subscribe
    public void onManagedResourceUpdatedEvent(ManagedResourceUpdatedEvent updateEvent) {
        ManagedResource newResource = updateEvent.getNewResource();
        sendHistory(newResource);
    }

}
