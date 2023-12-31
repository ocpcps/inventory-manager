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
package com.osstelecom.db.inventory.visualization.session;

import com.osstelecom.db.inventory.manager.dao.ResourceConnectionDao;
import com.osstelecom.db.inventory.manager.dto.FilterDTO;
import com.osstelecom.db.inventory.manager.exception.ArangoDaoException;
import com.osstelecom.db.inventory.manager.exception.DomainNotFoundException;
import com.osstelecom.db.inventory.manager.exception.GenericException;
import com.osstelecom.db.inventory.manager.exception.InvalidRequestException;
import com.osstelecom.db.inventory.manager.exception.ResourceNotFoundException;
import com.osstelecom.db.inventory.manager.exception.SchemaNotFoundException;
import com.osstelecom.db.inventory.manager.jobs.DBJobInstance;
import com.osstelecom.db.inventory.manager.operation.DbJobManager;
import com.osstelecom.db.inventory.manager.operation.DomainManager;
import com.osstelecom.db.inventory.manager.request.FilterRequest;
import com.osstelecom.db.inventory.manager.request.FindManagedResourceRequest;
import com.osstelecom.db.inventory.manager.request.GetServiceRequest;
import com.osstelecom.db.inventory.manager.resources.CircuitResource;
import com.osstelecom.db.inventory.manager.resources.Domain;
import com.osstelecom.db.inventory.manager.resources.GraphList;
import com.osstelecom.db.inventory.manager.resources.ManagedResource;
import com.osstelecom.db.inventory.manager.resources.ResourceConnection;
import com.osstelecom.db.inventory.manager.resources.ServiceResource;
import com.osstelecom.db.inventory.manager.resources.model.ResourceSchemaModel;
import com.osstelecom.db.inventory.manager.response.FilterResponse;
import com.osstelecom.db.inventory.manager.response.FindManagedResourceResponse;
import com.osstelecom.db.inventory.manager.response.GetServiceResponse;
import com.osstelecom.db.inventory.manager.session.CircuitSession;
import com.osstelecom.db.inventory.manager.session.GraphSession;
import com.osstelecom.db.inventory.manager.session.ResourceSession;
import com.osstelecom.db.inventory.manager.session.SchemaSession;
import com.osstelecom.db.inventory.manager.session.ServiceSession;
import com.osstelecom.db.inventory.manager.session.UtilSession;
import com.osstelecom.db.inventory.visualization.dto.ThreeJSLinkDTO;
import com.osstelecom.db.inventory.visualization.dto.ThreeJSViewDTO;
import com.osstelecom.db.inventory.visualization.dto.ThreeJsNodeDTO;
import com.osstelecom.db.inventory.visualization.exception.InvalidGraphException;
import com.osstelecom.db.inventory.visualization.request.ExpandNodeRequest;
import com.osstelecom.db.inventory.visualization.request.GetCircuitByConnectionTopologyRequest;
import com.osstelecom.db.inventory.visualization.request.GetConnectionsByCircuitRequest;
import com.osstelecom.db.inventory.visualization.request.GetConnectionsByServiceRequest;
import com.osstelecom.db.inventory.visualization.request.GetDomainTopologyRequest;
import com.osstelecom.db.inventory.visualization.request.GetServiceByConnectionTopologyRequest;
import com.osstelecom.db.inventory.visualization.request.GetStructureTopologyDependencyRequest;
import com.osstelecom.db.inventory.visualization.response.ThreeJsViewResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Classe InventoryManagerApplication
 *
 * <p>
 * Descrição: Classe principal da aplicação Inventory Manager. Esta classe é
 * responsável por inicializar e configurar a aplicação.</p>
 *
 * <p>
 * Configurações:
 * <ul>
 * <li>Exclui as configurações de autoconfiguração do MongoDB
 * (MongoAutoConfiguration e MongoDataAutoConfiguration) para evitar conflitos
 * com o banco de dados da aplicação.</li>
 * <li>Realiza a varredura de componentes no pacote
 * "com.osstelecom.db.inventory".</li>
 * <li>Ativa o agendamento (scheduling) da aplicação.</li>
 * </ul>
 * </p>
 *
 * <p>
 * Fluxo:
 * <ol>
 * <li>O método main é responsável por inicializar a aplicação Inventory
 * Manager.</li>
 * <li>O método onStartup é um event listener que é acionado quando a aplicação
 * está pronta para ser executada.</li>
 * <li>O método onShutDown é um event listener que é acionado quando a aplicação
 * está sendo encerrada.</li>
 * <li>O método disableSslVerification é responsável por desabilitar a validação
 * de certificados autoassinados, garantindo que a aplicação aceite conexões SSL
 * não confiáveis.</li>
 * </ol>
 * </p>
 *
 * <p>
 * Exceptions:
 * <ul>
 * <li>Não há exceções específicas lançadas por esta classe.</li>
 * </ul>
 * </p>
 *
 * @version 1.0
 * @since data de criação ou modificação
 * @see SpringBootApplication
 * @see ComponentScan
 * @see EnableScheduling
 *
 * @author Lucas Nishimura
 * @created 12.01.2023
 */
