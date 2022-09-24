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

import com.osstelecom.db.inventory.manager.exception.DomainAlreadyExistsException;
import com.osstelecom.db.inventory.manager.request.CreateDomainRequest;
import com.osstelecom.db.inventory.manager.request.DeleteDomainRequest;
import com.osstelecom.db.inventory.manager.resources.Domain;
import com.osstelecom.db.inventory.manager.response.CreateDomainResponse;
import com.osstelecom.db.inventory.manager.response.DeleteDomainResponse;
import com.osstelecom.db.inventory.manager.session.DomainSession;
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
 * @created 17.09.2022
 */
@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestClassOrder(ClassOrderer.OrderAnnotation.class)
@Order(1)
public class DomainSessionTest {

    @Autowired
    private DomainSession domainSession;

    @Test
    @DisplayName("Check if Domain Session Exists")
    @Order(1)
    public void domainSessionExists() throws Exception {
        Assertions.assertNotNull(domainSession);

    }

    @Test
    @DisplayName("Create Domain Test")
    @Order(2)
    public void createDomainTest() throws Exception {
        CreateDomainRequest request = new CreateDomainRequest();
        request.setPayLoad(new Domain());
        request.getPayLoad().setDomainName("test");
        CreateDomainResponse response = domainSession.createDomain(request);
        Assertions.assertTrue(response.getStatusCode() == 200);
    }

    @Test
    @DisplayName("Create Duplicated Domain Test")
    @Order(3)
    public void createDuplicatedTest() throws Exception {

        CreateDomainRequest request = new CreateDomainRequest();
        request.setPayLoad(new Domain());
        request.getPayLoad().setDomainName("test");
        Assertions.assertThrows(DomainAlreadyExistsException.class, () -> {
            CreateDomainResponse response = domainSession.createDomain(request);
            Assertions.assertFalse(response.getStatusCode() == 200);
        });
    }

    @Test
    @DisplayName("Delete Domain Test")
    @Order(4)
    public void deleteDomainTest() throws Exception {
        DeleteDomainRequest request = new DeleteDomainRequest("test");
        DeleteDomainResponse response = domainSession.deleteDomain(request);
        Assertions.assertTrue(response.getStatusCode() == 200);
    }

//    @Test
//    @Order(5)
//    @DisplayName("Create Domain Test Again")
//    public void createDomainAgainTest() throws Exception {
//        CreateDomainRequest request = new CreateDomainRequest();
//        request.setPayLoad(new Domain());
//        request.getPayLoad().setDomainName("test");
//        CreateDomainResponse response = domainSession.createDomain(request);
//        Assertions.assertTrue(response.getStatusCode() == 200);
//
//    }

}
