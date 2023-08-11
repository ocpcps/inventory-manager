/*
 * Copyright (C) 2023 Lucas Nishimura <lucas.nishimura@gmail.com>
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
package com.osstelecom.db.inventory.correlation.session;

import com.osstelecom.db.inventory.correlation.request.CorrelationRequest;
import com.osstelecom.db.inventory.manager.dto.FilterDTO;
import com.osstelecom.db.inventory.manager.dto.UpdateResourceForCorrelationDTO;
import com.osstelecom.db.inventory.manager.exception.ArangoDaoException;
import com.osstelecom.db.inventory.manager.exception.DomainNotFoundException;
import com.osstelecom.db.inventory.manager.exception.InvalidRequestException;
import com.osstelecom.db.inventory.manager.exception.ResourceNotFoundException;
import com.osstelecom.db.inventory.manager.operation.ManagedResourceManager;
import com.osstelecom.db.inventory.manager.resources.GraphList;
import com.osstelecom.db.inventory.manager.resources.ManagedResource;
import java.io.IOException;
import java.util.logging.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Processa as solicitações de Correlação do TEMS
 *
 * @author Lucas Nishimura <lucas.nishimura@gmail.com>
 * @created 10.08.2023
 */
@Service
public class CorrelationSession {

    @Autowired
    private ManagedResourceManager resourceManager;

    private Logger logger = LoggerFactory.getLogger(CorrelationSession.class);

    /**
     * Processa uma solicitação de correlação que pode vir de qualquer sistema
     * satelite
     *
     * @param request
     * @throws
     * com.osstelecom.db.inventory.manager.exception.InvalidRequestException
     * @throws
     * com.osstelecom.db.inventory.manager.exception.DomainNotFoundException
     * @throws com.osstelecom.db.inventory.manager.exception.ArangoDaoException
     */
    public void processCorrelationRequest(CorrelationRequest request) throws InvalidRequestException, DomainNotFoundException, ArangoDaoException {
        if (request.getPayLoad() != null) {
            UpdateResourceForCorrelationDTO correlationDto = request.getPayLoad();
            for (String correlationAddress : correlationDto.getCorrelationIds()) {
                String[] parts = correlationAddress.split("/", 2);  // split em 2 partes usando "/" como delimitador

                if (parts.length == 2) {
                    String domain = parts[0];
                    String correlationId = parts[1];
                    /**
                     * Aqui temos o dominio e correlationID ou seja é
                     * trabalhável, vai procurar nos nós, me pergunto se uma
                     * view não é mais performática para esta tarefa
                     */
                    FilterDTO filter = new FilterDTO("@correlationId  in v.correlationIds[*] ");
                    filter.setDomainName(domain);
                    filter.getBindings().put("correlationId", correlationId);
                    try {
                        GraphList<ManagedResource> nodes = resourceManager.getNodesByFilter(filter, domain);
                        try {
                            nodes.forEach(n -> {
                                /**
                                 * Tem nó para atualizar, vai fazer o fetch.
                                 */

                            });
                        } catch (IOException ex) {
                        }
                    } catch (ResourceNotFoundException ex) {
                        /**
                         * Este erro é esperado visto que pode não ter match,
                         * adicionar response indicando os que não deram match e
                         * os que deram match
                         */

                    }

                } else {
                    throw new InvalidRequestException("Invalid Correlation Address provided");
                }
            }

        } else {
            throw new InvalidRequestException("Correlation payload is null");
        }
    }
}