@Service
public class FilterViewSession {

    @Autowired
    private SchemaSession schemaSession;

    @Autowired
    private ResourceSession resourceSession;

    @Autowired
    private CircuitSession circuitSession;

    @Autowired
    private ServiceSession serviceSession;

    @Autowired
    private GraphSession graphSession;

    @Autowired
    private UtilSession utils;

    @Autowired
    private DomainManager domainManager;

    @Autowired
    private DbJobManager dbJobManager;

    @Autowired
    private ResourceConnectionDao resourceConnectionDao;

    private org.slf4j.Logger logger = LoggerFactory.getLogger(FilterViewSession.class);

    /**
     * @deprecated @param limitSize
     * @return
     * @throws DomainNotFoundException
     * @throws ArangoDaoException
     * @throws InvalidRequestException
     * @throws ResourceNotFoundException
     */
    public ThreeJsViewResponse getSampleResult(Long limitSize)
            throws DomainNotFoundException, ArangoDaoException, InvalidRequestException, ResourceNotFoundException {
        Domain domain = domainManager.getDomain("co");
        FilterRequest request = new FilterRequest(new FilterDTO());
        request.getPayLoad().setAqlFilter(" doc.nodeAddress != 'xyz' ");
        request.getPayLoad().setLimit(limitSize);
        request.getPayLoad().setOffSet(0L);
        request.setRequestDomain(domain.getDomainName());
        request.getPayLoad().getObjects().add("connections");
        FilterResponse filterResponse = resourceSession.findManagedResourceByFilter(request);
        ThreeJsViewResponse response = new ThreeJsViewResponse(new ThreeJSViewDTO(filterResponse));
        return response;
    }

