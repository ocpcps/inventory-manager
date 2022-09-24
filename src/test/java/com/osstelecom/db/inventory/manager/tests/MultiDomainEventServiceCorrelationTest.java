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
package com.osstelecom.db.inventory.manager.tests;

import com.osstelecom.db.inventory.manager.dto.CircuitPathDTO;
import com.osstelecom.db.inventory.manager.dto.ConnectionDTO;
import com.osstelecom.db.inventory.manager.exception.DomainNotFoundException;
import com.osstelecom.db.inventory.manager.exception.InvalidRequestException;
import com.osstelecom.db.inventory.manager.request.CreateCircuitPathRequest;
import com.osstelecom.db.inventory.manager.request.CreateCircuitRequest;
import com.osstelecom.db.inventory.manager.request.CreateConnectionRequest;
import com.osstelecom.db.inventory.manager.request.CreateDomainRequest;
import com.osstelecom.db.inventory.manager.request.CreateManagedResourceRequest;
import com.osstelecom.db.inventory.manager.request.CreateServiceRequest;
import com.osstelecom.db.inventory.manager.request.DeleteDomainRequest;
import com.osstelecom.db.inventory.manager.resources.CircuitResource;
import com.osstelecom.db.inventory.manager.resources.Domain;
import com.osstelecom.db.inventory.manager.resources.ManagedResource;
import com.osstelecom.db.inventory.manager.resources.ResourceConnection;
import com.osstelecom.db.inventory.manager.resources.ServiceResource;
import com.osstelecom.db.inventory.manager.response.CreateCircuitResponse;
import com.osstelecom.db.inventory.manager.response.CreateDomainResponse;
import com.osstelecom.db.inventory.manager.response.CreateManagedResourceResponse;
import com.osstelecom.db.inventory.manager.response.CreateResourceConnectionResponse;
import com.osstelecom.db.inventory.manager.response.DeleteDomainResponse;
import com.osstelecom.db.inventory.manager.session.CircuitSession;
import com.osstelecom.db.inventory.manager.session.DomainSession;
import com.osstelecom.db.inventory.manager.session.ResourceSession;
import com.osstelecom.db.inventory.manager.session.ServiceSession;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.ClassOrderer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestClassOrder;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 *
 * @author Lucas Nishimura <lucas.nishimura@gmail.com>
 * @created 23.09.2022
 */
@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestClassOrder(ClassOrderer.OrderAnnotation.class)
@Order(4)
public class MultiDomainEventServiceCorrelationTest {
    
    @Autowired
    private DomainSession domainSession;
    
    @Autowired
    private ResourceSession resourceSession;
    
    @Autowired
    private CircuitSession circuitSession;
    
    @Autowired
    private ServiceSession serviceSession;
    
    @Test
    @DisplayName("Check if Domain Session Exists")
    @Order(1)
    public void domainSessionExists() throws Exception {
        Assertions.assertNotNull(domainSession);
        
    }
    
    @Test
    @DisplayName("Create Domain Test1")
    @Order(2)
    public void createDomainTest1() throws Exception {
        CreateDomainRequest request1 = new CreateDomainRequest();
        request1.setPayLoad(new Domain());
        request1.getPayLoad().setDomainName("test_1");
        CreateDomainResponse response = domainSession.createDomain(request1);
        Assertions.assertTrue(response.getStatusCode() == 200);
        
    }
    
    @Test
    @DisplayName("Create Domain Test2")
    @Order(2)
    public void createDomainTest2() throws Exception {
        CreateDomainRequest request2 = new CreateDomainRequest();
        request2.setPayLoad(new Domain());
        request2.getPayLoad().setDomainName("test_2");
        CreateDomainResponse response2 = domainSession.createDomain(request2);
        Assertions.assertTrue(response2.getStatusCode() == 200);
    }
    
    private CreateManagedResourceRequest createManagedResourceRequest(String name, String domain) throws DomainNotFoundException, InvalidRequestException {
        CreateManagedResourceRequest request = new CreateManagedResourceRequest();
        ManagedResource resource = new ManagedResource(domainSession.getDomain(domain).getPayLoad());
        resource.setNode(name);
        resource.setNodeAddress(name);
        resource.setAttributeSchemaName("resource.default");
        resource.setClassName("resource.default");
        resource.getAttributes().put("name", name);
        request.setPayLoad(resource);
        request.setRequestDomain(resource.getDomain().getDomainName());
        return request;
    }
    
    @Test
    @DisplayName("Create A Point")
    @Order(3)
    public void createAPoint() throws Exception {
        CreateManagedResourceRequest aPointRequest = this.createManagedResourceRequest("A-POINT", "test_1");
        Assertions.assertNotNull(resourceSession, "Resource Session is null, please fix me");
        CreateManagedResourceResponse aResponse = resourceSession.createManagedResource(aPointRequest);
        Assertions.assertTrue(aResponse.getStatusCode() == 200, "Failed to create A-POINT");
        
    }
    
