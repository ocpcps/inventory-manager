/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.osstelecom.db.inventory.manager.client.test;

import com.osstelecom.db.inventory.manager.client.InventoryManagerClient;
import com.osstelecom.db.inventory.manager.objects.Filter;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Lucas Nishimura <lucas.nishimura@gmail.com>
 * @created 17.08.2022
 */
public class TestClient {

    public static void main(String[] args) {
        InventoryManagerClient c = new InventoryManagerClient("http://10.200.20.237:9080", "nishimura");
        try {
            c.listDomains().forEach(domain -> {
                System.out.println("Found Domain:" + domain.getDomainName());
            });
            
            Filter filter = new Filter();
            filter.setDomain("network");
            filter.setAqlFilter("doc.nodeAddress like @name ");
            filter.addBingind("name", "%spo%");
            filter.addObject("connections");
            
            Filter result = c.findResourcesByFilter(filter);
            System.out.println("Found:" + result.getConnections().size() +" Connections");
            
        } catch (IOException ex) {
            Logger.getLogger(TestClient.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
