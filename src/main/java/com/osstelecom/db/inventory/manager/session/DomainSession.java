/*
 * Copyright (C) 2021 Lucas Nishimura <lucas.nishimura@gmail.com>
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

import com.osstelecom.db.inventory.manager.dto.FilterDTO;
import com.osstelecom.db.inventory.manager.exception.ArangoDaoException;
import com.osstelecom.db.inventory.manager.exception.AttributeNotFoundException;
import com.osstelecom.db.inventory.manager.exception.DomainAlreadyExistsException;
import com.osstelecom.db.inventory.manager.exception.DomainNotFoundException;
import com.osstelecom.db.inventory.manager.exception.GenericException;
import com.osstelecom.db.inventory.manager.exception.InvalidRequestException;
import com.osstelecom.db.inventory.manager.exception.ResourceNotFoundException;
import com.osstelecom.db.inventory.manager.exception.SchemaNotFoundException;
import com.osstelecom.db.inventory.manager.exception.ScriptRuleException;
import com.osstelecom.db.inventory.manager.jobs.DBJobInstance;
import com.osstelecom.db.inventory.manager.jobs.DbJobStage;
import com.osstelecom.db.inventory.manager.operation.CircuitResourceManager;
import com.osstelecom.db.inventory.manager.operation.DbJobManager;
import com.osstelecom.db.inventory.manager.operation.DomainManager;
import com.osstelecom.db.inventory.manager.operation.ManagedResourceManager;
import com.osstelecom.db.inventory.manager.operation.ResourceConnectionManager;

import com.osstelecom.db.inventory.manager.request.CreateDomainRequest;
import com.osstelecom.db.inventory.manager.request.DeleteDomainRequest;
import com.osstelecom.db.inventory.manager.request.GetRequest;
import com.osstelecom.db.inventory.manager.request.UpdateDomainRequest;
import com.osstelecom.db.inventory.manager.resources.CircuitResource;
import com.osstelecom.db.inventory.manager.resources.Domain;
import com.osstelecom.db.inventory.manager.resources.StringResponse;
import com.osstelecom.db.inventory.manager.resources.GraphList;
import com.osstelecom.db.inventory.manager.resources.ManagedResource;
import com.osstelecom.db.inventory.manager.resources.ResourceConnection;
import com.osstelecom.db.inventory.manager.resources.exception.AttributeConstraintViolationException;
import com.osstelecom.db.inventory.manager.response.CreateDomainResponse;
import com.osstelecom.db.inventory.manager.response.DeleteDomainResponse;
import com.osstelecom.db.inventory.manager.response.DomainResponse;
import com.osstelecom.db.inventory.manager.response.GetDomainsResponse;
import com.osstelecom.db.inventory.manager.response.UpdateDomainResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 *
 * @author Lucas Nishimura
 * @created 15.12.2021
 */
@Service
public class DomainSession {

    @Autowired
    private DomainManager domainManager;

    @Autowired
    private ManagedResourceManager managedResourceManager;

    @Autowired
    private CircuitResourceManager circuitManager;

    @Autowired
    private ResourceConnectionManager resourceConnectionManager;

    @Autowired
    private DbJobManager jobManager;

    private final Logger logger = LoggerFactory.getLogger(DomainSession.class);

    private Map<String, Boolean> runningReconcilations = new ConcurrentHashMap<>();

    /**
     * Deleta um domain, cuidado pois deleta tudo que está dentro do domain
     *
     * @param request
     * @return
     * @throws DomainNotFoundException
     * @throws ArangoDaoException
     * @throws ResourceNotFoundException
     * @throws IOException
     */
    public DeleteDomainResponse deleteDomain(DeleteDomainRequest request) throws DomainNotFoundException, ArangoDaoException, ResourceNotFoundException, IOException {
        return new DeleteDomainResponse(domainManager.deleteDomain(request.getPayLoad()));
    }

    /**
     * Cria um novo dominio
     *
     * @param domainRequest
     * @return
     * @throws DomainAlreadyExistsException
     * @throws GenericException
     */
    public CreateDomainResponse createDomain(CreateDomainRequest domainRequest) throws DomainAlreadyExistsException, GenericException {
        try {
            return new CreateDomainResponse(this.domainManager.createDomain(domainRequest.getPayLoad()));
        } catch (DomainAlreadyExistsException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new GenericException(ex.getMessage());
        }
    }

    /**
     * Recupera a lista dos dominios existentes
     *
     * @return
     */
    public GetDomainsResponse getAllDomains() {
        return new GetDomainsResponse(this.domainManager.getAllDomains());
    }