    /**
     * Obtem as estruturas relacionadas a este recurso
     *
     * @param request
     * @return
     * @throws DomainNotFoundException
     * @throws ArangoDaoException
     * @throws ResourceNotFoundException
     * @throws InvalidRequestException
     */
    public ThreeJsViewResponse getResourceStrucureDependency(GetStructureTopologyDependencyRequest request)
            throws DomainNotFoundException, ArangoDaoException, ResourceNotFoundException, InvalidRequestException,
            InvalidGraphException {
        Domain domain = domainManager.getDomain(request.getRequestDomain());
        ThreeJSViewDTO viewData = new ThreeJSViewDTO();
        /**
         * Vamos arrumar o Recurso que veio
         */
        ManagedResource resource = (ManagedResource) request.getPayLoad();

        resource.setDomain(domain);
        logger.debug("Searching Resource: [{}/{}]", resource.getKey(), resource.getDomainName());
        resource = this.resourceSession.findManagedResource(resource);

        /**
         * Encontrei o Resource. Se e ele for parent procuro os filhos se ele
         * for filho, procuro o parent e os filhos
         */
        String parentKey = resource.getKey();
        if (resource.getStructureId() != null && !resource.getStructureId().trim().equals("")) {
            logger.debug("Searching for Parent Element:[{}/{}]", resource.getStructureId(), resource.getDomainName());
            //
            // É um Filho, vamos encontrar seu pai e consequentemente seus irmãos.
            //

            ManagedResource parent = new ManagedResource(domain, resource.getStructureId());
            parent = this.resourceSession.findManagedResource(parent);
            //
            // Encontrou o Pai agora vamos procurar os filhos
            //
            logger.debug("Parent Found, searching for children");
            viewData.getNodes().add(new ThreeJsNodeDTO(parent));

            parentKey = parent.getKey();

        } else {
            viewData.getNodes().add(new ThreeJsNodeDTO(resource));
        }

        String childNodeAqlFilter = " doc.structureId == @structureId";

        Map<String, Object> bindings = new HashMap<>();
        bindings.put("structureId", parentKey);
        FilterDTO filterChildResource = new FilterDTO(childNodeAqlFilter, bindings, domain.getDomainName());
        filterChildResource.getObjects().add("nodes");
        GraphList<ManagedResource> nodes = this.resourceSession.findManagedResourceByFilter(filterChildResource);

        //
        // Ok Encontramos nodes, agora precisamos procurar as conexões...
        //
        List<String> nodeIds = new ArrayList<>();
        try {
            nodes.forEach(node -> {
                nodeIds.add(node.getKey());
                viewData.getNodes().add(new ThreeJsNodeDTO(node));
            });
        } catch (IllegalStateException ex) {
            logger.error("Failed to Fetch Nodes from Stream", ex);
        }

        //
        // Agora vamos recuperar as conexões dos elementos
        //
        String childConnectionsAqlFilter = " doc.fromResource._key "
                + " in @nodeIds "
                + "or  doc.toResource._key  in @nodeIds ";

        bindings.clear();
        bindings.put("nodeIds", nodeIds);
        FilterDTO filterChildConnections = new FilterDTO(childConnectionsAqlFilter, bindings, domain.getDomainName());
        filterChildConnections.getObjects().add("connections");
        GraphList<ResourceConnection> connections = this.resourceSession
                .findResourceConnectionByFilter(filterChildConnections);
        try {
            connections.forEach(connection -> {
                ThreeJSLinkDTO link = new ThreeJSLinkDTO(connection);
                if (!viewData.getLinks().contains(link)) {
                    if (viewData.getNodeMap().containsKey(link.getSource())
                            && viewData.getNodeMap().containsKey(link.getTarget())) {
                        viewData.getLinks().add(link);
                    }
                }
            });
        } catch (IllegalStateException ex) {
            logger.error("Failed to Fetch Connections from Stream", ex);
        }

        viewData.validate(true);
        ThreeJsViewResponse response = new ThreeJsViewResponse(viewData);
        return response;
    }

    /**
     * Obtem a topologia do dominio com base em um filtro
     *
     * @param request
     * @throws IOException
     * @throws IllegalStateException
     */
    public ThreeJsViewResponse getDomainTopologyByFilter(GetDomainTopologyRequest request)
            throws InvalidRequestException,
            ArangoDaoException, DomainNotFoundException, ResourceNotFoundException, InvalidGraphException,
            IllegalStateException {

        logger.debug("Received:");
        logger.debug(utils.toJson(request));
        //
        // Vamos fazer uma persquisa de NODES!, ele vai ser o ponto inicial
        //
        this.domainManager.getDomain(request.getRequestDomain());
        request.getPayLoad().setDomainName(request.getRequestDomain());
        GraphList<ManagedResource> nodes = this.resourceSession.findManagedResourceByFilter(request.getPayLoad());

        ThreeJSViewDTO view = new ThreeJSViewDTO();
        nodes.forEach(m -> {
            ResourceSchemaModel schemaModel;
            try {
                schemaModel = schemaSession.loadSchema(m.getAttributeSchemaName());
                m.setSchemaModel(schemaModel);
            } catch (SchemaNotFoundException | GenericException e) {
                logger.error(e.getMessage());
            }
            ThreeJsNodeDTO node = new ThreeJsNodeDTO(m);
            view.addNode(node);
        });

        if (view.getNodeCount() > 0) {
            FilterDTO connectionsFilter = new FilterDTO();
            connectionsFilter.setDomainName(request.getRequestDomain());
            String connectionsAQLFilter = " doc.fromResource._key "
                    + " in @nodeIds "
                    + "or  doc.toResource._key  in @nodeIds ";

            connectionsFilter.addObject("connection");
            connectionsFilter.setAqlFilter(connectionsAQLFilter);
            connectionsFilter.addBinding("nodeIds", view.getNodeIds());

            GraphList<ResourceConnection> connections = this.resourceSession
                    .findResourceConnectionByFilter(connectionsFilter);

            //
            // Set link of the visible nodes
            //
            view.setLinksByGraph(connections);
        }

        //
        // Check if the Graph is consistent
        //
        view.validate(true);

        return new ThreeJsViewResponse(view);
    }

