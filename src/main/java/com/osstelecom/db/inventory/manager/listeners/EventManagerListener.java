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
import com.osstelecom.db.inventory.manager.events.BasicEvent;
import com.osstelecom.db.inventory.manager.events.BasicResourceEvent;
import com.osstelecom.db.inventory.manager.events.BasicUpdateEvent;
import com.osstelecom.db.inventory.manager.events.IEvent;
import com.osstelecom.db.inventory.manager.jobs.DBJobInstance;
import com.osstelecom.db.inventory.manager.operation.DbJobManager;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;

/**
 * Gerencia os eventos do sistema
 *
 * @author Lucas Nishimura
 * @created 10.04.2022
 */
@Service()
@DependsOn(value = {"dbJobManager"})
public class EventManagerListener implements SubscriberExceptionHandler, Runnable, IEventListener {

    private EventBus eventBus = new EventBus(this);

    private Logger logger = LoggerFactory.getLogger(EventManagerListener.class);

    /**
     * Avaliar de colocarmos essa queue em algum lugar persistente
     */
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

            /**
             * Vamos criar um simples Stats Thread..penso que isso deveria estar
             * no job manager
             */
            new Thread(() -> {
                while (running) {
                    if (eventQueue.size() > 950) {
                        logger.warn("Event Queue Size:[{}]", eventQueue.size());
                    } else {
                        if (!eventQueue.isEmpty()) {
                            logger.debug("Event Queue Size:[{}]", eventQueue.size());
                        }
                    }
                    if (jobManager != null) {
                        List<DBJobInstance> runningJobs = jobManager.getRunningJobs();
                        if (!runningJobs.isEmpty()) {
                            runningJobs.forEach(r -> {
                                logger.debug("Job:[{}] Running Since: {}", r.getJobId(), r.getJobStarted());
                            });
                        }
                    }
                    try {
                        Thread.sleep(10000); // 10 segundos
                    } catch (InterruptedException ex) {
                        java.util.logging.Logger.getLogger(EventManagerListener.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }).start();

        }
        this.eventBus.register(this);
    }

    @Override
    public void registerListener(Object listener) {
        this.eventBus.register(listener);
        logger.debug("New Event Listener Registered:[{}]", listener.getClass().getCanonicalName());
    }

    /**
     * Recebe a notificação de um evento e envia para o EventBus
     *
     * @param event
     * @return
     */
    @Override
    public synchronized boolean notifyResourceEvent(BasicResourceEvent event) {

        //
        // the queue is limited to 1000 Events, after that will be blocking...
        //
        DBJobInstance job = jobManager.createJobInstance(event.getClass().getName());
        event.setMdcId(MDC.get("x-netcompass-requestId"));
        event.setRelatedJob(job);
        return eventQueue.offer(event);
    }

    /**
     * Trata os eventos genéricos que não estão ligados aos recursos,
     *
     * @Todo: pensar se faz sentido migrar para uma fila separada.
     * @param genericEvent
     * @return
     */
    @Override
    public synchronized boolean notifyGenericEvent(BasicEvent genericEvent) {
        DBJobInstance job = jobManager.createJobInstance("GenericEvent");
        genericEvent.setMdcId(MDC.get("x-netcompass-requestId"));
        genericEvent.setRelatedJob(job);
        return eventQueue.offer(genericEvent);
    }

    /**
     * Trata os eventos genéricos que não estão ligados updateEvent recursos,
     *
     * @param updateEvent
     * @Todo: pensar se faz sentido migrar para uma fila separada.
     * @param genericEvent
     * @return
     */
    @Override
    public synchronized boolean notifyGenericEvent(BasicUpdateEvent updateEvent) {
        DBJobInstance job = jobManager.createJobInstance(updateEvent.getClass().getName());
        updateEvent.setMdcId(MDC.get("x-netcompass-requestId"));
        updateEvent.setRelatedJob(job);
        return eventQueue.offer(updateEvent);
    }

    /**
     * Intercepta as exceptions geradas pelo eventbus nas subscriptions
     *
     * @param thrwbl
     * @param sec
     */
    @Override
    public void handleException(Throwable thrwbl, SubscriberExceptionContext sec) {
        logger.error("Subscription Error in EventBUS Please Check Me:[{}]", thrwbl.getMessage());
        logger.error("Event BUS Error", thrwbl);
        thrwbl.printStackTrace();
    }

    /**
     * Faz o processamento da fila interna de eventos
     */
    @Override
    public void run() {
        logger.debug("Event Processor Thread Started");
        while (running) {
            try {
                Object eventObject = eventQueue.poll(5, TimeUnit.SECONDS);
                if (eventObject != null) {
                    try {
                        //
                        // Precisa Notificar o Job Manager que uma Job de Atualização está em curso
                        //
                        String eventProcessindInstanceId = UUID.randomUUID().toString();
                        if (eventObject instanceof IEvent) {
                            DBJobInstance job = ((IEvent) eventObject).getRelatedJob();
                            String mdcID = ((IEvent) eventObject).getMdcId();
                            if (mdcID != null) {
                                MDC.put("x-netcompass-requestId", mdcID);
                            } else {
                                MDC.put("x-netcompass-requestId", "LT-" + job.getJobId());
                            }
                            jobManager.notifyJobStart(job);

                        } else {
                            logger.warn("Processing Instance Of:[{}]", eventObject.getClass().getName());
                            MDC.put("x-netcompass-requestId", "GEN-" + eventProcessindInstanceId);
                        }

                        Long start = System.currentTimeMillis();
                        eventBus.post(eventObject);
                        Long end = System.currentTimeMillis();
                        Long took = end - start;
                        logger.debug("End Processing Event: [{}] Done ID:[{}] Took:[{}]ms Queue Size:[{}]", eventObject.getClass().getCanonicalName(), eventProcessindInstanceId, took, eventQueue.size());

                        if (eventObject instanceof IEvent) {
                            DBJobInstance job = ((IEvent) eventObject).getRelatedJob();
                            jobManager.notifyJobEnd(job);
                        }
                    } finally {
                        MDC.clear();
                    }
                }
            } catch (InterruptedException ex) {
                logger.error("Error on Processing Event: [{}]", ex.getMessage());
            }
        }
        logger.warn("Event Processor Thread Ended");
    }
}
