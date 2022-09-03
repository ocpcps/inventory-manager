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
package com.osstelecom.db.inventory.session.tests;

import com.osstelecom.db.inventory.manager.dto.DomainDTO;
import com.osstelecom.db.inventory.manager.exception.ArangoDaoException;
import com.osstelecom.db.inventory.manager.exception.DomainAlreadyExistsException;
import com.osstelecom.db.inventory.manager.exception.DomainNotFoundException;
import com.osstelecom.db.inventory.manager.exception.GenericException;
import com.osstelecom.db.inventory.manager.exception.InvalidRequestException;
import com.osstelecom.db.inventory.manager.exception.SchemaNotFoundException;
import com.osstelecom.db.inventory.manager.exception.ScriptRuleException;
import com.osstelecom.db.inventory.manager.request.CreateDomainRequest;
import com.osstelecom.db.inventory.manager.request.CreateManagedResourceRequest;
import com.osstelecom.db.inventory.manager.resources.exception.AttributeConstraintViolationException;
import com.osstelecom.db.inventory.manager.response.CreateDomainResponse;
import com.osstelecom.db.inventory.manager.response.CreateManagedResourceResponse;
import com.osstelecom.db.inventory.manager.session.DomainSession;
import com.osstelecom.db.inventory.manager.session.ResourceSession;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.Assertions.from;
import org.junit.Test;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.TestMethodOrder;

/**
 *
 * @author Lucas Nishimura <lucas.nishimura@gmail.com>
 * @created 03.09.2022
 */
@SpringBootTest
@RunWith(SpringJUnit4ClassRunner.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ResourceSessionTest {

    @Autowired
    private DomainSession domainSession;

    @Autowired
    private ResourceSession resourceSession;

    @Test
    @Order(1)
    public void testIfDomainSessionExists() {
        assertThat(domainSession).isNotNull();
    }

    @Test
    @Order(2)
    public void testIfResourceSessionExists() {
        assertThat(resourceSession).isNotNull();
    }

    @Test
    @Order(3)
    public void createDomainTest() throws DomainAlreadyExistsException, GenericException {
        CreateDomainRequest createDomainRequest = new CreateDomainRequest();
        createDomainRequest.setPayLoad(new DomainDTO());
        createDomainRequest.getPayLoad().setDomainName("AutomatedTest");
        CreateDomainResponse response = domainSession.createDomain(createDomainRequest);
        assertThat(response)
                .returns(200, from(CreateDomainResponse::getStatusCode))
                .doesNotReturn(null, from(CreateDomainResponse::getPayLoad));
    }

    @Test
    @Order(4)
    public void createManagedResouce() throws SchemaNotFoundException, AttributeConstraintViolationException, GenericException, ScriptRuleException, InvalidRequestException, DomainNotFoundException, ArangoDaoException {
        CreateManagedResourceRequest request = new CreateManagedResourceRequest();
        request.getPayLoad().setNodeAddress("Teste1Node");
        request.getPayLoad().setName("Teste1Node");
        request.getPayLoad().setClassName("resource.Default");
        request.getPayLoad().setAttributeSchemaName("resource.default");
        CreateManagedResourceResponse response = resourceSession.createManagedResource(request);
        assertThat(response)
                .returns(200, from(CreateManagedResourceResponse::getStatusCode))
                .doesNotReturn(null, from(CreateManagedResourceResponse::getPayLoad));
    }
}
