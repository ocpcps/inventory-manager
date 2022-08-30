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

import com.osstelecom.db.inventory.manager.dto.DomainDTO;
import com.osstelecom.db.inventory.manager.exception.DomainAlreadyExistsException;
import com.osstelecom.db.inventory.manager.exception.DomainNotFoundException;
import com.osstelecom.db.inventory.manager.exception.GenericException;
import com.osstelecom.db.inventory.manager.request.CreateDomainRequest;
import com.osstelecom.db.inventory.manager.request.DeleteDomainRequest;
import com.osstelecom.db.inventory.manager.response.CreateDomainResponse;
import com.osstelecom.db.inventory.manager.response.DeleteDomainResponse;
import com.osstelecom.db.inventory.manager.response.GetDomainsResponse;
import com.osstelecom.db.inventory.manager.session.DomainSession;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.Assertions.from;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import org.junit.Test;
import org.junit.jupiter.api.DisplayName;

/**
 * Testes de Unidade do négocio de criação de Domains..
 *
 * @author Lucas Nishimura <lucas.nishimura@gmail.com>
 * @created 30.08.2022
 */
@SpringBootTest
@RunWith(SpringJUnit4ClassRunner.class)
public class DomainSessionTest {

    @Autowired
    private DomainSession domainSession;

    @Test
    @DisplayName("Test if Create Domain is ok")
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
    @DisplayName("Test if Domain deletion is ok")
    public void deleteDomainTest() throws DomainAlreadyExistsException, GenericException, DomainNotFoundException {
        DeleteDomainRequest deleteRequest = new DeleteDomainRequest("AutomatedTest");
        DeleteDomainResponse response = domainSession.deleteDomain(deleteRequest);
        assertThat(response).returns(200, from(DeleteDomainResponse::getStatusCode))
                .doesNotReturn(null, from(DeleteDomainResponse::getPayLoad));
    }

    @Test
    public void getInexistentDomain() {
        assertThatThrownBy(() -> {
            domainSession.getDomain("xpty");
        }).isInstanceOf(DomainNotFoundException.class);

    }
    
    @Test
    public void getAllDomains() {
        assertThat(domainSession.getAllDomains())
                .returns(200, from(GetDomainsResponse::getStatusCode));
    }
}
