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
package com.osstelecom.db.inventory.manager.session;

import com.osstelecom.db.inventory.manager.dto.CircuitPathDTO;
import com.osstelecom.db.inventory.manager.exception.ArangoDaoException;
import com.osstelecom.db.inventory.manager.exception.DomainNotFoundException;
import com.osstelecom.db.inventory.manager.exception.GenericException;
import com.osstelecom.db.inventory.manager.exception.InvalidRequestException;
import com.osstelecom.db.inventory.manager.exception.ResourceNotFoundException;
import com.osstelecom.db.inventory.manager.exception.SchemaNotFoundException;
import com.osstelecom.db.inventory.manager.exception.ScriptRuleException;
import com.osstelecom.db.inventory.manager.operation.DomainManager;
import com.osstelecom.db.inventory.manager.request.CreateCircuitPathRequest;
import com.osstelecom.db.inventory.manager.request.CreateCircuitRequest;
import com.osstelecom.db.inventory.manager.request.GetCircuitPathRequest;
import com.osstelecom.db.inventory.manager.resources.CircuitResource;
import com.osstelecom.db.inventory.manager.resources.ManagedResource;
import com.osstelecom.db.inventory.manager.resources.ResourceConnection;
import com.osstelecom.db.inventory.manager.resources.exception.AttributeConstraintViolationException;
import com.osstelecom.db.inventory.manager.response.CreateCircuitPathResponse;
import com.osstelecom.db.inventory.manager.response.CreateCircuitResponse;
import com.osstelecom.db.inventory.manager.response.GetCircuitPathResponse;
import java.util.ArrayList;
import java.util.Date;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 *
 * @author Lucas Nishimura <lucas.nishimura@gmail.com>
 * @created 08.08.2022
 */
@Service
public class CircuitSession {

    @Autowired
    private DomainManager domainManager;

    @Autowired
    private UtilSession utils;

    private Logger logger = LoggerFactory.getLogger(CircuitSession.class);

    /**
     * Cria um circuito
     *
     * @param request
     * @return
     * @throws ResourceNotFoundException
     * @throws GenericException
     * @throws SchemaNotFoundException
     * @throws AttributeConstraintViolationException
     * @throws ScriptRuleException
     * @throws DomainNotFoundException
     */
    public CreateCircuitResponse createCircuit(CreateCircuitRequest request) throws ResourceNotFoundException, GenericException, SchemaNotFoundException, AttributeConstraintViolationException, ScriptRuleException, DomainNotFoundException, ArangoDaoException {
        if (request.getPayLoad().getaPoint().getDomain() == null) {
            if (request.getPayLoad().getaPoint().getDomainName() != null) {
                request.getPayLoad().getaPoint().setDomain(domainManager.getDomain(request.getPayLoad().getaPoint().getDomainName()));
            } else {
                request.getPayLoad().getaPoint().setDomain(domainManager.getDomain(request.getRequestDomain()));
            }

        }

        if (request.getPayLoad().getzPoint().getDomain() == null) {
            if (request.getPayLoad().getzPoint().getDomainName() != null) {
                request.getPayLoad().getzPoint().setDomain(domainManager.getDomain(request.getPayLoad().getzPoint().getDomainName()));
            } else {
                request.getPayLoad().getzPoint().setDomain(domainManager.getDomain(request.getRequestDomain()));
            }

        }

        if (request.getPayLoad().getNodeAddress() == null) {
            request.getPayLoad().setNodeAddress(request.getPayLoad().getName());
        }

        //
        // The "From" Circuit Source
        //
        ManagedResource aPoint = domainManager.findManagedResource(request.getPayLoad().getaPoint());

        //
        // The "To" Circuit Destination
        //
        ManagedResource zPoint = domainManager.findManagedResource(request.getPayLoad().getzPoint());

        CircuitResource circuit = request.getPayLoad();
        if (circuit.getAttributeSchemaName().equalsIgnoreCase("default")) {
            circuit.setAttributeSchemaName("circuit.default");
        }

        if (circuit.getClassName().equalsIgnoreCase("Default")) {
            circuit.setClassName("circuit.Default");
        }

        circuit.setaPoint(aPoint);
        circuit.setzPoint(zPoint);
        circuit.setDomain(domainManager.getDomain(request.getRequestDomain()));
        circuit.setInsertedDate(new Date());
        CreateCircuitResponse response = new CreateCircuitResponse(domainManager.createCircuitResource(circuit));
        return response;
    }