    /**
     * Expand um nó no mapa
     *
     * @param request
     * @return
     * @throws DomainNotFoundException
     * @throws ArangoDaoException
     * @throws ResourceNotFoundException
     * @throws InvalidRequestException
     * @throws InvalidGraphException
     */
    public ThreeJsViewResponse expandNodeById(ExpandNodeRequest request) throws DomainNotFoundException,
            ArangoDaoException, ResourceNotFoundException, InvalidRequestException, InvalidGraphException {
        Domain domain = this.domainManager.getDomain(request.getRequestDomain());
        DBJobInstance job = dbJobManager.createJobInstance("Expand Node:");
        dbJobManager.notifyJobStart(job);
        ManagedResource resource = new ManagedResource(domain, request.getPayLoad().getNodeId());
        //
        // Recupera a referencia do resource do DB
        //
        resource = this.resourceSession.findManagedResource(resource);

        GraphList<ResourceConnection> result = this.graphSession.expandNode(resource, request.getPayLoad().getDirection(), request.getPayLoad().getDepth());
        ThreeJsViewResponse response = new ThreeJsViewResponse(new ThreeJSViewDTO(result).validate());
        dbJobManager.notifyJobEnd(job);
        return response;
    }

    /**
     * Dado uma conexão procura os circuitos que dependem dela
     *
     * @param request
     * @return
     * @throws DomainNotFoundException
     * @throws ArangoDaoException
     * @throws ResourceNotFoundException
     * @throws InvalidRequestException
     * @throws InvalidGraphException
     */
    public ThreeJsViewResponse getCircuitsByConnectionId(GetCircuitByConnectionTopologyRequest request)
            throws DomainNotFoundException, ArangoDaoException, ResourceNotFoundException, InvalidRequestException,
            InvalidGraphException {
        ThreeJSViewDTO view = new ThreeJSViewDTO();
        Domain domain = domainManager.getDomain(request.getRequestDomain());
        ResourceConnection connection = request.getPayLoad();
        connection.setDomain(domain);
        connection = this.resourceSession.findResourceConnection(connection);
        //
        // OK A conexão Existe, agora vamos procurar os Circuitos...
        //

        if (!connection.getCircuits().isEmpty()) {
            logger.debug("Found:[{}] Circuits for Connection ID:[{}]", connection.getCircuits().size(),
                    connection.getKey());
            FilterDTO filter = new FilterDTO();
            filter.setDomainName(domain.getDomainName());
            filter.addBinding("circuitIds", connection.getCircuits());
            filter.setAqlFilter("doc._id in @circuitIds");
            filter.addObject("circuit");

            GraphList<CircuitResource> circuitsFound = this.circuitSession.findCircuitResourceByFilter(filter);

            if (!circuitsFound.isEmpty()) {
                view.setCircuitGraph(circuitsFound);
            }
        }
        //
        // Valida se o Grafo enviado é válido
        //

        view.validate();
        return new ThreeJsViewResponse(view);
    }