    /**
     * Recupera um domain a partir do nome
     *
     * @param domainName
     * @return
     * @throws DomainNotFoundException
     * @throws InvalidRequestException
     * @throws ArangoDaoException
     * @throws ResourceNotFoundException
     * @throws IOException
     */
    public DomainResponse getDomain(String domainName) throws DomainNotFoundException, InvalidRequestException, ArangoDaoException, ResourceNotFoundException, IOException {
        if (domainName == null) {
            throw new InvalidRequestException("domainName cannot be null");
        }
        return new DomainResponse(domainManager.getDomain(domainName));
    }

    /**
     * Atualiza um domain
     *
     * @param domainRequest
     * @return
     * @throws DomainNotFoundException
     * @throws ArangoDaoException
     */
    public UpdateDomainResponse updateDomain(UpdateDomainRequest domainRequest) throws DomainNotFoundException, ArangoDaoException {
        Domain currentDomain = domainManager.getDomain(domainRequest.getRequestDomain());
        if (currentDomain.getDomainDescription() != domainRequest.getPayLoad().getDomainDescription()) {
            currentDomain.setLastUpdate(new Date());
            currentDomain.setDomainDescription(domainRequest.getPayLoad().getDomainDescription());
            UpdateDomainResponse response = new UpdateDomainResponse(this.domainManager.updateDomain(currentDomain));
            return response;
        }
        return new UpdateDomainResponse(domainRequest.getPayLoad());
    }

    public StringResponse reconcileDomain(GetRequest req) throws DomainNotFoundException, ArangoDaoException {
        if (runningReconcilations.containsKey(req.getRequestDomain())) {
            return new StringResponse("Already Running");
        } else {
            /**
             * This will Trigger a massive update in the system, use with
             * caution
             */
            Domain domain = this.domainManager.getDomain(req.getRequestDomain());
            logger.debug("Starting Reconciliation Thread");
            new Thread(() -> {
                try {
                    this.reconcileDomain(domain, true);
                } catch (DomainNotFoundException | ArangoDaoException | InvalidRequestException ex) {
                    logger.error("Failed Reconcile", ex);
                } finally {
                    logger.debug("Reconciliation Job Completed");
                }
            }, "com.osstelecom.db.inventory.manager.session.RECONCILIATION-" + req.getRequestDomain()).start();
            runningReconcilations.put(req.getRequestDomain(), true);
            return new StringResponse("Domain:[" + req.getRequestDomain() + "] Reconciliation Started");
        }
    }

