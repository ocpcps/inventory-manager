package com.osstelecom.db.inventory.manager.operation;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import javax.management.ServiceNotFoundException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import com.arangodb.entity.DocumentUpdateEntity;
import com.google.common.eventbus.Subscribe;
import com.osstelecom.db.inventory.manager.dao.ConsumableMetricDao;
import com.osstelecom.db.inventory.manager.dao.ManagedResourceDao;
import com.osstelecom.db.inventory.manager.dto.FilterDTO;
import com.osstelecom.db.inventory.manager.events.ConsumableMetricCreatedEvent;
import com.osstelecom.db.inventory.manager.events.ConsumableMetricUpdatedEvent;
import com.osstelecom.db.inventory.manager.events.ResourceConnectionCreatedEvent;
import com.osstelecom.db.inventory.manager.events.ResourceConnectionDeletedEvent;
import com.osstelecom.db.inventory.manager.events.ResourceConnectionUpdatedEvent;
import com.osstelecom.db.inventory.manager.exception.ArangoDaoException;
import com.osstelecom.db.inventory.manager.exception.DomainNotFoundException;
import com.osstelecom.db.inventory.manager.exception.InvalidRequestException;
import com.osstelecom.db.inventory.manager.exception.ResourceNotFoundException;
import com.osstelecom.db.inventory.manager.listeners.EventManagerListener;
import com.osstelecom.db.inventory.manager.resources.BasicResource;
import com.osstelecom.db.inventory.manager.resources.ConsumableMetric;
import com.osstelecom.db.inventory.manager.resources.Domain;
import com.osstelecom.db.inventory.manager.resources.GraphList;
import com.osstelecom.db.inventory.manager.resources.ManagedResource;
import com.osstelecom.db.inventory.manager.resources.ResourceConnection;
import com.osstelecom.db.inventory.manager.resources.exception.MetricConstraintException;

@Service
public class ConsumableMetricManager extends Manager {

    @Autowired
    private LockManager lockManager;

    @Autowired
    private EventManagerListener eventManager;

    @Autowired
    private ConsumableMetricDao consumableMetricDao;

    @Autowired
    private ManagedResourceDao managedResourceDao;

    @EventListener(ApplicationReadyEvent.class)
    private void onStartUp() {
        this.eventManager.registerListener(this);
    }

    /**
     * Create a consumable metric
     *
     * @param name
     * @return
     */
    public ConsumableMetric createConsumableMetric(ConsumableMetric consumableMetric) throws ArangoDaoException {
        String timerId = startTimer("createConsumableMetric");
        try {
            lockManager.lock();

            consumableMetricDao.insertConsumableMetric(consumableMetric);

            endTimer(timerId);
            //
            // Notifica o event Manager da Metrica criada
            //
            ConsumableMetricCreatedEvent event = new ConsumableMetricCreatedEvent(consumableMetric);
            eventManager.notifyGenericEvent(event);
            return consumableMetric;
        } catch (Exception e) {
            ArangoDaoException ex = new ArangoDaoException("Error while creating ConsumableMetric", e);
            e.printStackTrace();
            throw ex;
        } finally {
            if (lockManager.isLocked()) {
                lockManager.unlock();
            }
            endTimer(timerId);
        }
    }

    /**
     * Update a consumable metric
     *
     * @param name
     * @return
     * @throws ArangoDaoException
     */
    public ConsumableMetric updateConsumableMetric(ConsumableMetric consumableMetric) throws ArangoDaoException {
        String timerId = startTimer("updateConsumableMetric");
        try {
            lockManager.lock();

            DocumentUpdateEntity<ConsumableMetric> result = consumableMetricDao
                    .updateConsumableMetric(consumableMetric);
            ConsumableMetric newService = result.getNew();
            ConsumableMetric oldService = result.getOld();
            endTimer(timerId);
            //
            // Notifica o event Manager da Metrica criada
            //
            ConsumableMetricUpdatedEvent event = new ConsumableMetricUpdatedEvent(oldService, newService);
            eventManager.notifyGenericEvent(event);
            return newService;
        } catch (Exception e) {
            ArangoDaoException ex = new ArangoDaoException("Error while updating ConsumableMetric", e);
            e.printStackTrace();
            throw ex;
        } finally {
            if (lockManager.isLocked()) {
                lockManager.unlock();
            }
            endTimer(timerId);
        }
    }

    /**
     * Deleta um serviço
     *
     * @param consumableMetric
     * @return
     * @throws ArangoDaoException
     */
    public ConsumableMetric deleteConsumableMetric(ConsumableMetric consumableMetric) throws ArangoDaoException {
        try {
            lockManager.lock();
            this.consumableMetricDao.deleteConsumableMetric(consumableMetric);
            return consumableMetric;
        } finally {
            if (lockManager.isLocked()) {
                lockManager.unlock();
            }
        }
    }

    public GraphList<ConsumableMetric> findServiceByFilter(FilterDTO filter, Domain domain)
            throws ArangoDaoException, ResourceNotFoundException, InvalidRequestException {
        return this.consumableMetricDao.findConsumableMetricByFilter(filter, domain);
    }

    /**
     * Retrieves a domain by name
     *
     * @param domainName
     * @return
     * @throws DomainNotFoundException
     * @throws ArangoDaoException
     * @throws InvalidRequestException
     * @throws ServiceNotFoundException
     */
    public ConsumableMetric getConsumableMetric(ConsumableMetric consumableMetric)
            throws ResourceNotFoundException, ArangoDaoException, InvalidRequestException {
        return this.consumableMetricDao.findConsumableMetric(consumableMetric);
    }

