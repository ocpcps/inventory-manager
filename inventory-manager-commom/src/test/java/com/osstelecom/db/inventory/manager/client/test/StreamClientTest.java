/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.osstelecom.db.inventory.manager.client.test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.osstelecom.db.inventory.manager.client.InventoryManagementStreamClient;
import java.util.HashMap;
import java.util.Scanner;
import java.util.concurrent.ExecutionException;

/**
 *
 * @author Lucas Nishimura <lucas.nishimura@gmail.com>
 * @created 18.08.2022
 */
public class StreamClientTest {

    public static void main(String[] args) throws InterruptedException, ExecutionException, JsonProcessingException {
        InventoryManagementStreamClient client = new InventoryManagementStreamClient("ws://127.0.0.1:9080/stomp");
        client.connect();
        System.out.println("Connected...");
        client.subscribe("/topic/response");
        HashMap<String, String> test = new HashMap<>();
        test.put("Name", "nishi");
        client.send("/app/requests", test);

        new Scanner(System.in).nextLine(); // Don't close immediately.
        client.disconnect();
    }
}