    @Test
    @DisplayName("Create Z Point")
    @Order(4)
    public void createZpoint() throws Exception {
        CreateManagedResourceRequest zPointRequest = this.createManagedResourceRequest("Z-POINT", "test_1");
        CreateManagedResourceResponse zResponse = resourceSession.createManagedResource(zPointRequest);
        Assertions.assertTrue(zResponse.getStatusCode() == 200, "Failed to create A-POINT");
    }
    
    @Test
    @DisplayName("Connect A-Z Point")
    @Order(5)
    public void connectAPointToZPoint() throws Exception {
        CreateConnectionRequest request = new CreateConnectionRequest();
        request.setRequestDomain("test_1");
        request.setPayLoad(new ConnectionDTO());
        request.getPayLoad().setFromNodeAddress("A-POINT");
        request.getPayLoad().setFromClassName("resource.default");
        request.getPayLoad().setToNodeAddress("Z-POINT");
        request.getPayLoad().setToClassName("resource.default");
        request.getPayLoad().setConnectionClass("connection.default");
        request.getPayLoad().setNodeAddress("ATOZ.CONNECTION");
        CreateResourceConnectionResponse response = this.resourceSession.createResourceConnection(request);
        Assertions.assertTrue(response.getStatusCode() == 200, "Failed to Connect A-POINT to Z.POINT");
    }
    
    @Test
    @DisplayName("Create A-Z Circuit")
    @Order(6)
    public void createAZCircuit() throws Exception {
        CreateCircuitRequest request = new CreateCircuitRequest();
        request.setRequestDomain("test_1");
        Domain domain = this.domainSession.getDomain("test_1").getPayLoad();
        CircuitResource circuit = new CircuitResource(domain);
        
        ManagedResource aPoint = new ManagedResource(domain);
        aPoint.setDomainName(domain.getDomainName());
        aPoint.setNodeAddress("A-POINT");
        aPoint.setClassName("resource.default");
        
        circuit.setaPoint(aPoint);
        
        ManagedResource zPoint = new ManagedResource(domain);
        zPoint.setDomainName(domain.getDomainName());
        zPoint.setNodeAddress("Z-POINT");
        zPoint.setClassName("resource.default");
        
        circuit.setzPoint(zPoint);
        circuit.setNodeAddress("A-Z-CIRCUIT");
        circuit.setClassName("circuit.default");
        request.setPayLoad(circuit);
        CreateCircuitResponse response = circuitSession.createCircuit(request);
        Assertions.assertTrue(response.getStatusCode() == 200, "Failed to Create Circuit");
    }
    
    @Test
    @DisplayName("Create A-Z Circuit Path")
    @Order(7)
    public void createCircuitPath() throws Exception {
        CreateCircuitPathRequest request = new CreateCircuitPathRequest();
        request.setRequestDomain("test_1");
        Domain domain = this.domainSession.getDomain("test_1").getPayLoad();
        CircuitPathDTO path = new CircuitPathDTO();
        ResourceConnection connection1 = new ResourceConnection();
        connection1.setDomain(domain);
        connection1.setFrom(this.createManagedResourceRequest("A-POINT", "test_1").getPayLoad());
        connection1.setTo(this.createManagedResourceRequest("Z-POINT", "test_1").getPayLoad());
        path.getPaths().add(connection1);
        CircuitResource circuit = new CircuitResource(domain);
        circuit.setNodeAddress("A-Z-CIRCUIT");
        circuit.setClassName("circuit.default");
        path.setCircuit(circuit);
        request.setPayLoad(path);
        this.circuitSession.createCircuitPath(request);
    }
    
    @Test
    @DisplayName("Create A-Z Circuit Path")
    @Order(8)
    public void createService() throws Exception {
        CreateServiceRequest request = new CreateServiceRequest();
        request.setRequestDomain("test_1");
        Domain domain = this.domainSession.getDomain("test_1").getPayLoad();
        ServiceResource service = new ServiceResource();
        
        service.setDomain(domain);
        service.setDomainName(domain.getDomainName());
        this.serviceSession.createService(request);
    }
    
    @Test
    @DisplayName("Delete Domain Test")
    @Order(1000)
    public void deleteDomainsTest1() throws Exception {
        DeleteDomainRequest request = new DeleteDomainRequest("test_1");
        DeleteDomainResponse response = domainSession.deleteDomain(request);
        Assertions.assertTrue(response.getStatusCode() == 200);
    }
    
    @Test
    @DisplayName("Delete Domain Test")
    @Order(1001)
    public void deleteDomainsTest2() throws Exception {
        DeleteDomainRequest request = new DeleteDomainRequest("test_2");
        DeleteDomainResponse response = domainSession.deleteDomain(request);
        Assertions.assertTrue(response.getStatusCode() == 200);
    }
}
