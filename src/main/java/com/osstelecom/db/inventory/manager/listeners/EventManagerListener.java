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

import org.slf4j.Logger;
import org.springframework.stereotype.Service;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.SubscriberExceptionContext;
import com.google.common.eventbus.SubscriberExceptionHandler;
import com.osstelecom.db.inventory.manager.events.BasicResourceEvent;
import com.osstelecom.db.inventory.manager.jobs.DBJobInstance;
import com.osstelecom.db.inventory.manager.operation.DbJobManager;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

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

    @Autowired
    private DbJobManager jobManager;

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

    public void registerListener(Object listener) {
        this.eventBus.register(listener);

    }

    /**
     * Recebe a notificação de um evento e envia para o EventBus
     *
     * @param event
     */
    public synchronized boolean notifyResourceEvent(BasicResourceEvent event) {

        //
        // the queue is limited to 1000 Events, after that will be blocking...
        //
        return eventQueue.offer(event);
    }

    /**
     * Trata os eventos genéricos que não estão ligados aos recursos,
     *
     * @Todo: pensar se faz sentido migrar para uma fila separada.
     * @param genericEvent
     * @return
     */
    public synchronized boolean notifyGenericEvent(Object genericEvent) {
        return eventQueue.offer(genericEvent);
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
        thrwbl.printStackTrace();
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
                    //
                    // Precisa Notificar o Job Manager que uma Job de Atualização está em curso
                    //
                    DBJobInstance job = jobManager.createJobInstance();
                    jobManager.notifyJobStart(job);
                    String eventProcessindInstanceId = UUID.randomUUID().toString();
                    Long start = System.currentTimeMillis();
                    eventBus.post(event);
                    Long end = System.currentTimeMillis();
                    Long took = end - start;
                    logger.debug("End Processing Event: [{}] Done ID:[{}] Took:[{}]ms Queue Size:[{}]", event.getClass().getCanonicalName(), eventProcessindInstanceId, took, eventQueue.size());
                    jobManager.notifyJobEnd(job);
                }
            } catch (InterruptedException ex) {
//                logger.error("Error on Processing Event: [{}]", ex.getMessage());
//                Thread.currentThread().interrupt();
            }
        }
    }
}
