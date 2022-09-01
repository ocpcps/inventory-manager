/*
 * Copyright (C) 2022 Lucas Nishimura <lucas.nishimura@gmail.com>
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
package com.osstelecom.db.inventory.manager.listeners;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.common.eventbus.SubscriberExceptionContext;
import com.google.common.eventbus.SubscriberExceptionHandler;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.osstelecom.db.inventory.manager.events.CircuitResourceCreatedEvent;
import com.osstelecom.db.inventory.manager.events.CircuitResourceUpdatedEvent;
import com.osstelecom.db.inventory.manager.events.ConsumableMetricCreatedEvent;
import com.osstelecom.db.inventory.manager.events.DomainCreatedEvent;
import com.osstelecom.db.inventory.manager.events.ManagedResourceCreatedEvent;
import com.osstelecom.db.inventory.manager.events.ManagedResourceUpdatedEvent;
import com.osstelecom.db.inventory.manager.events.ProcessCircuityIntegrityEvent;
import com.osstelecom.db.inventory.manager.events.ResourceLocationCreatedEvent;
import com.osstelecom.db.inventory.manager.events.ResourceSchemaUpdatedEvent;
import com.osstelecom.db.inventory.manager.exception.ArangoDaoException;
import com.osstelecom.db.inventory.manager.operation.DomainManager;
import com.osstelecom.db.inventory.manager.session.CircuitSession;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

/**
 * Gerencia os eventos do sistema
 *
 * @author Lucas Nishimura <lucas.nishimura@gmail.com>
 * @created 10.04.2022
 */
@Service
public class EventManagerListener implements SubscriberExceptionHandler, Runnable {
    
    private EventBus eventBus = new EventBus(this);
    
    private Logger logger = LoggerFactory.getLogger(EventManagerListener.class);
    
    private AtomicLong eventSeq = new AtomicLong(System.currentTimeMillis());
    
    private LinkedBlockingQueue<Object> eventQueue = new LinkedBlockingQueue<>(1000);

    /**
     * *
     * Valida se precisar ser o domain manager, me parece que o correto seria
     * descer pela session.
     */
    private DomainManager domainmanager;
    
    @Autowired
    private CircuitSession circuitSession;
    
    private Boolean running = false;
    
    private Thread me = new Thread(this);
    
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    
    public void setDomainManager(DomainManager domainmanager) {
        this.domainmanager = domainmanager;
    }
    
    @EventListener(ApplicationReadyEvent.class)
    private void registerEventBus() {
        if (!running) {
            this.running = true;
            this.me.setName("EventManagerSession_THREAD");
            this.me.start();
            
        }
        this.eventBus.register(this);
    }

    /**
     * Recebe a notificação de um evento e envia para o EventBus
     *
     * @param event
     */
    public void notifyEvent(Object event) {
        //
        // the queue is limited to 1000 Events
        //
        eventQueue.offer(event);
        
    }

    /**
     * Intercepta as exceptions geradas pelo eventbus nas subscriptions
     *
     * @param thrwbl
     * @param sec
     */
    @Override
    public void handleException(Throwable thrwbl, SubscriberExceptionContext sec) {
        logger.error("Subscription Error in EventBUS Please Check ME:", thrwbl);
    }

    /**
     * Called when a Managed Resource is created
     *
     * @param resource
     */
    @Subscribe
    public void onManagedResourceCreatedEvent(ManagedResourceCreatedEvent resource) {
        
    }

    /**
     * Called When a New Domain is Created
     *
     * @param domain
     */
    @Subscribe
    public void onDomainCreatedEvent(DomainCreatedEvent domain) {
    }

    /**
     * Called when a Resource Location is created
     *
     * @param resourceLocation
     */
    @Subscribe
    public void onResourceLocationCreatedEvent(ResourceLocationCreatedEvent resourceLocation) {
        
    }

    /**
     * Called when a circuit resource is created 22 as 24
     *
     * @param circuit
     */
    @Subscribe
    public void onCircuitResourceCreatedEvent(CircuitResourceCreatedEvent circuit) {
        
    }
    
    @Subscribe
    public void onConsumableMetricCreatedEvent(ConsumableMetricCreatedEvent metric) {
        
    }

    /**
     * An resource Schema update just Happened...we neeed to update and check
     * all resources...
     *
     * @param update
     */
    @Subscribe
    public void onResourceSchameUpdatedEvent(ResourceSchemaUpdatedEvent update) {
        if (this.domainmanager != null) {
            //
            // Notify the schema session that a schema has changed
            // Now, it will search for:
            // Nodes to be updates -> Connections that relies on those nodes
            //
            this.domainmanager.processSchemaUpdatedEvent(update);
        }
    }
    
    @Subscribe
    public void onManagedResourceUpdatedEvent(ManagedResourceUpdatedEvent updateEvent) {
        logger.debug("Managed Resource [" + updateEvent.getOldResource().getId() + "] Updated: ");
    }
    
    @Subscribe
    public void onCircuitResourceUpdatedEvent(CircuitResourceUpdatedEvent updateEvent) {
        logger.debug("Resource Connection[" + updateEvent.getOldResource().getId() + "] Updated FOM:[" + updateEvent.getOldResource().getOperationalStatus() + "] TO:[" + updateEvent.getNewResource().getOperationalStatus() + "]");
//        logger.debug("\n" + gson.toJson(updateEvent.getOldResource()));

    }
    
    @Subscribe
    public void onProcessCircuityIntegrityEvent(ProcessCircuityIntegrityEvent processEvent) throws ArangoDaoException {
        logger.debug("A Circuit[" + processEvent.getCircuit().getId() + "] Dependency has been updated ,Integrity Needs to be recalculated");
        //
        // Now we should Notify the ImpactManagerSession
        //
        this.circuitSession.computeCircuitIntegrity(processEvent.getCircuit());
    }
    
    @Override
    public void run() {
        while (running) {
            try {
                Object event = eventQueue.poll(5, TimeUnit.SECONDS);
                if (event != null) {
                    logger.debug("Processing Event: [" + event.getClass().getCanonicalName() + "]");
                    eventBus.post(event);
                }
            } catch (InterruptedException ex) {
                
            }
        }
    }
}
