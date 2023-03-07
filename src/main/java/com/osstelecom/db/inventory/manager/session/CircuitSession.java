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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.osstelecom.db.inventory.manager.dto.CircuitPathDTO;
import com.osstelecom.db.inventory.manager.exception.ArangoDaoException;
import com.osstelecom.db.inventory.manager.exception.DomainNotFoundException;
import com.osstelecom.db.inventory.manager.exception.GenericException;
import com.osstelecom.db.inventory.manager.exception.InvalidRequestException;
import com.osstelecom.db.inventory.manager.exception.ResourceNotFoundException;
import com.osstelecom.db.inventory.manager.exception.SchemaNotFoundException;
import com.osstelecom.db.inventory.manager.exception.ScriptRuleException;
import com.osstelecom.db.inventory.manager.operation.CircuitResourceManager;
import com.osstelecom.db.inventory.manager.operation.DomainManager;
import com.osstelecom.db.inventory.manager.operation.ManagedResourceManager;
import com.osstelecom.db.inventory.manager.operation.ResourceConnectionManager;
import com.osstelecom.db.inventory.manager.request.CreateCircuitPathRequest;
import com.osstelecom.db.inventory.manager.request.CreateCircuitRequest;
import com.osstelecom.db.inventory.manager.request.FilterRequest;
import com.osstelecom.db.inventory.manager.request.GetCircuitPathRequest;
import com.osstelecom.db.inventory.manager.request.PatchCircuitResourceRequest;
import com.osstelecom.db.inventory.manager.resources.CircuitResource;
import com.osstelecom.db.inventory.manager.resources.Domain;
import com.osstelecom.db.inventory.manager.resources.GraphList;
import com.osstelecom.db.inventory.manager.resources.ManagedResource;
import com.osstelecom.db.inventory.manager.resources.ResourceConnection;
import com.osstelecom.db.inventory.manager.resources.exception.AttributeConstraintViolationException;
import com.osstelecom.db.inventory.manager.response.CreateCircuitPathResponse;
import com.osstelecom.db.inventory.manager.response.CreateCircuitResponse;
import com.osstelecom.db.inventory.manager.response.FilterResponse;
import com.osstelecom.db.inventory.manager.response.GetCircuitPathResponse;
import com.osstelecom.db.inventory.manager.response.PatchCircuitResourceResponse;
import java.io.IOException;

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
    private CircuitResourceManager circuitResourceManager;

    @Autowired
    private ResourceConnectionManager resourceConnectionManager;

    @Autowired
    private ManagedResourceManager managedResourceManager;

    private Logger logger = LoggerFactory.getLogger(CircuitSession.class);

    /**
     * Atualiza um circuito no netcompass
     *
     * @param request
     */
    public PatchCircuitResourceResponse patchCircuitResource(PatchCircuitResourceRequest request) throws DomainNotFoundException, ResourceNotFoundException, ArangoDaoException, IOException, InvalidRequestException {

        if (request == null && request.getPayLoad() == null) {
            throw new InvalidRequestException("Please provide data in the request and payLoad");
        }

        CircuitResource requestedCircuit = request.getPayLoad();

        //
        // domain or domainName must be set
        //
        if (requestedCircuit.getDomainName() == null) {
            //
            // Set o domainname
            //
            requestedCircuit.setDomainName(request.getRequestDomain());
        }

        //
        // Arruma o domain para funcionar certinho
        //
        requestedCircuit.setDomain(this.domainManager.getDomain(requestedCircuit.getDomainName()));
        requestedCircuit.setDomainName(requestedCircuit.getDomain().getDomainName());

        //
        // Obtem a referencia do DB
        //
        CircuitResource fromDbCircuit = this.circuitResourceManager.findCircuitResource(requestedCircuit);

        if (requestedCircuit.getName() != null) {
            if (requestedCircuit.getName().indexOf("$") != -1) {
                //SELECT
            }
            else{
                fromDbCircuit.setName(requestedCircuit.getName());
            }
        }

        if (requestedCircuit.getNodeAddress() != null) {
            if (requestedCircuit.getNodeAddress().indexOf("$") != -1) {
                //SELECT
            }
            else{
                fromDbCircuit.setNodeAddress(requestedCircuit.getNodeAddress());
            }
        }

        if (requestedCircuit.getClassName() != null) {
            if (requestedCircuit.getClassName().indexOf("$") != -1) {
                //SELECT
            }
            else{
                fromDbCircuit.setClassName(requestedCircuit.getClassName());
            }
        }

        if (requestedCircuit.getOperationalStatus() != null) {
            if (requestedCircuit.getOperationalStatus().indexOf("$") != -1) {
                //SELECT
            }
            else{
                fromDbCircuit.setOperationalStatus(requestedCircuit.getOperationalStatus());
            }
        }

        if (requestedCircuit.getAdminStatus() != null) {
            if (requestedCircuit.getAdminStatus().indexOf("$") != -1) {
                //SELECT
            }
            else{
                fromDbCircuit.setAdminStatus(requestedCircuit.getAdminStatus());
            }
        }

        //
        // Atualiza os atributos
        //
        if (requestedCircuit.getAttributes() != null && !requestedCircuit.getAttributes().isEmpty()) {
            requestedCircuit.getAttributes().forEach((name, attribute) -> {
                if (fromDbCircuit.getAttributes() != null) {
                    if (fromDbCircuit.getAttributes().containsKey(name)) {
                        fromDbCircuit.getAttributes().replace(name, attribute);
                    } else {
                        fromDbCircuit.getAttributes().put(name, attribute);
                    }
                }
            });
        }

        //
        // Atualiza os atributos de rede
        //
        if (requestedCircuit.getDiscoveryAttributes() != null && !requestedCircuit.getDiscoveryAttributes().isEmpty()) {
            requestedCircuit.getDiscoveryAttributes().forEach((name, attribute) -> {
                if (fromDbCircuit.getDiscoveryAttributes() != null) {
                    if (fromDbCircuit.getDiscoveryAttributes().containsKey(name)) {
                        fromDbCircuit.getDiscoveryAttributes().replace(name, attribute);
                    } else {
                        fromDbCircuit.getDiscoveryAttributes().put(name, attribute);
                    }
                }
            });
        }

//        if (requestedCircuit.getDependentService() != null) {
//            //
//            // Valida se o serviço existe
//            //
//            ServiceResource service = this.circuitResourceManager.getService(requestedCircuit.getDependentService());
//
//            //
//            // Atualiza para referencia do DB
//            //
//            requestedCircuit.setDependentService(service);
//
//            //
//            // Agora vamos ver se o serviço é de um dominio diferente do recurso... não podem ser do mesmo
//            //
//            if (service.getDomain().getDomainName().equals(requestedCircuit.getDomain().getDomainName())) {
//                throw new InvalidRequestException("Resource and Parent Service cannot be in the same domain.");
//            }
//
//            if (fromDbCircuit.getDependentService() == null) {
//                //
//                // Está criando a dependencia...
//                //
//                fromDbCircuit.setDependentService(requestedCircuit.getDependentService());
//            } else if (!fromDbCircuit.getDependentService().equals(service)) {
//                fromDbCircuit.setDependentService(requestedCircuit.getDependentService());
//            }
//        }
        return new PatchCircuitResourceResponse(this.circuitResourceManager.updateCircuitResource(fromDbCircuit));
    }

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
    public CreateCircuitResponse createCircuit(CreateCircuitRequest request)
            throws ResourceNotFoundException, GenericException, SchemaNotFoundException,
            AttributeConstraintViolationException, ScriptRuleException, DomainNotFoundException, ArangoDaoException, InvalidRequestException {
        if (request.getPayLoad().getaPoint().getDomain() == null) {
            if (request.getPayLoad().getaPoint().getDomainName() != null) {
                request.getPayLoad().getaPoint()
                        .setDomain(domainManager.getDomain(request.getPayLoad().getaPoint().getDomainName()));
            } else {
                request.getPayLoad().getaPoint().setDomain(domainManager.getDomain(request.getRequestDomain()));
            }

        }
        //
        // Default to UP
        //
        if (request.getPayLoad().getOperationalStatus() == null) {
            request.getPayLoad().setOperationalStatus("UP");
        }

        if (request.getPayLoad().getzPoint().getDomain() == null) {
            if (request.getPayLoad().getzPoint().getDomainName() != null) {
                request.getPayLoad().getzPoint()
                        .setDomain(domainManager.getDomain(request.getPayLoad().getzPoint().getDomainName()));
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
        ManagedResource aPoint = managedResourceManager.findManagedResource(request.getPayLoad().getaPoint());

        //
        // The "To" Circuit Destination
        //
        ManagedResource zPoint = managedResourceManager.findManagedResource(request.getPayLoad().getzPoint());

        CircuitResource circuit = request.getPayLoad();
        if (circuit.getAttributeSchemaName() == null) {
            circuit.setAttributeSchemaName("circuit.default");
        } else if (circuit.getAttributeSchemaName().equals("default")) {
            circuit.setAttributeSchemaName("circuit.default");
        }

        if (circuit.getClassName() == null) {
            circuit.setClassName("circuit.Default");
        }

        circuit.setaPoint(aPoint);
        circuit.setzPoint(zPoint);
        circuit.setDomain(domainManager.getDomain(request.getRequestDomain()));
        circuit.setInsertedDate(new Date());
        return new CreateCircuitResponse(
                circuitResourceManager.createCircuitResource(circuit));
    }

    /**
     * Lista o path de um circuito
     *
     * @param request
     * @return
     * @throws ResourceNotFoundException
     */
    public GetCircuitPathResponse findCircuitPath(GetCircuitPathRequest request)
            throws ResourceNotFoundException, DomainNotFoundException, ArangoDaoException, InvalidRequestException {
        CircuitPathDTO circuitDto = request.getPayLoad();
        CircuitResource circuit = circuitDto.getCircuit();
        circuit.setDomainName(request.getRequestDomain());
        circuit.setDomain(domainManager.getDomain(circuit.getDomainName()));
        circuit = circuitResourceManager.findCircuitResource(circuitDto.getCircuit());
        circuitDto.setCircuit(circuit);
        circuitDto.setPaths(circuitResourceManager.findCircuitPaths(circuit).toList());

        logger.debug("Found [{}] Paths for Circuit: [{}/{}] Class: ({})", circuitDto.getPaths().size(),
                circuit.getNodeAddress(), circuit.getDomainName(), circuit.getClassName());

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
                if (brokenNodes.contains(circuit.getzPoint().getId())
                        || brokenNodes.contains(circuit.getaPoint().getId())) {
                    circuit.setBroken(true);
                }
                circuit.setBrokenResources(brokenNodes);
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
    public CreateCircuitPathResponse createCircuitPath(CreateCircuitPathRequest request)
            throws DomainNotFoundException, ResourceNotFoundException, ArangoDaoException, InvalidRequestException {
        CreateCircuitPathResponse r = new CreateCircuitPathResponse(request.getPayLoad());
        //
        // Valida se temos paths...na request
        //

        Domain domain = this.domainManager.getDomain(request.getRequestDomain());

        CircuitResource circuit = request.getPayLoad().getCircuit();
        if (circuit.getDomainName() == null) {
            circuit.setDomainName(domain.getDomainName());

//            String domainName = this.domainManager.getDomainNameFromId(circuit.getId());
//            circuit.setDomainName(domainName);
        }
        circuit.setDomain(domainManager.getDomain(circuit.getDomainName()));
        circuit = circuitResourceManager.findCircuitResource(circuit);
        request.getPayLoad().setCircuit(circuit);
        if (!request.getPayLoad().getPaths().isEmpty()) {
            List<ResourceConnection> resolved = new ArrayList<>();
            logger.debug("Paths Size: {}", request.getPayLoad().getPaths().size());
            for (ResourceConnection requestedPath : request.getPayLoad().getPaths()) {
                logger.debug("Path In Domain : {}", requestedPath.getDomainName());
                if (requestedPath.getDomainName() == null) {
                    requestedPath.setDomain(domain);
                } else {
                    requestedPath.setDomain(domainManager.getDomain(requestedPath.getDomainName()));
                }
                //
                // Valida se dá para continuar
                //
                if (requestedPath.getNodeAddress() == null
                        && (requestedPath.getFrom() == null || requestedPath.getTo() == null)) {
                    //
                    // No Node Address
                    //
                    InvalidRequestException ex = new InvalidRequestException(
                            "Please give at least,nodeAddress or from and to");
                    ex.addDetails("connection", requestedPath);
                    throw ex;
                }

                ResourceConnection b = resourceConnectionManager.findResourceConnection(requestedPath);
                if (!b.getCircuits().contains(circuit.getId())) {
                    b.getCircuits().add(circuit.getId());
                    //
                    // This needs updates
                    //
                    if (!circuit.getCircuitPath().contains(b.getId())) {
                        circuit.getCircuitPath().add(b.getId());

                    }

                } else {
                    logger.warn("Connection: [{}] Already Has Circuit: {}", b.getId(), circuit.getId());
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
                resolved = resourceConnectionManager.updateResourceConnections(resolved, circuit.getDomain());
                request.getPayLoad().getPaths().clear();
                request.getPayLoad().getPaths().addAll(resolved);
                circuit = circuitResourceManager.updateCircuitResource(circuit);
            } else {
                //
                // Aqui temos um problema que precisamos ver se precisamos tratar.
                //
                logger.warn("Resolved Path Differs from Request: {}", resolved.size());
            }
        }
        return r;
    }

    public FilterResponse findCircuitByFilter(FilterRequest filter) throws InvalidRequestException, ArangoDaoException, DomainNotFoundException, ResourceNotFoundException {
        FilterResponse response = new FilterResponse(filter.getPayLoad());
        if (filter.getPayLoad().getObjects().contains("circuit") || filter.getPayLoad().getObjects().contains("circuits")) {
            Domain domain = domainManager.getDomain(filter.getRequestDomain());
            GraphList<CircuitResource> graphList = circuitResourceManager.findCircuitsByFilter(filter.getPayLoad(), domain);
            response.getPayLoad().setCircuits(graphList.toList());
            response.getPayLoad().setCircuitCount(graphList.size());
            response.setSize(graphList.size());
        } else {
            throw new InvalidRequestException("Filter object does not have circuit");
        }

        return response;
    }

}