    /**
     * If for some reason we had updates direct in the database, we need to fix
     * all crossed references if withinDomain is true, it will reconcile only in
     * the specified domain
     */
    private void reconcileDomain(Domain domain, Boolean withinDomain) throws DomainNotFoundException, ArangoDaoException, InvalidRequestException {

        /**
         * Now for each Node, wi will update
         */
        DBJobInstance job = this.jobManager.createJobInstance("Reconciliation JOB:[" + domain.getDomainName() + "]");
        try {
            jobManager.notifyJobStart(job);
            try (GraphList<ManagedResource> resources = this.managedResourceManager.findAll(domain)) {

                DbJobStage updateResourcesStage = job.createJobStage("UPDATE_RESOURCES", domain.getDomainName());
                updateResourcesStage.setJobDescription("Update All Resources in The Domain[" + domain.getDomainName() + "], Forcing Cascade Update");
                updateResourcesStage.setTotalRecords(resources.size());
                logger.debug("Found: [{}] Resources to update:", resources.size());
                resources.forEach(resource -> {
                    try {
                        this.managedResourceManager.update(resource);
                        updateResourcesStage.incrementDoneRecords();
                    } catch (ArangoDaoException | InvalidRequestException | AttributeConstraintViolationException | ScriptRuleException | SchemaNotFoundException | GenericException | ResourceNotFoundException | AttributeNotFoundException ex) {
                        updateResourcesStage.incrementErrors();
                        logger.error("Failed to Update Resource:[{}]", resource.getId(), ex);
                    }

                });
                job.endJobStage(updateResourcesStage);

            } finally {
                logger.debug("Reconciliation of Resources is Done.");
            }
        } catch (Exception ex) {
            //
            // Generic Exception
            //
            logger.error("Generic Exception", ex);
        }

        logger.debug("Starting Circuits Reconciliation");
        /**
         * m-br-rs-csl-cax-gwd-02
         *
         * Vamos reconciliar os circuitos
         */
        try {
            try (GraphList<CircuitResource> circuits = this.circuitManager.findAll(domain)) {

                DbJobStage updateResourcesStage = job.createJobStage("UPDATE_CIRCUITS", domain.getDomainName());
                updateResourcesStage.setJobDescription("Update All Circuits in The Domain, Forcing Cascade Update");
                updateResourcesStage.setTotalRecords(circuits.size());
                if (!circuits.isEmpty()) {
                    circuits.forEach(circuit -> {
                        String uuid = UUID.randomUUID().toString();
                        logger.debug("[{}] Starting Reconciliation Session for:[{}]", uuid, circuit.getId());
                        try {
                            if (!circuit.getCircuitPath().isEmpty()) {
                                FilterDTO filter = new FilterDTO();
                                filter.setDomainName(circuit.getDomain().getDomainName());
                                filter.addBinding("circuitId", circuit.getId());
                                filter.setAqlFilter(" @circuitId in doc.circuits[*]");
                                filter.addObject("connections");

                                try {
                                    GraphList<ResourceConnection> connections = this.resourceConnectionManager.getConnectionsByFilter(filter, circuit.getDomainName());
                                    logger.debug("[{}] Reconciliation Found: [{}/{}] Connections for:[{}]", uuid, connections.size(), circuit.getCircuitPath().size(), circuit.getId());
                                    List<ResourceConnection> dirtyConnections = new ArrayList<>();
                                    try {
                                        connections.forEach(connection -> {
                                            if (!circuit.getCircuitPath().contains(connection.getId())) {
                                                logger.debug("[{}] - Connection:[{}] is not on Circuit:[{}]", uuid, connection.getId(), circuit.getId());
                                                dirtyConnections.add(connection);
                                            }
                                        });

                                        if (!dirtyConnections.isEmpty()) {
                                            logger.debug("[{}] - Dirty Circuit Founds:[{}] in {}", uuid, dirtyConnections.size(), circuit.getId());
                                            //
                                            // Se ficou algum sobrando é inconsistencia para gente remover
                                            //
                                            for (ResourceConnection dirtyConnection : dirtyConnections) {
                                                try {
                                                    ResourceConnection fromDb = this.resourceConnectionManager.findResourceConnection(dirtyConnection);
                                                    if (fromDb.getCircuits().contains(circuit.getId())) {
                                                        logger.debug("[{}] - Removing Dirty Circuit:[{}] From:[{}]", uuid, circuit.getId(), fromDb.getId());
                                                        fromDb.getCircuits().remove(circuit.getId());
                                                        try {
                                                            this.resourceConnectionManager.updateResourceConnection(fromDb);
                                                        } catch (ArangoDaoException | AttributeConstraintViolationException ex) {
                                                            updateResourcesStage.incrementErrors();
                                                        }
                                                    }
                                                } catch (ArangoDaoException | InvalidRequestException ex) {
                                                    updateResourcesStage.incrementErrors();
                                                } catch (ResourceNotFoundException ex) {
                                                    //
                                                    // Neste caso a conexão não foi encontrada no circuito
                                                    //
                                                    circuit.getCircuitPath().remove(dirtyConnection.getId());

                                                }
                                            }
                                            try {
                                                this.circuitManager.updateCircuitResource(circuit);
                                            } catch (SchemaNotFoundException | GenericException | AttributeConstraintViolationException | ScriptRuleException ex) {
                                                logger.error("Failed to Update Circuit", ex);
                                            }
                                        } else {
                                            logger.debug("[{}] - Reconciliation of:[{}] is Done Without Nothing Wrong.", uuid, circuit.getId());
                                        }

                                    } catch (IllegalStateException ex) {
                                        updateResourcesStage.incrementErrors();
                                        logger.error("Failed to Fecth Data on Circuit Reconcialiation", ex);
                                    }
                                } catch (ResourceNotFoundException ex) {
                                    //
                                    // nenhum conexão foi encontrada
                                    //
                                    circuit.getCircuitPath().clear();
                                    try {
                                        this.circuitManager.updateCircuitResource(circuit);
                                    } catch (SchemaNotFoundException | ArangoDaoException | AttributeConstraintViolationException | GenericException | ScriptRuleException ex1) {
                                        //
                                        // Não muito  o que fazer aqui então vou omitir
                                        //
                                    }
                                }

                            }

                        } catch (ArangoDaoException | DomainNotFoundException | InvalidRequestException ex) {
                            updateResourcesStage.incrementErrors();
                            logger.error("Failed to Update Circuit:[{}]", circuit.getId(), ex);
                        }
                    });
                } else {
                    logger.warn("No Circuits Found on domain:[{}]", domain.getDomainName());
                }
                job.endJobStage(updateResourcesStage);

            } finally {
                jobManager.notifyJobEnd(job);
                runningReconcilations.remove(domain.getDomainName());
            }
        } catch (Exception ex) {
            logger.error("Generic Exception 2", ex);
        }

    }
}