    /**
     * Recebe a notificação de que uma conexão foi criada
     *
     * @param connectionCreatedEvent
     */
    @Subscribe
    public void onResourceConnectionCreatedEvent(ResourceConnectionCreatedEvent connectionCreatedEvent) {
        ResourceConnection newConnection = connectionCreatedEvent.getNewResource();
        Map<String, Double> childValues = processConsumerMetric(newConnection.getFrom());
        processConsumableMetric(newConnection.getTo(), childValues, true);
    }

    /**
     * Recebe a notificação de que uma conexão foi deletada
     *
     * @param connectionDeletedEvent
     */
    @Subscribe
    public void onResourceConnectionDeletedEvent(ResourceConnectionDeletedEvent connectionDeletedEvent) {
        ResourceConnection oldConnection = connectionDeletedEvent.getOldResource();
        Map<String, Double> childValues = processConsumerMetric(oldConnection.getFrom());
        processConsumableMetric(oldConnection.getTo(), childValues, false);
    }

    /**
     * Recebi uma notificação de que uma conexão foi foi atualizada
     *
     * @param connectionUpdateEvent
     */
    @Subscribe
    public void onResourceConnectionUpdatedEvent(ResourceConnectionUpdatedEvent connectionUpdateEvent) {
        ResourceConnection oldConnection = connectionUpdateEvent.getOldResource();
        Map<String, Double> childValues = processConsumerMetric(oldConnection.getFrom());
        processConsumableMetric(oldConnection.getTo(), childValues, false);

        ResourceConnection newConnection = connectionUpdateEvent.getNewResource();
        childValues = processConsumerMetric(newConnection.getFrom());
        processConsumableMetric(newConnection.getTo(), childValues, true);
    }
    
    private void processConsumableMetric(BasicResource toResource, Map<String, Double> childValues, Boolean consume) {

        GraphList<BasicResource> consumableParents = this.consumableMetricDao
                .findParentsWithMetrics(toResource);
        for (BasicResource parentResource : consumableParents.toList()) {
            try {
                ConsumableMetric parentMetric = parentResource.getConsumableMetric();
                Double childValue = childValues.get(parentMetric.getMetricName());
                if (childValue != null) {
                    Double total = calculateParent(parentMetric.getMetricValue(),
                            childValue, consume);
                    parentMetric.setMetricValue(total);

                    ManagedResource resource = managedResourceDao
                            .findResource(new ManagedResource(parentResource.getDomain(), parentResource.getId()));
                    resource.setConsumableMetric(parentMetric);
                    managedResourceDao.updateResource(resource);
                }
            } catch (MetricConstraintException | InvalidRequestException | ResourceNotFoundException
                    | ArangoDaoException e) {
                e.printStackTrace();
            }
        }

        if (toResource.getConsumableMetric() != null) {
            try {
                ConsumableMetric parentMetric = toResource.getConsumableMetric();
                Double childValue = childValues.get(parentMetric.getMetricName());
                if (childValue != null) {
                    Double total = calculateParent(parentMetric.getMetricValue(),
                            childValue, consume);
                    parentMetric.setMetricValue(total);

                    ManagedResource resource = managedResourceDao
                            .findResource(new ManagedResource(toResource.getDomain(), toResource.getId()));
                    resource.setConsumableMetric(parentMetric);
                    managedResourceDao.updateResource(resource);
                }
            } catch (MetricConstraintException | InvalidRequestException | ResourceNotFoundException
                    | ArangoDaoException e) {
                e.printStackTrace();
            }
        }
    }

    private Map<String, Double> processConsumerMetric(BasicResource fromResource) {

        Map<String, Double> result = new HashMap<>();

        GraphList<BasicResource> consumerChilds = this.consumableMetricDao
                .findChildsWithMetrics(fromResource);

        for (BasicResource childResource : consumerChilds.toList()) {
            String metricName = childResource.getConsumerMetric().getMetricName();

            if (result.get(metricName) != null) {
                BigDecimal total = BigDecimal.valueOf(result.get(metricName));
                total = total.add(BigDecimal.valueOf(childResource.getConsumerMetric().getMetricValue()));
                result.remove(metricName);
                result.put(metricName, total.doubleValue());
            } else {
                result.put(metricName, childResource.getConsumerMetric().getMetricValue());
            }
        }

        String metricName = fromResource.getConsumerMetric().getMetricName();
        if (result.get(metricName) != null) {
            BigDecimal total = BigDecimal.valueOf(result.get(metricName));
            total = total.add(BigDecimal.valueOf(fromResource.getConsumerMetric().getMetricValue()));
            result.remove(metricName);
            result.put(metricName, total.doubleValue());
        } else {
            result.put(metricName, fromResource.getConsumerMetric().getMetricValue());
        }

        return result;
    }

    private Double calculateParent(Double parentValue, Double childValue,
            Boolean consume) {
        if (consume.booleanValue()) {
            return BigDecimal.valueOf(parentValue).subtract(BigDecimal.valueOf(childValue)).doubleValue();
        } else {
            return BigDecimal.valueOf(parentValue).add(BigDecimal.valueOf(childValue)).doubleValue();
        }
    }

}