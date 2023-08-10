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
import com.osstelecom.db.inventory.manager.dto.FilterDTO;
import com.osstelecom.db.inventory.manager.exception.ArangoDaoException;
import com.osstelecom.db.inventory.manager.exception.DomainNotFoundException;
import com.osstelecom.db.inventory.manager.exception.GenericException;
import com.osstelecom.db.inventory.manager.exception.InvalidRequestException;
import com.osstelecom.db.inventory.manager.exception.ResourceNotFoundException;
import com.osstelecom.db.inventory.manager.exception.SchemaNotFoundException;
import com.osstelecom.db.inventory.manager.exception.ScriptRuleException;
import com.osstelecom.db.inventory.manager.operation.CircuitResourceManager;
import com.osstelecom.db.inventory.manager.operation.DomainManager;
import com.osstelecom.db.inventory.manager.operation.GraphManager;
import com.osstelecom.db.inventory.manager.operation.ManagedResourceManager;
import com.osstelecom.db.inventory.manager.operation.ResourceConnectionManager;
import com.osstelecom.db.inventory.manager.operation.ServiceManager;
import com.osstelecom.db.inventory.manager.request.CreateCircuitPathRequest;
import com.osstelecom.db.inventory.manager.request.CreateCircuitRequest;
import com.osstelecom.db.inventory.manager.request.DeleteCircuitRequest;
import com.osstelecom.db.inventory.manager.request.FilterRequest;
import com.osstelecom.db.inventory.manager.request.GetCircuitPathRequest;
import com.osstelecom.db.inventory.manager.request.PatchCircuitResourceRequest;
import com.osstelecom.db.inventory.manager.resources.CircuitResource;
import com.osstelecom.db.inventory.manager.resources.Domain;
import com.osstelecom.db.inventory.manager.resources.GraphList;
import com.osstelecom.db.inventory.manager.resources.ManagedResource;
import com.osstelecom.db.inventory.manager.resources.ResourceConnection;
import com.osstelecom.db.inventory.manager.resources.ServiceResource;
import com.osstelecom.db.inventory.manager.resources.exception.AttributeConstraintViolationException;
import com.osstelecom.db.inventory.manager.response.CreateCircuitPathResponse;
import com.osstelecom.db.inventory.manager.response.CreateCircuitResponse;
import com.osstelecom.db.inventory.manager.response.DeleteCircuitResponse;
import com.osstelecom.db.inventory.manager.response.FilterResponse;
import com.osstelecom.db.inventory.manager.response.GetCircuitPathResponse;
import com.osstelecom.db.inventory.manager.response.GetCircuitResponse;
import com.osstelecom.db.inventory.manager.response.PatchCircuitResourceResponse;
import java.io.IOException;

