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
import java.util.List;

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
    public GetCircuitPathResponse findCircuitPath(GetCircuitPathRequest request) throws ResourceNotFoundException, DomainNotFoundException, ArangoDaoException {
        CircuitPathDTO circuitDto = request.getPayLoad();
        CircuitResource circuit = circuitDto.getCircuit();
        circuit.setDomainName(request.getRequestDomain());
        circuit.setDomain(domainManager.getDomain(circuit.getDomainName()));
        circuit = domainManager.findCircuitResource(circuitDto.getCircuit());
        circuitDto.setCircuit(circuit);
        circuitDto.setPaths(domainManager.findCircuitPaths(circuit).toList());

        logger.debug("Found [" + circuitDto.getPaths().size() + "] Paths for Circuit: [" + circuit.getNodeAddress() + "/" + circuit.getDomainName() + "] Class: (" + circuit.getClassName() + ")");

        if (!circuitDto.getPaths().isEmpty()) {
            for (ResourceConnection connection : circuitDto.getPaths()) {

                //
                // get current node status
                //
                if (!connection.getOperationalStatus().equalsIgnoreCase("UP")) {
                    circuit.setDegrated(true);
                }

            }

            List<String> brokenNodes = this.domainManager.checkBrokenGraph(circuitDto.getPaths(), circuit.getaPoint());

            if (!brokenNodes.isEmpty()) {
                //
                // Check if the broken nodes has the zPoint or aPoint,
                // If so it means that the circuit is broken!
                //
                if (brokenNodes.contains(circuit.getzPoint().getId()) || brokenNodes.contains(circuit.getaPoint().getId())) {
                    circuit.setBroken(true);
                }
                circuit.setBrokenResources(brokenNodes);
            }

        }
        GetCircuitPathResponse response = new GetCircuitPathResponse(circuitDto);
        return response;
    }

    /**
     * Check if a circuit is OK or Not, if the transition happen, we should
     * update it
     *
     * @param circuit
     * @param connections
     * @param target
     */
    public void computeCircuitIntegrity(CircuitResource circuit) throws ArangoDaoException {
        //
        // Forks with the logic of checking integrity
        //
        Long start = System.currentTimeMillis();
        Boolean stateChanged = false;
        List<ResourceConnection> connections = this.domainManager.findCircuitPaths(circuit).toList();
        Boolean degratedFlag = false;
        for (ResourceConnection connection : connections) {

            //
            // get current node status
            //
            if (!connection.getOperationalStatus().equalsIgnoreCase("UP")) {
                if (!circuit.getDegrated()) {
                    //
                    // Transitou de normal para degradado
                    //
                    degratedFlag = true;

                }
            }

        }

        if (circuit.getDegrated()) {
            if (!degratedFlag) {
                //
                // Estava degradado e agora normalizou
                //
                circuit.setDegrated(false);
                stateChanged = true;
            }
        } else {
            if (degratedFlag) {
                //
                // Estava bom e agora degradou
                //
                circuit.setDegrated(true);
                stateChanged = true;
            }
        }

        //
        // Checks the current state of the circuit
        //
        List<String> brokenNodes = this.domainManager.checkBrokenGraph(connections, circuit.getaPoint());

        //
        //
        //
        if (!brokenNodes.isEmpty()) {
            //
            // Check if the broken nodes has the zPoint or aPoint,
            // If so it means that the circuit is broken!
            //
            if (brokenNodes.contains(circuit.getzPoint().getId()) || brokenNodes.contains(circuit.getaPoint().getId())) {
                if (!circuit.getBroken()) {
                    //
                    // Transitou para Broken..
                    //
                    circuit.setBroken(true);
                    stateChanged = true;
                }
            } else if (circuit.getBroken()) {
                //
                // Normalizou
                //
                circuit.setBroken(false);
                stateChanged = true;

            }

            circuit.setBrokenResources(brokenNodes);
        } else if (circuit.getBroken()) {
            //
            // Normalizou
            //
            circuit.setBroken(false);
            stateChanged = true;
        }
        if (circuit.getBroken()) {
            circuit.setOperationalStatus("DOWN");
        } else {
            circuit.setOperationalStatus("UP");
        }
        Long end = System.currentTimeMillis();
        Long took = end - start;
        logger.debug("Check Circuit Integrity for [" + circuit.getId() + "] Took: " + took + " ms State Changed: " + stateChanged);
        if (stateChanged) {
            this.domainManager.updateCircuitResource(circuit);
        }
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
            List<ResourceConnection> resolved = new ArrayList<>();
            logger.debug("Paths Size:" + request.getPayLoad().getPaths().size());
//            logger.debug(utils.toJson(request.getPayLoad()));
            for (ResourceConnection requestedPath : request.getPayLoad().getPaths()) {

                requestedPath.setDomain(domainManager.getDomain(requestedPath.getDomainName()));
                //
                // Valida se dá para continuar
                //
                if (requestedPath.getNodeAddress() == null && (requestedPath.getFrom() == null || requestedPath.getTo() == null)) {
                    //
                    // No Node Address
                    //
                    InvalidRequestException ex = new InvalidRequestException("Please give at least,nodeAddress or from and to");
                    ex.setDetails("connection", requestedPath);
                    throw ex;
                }

                ResourceConnection b = domainManager.findResourceConnection(requestedPath);
                if (!b.getCircuits().contains(circuit.getId())) {
                    b.getCircuits().add(circuit.getId());
                    //
                    // This needs updates
                    //
//                    b = domainManager.updateResourceConnection(b);
                    if (!circuit.getCircuitPath().contains(b.getId())) {
                        circuit.getCircuitPath().add(b.getId());
//                        circuit = domainManager.updateCircuitResource(circuit);
                    }
                    //
                    // 
                    //

                } else {
                    logger.warn("Connection: [" + b.getId() + "] Already Has Circuit:" + circuit.getId());
                }
                resolved.add(b);

            }
            //
            // Melhorar esta validação! 
            // Dentro do processo de criação, podemos ter 
            // recebido paths inválidos...
            // Caso um path inválido seja passado o método 
            // domainManager.findResourceConnection(requestedPath)
            // Vai lançar um ResourceNotFoundException, impedindo a criação 
            // do circuito MAS nesse ponto as conexões já foram parcialmente atualizadas
            // 
            if (resolved.size() == request.getPayLoad().getPaths().size()) {

                //
                // Valida se funciona, mas batch update é muito mais rápido xD
                //
                resolved = domainManager.updateResourceConnections(resolved);
                request.getPayLoad().getPaths().clear();
                request.getPayLoad().getPaths().addAll(resolved);
                circuit = domainManager.updateCircuitResource(circuit);
            } else {
                //
                // Aqui temos um problema que precisamos ver se precisamos tratar.
                //
                logger.warn("Resolved Path Differs from Request:" + resolved.size());
            }
        }
        return r;
    }
}