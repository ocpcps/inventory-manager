/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.osstelecom.db.inventory.manager.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import org.springframework.messaging.converter.StringMessageConverter;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSession.Subscription;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

/**
 *
 * @author Lucas Nishimura <lucas.nishimura@gmail.com>
 * @created 18.08.2022
 */
public class InventoryManagementStreamClient {
    
    private WebSocketClient wsClient = new StandardWebSocketClient();
    private WebSocketStompClient stompClient;
    private StompSession stompSession;
    private final String urlAddress;
    private ConcurrentHashMap<String, MultiSessionHandler> handlers = new ConcurrentHashMap<>();
    private ObjectMapper om = new JsonMapper();
    
   
    
    public InventoryManagementStreamClient(String url) {
        this.urlAddress = url;
    }
    
    public void connect() throws InterruptedException, ExecutionException {
        this.stompClient = new WebSocketStompClient(wsClient);
        this.stompClient.setMessageConverter(new StringMessageConverter());
        ListenableFuture<StompSession> future = this.stompClient.connect(this.urlAddress, createSessionHandler("default"));
        this.stompSession = future.get();
        
    }
    
    public void disconnect() {
        this.stompSession.disconnect();
    }
    
    public void subscribe(String topicName) {
        Subscription s = this.stompSession.subscribe(topicName, createSessionHandler(topicName));
        this.handlers.get(topicName).setSubscriptionId(s.getSubscriptionId());
    }
    
    public void send(String destination, Object ob) throws JsonProcessingException {
        String payLoad = om.writeValueAsString(ob);
        System.out.println("Trying to Send:" + payLoad);
        this.stompSession.send(destination, payLoad);
        
    }
    
    private MultiSessionHandler createSessionHandler(String name) {
        MultiSessionHandler handler = null;
        if (!this.handlers.contains(name)) {
            handler = new MultiSessionHandler(name);
            this.handlers.put(name, handler);
        } else {
            handler = this.handlers.get(name);
        }
        return handler;
    }
    
    public void handleFrame(String topicName, StompHeaders headers, Object payload) {
        this.acknowledge(headers);
    }
    
    private void acknowledge(StompHeaders headers) {
        this.stompSession.acknowledge(headers, true);
    }
    
    public void handleException(String topicName, StompSession session, StompCommand command, StompHeaders headers, byte[] payload, Throwable exception) {
        exception.printStackTrace();
    }
    
    public void afterConnected(String topicName, StompSession session, StompHeaders connectedHeaders) {
        
    }
    /**
     * MultiSessionHandler for Topics
     */
    private class MultiSessionHandler extends StompSessionHandlerAdapter {
        
        private final String topicName;
        private String subscriptionId;
        
        public String getSubscriptionId() {
            return subscriptionId;
        }
        
        public void setSubscriptionId(String subscriptionId) {
            this.subscriptionId = subscriptionId;
        }
        
        public MultiSessionHandler(String topicName) {
            this.topicName = topicName;
            
        }
        
        @Override
        public void handleException(StompSession session, StompCommand command, StompHeaders headers, byte[] payload, Throwable exception) {
            super.handleException(session, command, headers, payload, exception);
            InventoryManagementStreamClient.this.handleException(topicName, session, command, headers, payload, exception);
        }
        
        @Override
        public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
            super.afterConnected(session, connectedHeaders);
            InventoryManagementStreamClient.this.afterConnected(topicName, session, connectedHeaders);
        }
        
        @Override
        public void handleFrame(StompHeaders headers, Object payload) {
            super.handleFrame(headers, payload);
            InventoryManagementStreamClient.this.handleFrame(topicName, headers, payload);
        }
        
    }
}
