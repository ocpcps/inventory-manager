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
package com.osstelecom.db.inventory.manager.session;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.common.eventbus.SubscriberExceptionContext;
import com.google.common.eventbus.SubscriberExceptionHandler;
import com.osstelecom.db.inventory.manager.events.CircuitResourceCreatedEvent;
import com.osstelecom.db.inventory.manager.events.ConsumableMetricCreatedEvent;
import com.osstelecom.db.inventory.manager.events.DomainCreatedEvent;
import com.osstelecom.db.inventory.manager.events.ManagedResourceCreatedEvent;
import com.osstelecom.db.inventory.manager.events.ResourceLocationCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
public class EventManagerSession implements SubscriberExceptionHandler {

    private EventBus eventBus = new EventBus(this);
    private Logger logger = LoggerFactory.getLogger(EventManagerSession.class);

    @EventListener(ApplicationReadyEvent.class)
    private void registerEventBus() {
        this.eventBus.register(this);
    }

    /**
     * Recebe a notificação de um evento e envia para o EventBus
     *
     * @param event
     */
    public void notifyEvent(Object event) {
        eventBus.post(event);
    }

    /**
     * Intercepta as exceptions geradas pelo eventbus nas subscriptions
     *
     * @param thrwbl
     * @param sec
     */
    @Override
    public void handleException(Throwable thrwbl, SubscriberExceptionContext sec) {
        logger.error("Subscription Error in EventBUS:", thrwbl);
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
}
