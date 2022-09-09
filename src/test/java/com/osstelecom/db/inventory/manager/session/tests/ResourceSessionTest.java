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
package com.osstelecom.db.inventory.manager.session.tests;

import static org.assertj.core.api.Assertions.from;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.osstelecom.db.inventory.manager.exception.DomainAlreadyExistsException;
import com.osstelecom.db.inventory.manager.exception.DomainNotFoundException;
import com.osstelecom.db.inventory.manager.exception.GenericException;
import com.osstelecom.db.inventory.manager.request.CreateDomainRequest;
import com.osstelecom.db.inventory.manager.request.DeleteDomainRequest;
import com.osstelecom.db.inventory.manager.resources.Domain;
import com.osstelecom.db.inventory.manager.response.CreateDomainResponse;
import com.osstelecom.db.inventory.manager.response.DeleteDomainResponse;
import com.osstelecom.db.inventory.manager.session.DomainSession;
import com.osstelecom.db.inventory.manager.session.ResourceSession;

/**
 * Provavelmente os testes estão bugando por causa da depedencia do groovy com a junit4
 * @author Lucas Nishimura <lucas.nishimura@gmail.com>
 * @created 03.09.2022
 */
@SpringBootTest
public class ResourceSessionTest {

    @Autowired
    private DomainSession domainSession;

    @Autowired
    private ResourceSession resourceSession;

    @Test
    public void testIfDomainSessionExists() {
        assertThat(domainSession).isNotNull();
        System.out.println("1");
    }

    @Test

    public void testIfResourceSessionExists() {
        assertThat(resourceSession).isNotNull();
        System.out.println("2");
    }

    @Test
    public void createDomainTest() throws DomainAlreadyExistsException, GenericException {
        CreateDomainRequest createDomainRequest = new CreateDomainRequest();
        createDomainRequest.setPayLoad(new Domain());
        createDomainRequest.getPayLoad().setDomainName("AutomatedTest");
        CreateDomainResponse response = domainSession.createDomain(createDomainRequest);
        domainSession.getAllDomains().getPayLoad().forEach(d -> {
            System.out.println(": DOMAIN FOUND:[" + d.getDomainName() + "]");
        });
        assertThat(response)
                .returns(200, from(CreateDomainResponse::getStatusCode))
                .doesNotReturn(null, from(CreateDomainResponse::getPayLoad));

        System.out.println("3");
    }
    

    @Test
    @DisplayName("Test if Domain deletion is ok")
    public void deleteDomainTest() throws DomainAlreadyExistsException, GenericException, DomainNotFoundException {
        DeleteDomainRequest deleteRequest = new DeleteDomainRequest("AutomatedTest");
        DeleteDomainResponse response = domainSession.deleteDomain(deleteRequest);
        assertThat(response).returns(200, from(DeleteDomainResponse::getStatusCode))
                .doesNotReturn(null, from(DeleteDomainResponse::getPayLoad));
    }
}
