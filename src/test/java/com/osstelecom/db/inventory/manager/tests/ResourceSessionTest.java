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

/**
 *
 * @author Lucas Nishimura <lucas.nishimura@gmail.com>
 * @created 17.09.2022
 */
//@SpringBootTest
//@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
//@TestClassOrder(ClassOrderer.OrderAnnotation.class)
//@Order(2)
public class ResourceSessionTest {
//
//    @Autowired
//    private DomainSession domainSession;
//
//    @Autowired
//    private ResourceSession resourceSession;
//
//    private Domain domain;
//
//    @Test
//    @DisplayName("Check if Domain Session Exists")
//    @Order(1)
//    public void domainSessionExists() throws Exception {
//        Assertions.assertNotNull(domainSession);
//
//    }
//
//    @Test
//    @DisplayName("Check if Domain Session Exists")
//    @Order(2)
//    public void resourceSessionExits() throws Exception {
//        Assertions.assertNotNull(resourceSession);
//
//    }
//
//    @Test
//    @DisplayName("Create Domain Test")
//    @Order(3)
//    public void createDomainTest() throws Exception {
//        CreateDomainRequest request = new CreateDomainRequest();
//        request.setPayLoad(new Domain());
//        request.getPayLoad().setDomainName("test");
//        CreateDomainResponse response = domainSession.createDomain(request);
//        Assertions.assertTrue(response.getStatusCode() == 200);
//        this.domain = response.getPayLoad();
//        Assertions.assertNotNull(this.domain, "OOPS Domain is Null Here");
//    }
//
//    @Test
//    @DisplayName("Created Simple Manged Resource")
//    @Order(4)
//    public void createManagedResource() throws Exception {
//        CreateManagedResourceRequest request = new CreateManagedResourceRequest();
//        ManagedResource resource = new ManagedResource(domainSession.getDomain("test").getPayLoad());
//        resource.setNode("teste-node-01");
//        resource.setNodeAddress("teste-node-01");
//        resource.setAttributeSchemaName("resource.default");
//        resource.setClassName("resource.default");
//        resource.getAttributes().put("name", "teste-node-01");
//        request.setPayLoad(resource);
//        request.setRequestDomain(resource.getDomain().getDomainName());
//        Assertions.assertNotNull(resourceSession, "Resource Session is null, please fix me");
//        CreateManagedResourceResponse response = resourceSession.createManagedResource(request);
//        Assertions.assertTrue(response.getStatusCode() == 200);
//    }
//
//    @Test
//    @DisplayName("Create Wrong Resouce Service Dependencies")
//    @Order(5)
//    public void createManagedResourceDependsOnservice() throws Exception {
//        CreateManagedResourceRequest request = new CreateManagedResourceRequest();
//        ManagedResource resource = new ManagedResource(domainSession.getDomain("test").getPayLoad());
//        resource.setNode("teste-node-02");
//        resource.setNodeAddress("teste-node-02");
//        resource.setAttributeSchemaName("resource.default");
//        resource.setClassName("resource.default");
//        resource.getAttributes().put("name", "teste-node-01");
////        resource.setDenpendsOnService("network_services/19199191");
//        request.setPayLoad(resource);
//        request.setRequestDomain(resource.getDomain().getDomainName());
//        Assertions.assertNotNull(resourceSession, "Resource Session is null, please fix me");
//
////        Assertions.assertThrows(ResourceNotFoundException.class, () -> {
////            CreateManagedResourceResponse response = resourceSession.createManagedResource(request);
////            Assertions.assertTrue(response.getStatusCode() == 200);
////        });
//
//    }
//
//    @Test
//    @DisplayName("Delete Domain Test")
//    @Order(50)
//    public void deleteDomainTest() throws Exception {
//        DeleteDomainRequest request = new DeleteDomainRequest("test");
//        DeleteDomainResponse response = domainSession.deleteDomain(request);
//        Assertions.assertTrue(response.getStatusCode() == 200);
//    }
//
////    @Test
////    @DisplayName("Delete Simple Manged Resource")
////    @Order(4)
////    public void deleteManagedResource() throws Exception {
////
////        CreateManagedResourceRequest request = new CreateManagedResourceRequest();
////        ManagedResource resource = new ManagedResource(domain);
////        resource.setNode("teste-node-01");
////        resource.setNodeAddress("teste-node-01");
////        resource.setAttributeSchemaName("resource.default");
////        resource.setClassName("resource.default");
////        resource.getAttributes().put("name", "teste-node-01");
////        CreateManagedResourceResponse response = resourceSession.createManagedResource(request);
////        Assertions.assertTrue(response.getStatusCode() == 200);
////
////    }
}
