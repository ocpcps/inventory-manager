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

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.SubscriberExceptionContext;
import com.google.common.eventbus.SubscriberExceptionHandler;

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

    private LinkedBlockingQueue<Object> eventQueue = new LinkedBlockingQueue<>(1000);

    private boolean running = false;

    public EventManagerListener() {
        if (!running) {
            Thread thread = new Thread(this);
            this.running = true;
            thread.setName("EventManagerSession_THREAD");
            thread.start();
        }
        this.eventBus.register(this);
    }

    public void registerListener(Object session) {
        this.eventBus.register(session);
    }

    /**
     * Recebe a notificação de um evento e envia para o EventBus
     *
     * @param event
     */
    public boolean notifyEvent(Object event) {
        //
        // the queue is limited to 1000 Events
        //
        return eventQueue.offer(event);
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
     * Faz o processamento da fila interna de eventos
     */
    @Override
    public void run() {
        while (running) {
            try {
                Object event = eventQueue.poll(5, TimeUnit.SECONDS);
                if (event != null) {
                    logger.debug("Processing Event: [{}]", event.getClass().getCanonicalName());
                    eventBus.post(event);
                }
            } catch (InterruptedException ex) {
                logger.error("Error on Processing Event: [{}]", ex.getMessage());
                Thread.currentThread().interrupt();
            }
        }
    }
}