    /**
     * Dado uma conexão procura os servicos que dependem dela
     *
     * @param request
     * @return
     * @throws DomainNotFoundException
     * @throws ArangoDaoException
     * @throws ResourceNotFoundException
     * @throws InvalidRequestException
     * @throws InvalidGraphException
     */
    public List<ServiceResource> getServicesByConnectionId(ResourceConnection connection, Domain domain)
            throws DomainNotFoundException, ArangoDaoException, ResourceNotFoundException, InvalidRequestException {

        List<ServiceResource> lista = new ArrayList<>();
        connection.setDomain(domain);
        connection = this.resourceSession.findResourceConnection(connection);
        //
        // OK A conexão Existe, agora vamos procurar os Circuitos...
        //

        if (!connection.getCircuits().isEmpty()) {
            logger.debug("Found:[{}] Circuits for Connection ID:[{}]", connection.getCircuits().size(),
                    connection.getKey());
            FilterDTO filter = new FilterDTO();
            filter.setDomainName(domain.getDomainName());
            filter.addBinding("circuitIds", connection.getCircuits());
            filter.setAqlFilter("doc._id in @circuitIds");
            filter.addObject("circuit");

            GraphList<CircuitResource> circuitsFound = this.circuitSession.findCircuitResourceByFilter(filter);

            for (CircuitResource circuito : circuitsFound.toList()) {
                for (String serviceId : circuito.getServices()) {
                    ServiceResource serviceResource = new ServiceResource();
                    serviceResource.setId(serviceId.split("/")[1]);
                    serviceResource.setDomain(domain);
                    GetServiceRequest serviceRequest = new GetServiceRequest();
                    serviceRequest.setPayLoad(serviceResource);
                    serviceRequest.setRequestDomain(circuito.getDomainName());
                    lista.add(serviceSession.getServiceById(serviceRequest).getPayLoad());
                }
            }
        }
        return lista;

    }

    public ThreeJsViewResponse getGraphServicesByConnection(GetServiceByConnectionTopologyRequest request)
            throws DomainNotFoundException, ArangoDaoException, ResourceNotFoundException, InvalidRequestException,
            InvalidGraphException {
        Domain domain = domainManager.getDomain(request.getRequestDomain());
        ResourceConnection connection = request.getPayLoad();
        List<ServiceResource> servicesFound = getServicesByConnectionId(connection, domain);
        ThreeJSViewDTO view = new ThreeJSViewDTO();
        if (!servicesFound.isEmpty()) {
            view.setServices(servicesFound);
        }
        view.validate();
        return new ThreeJsViewResponse(view);
    }

    public ThreeJsViewResponse getGraphNodesByService(GetServiceRequest request) throws DomainNotFoundException,
            ArangoDaoException, ResourceNotFoundException, InvalidRequestException, InvalidGraphException {
        if (request.getPayLoad().getId() == null) {
            throw new InvalidRequestException("ID Field Missing");
        }

        if (request.getRequestDomain() == null) {
            throw new DomainNotFoundException("Domain With Name:[" + request.getRequestDomain() + "] not found");
        }
        GetServiceResponse serviceId = serviceSession.getServiceById(request);
        List<ThreeJsNodeDTO> listaNodes = new ArrayList<ThreeJsNodeDTO>();
        List<ThreeJSLinkDTO> listaLinks = new ArrayList<ThreeJSLinkDTO>();
        List<CircuitResource> circuitsFound = serviceId.getPayLoad().getCircuits();

        for (CircuitResource circuito : circuitsFound) {
            GetConnectionsByCircuitRequest circuitRequest = new GetConnectionsByCircuitRequest();

            circuitRequest.setPayLoad(circuito);
            circuitRequest.setRequestDomain(circuito.getDomainName());
            ThreeJSViewDTO payload = getConnectionsByCircuit(circuitRequest).getPayLoad();
            listaLinks.addAll(payload.getLinks());
            listaNodes.addAll(payload.getNodes());
        }

        ThreeJSViewDTO view = new ThreeJSViewDTO();
        view.setLinks(listaLinks);
        view.setNodes(listaNodes);
        view.validate();
        return new ThreeJsViewResponse(view);
    }

