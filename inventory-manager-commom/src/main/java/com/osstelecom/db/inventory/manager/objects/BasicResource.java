/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

package com.osstelecom.db.inventory.manager.objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 *
 * @author  Lucas Nishimura <lucas.nishimura@gmail.com> 
 * @created 17.08.2022
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class BasicResource {
    private String name;
    private String domainName;
    private String nodeAddress;

    public BasicResource() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDomainName() {
        return domainName;
    }

    public void setDomainName(String domainName) {
        this.domainName = domainName;
    }

    public String getNodeAddress() {
        return nodeAddress;
    }

    public void setNodeAddress(String nodeAddress) {
        this.nodeAddress = nodeAddress;
    }
    
}