    /**
     * Lista o path de um circuito
     *
     * @param request
     * @return
     * @throws ResourceNotFoundException
     */
    public GetCircuitPathResponse getCircuitPath(GetCircuitPathRequest request) throws ResourceNotFoundException, DomainNotFoundException, ArangoDaoException {
        CircuitPathDTO circuitDto = request.getPayLoad();
        CircuitResource circuit = circuitDto.getCircuit();
        circuit.setDomainName(request.getRequestDomain());
        circuit.setDomain(domainManager.getDomain(circuit.getDomainName()));
        circuit = domainManager.findCircuitResource(circuitDto.getCircuit());
        circuitDto.setCircuit(circuit);
        circuitDto.setPaths(domainManager.findCircuitPath(circuit).toList());
        logger.debug("Found [" + circuitDto.getPaths().size() + "] Paths for Circuit: [" + circuit.getNodeAddress() + "]");
        for (ResourceConnection connection : circuitDto.getPaths()) {
            if (!connection.getOperationalStatus().equalsIgnoreCase("UP")) {
                circuitDto.setDegrated(true);
            }
        }
        GetCircuitPathResponse response = new GetCircuitPathResponse(circuitDto);
        return response;
    }

    /**
     * Creates a Circuit Path,
     *
     * @todo: colocar validação de A-Z Point
     * @param request
     * @return
     * @throws DomainNotFoundException
     * @throws ResourceNotFoundException
     * @throws ArangoDaoException
     */
    public CreateCircuitPathResponse createCircuitPath(CreateCircuitPathRequest request) throws DomainNotFoundException, ResourceNotFoundException, ArangoDaoException, InvalidRequestException {
        CreateCircuitPathResponse r = new CreateCircuitPathResponse(request.getPayLoad());
        //
        // Valida se temos paths...na request
        //

        CircuitResource circuit = request.getPayLoad().getCircuit();
        circuit.setDomain(domainManager.getDomain(circuit.getDomainName()));
        circuit = domainManager.findCircuitResource(circuit);

        request.getPayLoad().setCircuit(circuit);
        if (!request.getPayLoad().getPaths().isEmpty()) {
            ArrayList<ResourceConnection> resolved = new ArrayList<>();
            for (ResourceConnection a : request.getPayLoad().getPaths()) {

                a.setDomain(domainManager.getDomain(a.getDomainName()));
                //
                // Valida se dá para continuar
                //

                if (a.getNodeAddress() == null && (a.getFrom() == null || a.getTo() == null)) {
                    //
                    // 
                    //
                    InvalidRequestException ex = new InvalidRequestException("Please give at least,nodeAddress or from and to");
                    ex.setDetails("connection", a);
                    throw ex;
                }

                ResourceConnection b = domainManager.findResourceConnection(a);
                if (!b.getCircuits().contains(circuit.getId())) {
                    b.getCircuits().add(circuit.getId());
                    //
                    // This needs updates
                    //
                    b = domainManager.updateResourceConnection(b);
                    if (!circuit.getCircuitPath().contains(b.getId())) {
                        circuit.getCircuitPath().add(b.getId());
                        circuit = domainManager.updateCircuitResource(circuit);
                    }
                } else {
//                    System.out.println("ALREADY HEREEE!!!");
                }
                resolved.add(b);

            }
            //
            // Melhorar esta validação!
            // 
            if (resolved.size() == request.getPayLoad().getPaths().size()) {
                request.getPayLoad().getPaths().clear();
                request.getPayLoad().getPaths().addAll(resolved);
            }
        }
        return r;
    }
}