    public ThreeJsViewResponse getServiceByResource(FindManagedResourceRequest findRequest)
            throws InvalidRequestException, DomainNotFoundException, ResourceNotFoundException, ArangoDaoException,
            IllegalStateException, IOException, InvalidGraphException {
        FindManagedResourceResponse response = resourceSession.findManagedResourceById(findRequest);
        Domain domain = domainManager.getDomain(findRequest.getRequestDomain());

        List<ServiceResource> servicesFound = new ArrayList<>();
        String filter = "@resourceId in  doc.relatedNodes[*]";
        Map<String, Object> bindVars = new HashMap<>();
        ThreeJSViewDTO view = new ThreeJSViewDTO();

        bindVars.put("resourceId", response.getPayLoad().getId());
        //
        // Procura as conexões relacionadas no mesmo dominio
        //

        this.resourceConnectionDao
                .findResourceByFilter(new FilterDTO(filter, bindVars), response.getPayLoad().getDomain())
                .forEach((connection) -> {
                    try {
                        servicesFound.addAll(getServicesByConnectionId(connection, domain));
                    } catch (DomainNotFoundException | ArangoDaoException | ResourceNotFoundException
                            | InvalidRequestException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                });

        if (!servicesFound.isEmpty()) {
            view.setServices(servicesFound);
        }
        view.validate();
        return new ThreeJsViewResponse(view);

    }

    /**
     * Expande um circuito trazendo TODA sua topologia
     *
     * @param request
     * @return
     * @throws DomainNotFoundException
     * @throws ArangoDaoExceptionCircuitResource
     * @throws ResourceNotFoundException
     * @throws InvalidRequestException
     * @throws InvalidGraphException
     */
    public ThreeJsViewResponse getConnectionsByCircuit(GetConnectionsByCircuitRequest request)
            throws DomainNotFoundException, ArangoDaoException, ResourceNotFoundException, InvalidRequestException,
            InvalidGraphException {
        ThreeJSViewDTO view = new ThreeJSViewDTO();
        CircuitResource circuit = request.getPayLoad();
        Domain domain = domainManager.getDomain(request.getRequestDomain());
        circuit.setDomain(domain);
        //
        // Vamos procurar a Referencia do Circuito
        //
        circuit = this.circuitSession.findCircuitResource(circuit);

        //
        // Ok, se chegamos aqui encontramos o circuito, agora vamos procurar as conexões
        //
        GraphList<ResourceConnection> connections = this.resourceSession.findResourceConnectionByCircuit(circuit);

        view.setLinksByGraph(connections, true);

        //
        // Chegamos ao ponto de ter todas as conexões agora vamos montar o response
        //
        view.validate();
        return new ThreeJsViewResponse(view);
    }

    /**
     * Expande um circuito trazendo TODA sua topologia
     *
     * @param request
     * @return
     * @throws DomainNotFoundException
     * @throws ArangoDaoException
     * @throws ResourceNotFoundException
     * @throws InvalidRequestException
     * @throws InvalidGraphException
     */
    public ThreeJsViewResponse getConnectionsByService(GetConnectionsByServiceRequest request)
            throws DomainNotFoundException, ArangoDaoException, ResourceNotFoundException, InvalidRequestException,
            InvalidGraphException {
        ThreeJSViewDTO view = new ThreeJSViewDTO();
        ServiceResource service = request.getPayLoad();
        Domain domain = domainManager.getDomain(request.getRequestDomain());
        service.setDomain(domain);
        //
        // Vamos procurar a Referencia do service
        //
        service = this.serviceSession.findServiceResource(service);

        //
        // Ok, se chegamos aqui encontramos o circuito, agora vamos procurar as conexões
        //
        GraphList<ResourceConnection> connections = this.resourceSession.findResourceConnectionByService(service);

        view.setLinksByGraph(connections, true);

        //
        // Chegamos ao ponto de ter todas as conexões agora vamos montar o response
        //
        view.validate();
        return new ThreeJsViewResponse(view);
    }
}