/**
 *
 * @author Lucas Nishimura
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

    @Autowired
    private ServiceManager serviceManager;

    @Autowired
    private GraphManager graphManager;

    private Logger logger = LoggerFactory.getLogger(CircuitSession.class);

    /**
     * Atualiza um circuito no netcompass
     *
     * @param request
     * @return
     * @throws DomainNotFoundException
     * @throws ResourceNotFoundException
     * @throws ArangoDaoException
     * @throws IOException
     * @throws InvalidRequestException
     * @throws SchemaNotFoundException
     * @throws GenericException
     * @throws AttributeConstraintViolationException
     * @throws ScriptRuleException
     */
    public PatchCircuitResourceResponse patchCircuitResource(PatchCircuitResourceRequest request)
            throws DomainNotFoundException, ResourceNotFoundException, ArangoDaoException, IOException,
            InvalidRequestException, SchemaNotFoundException, GenericException, AttributeConstraintViolationException,
            ScriptRuleException {

        if (request == null || request.getPayLoad() == null) {
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
        String domainName = requestedCircuit.getDomainName();
        Domain domain = this.domainManager.getDomain(domainName);
        requestedCircuit.setDomain(domain);
        requestedCircuit.setDomainName(requestedCircuit.getDomain().getDomainName());

        //
        // Obtem a referencia do DB
        //
        CircuitResource fromDbCircuit = this.circuitResourceManager.findCircuitResource(requestedCircuit);

        if (requestedCircuit.getName() != null) {
            fromDbCircuit.setName(requestedCircuit.getName());
        }

        if (requestedCircuit.getNodeAddress() != null) {
            fromDbCircuit.setNodeAddress(requestedCircuit.getNodeAddress());
        }

        if (requestedCircuit.getClassName() != null) {
            fromDbCircuit.setClassName(requestedCircuit.getClassName());
        }

        if (requestedCircuit.getOperationalStatus() != null) {
            fromDbCircuit.setOperationalStatus(requestedCircuit.getOperationalStatus());
        }

        if (requestedCircuit.getAdminStatus() != null) {
            fromDbCircuit.setAdminStatus(requestedCircuit.getAdminStatus());
        }

        if (requestedCircuit.getBusinessStatus() != null) {
            fromDbCircuit.setBusinessStatus(requestedCircuit.getBusinessStatus());
        }

        if (requestedCircuit.getDescription() != null) {
            fromDbCircuit.setDescription(requestedCircuit.getDescription());
        }

        //
        // Arruma o domain do zPoint se existir.
        //
        if (requestedCircuit.getzPoint() != null) {
            if (request.getPayLoad().getzPoint().getDomain() == null) {
                if (request.getPayLoad().getzPoint().getDomainName() != null) {
                    request.getPayLoad().getzPoint()
                            .setDomain(domainManager.getDomain(request.getPayLoad().getzPoint().getDomainName()));
                    fromDbCircuit.setzPoint(requestedCircuit.getzPoint());
                } else {
                    request.getPayLoad().getzPoint().setDomain(domainManager.getDomain(request.getRequestDomain()));
                    fromDbCircuit.setzPoint(requestedCircuit.getzPoint());
                }

            }
        }

        //
        // Arruma o domain do aPoint se existir.
        //
        if (requestedCircuit.getaPoint() != null) {
            if (request.getPayLoad().getaPoint().getDomain() == null) {
                if (request.getPayLoad().getaPoint().getDomainName() != null) {
                    request.getPayLoad().getaPoint()
                            .setDomain(domainManager.getDomain(request.getPayLoad().getaPoint().getDomainName()));
                    fromDbCircuit.setaPoint(requestedCircuit.getaPoint());
                } else {
                    request.getPayLoad().getaPoint().setDomain(domainManager.getDomain(request.getRequestDomain()));
                    fromDbCircuit.setaPoint(requestedCircuit.getaPoint());
                }

            }

        }

        //
        // The "From" Circuit Source
        //
        ManagedResource aPoint = managedResourceManager.findManagedResource(request.getPayLoad().getaPoint());

        //
        // The "To" Circuit Destination
        //
        ManagedResource zPoint = managedResourceManager.findManagedResource(request.getPayLoad().getzPoint());

        /**
         * Obviamente! a e z não podem ser o mesmos
         */
        if (aPoint.equals(zPoint)) {
            throw new InvalidRequestException("aPoint and zPoint are equals");
        }

        fromDbCircuit.setaPoint(aPoint);
        fromDbCircuit.setzPoint(zPoint);

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
     * @throws
     * com.osstelecom.db.inventory.manager.exception.InvalidRequestException
     */
    public CreateCircuitResponse createCircuit(CreateCircuitRequest request)
            throws ResourceNotFoundException, GenericException, SchemaNotFoundException,
            AttributeConstraintViolationException, ScriptRuleException, DomainNotFoundException, ArangoDaoException,
            InvalidRequestException {

        if (request.getPayLoad().getaPoint().getDomain() == null) {
            if (request.getPayLoad().getaPoint().getDomainName() != null) {
                request.getPayLoad().getaPoint()
                        .setDomain(domainManager.getDomain(request.getPayLoad().getaPoint().getDomainName()));
            } else {
                request.getPayLoad().getaPoint().setDomain(domainManager.getDomain(request.getRequestDomain()));
            }

        }
        if (request.getPayLoad().getzPoint().getDomain() == null) {
            if (request.getPayLoad().getzPoint().getDomainName() != null) {
                request.getPayLoad().getzPoint()
                        .setDomain(domainManager.getDomain(request.getPayLoad().getzPoint().getDomainName()));
            } else {
                request.getPayLoad().getzPoint().setDomain(domainManager.getDomain(request.getRequestDomain()));
            }

        }
        //
        // Default to UP
        //
        if (request.getPayLoad().getOperationalStatus() == null) {
            request.getPayLoad().setOperationalStatus("Up");
        } else {
            if (request.getPayLoad().getOperationalStatus().trim().equals("")) {
                request.getPayLoad().setOperationalStatus("Up");
            }
        }

        if (request.getPayLoad().getNodeAddress() == null) {
            request.getPayLoad().setNodeAddress(request.getPayLoad().getName());
        } else {
            if (request.getPayLoad().getNodeAddress().trim().equals("")) {
                request.getPayLoad().setNodeAddress("Up");
            }
        }

        //
        // The "From" Circuit Source
        //
        ManagedResource aPoint = managedResourceManager.findManagedResource(request.getPayLoad().getaPoint());

        //
        // The "To" Circuit Destination
        //
        ManagedResource zPoint = managedResourceManager.findManagedResource(request.getPayLoad().getzPoint());

        /**
         * Obviamente! a e z não podem ser o mesmos
         */
        if (aPoint.equals(zPoint)) {
            throw new InvalidRequestException("aPoint and zPoint are equals");
        }

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
     * Recupera um circuito pelo iD e trás a lista de paths
     *
     * @param request
     * @return
     * @throws ResourceNotFoundException
     * @throws DomainNotFoundException
     * @throws ArangoDaoException
     * @throws InvalidRequestException
     */
    public GetCircuitPathResponse findCircuitPathById(GetCircuitPathRequest request) throws ResourceNotFoundException, DomainNotFoundException, ArangoDaoException, InvalidRequestException {

        /**
         * Vamos garantir que tenhamos pelo menos o domain e o id do circuito
         */
        if (request.getRequestDomain() == null) {
            throw new InvalidRequestException("Please provide a domain");
        }
        if (request.getCircuitId() == null) {

            throw new InvalidRequestException("Please provide a CircuidID (_key)");
        }
        CircuitResource circuit = new CircuitResource(domainManager.getDomain(request.getRequestDomain()), request.getCircuitId());
        request.getPayLoad().setCircuit(circuit);

        return this.findCircuitPath(request);
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
        if (circuit.getDomain() == null) {
            circuit.setDomain(domainManager.getDomain(circuit.getDomainName()));
        }
        circuit = circuitResourceManager.findCircuitResource(circuitDto.getCircuit());
        circuitDto.setCircuit(circuit);
        /**
         * Repassa todos os paths para o response, os paths são connections
         */
        circuitDto.setPaths(circuitResourceManager.findCircuitPaths(circuit, false).toList());

        logger.debug("Found [{}] Paths for Circuit: [{}/{}] Class: ({})", circuitDto.getPaths().size(),
                circuit.getNodeAddress(), circuit.getDomainName(), circuit.getClassName());

        if (!circuitDto.getPaths().isEmpty()) {
            for (ResourceConnection connection : circuitDto.getPaths()) {

                //
                // get current node status
                //
                if (!connection.getOperationalStatus().equalsIgnoreCase("Up")) {
                    circuit.setDegrated(true);
                }

            }

            List<String> brokenNodes = this.graphManager.checkBrokenGraph(circuitDto.getPaths(), circuit.getaPoint());

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

    public CircuitResource findCircuitResource(CircuitResource resource)
            throws ResourceNotFoundException, ArangoDaoException, InvalidRequestException {
        return this.circuitResourceManager.findCircuitResource(resource);
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
            throws DomainNotFoundException, ResourceNotFoundException, ArangoDaoException, InvalidRequestException,
            SchemaNotFoundException, GenericException, AttributeConstraintViolationException, ScriptRuleException {
        CreateCircuitPathResponse r = new CreateCircuitPathResponse(request.getPayLoad());
        //
        // Valida se temos paths...na request
        //
        Domain domain = this.domainManager.getDomain(request.getRequestDomain());

        /**
         * Melhor validação de circuito, se não enviar vai lança uma exception
         */
        if (request.getPayLoad().getCircuit() == null) {
            throw new InvalidRequestException("Circuit is null, please check if circuit is provided")
                    .addDetails("circuit", "missing");
        }

        CircuitResource circuit = request.getPayLoad().getCircuit();

        if (circuit.getDomainName() == null) {
            circuit.setDomainName(domain.getDomainName());

        }

        circuit.setDomain(domainManager.getDomain(circuit.getDomainName()));
        circuit = circuitResourceManager.findCircuitResource(circuit);
        request.getPayLoad().setCircuit(circuit);
        if (!request.getPayLoad().getPaths().isEmpty()) {
            List<ResourceConnection> resolved = new ArrayList<>();
            logger.debug("Paths Size: {}", request.getPayLoad().getPaths().size());
            for (ResourceConnection requestedPath : request.getPayLoad().getPaths()) {

                if (requestedPath.getDomainName() == null) {
                    requestedPath.setDomain(domain);
                    requestedPath.setDomainName(domain.getDomainName());
                } else {
                    requestedPath.setDomain(domainManager.getDomain(requestedPath.getDomainName()));
                }
                logger.debug("Path In Domain : {}", requestedPath.getDomainName());
                //
                // Valida se dá para continuar
                //
                if (requestedPath.getKey() == null && requestedPath.getId() == null) {
                    if (requestedPath.getNodeAddress() == null
                            && (requestedPath.getFrom() == null || requestedPath.getTo() == null)) {
                        //
                        // No Node Addres
                        //
                        InvalidRequestException ex = new InvalidRequestException(
                                "Please give at least,nodeAddress or from and to");
                        ex.addDetails("connection", requestedPath);
                        throw ex;
                    } else {
                        //
                        // Arruma os domains dos recursos abaixo
                        //

                        if (requestedPath.getFromResource() != null
                                && requestedPath.getFromResource().getDomainName() == null) {
                            requestedPath.getFromResource().setDomainName(domain.getDomainName());
                        }

                        if (requestedPath.getToResource() != null
                                && requestedPath.getToResource().getDomainName() == null) {
                            requestedPath.getToResource().setDomainName(domain.getDomainName());
                        }

                    }
                } else {
                    requestedPath.setDomain(domain);
                }

                ResourceConnection b = resourceConnectionManager.findResourceConnection(requestedPath);
                if (!b.getCircuits().contains(circuit.getId())) {
                    //
                    // Garante que a Conexão vai ter uma referencia ao circuito
                    //
                    b.getCircuits().add(circuit.getId());
                } else {
                    // logger.warn("Connection: [{}] Already Has Circuit: {}", b.getId(),
                    // circuit.getId());
                }

                //
                // Se o Circuito não tem a conexão adciona a conexão no circuito
                //
                if (!circuit.getCircuitPath().contains(b.getId())) {
                    circuit.getCircuitPath().add(b.getId());
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
                circuit = circuitResourceManager.updateCircuitPath(circuit);

            } else {
                //
                // Aqui temos um problema que precisamos ver se precisamos tratar.
                //
                logger.warn("Resolved Path Differs from Request: {}", resolved.size());
            }
        } else {
            logger.warn("Empty Paths, creating Empty Circuit");
        }
        return r;
    }

    public FilterResponse findCircuitByFilter(FilterRequest filter)
            throws InvalidRequestException, ArangoDaoException, DomainNotFoundException, ResourceNotFoundException {
        FilterResponse response = new FilterResponse(filter.getPayLoad());
        //
        // Validação para evitar abusos de uso da API
        //
        if (filter.getPayLoad() != null) {
            if (filter.getPayLoad().getLimit() != null) {
                if (filter.getPayLoad().getLimit() > 1000) {
                    throw new InvalidRequestException(
                            "Result Set Limit cannot be over 1000, please descrease limit value to a range between 0 and 1000");
                }
            }
        }

        if (filter.getPayLoad().getObjects().contains("circuit")
                || filter.getPayLoad().getObjects().contains("circuits")) {
            Domain domain = domainManager.getDomain(filter.getRequestDomain());
            GraphList<CircuitResource> graphList = circuitResourceManager.findCircuitsByFilter(filter.getPayLoad(),
                    domain);
            response.getPayLoad().setCircuits(graphList.toList());
            response.getPayLoad().setCircuitCount(graphList.size());
            response.setSize(graphList.size());
        } else {
            throw new InvalidRequestException("Filter object does not have circuit");
        }

        return response;
    }

    public GraphList<CircuitResource> findCircuitResourceByFilter(FilterDTO filter)
            throws ResourceNotFoundException, ArangoDaoException, InvalidRequestException, DomainNotFoundException {
        if (filter.getObjects().contains("circuit") || filter.getObjects().contains("circuits")) {
            GraphList<CircuitResource> circuitGraph = this.circuitResourceManager.findCircuitsByFilter(filter,
                    filter.getDomainName());
            return circuitGraph;
        } else {
            throw new InvalidRequestException("Invalida Object Type:[" + String.join(",", filter.getObjects()) + "]")
                    .addDetails("filter", filter);
        }
    }

    public GetCircuitResponse findCircuitById(GetCircuitPathRequest req)
            throws DomainNotFoundException, ArangoDaoException, ResourceNotFoundException, InvalidRequestException {
        Domain domain = this.domainManager.getDomain(req.getDomainName());
        CircuitResource circuit = new CircuitResource(domain, null);
        circuit.setKey(req.getCircuitId());
        circuit = this.circuitResourceManager.findCircuitResource(circuit);
        return new GetCircuitResponse(circuit);
    }

    /**
     * Deleta um circuito
     *
     * @param req
     * @return
     * @throws DomainNotFoundException
     * @throws ArangoDaoException
     */
    public DeleteCircuitResponse deleteCircuitById(DeleteCircuitRequest req)
            throws DomainNotFoundException, ArangoDaoException, InvalidRequestException {
        Domain domain = this.domainManager.getDomain(req.getRequestDomain());
        CircuitResource circuit = req.getPayLoad();
        circuit.setDomain(domain);

        if (circuit.getKey() == null && circuit.getId() == null) {
            throw new InvalidRequestException("Please provice CircuitID or CircuitKey");
        }

        if (circuit.getId() != null && !circuit.getId().contains("/")) {
            circuit.setId(domain.getCircuits() + "/" + circuit.getId());
        }

        if (circuit.getKey() != null && circuit.getId() == null) {
            circuit.setId(domain.getCircuits() + "/" + circuit.getKey());
        }

        //
        // Vamos validar se tem algum serviço usando este circuito, pois se houver, não
        // podemos deletar..
        //
        FilterDTO filter = new FilterDTO("@circuitId in doc.circuits[*]._id");

        filter.getBindings().put("circuitId", circuit.getId());

        try {
            GraphList<ServiceResource> result = this.serviceManager.findServiceByFilter(filter, domain);
            //
            // Ruim tem serviços depentendes do circuito
            //
            throw new InvalidRequestException(
                    "Circuit Cannot Be Deleted: Dependent Service Count:[" + result.size() + "]");
        } catch (ResourceNotFoundException ex) {
            //
            // Caminho feliz, pode excluir.
            //
            circuit = this.circuitResourceManager.deleteCircuitResource(circuit);
            return new DeleteCircuitResponse(circuit);
        }

    }
}
