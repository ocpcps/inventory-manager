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

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.arangodb.entity.DocumentUpdateEntity;
import com.osstelecom.db.inventory.manager.dto.BatchAttributeUpdateDTO;
import com.osstelecom.db.inventory.manager.dto.FilterDTO;
import com.osstelecom.db.inventory.manager.exception.ArangoDaoException;
import com.osstelecom.db.inventory.manager.exception.AttributeNotFoundException;
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
import com.osstelecom.db.inventory.manager.operation.ResourceLocationManager;
import com.osstelecom.db.inventory.manager.operation.ServiceManager;
import com.osstelecom.db.inventory.manager.request.CreateConnectionRequest;
import com.osstelecom.db.inventory.manager.request.CreateManagedResourceRequest;
import com.osstelecom.db.inventory.manager.request.DeleteManagedResourceRequest;
import com.osstelecom.db.inventory.manager.request.DeleteResourceConnectionRequest;
import com.osstelecom.db.inventory.manager.request.FilterRequest;
import com.osstelecom.db.inventory.manager.request.FindManagedResourceRequest;
import com.osstelecom.db.inventory.manager.request.FindResourceConnectionRequest;
import com.osstelecom.db.inventory.manager.request.ListManagedResourceRequest;
import com.osstelecom.db.inventory.manager.request.ListResourceConnectionRequest;
import com.osstelecom.db.inventory.manager.request.PatchManagedResourceRequest;
import com.osstelecom.db.inventory.manager.request.PatchResourceConnectionRequest;
import com.osstelecom.db.inventory.manager.resources.CircuitResource;
import com.osstelecom.db.inventory.manager.resources.Domain;
import com.osstelecom.db.inventory.manager.resources.GraphList;
import com.osstelecom.db.inventory.manager.resources.ManagedResource;
import com.osstelecom.db.inventory.manager.resources.ResourceConnection;
import com.osstelecom.db.inventory.manager.resources.ServiceResource;
import com.osstelecom.db.inventory.manager.resources.exception.AttributeConstraintViolationException;
import com.osstelecom.db.inventory.manager.resources.exception.ConnectionAlreadyExistsException;
import com.osstelecom.db.inventory.manager.resources.exception.MetricConstraintException;
import com.osstelecom.db.inventory.manager.resources.exception.NoResourcesAvailableException;
import com.osstelecom.db.inventory.manager.response.CreateManagedResourceResponse;
import com.osstelecom.db.inventory.manager.response.CreateResourceConnectionResponse;
import com.osstelecom.db.inventory.manager.response.DeleteManagedResourceResponse;
import com.osstelecom.db.inventory.manager.response.DeleteResourceConnectionResponse;
import com.osstelecom.db.inventory.manager.response.FilterResponse;
import com.osstelecom.db.inventory.manager.response.FindManagedResourceResponse;
import com.osstelecom.db.inventory.manager.response.FindResourceConnectionResponse;
import com.osstelecom.db.inventory.manager.response.PatchManagedResourceResponse;
import com.osstelecom.db.inventory.manager.response.PatchResourceConnectionResponse;
import com.osstelecom.db.inventory.manager.response.TypedListResponse;

import com.osstelecom.db.inventory.manager.request.UpdateBatchAttributeRequest;
import com.osstelecom.db.inventory.manager.response.UpdateBatchAttributeResponse;
import java.io.IOException;

/**
 * Classe ResourceSession - Netcompass
 *
 * Descrição: A classe ResourceSession é responsável por gerenciar sessões de
 * recursos (Resource) no sistema Netcompass. Ela fornece métodos para criar,
 * buscar, atualizar e remover recursos, bem como operações em lote para
 * recursos gerenciados (ManagedResource) e conexões de recursos
 * (ResourceConnection) no sistema.
 *
 * Fluxo: 1. A classe possui métodos para criar e atualizar recursos gerenciados
 * (ManagedResource) e conexões de recursos (ResourceConnection) no sistema
 * Netcompass. 2. Métodos de busca permitem recuperar recursos com base em
 * filtros e condições específicas, como o domínio (Domain) a que pertencem. 3.
 * Métodos para remover recursos podem ser utilizados para excluir recursos do
 * sistema. 4. A classe ResourceSession também suporta operações em lote para
 * atualizar atributos em vários recursos gerenciados e conexões de recursos de
 * uma só vez no sistema Netcompass.
 *
 * Atributos: - managedResourceManager: Gerenciador de recursos gerenciados que
 * fornece operações CRUD para os recursos gerenciados no sistema Netcompass. -
 * resourceConnectionManager: Gerenciador de conexões de recursos que fornece
 * operações CRUD para as conexões de recursos no sistema Netcompass. -
 * domainManager: Gerenciador de domínios (Domain) que fornece operações para
 * manipulação de domínios no sistema Netcompass. - serviceManager: Gerenciador
 * de serviços (ServiceResource) que fornece operações para manipulação de
 * serviços no sistema Netcompass.
 *
 * Uso: 1. Para criar um novo recurso gerenciado no sistema Netcompass, utilize
 * o método createManagedResource da classe ManagedResourceManager. 2. Para
 * atualizar os atributos de um recurso gerenciado no sistema Netcompass,
 * utilize o método updateManagedResource da classe ManagedResourceManager. 3.
 * Para buscar recursos gerenciados com base em filtros no sistema Netcompass,
 * utilize o método getManagedResourcesByFilter da classe
 * ManagedResourceManager. 4. Para criar uma nova conexão de recurso no sistema
 * Netcompass, utilize o método createResourceConnection da classe
 * ResourceConnectionManager. 5. Para atualizar os atributos de uma conexão de
 * recurso no sistema Netcompass, utilize o método updateResourceConnection da
 * classe ResourceConnectionManager. 6. Para buscar conexões de recursos com
 * base em filtros no sistema Netcompass, utilize o método
 * getResourceConnectionsByFilter da classe ResourceConnectionManager. 7.
 * Utilize os métodos fornecidos pelo DomainManager para manipular domínios no
 * sistema Netcompass, como criar, buscar, atualizar e remover domínios. 8.
 * Utilize os métodos fornecidos pelo ServiceManager para manipular serviços no
 * sistema Netcompass, como criar, buscar, atualizar e remover serviços.
 *
 * Observações: - As classes ManagedResourceManager, ResourceConnectionManager,
 * DomainManager e ServiceManager devem ser inicializadas e passadas como
 * parâmetros para o construtor da classe ResourceSession para que ela possa
 * funcionar corretamente no sistema Netcompass.
 *
 * @see ManagedResourceManager
 * @see ResourceConnectionManager
 * @see DomainManager
 * @see ServiceManager
 *
 * @author Lucas Nishimura
 * @created 15.12.2021
 */
@Service
public class ResourceSession {

    @Autowired
    private DomainManager domainManager;

    @Autowired
    private ManagedResourceManager manager;

    @Autowired
    private ResourceLocationManager resourceLocationManager;

    @Autowired
    private ResourceConnectionManager resourceConnectionManager;

    @Autowired
    private ManagedResourceManager managedResourceManager;

    @Autowired
    private CircuitResourceManager circuitManager;

    @Autowired
    private ServiceManager serviceManager;

    @Autowired
    private UtilSession utils;

    @Autowired
    private FilterProjectionSession filterProjectionSession;

    private Logger logger = LoggerFactory.getLogger(CircuitSession.class);

    /**
     * Cria uma nova conexão de recurso com base na solicitação fornecida.
     *
     * <p>
     * Este método suporta a criação de uma conexão de três maneiras:
     * <ul>
     * <li>Usando 'FromId' e 'ToId' do payload da solicitação.</li>
     * <li>Usando 'FromKey' e 'ToKey' do payload da solicitação.</li>
     * <li>Usando 'FromNodeAddress' e 'ToNodeAddress' do payload da
     * solicitação.</li>
     * </ul>
     * </p>
     *
     * <p>
     * Se nenhuma das condições acima for atendida, uma
     * {@link InvalidRequestException} é lançada.
     * </p>
     *
     * @param request A solicitação contendo as informações necessárias para
     *                criar a conexão de recurso.
     * @return Uma resposta contendo a conexão de recurso criada.
     * @throws ResourceNotFoundException             Se o recurso não for
     *                                               encontrado.
     * @throws ConnectionAlreadyExistsException      Se a conexão já existir.
     * @throws MetricConstraintException             Se houver uma violação de
     *                                               restrição
     *                                               métrica.
     * @throws NoResourcesAvailableException         Se não houver recursos
     *                                               disponíveis.
     * @throws GenericException                      Para exceções genéricas.
     * @throws SchemaNotFoundException               Se o esquema não for
     *                                               encontrado.
     * @throws AttributeConstraintViolationException Se houver uma violação de
     *                                               restrição de atributo.
     * @throws ScriptRuleException                   Se houver uma exceção de regra
     *                                               de script.
     * @throws InvalidRequestException               Se a solicitação for inválida.
     * @throws DomainNotFoundException               Se o domínio não for
     *                                               encontrado.
     * @throws ArangoDaoException                    Para exceções relacionadas ao
     *                                               ArangoDB.
     *
     * @since 08-08-2022: Prioriza os iDS aos Nomes
     */
    public CreateResourceConnectionResponse createResourceConnection(CreateConnectionRequest request)
            throws ResourceNotFoundException, ConnectionAlreadyExistsException, MetricConstraintException,
            NoResourcesAvailableException, GenericException, SchemaNotFoundException,
            AttributeConstraintViolationException, ScriptRuleException, InvalidRequestException,
            DomainNotFoundException, ArangoDaoException {

        //
        // @since 08-08-2022: Prioriza os iDS aos Nomes
        //
        if (request.getPayLoad().getFromId() != null && request.getPayLoad().getToId() != null) {
            ResourceConnection connection = new ResourceConnection(domainManager.getDomain(request.getRequestDomain()));
            connection.setName(request.getPayLoad().getConnectionName());
            connection.setClassName(request.getPayLoad().getConnectionClass());
            //
            // Temos dois IDs podemos proesseguir com a validação por aqui
            //
            FindManagedResourceRequest fromResourceRequest = new FindManagedResourceRequest(
                    request.getPayLoad().getFromId(), request.getRequestDomain());
            Domain fromDomain = this.domainManager.getDomain(fromResourceRequest.getRequestDomain());

            fromResourceRequest.setRequestDomain(request.getRequestDomain());
            ManagedResource fromResource = manager
                    .findManagedResource(new ManagedResource(fromDomain, fromResourceRequest.getResourceId()));

            FindManagedResourceRequest toResourceRequest = new FindManagedResourceRequest(
                    request.getPayLoad().getToId(), request.getRequestDomain());
            Domain toDomain = this.domainManager.getDomain(toResourceRequest.getRequestDomain());
            toResourceRequest.setRequestDomain(request.getRequestDomain());
            ManagedResource toResource = manager
                    .findManagedResource(new ManagedResource(toDomain, toResourceRequest.getResourceId()));

            connection.setFrom(fromResource);
            connection.setTo(toResource);
            if (request.getPayLoad().getNodeAddress() != null) {
                connection.setNodeAddress(request.getPayLoad().getNodeAddress());
            } else {
                connection.setNodeAddress(
                        request.getPayLoad().getFromNodeAddress() + "." + request.getPayLoad().getToNodeAddress());
            }
            connection.setOperationalStatus(request.getPayLoad().getOperationalStatus());
            connection.setAttributeSchemaName(request.getPayLoad().getAttributeSchemaName());
            connection.setDomain(domainManager.getDomain(request.getRequestDomain()));
            connection.setAttributes(request.getPayLoad().getAttributes());
            connection.setPropagateOperStatus(request.getPayLoad().getPropagateOperStatus());
            connection.setName(request.getPayLoad().getConnectionName());
            connection.setDescription(request.getPayLoad().getDescription());
            connection.setOwner(request.getUserId());
            connection.setAdminStatus(request.getPayLoad().getAdminStatus());
            connection.setBusinessStatus(request.getPayLoad().getBusinessStatus());
            connection.setCategory(request.getPayLoad().getCategory());

            if (request.getPayLoad().getKey() != null) {
                //
                // Manda um upsert
                //
                connection.setKey(request.getPayLoad().getKey());
            } else {
                connection.setInsertedDate(new Date());
            }

            /**
             * Sanitização do Oper Status
             */
            if (connection.getOperationalStatus() == null) {
                connection.setOperationalStatus("Up");
            } else {
                if (connection.getOperationalStatus().trim().equals("")) {
                    connection.setOperationalStatus("Up");
                }
            }

            if (connection.getAdminStatus() == null) {
                connection.setAdminStatus("Up");
            } else {
                if (connection.getAdminStatus().trim().equals("")) {
                    connection.setAdminStatus("Up");
                }
            }

            connection = resourceConnectionManager.createResourceConnection(connection);
            CreateResourceConnectionResponse response = new CreateResourceConnectionResponse(connection);
            return response;

        } else if (request.getPayLoad().getFromKey() != null && request.getPayLoad().getToKey() != null) {
            ResourceConnection connection = new ResourceConnection(domainManager.getDomain(request.getRequestDomain()));
            connection.setName(request.getPayLoad().getConnectionName());
            connection.setClassName(request.getPayLoad().getConnectionClass());
            //
            // Temos dois IDs podemos proesseguir com a validação por aqui
            //
            Domain domain = this.domainManager.getDomain(request.getRequestDomain());
            ManagedResource fromResource = new ManagedResource(domain);
            fromResource.setKey(request.getPayLoad().getFromKey());
            //
            // Como temos a KEY, vamos ignorar o attributeSchemaName
            //
            fromResource.setAttributeSchemaName(null);
            fromResource = manager.findManagedResource(fromResource);

            ManagedResource toResource = new ManagedResource(domain);
            toResource.setKey(request.getPayLoad().getToKey());
            //
            // Como temos a KEY, vamos ignorar o attributeSchemaName
            //
            toResource.setAttributeSchemaName(null);
            toResource = manager.findManagedResource(toResource);

            connection.setFrom(fromResource);
            connection.setTo(toResource);
            if (request.getPayLoad().getNodeAddress() != null) {
                connection.setNodeAddress(request.getPayLoad().getNodeAddress());
            } else {
                connection.setNodeAddress(
                        request.getPayLoad().getFromNodeAddress() + "." + request.getPayLoad().getToNodeAddress());
            }
            connection.setOperationalStatus(request.getPayLoad().getOperationalStatus());
            connection.setAttributeSchemaName(request.getPayLoad().getAttributeSchemaName());
            connection.setDomain(domainManager.getDomain(request.getRequestDomain()));
            connection.setAttributes(request.getPayLoad().getAttributes());
            connection.setPropagateOperStatus(request.getPayLoad().getPropagateOperStatus());
            connection.setName(request.getPayLoad().getConnectionName());
            connection.setDescription(request.getPayLoad().getDescription());
            connection.setOwner(request.getUserId());
            connection.setAdminStatus(request.getPayLoad().getAdminStatus());
            connection.setBusinessStatus(request.getPayLoad().getBusinessStatus());
            connection.setCategory(request.getPayLoad().getCategory());

            /**
             * Sanitização do Oper Status
             */
            if (connection.getOperationalStatus() == null) {
                connection.setOperationalStatus("Up");
            } else {
                if (connection.getOperationalStatus().trim().equals("")) {
                    connection.setOperationalStatus("Up");
                }
            }

            if (connection.getAdminStatus() == null) {
                connection.setAdminStatus("Up");
            } else {
                if (connection.getAdminStatus().trim().equals("")) {
                    connection.setAdminStatus("Up");
                }
            }

            if (request.getPayLoad().getKey() != null) {
                //
                // Manda um upsert
                //
                connection.setKey(request.getPayLoad().getKey());
            } else {
                connection.setInsertedDate(new Date());
            }

            connection = resourceConnectionManager.createResourceConnection(connection);
            CreateResourceConnectionResponse response = new CreateResourceConnectionResponse(connection);
            return response;

        } else if (request.getPayLoad().getFromNodeAddress() != null
                && request.getPayLoad().getToNodeAddress() != null) {
            //
            // Vou fazer de forma preguiçosa, eu tô resfriado e doente essa semana, não me
            // julguem
            //
            ResourceConnection connection = new ResourceConnection(domainManager.getDomain(request.getRequestDomain()));
            connection.setName(request.getPayLoad().getConnectionName());
            connection.setClassName(request.getPayLoad().getConnectionClass());
            //
            // Temos dois IDs podemos proesseguir com a validação por aqui
            //
            Domain domain = this.domainManager.getDomain(request.getRequestDomain());
            ManagedResource fromResource = new ManagedResource(domain);
            fromResource.setNodeAddress(request.getPayLoad().getFromNodeAddress());
            fromResource.setClassName(request.getPayLoad().getFromClassName());
            fromResource.setAttributeSchemaName(null);
            fromResource = this.manager.findManagedResource(fromResource);

            ManagedResource toResource = new ManagedResource(domain);
            toResource.setNodeAddress(request.getPayLoad().getToNodeAddress());
            toResource.setClassName(request.getPayLoad().getToClassName());

            toResource.setAttributeSchemaName(null);
            toResource = manager.findManagedResource(toResource);
            connection.setFrom(fromResource);
            connection.setTo(toResource);
            if (request.getPayLoad().getNodeAddress() != null) {
                connection.setNodeAddress(request.getPayLoad().getNodeAddress());
            } else {
                connection.setNodeAddress(
                        request.getPayLoad().getFromNodeAddress() + "." + request.getPayLoad().getToNodeAddress());
            }
            connection.setOperationalStatus(request.getPayLoad().getOperationalStatus());
            connection.setAttributeSchemaName(request.getPayLoad().getAttributeSchemaName());
            connection.setDomain(domainManager.getDomain(request.getRequestDomain()));
            connection.setAttributes(request.getPayLoad().getAttributes());
            connection.setPropagateOperStatus(request.getPayLoad().getPropagateOperStatus());
            connection.setName(request.getPayLoad().getConnectionName());
            connection.setDescription(request.getPayLoad().getDescription());
            connection.setOwner(request.getUserId());
            connection.setAdminStatus(request.getPayLoad().getAdminStatus());
            connection.setBusinessStatus(request.getPayLoad().getBusinessStatus());
            connection.setCategory(request.getPayLoad().getCategory());

            /**
             * Sanitização do Oper Status
             */
            if (connection.getOperationalStatus() == null) {
                connection.setOperationalStatus("Up");
            } else {
                if (connection.getOperationalStatus().trim().equals("")) {
                    connection.setOperationalStatus("Up");
                }
            }

            if (connection.getAdminStatus() == null) {
                connection.setAdminStatus("Up");
            } else {
                if (connection.getAdminStatus().trim().equals("")) {
                    connection.setAdminStatus("Up");
                }
            }

            if (request.getPayLoad().getKey() != null) {
                //
                // Manda um upsert
                //
                connection.setKey(request.getPayLoad().getKey());
            } else {
                connection.setInsertedDate(new Date());
            }

            connection = resourceConnectionManager.createResourceConnection(connection);
            CreateResourceConnectionResponse response = new CreateResourceConnectionResponse(connection);
            return response;

        } else {
            throw new InvalidRequestException("Cannot Create Connection");
        }
    }

    /**
     * Deleta uma conexão de recurso com base na solicitação fornecida.
     *
     * <p>
     * Antes de deletar a conexão, o método verifica:
     * <ul>
     * <li>Se o ID do recurso foi fornecido.</li>
     * <li>Se o nome do domínio do recurso foi fornecido.</li>
     * </ul>
     * </p>
     *
     * <p>
     * Após a validação inicial, o método busca a conexão de recurso para
     * garantir que ela exista. Em seguida, verifica se há algum circuito que
     * dependa dessa conexão. Se houver dependências, a conexão não pode ser
     * deletada e uma exceção é lançada.
     * </p>
     *
     * @param request A solicitação contendo as informações necessárias para
     *                deletar a conexão de recurso.
     * @return Uma resposta contendo detalhes da conexão de recurso deletada.
     * @throws InvalidRequestException   Se a solicitação for inválida ou se
     *                                   houver dependências que impedem a deleção.
     * @throws DomainNotFoundException   Se o domínio não for encontrado.
     * @throws ArangoDaoException        Para exceções relacionadas ao ArangoDB.
     * @throws ResourceNotFoundException Se o recurso ou a conexão não for
     *                                   encontrado.
     */
    public DeleteResourceConnectionResponse deleteResourceConnection(DeleteResourceConnectionRequest request)
            throws InvalidRequestException, DomainNotFoundException, ArangoDaoException, ResourceNotFoundException {

        if (request.getResourceId() == null) {
            throw new InvalidRequestException("Please Provide Resource ID to delete");
        } else if (request.getRequestDomain() == null) {
            throw new InvalidRequestException("Please Provide Domain Name of Resource to  delete");
        }

        Domain domain = this.domainManager.getDomain(request.getRequestDomain());
        //
        // Precisa Enconrar A Connection.
        //
        ResourceConnection connection = this.resourceConnectionManager
                .findResourceConnection(new ResourceConnection(domain, request.getResourceId()));

        //
        // Uma vez que achamos a conecction, a gente precisa ver se algum circuito usa
        // ela, pois se usar ela não pode ser removida!
        //
        Map<String, Object> bindings = new HashMap<>();
        bindings.put("connectionId", connection.getId());
        FilterDTO connectionFilter = new FilterDTO();
        connectionFilter.setAqlFilter("@connectionId in doc.circuitPath[*] ");
        connectionFilter.getObjects().add("circuits");
        connectionFilter.setBindings(bindings);

        try {

            GraphList<CircuitResource> circuits = circuitManager.findCircuitsByFilter(connectionFilter, domain);

            //
            // Se chegou aqui é porque tem conexões que dependem do recurso, não podemos
            // deletar
            //
            throw new InvalidRequestException(("Resource ID is Used By:[" + circuits.size()
                    + "] Connections, please remove theses dependencies, before delete"));
        } catch (ResourceNotFoundException ex) {

            connection = this.resourceConnectionManager.deleteResourceConnection(connection);
            DeleteResourceConnectionResponse response = new DeleteResourceConnectionResponse(connection);
            return response;
        }
    }

    /**
     * Deleta um recurso gerenciado com base na solicitação fornecida.
     *
     * <p>
     * Antes de deletar o recurso, o método verifica:
     * <ul>
     * <li>Se o ID do recurso foi fornecido.</li>
     * <li>Se o nome do domínio do recurso foi fornecido.</li>
     * </ul>
     * </p>
     *
     * <p>
     * Após a validação inicial, o método busca o recurso gerenciado para
     * garantir que ele exista. Em seguida, verifica se há alguma conexão ou
     * circuito que dependa desse recurso. Se houver dependências, o recurso não
     * pode ser deletado e uma exceção é lançada.
     * </p>
     *
     * @param request A solicitação contendo as informações necessárias para
     *                deletar o recurso gerenciado.
     * @return Uma resposta contendo detalhes do recurso gerenciado deletado.
     * @throws InvalidRequestException   Se a solicitação for inválida ou se
     *                                   houver dependências que impedem a deleção.
     * @throws DomainNotFoundException   Se o domínio não for encontrado.
     * @throws ArangoDaoException        Para exceções relacionadas ao ArangoDB.
     * @throws ResourceNotFoundException Se o recurso gerenciado ou alguma
     *                                   dependência (conexão ou circuito) não for
     *                                   encontrada.
     */
    public DeleteManagedResourceResponse deleteManagedResource(DeleteManagedResourceRequest request)
            throws InvalidRequestException, DomainNotFoundException, ArangoDaoException, ResourceNotFoundException {
        if (request.getResourceId() == null) {
            throw new InvalidRequestException("Please Provide Resource ID to delete");
        } else if (request.getRequestDomain() == null) {
            throw new InvalidRequestException("Please Provide Domain Name of Resource to  delete");
        }

        Domain domain = this.domainManager.getDomain(request.getRequestDomain());
        //
        // Precisa Enconrar o Recurso.
        //
        ManagedResource resource = this.managedResourceManager
                .findManagedResourceById(new ManagedResource(domain, request.getResourceId()));

        //
        // O Problema do delete de um recurso é que ele pode ser necessário para alguma
        // conexão e consequentemente um circuito...
        // Então vamos ver se esse recurso é necessário para alguma conexão deste
        // dominio
        //
        Map<String, Object> bindings = new HashMap<>();
        bindings.put("resourceId", resource.getId());
        FilterDTO connectionFilter = new FilterDTO();
        connectionFilter.setAqlFilter("doc.fromResource._id == @resourceId or doc.toResource._id == @resourceId ");
        connectionFilter.getObjects().add("connections");
        connectionFilter.setBindings(bindings);
        try {
            GraphList<ResourceConnection> connections = resourceConnectionManager
                    .getConnectionsByFilter(connectionFilter, domain.getDomainName());
            //
            // Se chegou aqui é porque tem conexões que dependem do recurso, não podemos
            // deletar
            //
            throw new InvalidRequestException(("Resource ID is Used By:[" + connections.size()
                    + "] Connections, please remove theses dependencies, before delete"));
        } catch (ResourceNotFoundException ex) {
            //
            // Neste Caso isso é desejado, não tem conexões que dependem dele...
            //

            FilterDTO circuitFilter = new FilterDTO();
            circuitFilter.setAqlFilter("doc.aPoint._id == @resourceId or doc.zPoint._id == @resourceId ");
            circuitFilter.getObjects().add("connections");
            circuitFilter.setBindings(bindings);
            try {
                GraphList<CircuitResource> circuits = circuitManager.findCircuitsByFilter(circuitFilter, domain);
                throw new InvalidRequestException(("Resource ID is Used By:[" + circuits.size()
                        + "] Circuits, please remove theses dependencies, before delete"));
            } catch (ResourceNotFoundException exCic) {
                resource = this.managedResourceManager.delete(resource);
                DeleteManagedResourceResponse response = new DeleteManagedResourceResponse(resource);
                //
                // Precisa de um evento de Delete ?
                //
                return response;
            }
        }

    }

    /**
     * Busca um recurso gerenciado com base no ID ou domínio fornecidos na
     * solicitação.
     *
     * <p>
     * Este método é responsável por buscar um recurso gerenciado com base no ID
     * ou domínio fornecidos na solicitação (request). Ele realiza as seguintes
     * etapas:
     * </p>
     *
     * <ol>
     * <li>Verifica se o campo resourceId na solicitação não é nulo. Se for
     * nulo, verifica se o campo requestDomain também não é nulo. Se ambos os
     * campos estiverem vazios, lança uma exceção InvalidRequestException
     * indicando que os campos resourceId e domain não podem ser vazios ou
     * nulos.</li>
     * <li>Se o campo resourceId não for nulo, tenta obter o domínio associado
     * ao requestDomain da solicitação.</li>
     * <li>Cria um novo objeto ManagedResource usando o domínio obtido e o
     * resourceId fornecido na solicitação.</li>
     * <li>Chama o método findManagedResource no objeto manager passando o
     * objeto ManagedResource criado anteriormente para buscar o recurso
     * gerenciado correspondente.</li>
     * <li>Retorna a resposta encapsulando o recurso gerenciado encontrado na
     * FindManagedResourceResponse.</li>
     * <li>Se ocorrer uma exceção IllegalArgumentException durante a busca por
     * recursos gerenciados, lança uma exceção InvalidRequestException com uma
     * mensagem apropriada indicando que o resourceId fornecido é inválido.</li>
     * </ol>
     *
     * @param request A solicitação contendo o resourceId e/ou o requestDomain
     *                para buscar o recurso gerenciado.
     * @return Uma resposta contendo o recurso gerenciado encontrado encapsulado
     *         em FindManagedResourceResponse.
     * @throws InvalidRequestException   Se resourceId e requestDomain forem ambos
     *                                   vazios ou nulos, ou se o resourceId
     *                                   fornecido for inválido.
     * @throws DomainNotFoundException   Se o domínio associado ao requestDomain
     *                                   não for encontrado.
     * @throws ResourceNotFoundException Se o recurso gerenciado não for
     *                                   encontrado com base nos parâmetros
     *                                   fornecidos.
     * @throws ArangoDaoException        Se ocorrer um erro ao buscar o recurso
     *                                   gerenciado na camada de acesso aos dados.
     */
    public FindManagedResourceResponse findManagedResourceById(FindManagedResourceRequest request)
            throws InvalidRequestException, DomainNotFoundException, ResourceNotFoundException, ArangoDaoException {
        if (request.getResourceId() == null) {
            if (request.getRequestDomain() == null) {
                throw new InvalidRequestException("Field resourceId and domain cannot be empty or null");
            } else {
                try {
                    Domain domain = this.domainManager.getDomain(request.getRequestDomain());
                    return new FindManagedResourceResponse(
                            this.manager.findManagedResource(new ManagedResource(domain, request.getResourceId())));
                } catch (IllegalArgumentException exception) {
                    throw new InvalidRequestException("ResourceId Invalid DOMAIN:[" + request.getRequestDomain() + "]");
                }
            }
        } else {
            try {
                Domain domain = this.domainManager.getDomain(request.getRequestDomain());
                return new FindManagedResourceResponse(
                        this.manager.findManagedResource(new ManagedResource(domain, request.getResourceId())));
            } catch (IllegalArgumentException exception) {
                throw new InvalidRequestException("ResourceId Invalid UUID:[" + request.getResourceId() + "]");
            }

        }

    }

    /**
     *
     * @param request
     * @return
     * @throws DomainNotFoundException
     * @throws ResourceNotFoundException
     * @throws ArangoDaoException
     * @throws InvalidRequestException
     */
    public TypedListResponse listManagedResources(ListManagedResourceRequest request)
            throws DomainNotFoundException, ResourceNotFoundException, ArangoDaoException, InvalidRequestException {
        FilterDTO filter = new FilterDTO();
        filter.setAqlFilter(" ");
        filter.setSortCondition("sort doc.nodeAddress asc");
        filter.getObjects().add("nodes");
        filter.setLimit(10L);
        filter.setOffSet(0L);
        TypedListResponse response = new TypedListResponse(this.managedResourceManager
                .getNodesByFilter(filter, request.getRequestDomain()).toList());
        return response;
    }

    public TypedListResponse listResourceConnection(ListResourceConnectionRequest request)
            throws DomainNotFoundException, ResourceNotFoundException, ArangoDaoException, InvalidRequestException {
        FilterDTO filter = new FilterDTO();
        filter.setAqlFilter(" ");
        filter.setSortCondition("sort doc.nodeAddress asc");
        filter.getObjects().add("connections");
        filter.setLimit(10L);
        filter.setOffSet(0L);
        TypedListResponse response = new TypedListResponse(this.resourceConnectionManager
                .getConnectionsByFilter(filter, request.getRequestDomain()).toList());
        return response;
    }

    /**
     * Cria um novo recurso gerenciado com base nos parâmetros fornecidos na
     * solicitação.
     *
     * <p>
     * Este método é responsável por criar um novo recurso gerenciado com base
     * nos parâmetros fornecidos na solicitação (request). Ele realiza as
     * seguintes etapas:
     * </p>
     *
     * <ol>
     * <li>Verifica se a solicitação (request) não é nula. Se for nula, lança
     * uma exceção InvalidRequestException com uma mensagem indicando que a
     * solicitação é nula.</li>
     * <li>Verifica se o campo payLoad na solicitação não é nulo. Se for nulo,
     * lança uma exceção InvalidRequestException com uma mensagem indicando que
     * é necessário fornecer um recurso para ser criado.</li>
     * <li>Obtém o recurso gerenciado a ser criado a partir do campo payLoad na
     * solicitação.</li>
     * <li>Obtém o domínio associado ao campo requestDomain na solicitação e
     * atribui-o ao recurso gerenciado.</li>
     * <li>Verifica se o domínio associado ao recurso gerenciado não é nulo. Se
     * for nulo, lança uma exceção DomainNotFoundException com uma mensagem
     * indicando que o domínio fornecido não foi encontrado.</li>
     * <li>Verifica se o campo name no recurso gerenciado não é nulo ou vazio.
     * Se for nulo ou vazio, verifica se o campo nodeAddress no recurso
     * gerenciado não é nulo ou vazio e atribui-o ao campo name. Se ambos os
     * campos estiverem vazios, lança uma exceção InvalidRequestException com
     * uma mensagem indicando que é necessário fornecer um nome para o
     * recurso.</li>
     * <li>Verifica se o campo attributeSchemaName no recurso gerenciado não é
     * nulo. Se for nulo, atribui o valor "resource.default" ao campo. Se o
     * campo não começar com "resource", lança uma exceção
     * InvalidRequestException com uma mensagem indicando que o nome do esquema
     * deve começar com "resource".</li>
     * <li>Verifica se o campo className no recurso gerenciado não é nulo. Se
     * for nulo, atribui o valor "resource.Default" ao campo. Se o campo não
     * começar com "resource", lança uma exceção InvalidRequestException com uma
     * mensagem indicando que o nome da classe deve começar com "resource".</li>
     * <li>Verifica se o campo operationalStatus no recurso gerenciado não é
     * nulo. Se for nulo, atribui o valor "Up" ao campo.</li>
     * <li>Verifica se o campo adminStatus no recurso gerenciado não é nulo. Se
     * for nulo, atribui o valor "Up" ao campo.</li>
     * <li>Verifica se o campo nodeAddress no recurso gerenciado não é nulo. Se
     * for nulo, atribui o valor do campo name ao campo nodeAddress.</li>
     * <li>Se o campo structureId no recurso gerenciado não for nulo e não
     * estiver vazio, obtém o recurso gerenciado associado ao ID da estrutura,
     * valida se o ID da estrutura existe e atribui o ID da estrutura corrigido
     * ao campo structureId.</li>
     * <li>Atribui o ID do usuário fornecido na solicitação aos campos owner e
     * author do recurso gerenciado.</li>
     * <li>Atribui a data atual ao campo insertedDate do recurso
     * gerenciado.</li>
     * <li>Chama o método create no objeto manager, passando o recurso
     * gerenciado, para criar o novo recurso.</li>
     * <li>Registra o tempo de execução da operação em milissegundos.</li>
     * <li>Retorna a resposta encapsulando o recurso gerenciado criado na
     * CreateManagedResourceResponse.</li>
     * </ol>
     *
     * @param request A solicitação contendo os parâmetros necessários para
     *                criar o recurso gerenciado.
     * @return Uma resposta contendo o novo recurso gerenciado criado
     *         encapsulado em CreateManagedResourceResponse.
     * @throws SchemaNotFoundException               Se o esquema associado ao
     *                                               recurso
     *                                               gerenciado não for encontrado.
     * @throws AttributeConstraintViolationException Se ocorrer uma violação de
     *                                               restrição de atributo ao criar
     *                                               o recurso gerenciado.
     * @throws GenericException                      Se ocorrer um erro genérico
     *                                               durante a criação do
     *                                               recurso gerenciado.
     * @throws ScriptRuleException                   Se ocorrer um erro ao aplicar
     *                                               uma regra de
     *                                               script durante a criação do
     *                                               recurso gerenciado.
     * @throws InvalidRequestException               Se a solicitação for nula, se o
     *                                               campo
     *                                               payLoad for nulo ou se os
     *                                               campos obrigatórios estiverem
     *                                               vazios ou nulos.
     * @throws DomainNotFoundException               Se o domínio associado ao
     *                                               recurso
     *                                               gerenciado não for encontrado.
     * @throws ArangoDaoException                    Se ocorrer um erro ao acessar a
     *                                               camada de
     *                                               dados ArangoDB durante a
     *                                               criação do recurso gerenciado.
     * @throws ResourceNotFoundException             Se o recurso gerenciado não for
     *                                               encontrado com base nos
     *                                               parâmetros fornecidos.
     * @throws AttributeNotFoundException            Se um atributo não for
     *                                               encontrado
     *                                               durante a criação do recurso
     *                                               gerenciado.
     */
    public CreateManagedResourceResponse createManagedResource(CreateManagedResourceRequest request)
            throws SchemaNotFoundException, AttributeConstraintViolationException, GenericException,
            ScriptRuleException, InvalidRequestException, DomainNotFoundException, ArangoDaoException,
            ResourceNotFoundException, AttributeNotFoundException {
        Long start = System.currentTimeMillis();
        if (request == null) {
            throw new InvalidRequestException("Request is NULL!");
        }

        if (request.getPayLoad() == null) {
            logger.warn("Invalid Request Received:[{}]", this.utils.toJson(request));
            throw new InvalidRequestException("Please Provide a Resrouce !");
        }

        ManagedResource resource = request.getPayLoad();
        resource.setDomain(domainManager.getDomain(request.getRequestDomain()));

        if (resource.getDomain() == null) {
            throw new DomainNotFoundException("Domain WIth Name:[" + request.getRequestDomain() + "] not found");
        }
        if (resource.getName() == null || resource.getName().trim().equals("")) {
            if (resource.getNodeAddress() != null && !resource.getNodeAddress().trim().equals("")) {
                resource.setName(resource.getNodeAddress());
            } else {
                throw new InvalidRequestException("Please Give a name");
            }
        }

        if (resource.getAttributeSchemaName() == null) {
            resource.setAttributeSchemaName("resource.default");
        } else if (!resource.getAttributeSchemaName().startsWith("resource")) {
            throw new InvalidRequestException("Schema Name Has to Start with resource.");
        }

        if (resource.getClassName() == null) {
            resource.setClassName("resource.Default");
        } else if (!resource.getClassName().startsWith("resource")) {
            throw new InvalidRequestException("Class Name Has to Start with resource.");
        }

        /**
         * Sanitização do Oper Status
         */
        if (resource.getOperationalStatus() == null) {
            resource.setOperationalStatus("Up");
        } else {
            if (resource.getOperationalStatus().trim().equals("")) {
                resource.setOperationalStatus("Up");
            }
        }

        if (resource.getAdminStatus() == null) {
            resource.setAdminStatus("Up");
        } else {
            if (resource.getAdminStatus().trim().equals("")) {
                resource.setAdminStatus("Up");
            }
        }

        //
        // Avaliar se podemos melhorar isso, usando um nome canonico, com o className +
        // name
        //
        if (resource.getNodeAddress() == null) {
            resource.setNodeAddress(resource.getName());
        }

        if (resource.getStructureId() != null && !resource.getStructureId().equals("")) {
            ManagedResource structureResource = new ManagedResource(resource.getDomain(), resource.getStructureId());
            //
            // Valida se o id de Estrutura Existe
            //
            structureResource = this.findManagedResource(structureResource);
            resource.setStructureId(structureResource.getKey());
        }

        resource.setOwner(request.getUserId());
        resource.setAuthor(request.getUserId());
        resource.setInsertedDate(new Date());
        resource = manager.create(resource);
        Long end = System.currentTimeMillis();
        Long took = end - start;
        logger.debug("Create Resource:[{}] Took: {} ms", resource.getNodeAddress(), took);
        return new CreateManagedResourceResponse(resource);
    }

    /**
     * Retorna uma resposta de filtro contendo os recursos gerenciados com base
     * na solicitação fornecida.
     *
     * <p>
     * Este método realiza a filtragem dos recursos gerenciados com base nos
     * critérios definidos na solicitação. Ele pode filtrar os nós (nodes) e/ou
     * as conexões (connections) presentes no domínio especificado.
     * </p>
     *
     * <p>
     * A solicitação de filtro pode conter parâmetros como limite de resultados
     * e objetos a serem filtrados. O limite de resultados é limitado para não
     * exceder 1000 para evitar abusos da API.
     * </p>
     *
     * <p>
     * O método também verifica a necessidade de computar links fracos (weak
     * links) entre os recursos, mas essa funcionalidade pode estar desabilitada
     * no sistema.
     * </p>
     *
     * @param filter A solicitação de filtro contendo os critérios de filtragem.
     * @return Uma resposta contendo os recursos gerenciados que atendem aos
     *         critérios de filtragem.
     * @throws InvalidRequestException   Se a solicitação for inválida ou se
     *                                   houver algum parâmetro inválido, como
     *                                   limite de resultados muito alto.
     * @throws ArangoDaoException        Para exceções relacionadas ao ArangoDB.
     * @throws DomainNotFoundException   Se o domínio não for encontrado.
     * @throws ResourceNotFoundException Se algum recurso gerenciado não for
     *                                   encontrado.
     */
    public FilterResponse findManagedResourceByFilter(FilterRequest filter)
            throws InvalidRequestException, ArangoDaoException, DomainNotFoundException, ResourceNotFoundException {

        FilterResponse response = new FilterResponse(filter.getPayLoad());

        //
        // Validação para evitar abusos de uso da API
        //
        if (filter.getPayLoad() != null) {
            if (filter.getPayLoad().getLimit() != null) {
                if (filter.getPayLoad().getLimit() > 10000) {
                    throw new InvalidRequestException(
                            "Result Set Limit cannot be over 1000, please descrease limit value to a range between 0 and 1000");
                } else {
                    if (filter.getPayLoad().getLimit() < 0L) {
                        filter.getPayLoad().setLimit(1000L);
                    }
                }
            } else {
                filter.getPayLoad().setLimit(1000L);
            }
        }

        if (filter.getPayLoad().getObjects().contains("nodes") || filter.getPayLoad().getObjects().contains("node")) {
            GraphList<ManagedResource> nodesGraph = managedResourceManager.getNodesByFilter(filter.getPayLoad(),
                    filter.getRequestDomain());
            List<ManagedResource> nodes = nodesGraph.toList();
            response.getPayLoad().setNodes(nodes);
            response.getPayLoad().setNodeCount(nodesGraph.size());
            response.setSize(nodesGraph.size());
            response.setArangoStats(nodesGraph.getStats());
        } else if (filter.getPayLoad().getObjects().contains("connections")
                || filter.getPayLoad().getObjects().contains("connection")) {
            GraphList<ResourceConnection> connectionsGraph = resourceConnectionManager
                    .getConnectionsByFilter(filter.getPayLoad(), filter.getRequestDomain());
            List<ResourceConnection> connections = connectionsGraph.toList();
            response.getPayLoad().setConnections(connections);
            response.getPayLoad().setConnectionsCount(connectionsGraph.size());
            response.setSize(connectionsGraph.size());
            response.setArangoStats(connectionsGraph.getStats());
            if (filter.getPayLoad().getComputeWeakLinks()) {
                //
                // Computação de Links Fracos Desabilitada
                //
                throw new InvalidRequestException("Weak Links Calculation is Disabled on this system");
            }
        }

        return this.filterProjectionSession.filterProjection(filter.getPayLoad(), response);
    }

    /**
     *
     * @param filter
     * @return
     * @throws InvalidRequestException
     * @throws ArangoDaoException
     * @throws DomainNotFoundException
     * @throws ResourceNotFoundException
     */
    public GraphList<ManagedResource> findManagedResourceByFilter(FilterDTO filter)
            throws InvalidRequestException, ArangoDaoException, DomainNotFoundException, ResourceNotFoundException {
        if (filter.getObjects() != null) {
            if (filter.getObjects().contains("nodes") || filter.getObjects().contains("node")) {
                GraphList<ManagedResource> nodesGraph = this.managedResourceManager.getNodesByFilter(filter,
                        filter.getDomainName());

                return nodesGraph;
            } else {
                throw new InvalidRequestException("Filter Object is now known")
                        .addDetails("filter", filter);
            }
        } else {
            throw new InvalidRequestException("No Object Found for filter")
                    .addDetails("filter", filter);
        }

    }

    /**
     *
     * @param filter
     * @return
     * @throws InvalidRequestException
     * @throws ArangoDaoException
     * @throws DomainNotFoundException
     * @throws ResourceNotFoundException
     */
    public GraphList<ResourceConnection> findResourceConnectionByFilter(FilterDTO filter)
            throws InvalidRequestException, ArangoDaoException, DomainNotFoundException, ResourceNotFoundException {
        if (filter.getObjects().contains("connections") || filter.getObjects().contains("connection")) {
            GraphList<ResourceConnection> nodesGraph = this.resourceConnectionManager.getConnectionsByFilter(filter,
                    filter.getDomainName());
            return nodesGraph;
        } else {
            throw new InvalidRequestException("Invalida Object Type:[" + String.join(",", filter.getObjects()) + "]")
                    .addDetails("filter", filter);
        }
    }

    /**
     * Retorna as areastas de um circuito no grafo
     *
     * @param cic
     * @return
     */
    public GraphList<ResourceConnection> findResourceConnectionByCircuit(CircuitResource cic)
            throws InvalidRequestException, ArangoDaoException, DomainNotFoundException, ResourceNotFoundException {
        FilterDTO filter = new FilterDTO();
        filter.setDomainName(cic.getDomain().getDomainName());
        filter.addBinding("circuitId", cic.getId());
        // in doc.relatedNodes[*]
        filter.setAqlFilter(" @circuitId in doc.circuits[*]");
        filter.addObject("connections");
        return this.findResourceConnectionByFilter(filter);
    }

    /**
     * Retorna as areastas de um servico no grafo
     *
     * @param service
     * @return
     */
    public GraphList<ResourceConnection> findResourceConnectionByService(ServiceResource service)
            throws InvalidRequestException, ArangoDaoException, DomainNotFoundException, ResourceNotFoundException {
        FilterDTO filter = new FilterDTO();
        filter.setDomainName(service.getDomain().getDomainName());
        filter.addBinding("serviceId", service.getId());
        // in doc.relatedNodes[*]
        filter.setAqlFilter(" @serviceId in doc.services[*]");
        filter.addObject("connections");
        return this.findResourceConnectionByFilter(filter);
    }

    public String findManagedResource(FilterRequest filter) {
        FilterDTO filterDTO = filter.getPayLoad();

        String json = this.manager.findManagedResource(filterDTO);
        return this.filterProjectionSession.filterJson(json, filterDTO.getFields());
    }

    public ManagedResource findManagedResource(ManagedResource resource)
            throws ResourceNotFoundException, ArangoDaoException, InvalidRequestException {
        return this.manager.findManagedResource(resource);
    }

    public ResourceConnection findResourceConnection(ResourceConnection connection)
            throws ResourceNotFoundException, ArangoDaoException, InvalidRequestException {

        return this.resourceConnectionManager.findResourceConnection(connection);
    }

    /**
     * Método patchManagedResource
     * <p>
     * Descrição: Este método é responsável por atualizar os campos de um
     * recurso gerenciado (ManagedResource) com base nas informações fornecidas
     * no objeto PatchManagedResourceRequest. O método recebe como parâmetro um
     * objeto PatchManagedResourceRequest contendo o recurso a ser atualizado e
     * suas informações de atualização.
     * </p>
     * <p>
     * Fluxo:
     * <ol>
     * <li>O método inicia obtendo o recurso a ser atualizado (requestedPatch) a
     * partir do objeto PatchManagedResourceRequest fornecido como
     * parâmetro.</li>
     * <li>Em seguida, é realizado um ajuste no domínio do recurso para garantir
     * seu correto funcionamento. O método obtém o domínio (domain) do recurso
     * por meio do domainManager e atribui o nome do domínio ao recurso
     * (requestedPatch).</li>
     * <li>O método decide se deve priorizar o campo ID ou o campo KEY para
     * realizar a busca do recurso a ser atualizado (fromDBResource). Se o campo
     * ID estiver presente, é criado um novo objeto ManagedResource (searchObj)
     * contendo o domínio e o ID do recurso para utilizá-lo na busca; caso
     * contrário, o objeto requestedPatch é usado para realizar a busca.</li>
     * <li>Em seguida, o método busca o recurso original (fromDBResource) com
     * base no objeto searchObj obtido no passo anterior.</li>
     * <li>Se houver informações de atualização fornecidas no objeto
     * requestedPatch, o método atualiza o recurso original (fromDBResource) com
     * as informações fornecidas. As informações que podem ser atualizadas
     * incluem o nome, endereço do nó, classe, status operacional, status
     * administrativo, atributos, descrição, tipo de recurso, identificação de
     * estrutura, categoria, status comercial, atributos de descoberta e serviço
     * dependente.</li>
     * <li>Em seguida, o método atualiza os atributos de métrica consumível e
     * métrica do consumidor do recurso.</li>
     * <li>Por fim, o método atualiza o recurso gerenciado no banco de dados por
     * meio do método update do managedResourceManager e retorna o recurso
     * atualizado encapsulado em um objeto PatchManagedResourceResponse.</li>
     * </ol>
     * </p>
     *
     * Exceptions:
     * <ul>
     * <li>DomainNotFoundException: Lançada se o domínio do recurso não for
     * encontrado ao realizar o ajuste do domínio.</li>
     * <li>ResourceNotFoundException: Lançada se o recurso original não for
     * encontrado com base nas informações de busca.</li>
     * <li>ArangoDaoException: Lançada se ocorrer algum erro durante a interação
     * com o banco de dados ArangoDB.</li>
     * <li>InvalidRequestException: Lançada em diferentes cenários, como quando
     * há uma tentativa de atualizar um recurso e seu serviço dependente no
     * mesmo domínio, ou quando o campo "className" possui valor "Default".</li>
     * <li>AttributeConstraintViolationException: Lançada se alguma restrição de
     * atributo for violada durante a atualização do recurso.</li>
     * <li>ScriptRuleException: Lançada se ocorrer um erro relacionado a alguma
     * regra de script durante a atualização.</li>
     * <li>SchemaNotFoundException: Lançada se o esquema do recurso não for
     * encontrado durante a atualização.</li>
     * <li>GenericException: Lançada se ocorrer algum outro erro não
     * especificado durante a execução do método.</li>
     * </ul>
     *
     * Campos atualizáveis:
     * <ul>
     * <li>name: Nome do recurso.</li>
     * <li>nodeAddress: Endereço do nó do recurso.</li>
     * <li>className: Nome da classe do recurso (exceto "Default").</li>
     * <li>operationalStatus: Status operacional do recurso.</li>
     * <li>adminStatus: Status administrativo do recurso.</li>
     * <li>attributes: Conjunto de atributos do recurso.</li>
     * <li>description: Descrição do recurso.</li>
     * <li>resourceType: Tipo de recurso.</li>
     * <li>structureId: Identificação da estrutura relacionada ao recurso.</li>
     * <li>category: Categoria do recurso.</li>
     * <li>businessStatus: Status comercial do recurso.</li>
     * <li>discoveryAttributes: Atributos de descoberta do recurso.</li>
     * <li>dependentService: Serviço dependente associado ao recurso.</li>
     * <li>consumableMetric: Métrica consumível do recurso.</li>
     * <li>consumerMetric: Métrica do consumidor do recurso.</li>
     * </ul>
     *
     * @param patchRequest Objeto PatchManagedResourceRequest contendo o recurso
     *                     a ser atualizado e suas informações de atualização.
     * @return Objeto PatchManagedResourceResponse contendo o recurso
     *         atualizado.
     * @throws DomainNotFoundException               Se o domínio do recurso não for
     *                                               encontrado.
     * @throws ResourceNotFoundException             Se o recurso original não for
     *                                               encontrado.
     * @throws ArangoDaoException                    Se ocorrer algum erro ao
     *                                               interagir com o banco
     *                                               de dados ArangoDB.
     * @throws InvalidRequestException               Se ocorrerem problemas
     *                                               relacionados a
     *                                               requisições inválidas.
     * @throws AttributeConstraintViolationException Se alguma restrição de
     *                                               atributo for violada durante a
     *                                               atualização.
     * @throws ScriptRuleException                   Se ocorrer um erro relacionado
     *                                               a alguma regra
     *                                               de script durante a
     *                                               atualização.
     * @throws SchemaNotFoundException               Se o esquema do recurso não for
     *                                               encontrado durante a
     *                                               atualização.
     * @throws GenericException                      Se ocorrer algum outro erro não
     *                                               especificado
     *                                               durante a execução do método.
     */
    public PatchManagedResourceResponse patchManagedResource(PatchManagedResourceRequest patchRequest)
            throws DomainNotFoundException, ResourceNotFoundException, ArangoDaoException, InvalidRequestException,
            AttributeConstraintViolationException, ScriptRuleException, SchemaNotFoundException, GenericException,
            AttributeNotFoundException {
        //
        //
        //
        ManagedResource requestedPatch = patchRequest.getPayLoad();
        ManagedResource searchObj = null;
        //
        // Arruma o domain para funcionar certinho
        //
        requestedPatch.setDomain(this.domainManager.getDomain(patchRequest.getRequestDomain()));
        requestedPatch.setDomainName(requestedPatch.getDomain().getDomainName());

        //
        // Garante que vamos priorizar o ID ou, o KEY ( FUTURO )
        //
        if (requestedPatch.getId() != null && !requestedPatch.getId().trim().equals("")) {
            //
            // Temos um ID, sanitiza para ficar bom
            //
            searchObj = new ManagedResource(requestedPatch.getDomain(), requestedPatch.getId());
        } else {
            searchObj = requestedPatch;
        }

        ManagedResource fromDBResource = this.findManagedResource(searchObj);

        //
        // Se chegamos aqui, temos coisas para atualizar...
        // @ Todo, comparar para ver se houve algo que realmente mudou..
        //
        if (requestedPatch.getName() != null) {
            fromDBResource.setName(requestedPatch.getName());
        }

        if (requestedPatch.getNodeAddress() != null) {
            fromDBResource.setNodeAddress(requestedPatch.getNodeAddress());
        }

        if (requestedPatch.getClassName() != null && !requestedPatch.getClassName().equals("Default")) {
            fromDBResource.setClassName(requestedPatch.getClassName());
        }

        if (requestedPatch.getOperationalStatus() != null) {
            fromDBResource.setOperationalStatus(requestedPatch.getOperationalStatus());
        }

        //
        // Pode atualizar o AtributeSchemaModel ? isso é bem custosooo vamos tratar isso
        // em outro lugar...
        //
        if (requestedPatch.getAdminStatus() != null) {
            fromDBResource.setAdminStatus(requestedPatch.getAdminStatus());
        }

        //
        // Atualiza os atributos
        //
        if (requestedPatch.getAttributes() != null && !requestedPatch.getAttributes().isEmpty()) {
            requestedPatch.getAttributes().forEach((name, attribute) -> {
                if (fromDBResource.getAttributes() != null) {
                    if (fromDBResource.getAttributes().containsKey(name)) {
                        fromDBResource.getAttributes().replace(name, attribute);
                    } else {
                        // fromDBResource.getAttributes().put(name, attribute);
                        fromDBResource.getAttributes().remove(name);
                    }
                }
            });
        }

        if (requestedPatch.getDescription() != null && !requestedPatch.getDescription().trim().equals("")) {
            if (!requestedPatch.getDescription().equals(fromDBResource.getDescription())) {
                fromDBResource.setDescription(requestedPatch.getDescription());
            }
        }

        if (requestedPatch.getResourceType() != null && !requestedPatch.getResourceType().trim().equals("")) {
            if (!requestedPatch.getResourceType().equals(fromDBResource.getResourceType())) {
                fromDBResource.setResourceType(requestedPatch.getResourceType());
            }
        }

        if (requestedPatch.getStructureId() != null && !requestedPatch.getStructureId().trim().equals("")) {
            if (!requestedPatch.getStructureId().equals(fromDBResource.getStructureId())) {
                ManagedResource structureResrouce = new ManagedResource(fromDBResource.getDomain(),
                        requestedPatch.getStructureId());
                //
                // Valida se o id de Estrutura Existe
                //
                structureResrouce = this.findManagedResource(structureResrouce);

                fromDBResource.setStructureId(requestedPatch.getStructureId());
            }
        }

        if (requestedPatch.getCategory() != null && !requestedPatch.getCategory().trim().equals("")) {
            if (!requestedPatch.getCategory().equals("default")) {
                if (!requestedPatch.getCategory().equals(fromDBResource.getCategory())) {
                    fromDBResource.setCategory(requestedPatch.getCategory());
                }
            }
        }

        if (requestedPatch.getBusinessStatus() != null && !requestedPatch.getBusinessStatus().trim().equals("")) {
            if (!requestedPatch.getBusinessStatus().equals(fromDBResource.getBusinessStatus())) {
                fromDBResource.setBusinessStatus(requestedPatch.getBusinessStatus());
            }
        }

        //
        // Atualiza os atributos de rede
        //
        if (requestedPatch.getDiscoveryAttributes() != null && !requestedPatch.getDiscoveryAttributes().isEmpty()) {
            requestedPatch.getDiscoveryAttributes().forEach((name, attribute) -> {
                if (fromDBResource.getDiscoveryAttributes() != null) {
                    if (fromDBResource.getDiscoveryAttributes().containsKey(name)) {
                        fromDBResource.getDiscoveryAttributes().replace(name, attribute);
                    } else {
                        fromDBResource.getDiscoveryAttributes().put(name, attribute);
                    }
                }
            });
        }

        if (requestedPatch.getDependentService() != null) {
            //
            // Valida se o serviço existe
            //
            ServiceResource service = this.serviceManager.getService(requestedPatch.getDependentService());

            //
            // Atualiza para referencia do DB
            //
            requestedPatch.setDependentService(service);

            //
            // Agora vamos ver se o serviço é de um dominio diferente do recurso... não
            // podem ser do mesmo
            //
            if (service.getDomain().getDomainName().equals(requestedPatch.getDomain().getDomainName())) {
                throw new InvalidRequestException("Resource and Parent Service cannot be in the same domain.");
            }

            if (fromDBResource.getDependentService() == null) {
                //
                // Está criando a dependencia...
                //
                fromDBResource.setDependentService(requestedPatch.getDependentService());
            } else if (!fromDBResource.getDependentService().equals(service)) {
                fromDBResource.setDependentService(requestedPatch.getDependentService());
            }
        }

        fromDBResource.setConsumableMetric(requestedPatch.getConsumableMetric());
        fromDBResource.setConsumerMetric(requestedPatch.getConsumerMetric());

        ManagedResource result = this.managedResourceManager.update(fromDBResource);
        return new PatchManagedResourceResponse(result);
    }

    /**
     * Método patchResourceConnection
     *
     * <p>
     * Descrição: Este método é responsável por atualizar os campos de uma
     * conexão de recurso (ResourceConnection) com base nas informações
     * fornecidas no objeto PatchResourceConnectionRequest. O método recebe como
     * parâmetro um objeto PatchResourceConnectionRequest contendo a conexão de
     * recurso a ser atualizada e suas informações de atualização.
     * </p>
     *
     * <p>
     * Fluxo:
     * <ol>
     * <li>O método inicia obtendo a conexão de recurso a ser atualizada
     * (requestedPatch) a partir do objeto PatchResourceConnectionRequest
     * fornecido como parâmetro.</li>
     * <li>Em seguida, é realizado um ajuste no domínio da conexão de recurso
     * para garantir seu correto funcionamento. O método obtém o domínio
     * (domain) da conexão de recurso por meio do domainManager e atribui o nome
     * do domínio à conexão de recurso (requestedPatch).</li>
     * <li>Cria-se uma nova conexão de recurso (connection) para ser usada na
     * busca do recurso original (fromDBResource) com base no ID fornecido no
     * objeto requestedPatch.</li>
     * <li>O método busca a conexão de recurso original (fromDBResource) com
     * base na conexão de recurso criada anteriormente.</li>
     * <li>Se houver informações de atualização fornecidas no objeto
     * requestedPatch, o método atualiza a conexão de recurso original
     * (fromDBResource) com as informações fornecidas. As informações que podem
     * ser atualizadas incluem o nome, endereço do nó, classe, status
     * operacional, status administrativo, atributos e atributos de
     * descoberta.</li>
     * <li>Se for fornecido um serviço dependente (dependentService) no objeto
     * requestedPatch, o método verifica se esse serviço existe e atualiza a
     * conexão de recurso com o serviço fornecido, verificando se eles pertencem
     * a domínios diferentes.</li>
     * <li>Por fim, o método atualiza a conexão de recurso no banco de dados por
     * meio do método updateResourceConnection do resourceConnectionManager e
     * retorna a conexão de recurso atualizada encapsulada em um objeto
     * PatchResourceConnectionResponse.</li>
     * </ol>
     * </p>
     *
     * Exceptions:
     * <ul>
     * <li>DomainNotFoundException: Lançada se o domínio da conexão de recurso
     * não for encontrado ao realizar o ajuste do domínio.</li>
     * <li>ResourceNotFoundException: Lançada se a conexão de recurso original
     * não for encontrada com base nas informações de busca.</li>
     * <li>ArangoDaoException: Lançada se ocorrer algum erro durante a interação
     * com o banco de dados ArangoDB.</li>
     * <li>InvalidRequestException: Lançada em diferentes cenários, como quando
     * há uma tentativa de atualizar uma conexão de recurso e seu serviço
     * dependente no mesmo domínio.</li>
     * <li>AttributeConstraintViolationException: Lançada se alguma restrição de
     * atributo for violada durante a atualização.</li>
     * </ul>
     *
     * Campos atualizáveis:
     * <ul>
     * <li>name: Nome da conexão de recurso.</li>
     * <li>nodeAddress: Endereço do nó da conexão de recurso.</li>
     * <li>className: Nome da classe da conexão de recurso (exceto
     * "Default").</li>
     * <li>operationalStatus: Status operacional da conexão de recurso.</li>
     * <li>adminStatus: Status administrativo da conexão de recurso.</li>
     * <li>attributes: Conjunto de atributos da conexão de recurso.</li>
     * <li>discoveryAttributes: Atributos de descoberta da conexão de
     * recurso.</li>
     * <li>dependentService: Serviço dependente associado à conexão de
     * recurso.</li>
     * </ul>
     *
     * @param request Objeto PatchResourceConnectionRequest contendo a conexão
     *                de recurso a ser atualizada e suas informações de atualização.
     * @return Objeto PatchResourceConnectionResponse contendo a conexão de
     *         recurso atualizada.
     * @throws DomainNotFoundException               Se o domínio da conexão de
     *                                               recurso não
     *                                               for encontrado.
     * @throws ResourceNotFoundException             Se a conexão de recurso
     *                                               original não
     *                                               for encontrada.
     * @throws ArangoDaoException                    Se ocorrer algum erro ao
     *                                               interagir com o banco
     *                                               de dados ArangoDB.
     * @throws InvalidRequestException               Se ocorrerem problemas
     *                                               relacionados a
     *                                               requisições inválidas.
     * @throws AttributeConstraintViolationException Se alguma restrição de
     *                                               atributo for violada durante a
     *                                               atualização.
     */
    public PatchResourceConnectionResponse patchResourceConnection(PatchResourceConnectionRequest request)
            throws DomainNotFoundException, ResourceNotFoundException, ArangoDaoException, InvalidRequestException,
            AttributeConstraintViolationException {
        //
        //
        //
        ResourceConnection requestedPatch = request.getPayLoad();

        //
        // Arruma o domain para funcionar certinho
        //
        requestedPatch.setDomain(this.domainManager.getDomain(request.getRequestDomain()));
        requestedPatch.setDomainName(requestedPatch.getDomain().getDomainName());

        ResourceConnection connection = new ResourceConnection(domainManager.getDomain(request.getRequestDomain()));
        connection.setId(requestedPatch.getId());
        ResourceConnection fromDBResource = this.findResourceConnection(connection);

        //
        // Se chegamos aqui, temos coisas para atualizar...
        // @ Todo, comparar para ver se houve algo que realmente mudou..
        //
        if (requestedPatch.getName() != null) {
            fromDBResource.setName(requestedPatch.getName());
        }

        if (requestedPatch.getNodeAddress() != null) {
            fromDBResource.setNodeAddress(requestedPatch.getNodeAddress());
        }

        if (requestedPatch.getClassName() != null && !requestedPatch.getClassName().equals("Default")) {
            fromDBResource.setClassName(requestedPatch.getClassName());
        }

        if (requestedPatch.getOperationalStatus() != null) {
            fromDBResource.setOperationalStatus(requestedPatch.getOperationalStatus());
        }

        if (requestedPatch.getBusinessStatus() != null) {
            fromDBResource.setBusinessStatus(requestedPatch.getBusinessStatus());
        }

        //
        // Pode atualizar o AtributeSchemaModel ? isso é bem custosooo vamos tratar isso
        // em outro lugar...
        //
        if (requestedPatch.getAdminStatus() != null) {
            fromDBResource.setAdminStatus(requestedPatch.getAdminStatus());
        }

        //
        // Atualiza os atributos
        //
        if (requestedPatch.getAttributes() != null && !requestedPatch.getAttributes().isEmpty()) {
            requestedPatch.getAttributes().forEach((name, attribute) -> {
                if (fromDBResource.getAttributes() != null) {
                    if (fromDBResource.getAttributes().containsKey(name)) {
                        fromDBResource.getAttributes().replace(name, attribute);
                    } else {
                        fromDBResource.getAttributes().put(name, attribute);
                    }
                }
            });
        }

        //
        // Atualiza os atributos de rede
        //
        if (requestedPatch.getDiscoveryAttributes() != null && !requestedPatch.getDiscoveryAttributes().isEmpty()) {
            requestedPatch.getDiscoveryAttributes().forEach((name, attribute) -> {
                if (fromDBResource.getDiscoveryAttributes() != null) {
                    if (fromDBResource.getDiscoveryAttributes().containsKey(name)) {
                        fromDBResource.getDiscoveryAttributes().replace(name, attribute);
                    } else {
                        fromDBResource.getDiscoveryAttributes().put(name, attribute);
                    }
                }
            });
        }

        if (requestedPatch.getDependentService() != null) {
            //
            // Valida se o serviço existe
            //
            ServiceResource service = this.serviceManager.getService(requestedPatch.getDependentService());

            //
            // Atualiza para referencia do DB
            //
            requestedPatch.setDependentService(service);

            //
            // Agora vamos ver se o serviço é de um dominio diferente do recurso... não
            // podem ser do mesmo
            //
            if (service.getDomain().getDomainName().equals(requestedPatch.getDomain().getDomainName())) {
                throw new InvalidRequestException("Resource and Parent Service cannot be in the same domain.");
            }

            if (fromDBResource.getDependentService() == null) {
                //
                // Está criando a dependencia...
                //
                fromDBResource.setDependentService(requestedPatch.getDependentService());
            } else if (!fromDBResource.getDependentService().equals(service)) {
                fromDBResource.setDependentService(requestedPatch.getDependentService());
            }
        }

        DocumentUpdateEntity<ResourceConnection> result = this.resourceConnectionManager
                .updateResourceConnection(fromDBResource);
        return new PatchResourceConnectionResponse(result.getNew());
    }

    public FindResourceConnectionResponse findResourceConnectionById(FindResourceConnectionRequest request)
            throws ResourceNotFoundException, ArangoDaoException, InvalidRequestException, DomainNotFoundException {
        //
        // Fix Domain
        //

        Domain domain = this.domainManager.getDomain(request.getRequestDomain());
        request.getPayLoad().setDomain(domain);

        return new FindResourceConnectionResponse(this.findResourceConnection(request.getPayLoad()));
    }

    /**
     * Método updateBatchAttribute
     *
     * <p>
     * Descrição: Este método é responsável por atualizar um conjunto de
     * atributos em vários recursos gerenciados (ManagedResource) com base nas
     * informações fornecidas no objeto UpdateBatchAttributeRequest. O método
     * recebe como parâmetro um objeto UpdateBatchAttributeRequest contendo os
     * atributos a serem atualizados e as condições de filtro para selecionar os
     * recursos a serem atualizados.
     * </p>
     *
     * <p>
     * Fluxo:
     * <ol>
     * <li>O método inicia obtendo o objeto BatchAttributeUpdateDTO
     * (updateRequestDTO) e o objeto FilterDTO (filter) do objeto
     * UpdateBatchAttributeRequest fornecido como parâmetro.</li>
     * <li>Caso o filtro não seja nulo, o método tentará resolver os filtros
     * para selecionar os recursos a serem atualizados.</li>
     * <li>Através do managedResourceManager, é obtido um GraphList contendo os
     * recursos que correspondem ao filtro e ao domínio da requisição
     * (request.getRequestDomain()).</li>
     * <li>É definido o total de objetos a serem atualizados no objeto
     * updateRequestDTO.</li>
     * <li>Em seguida, inicia-se o processo de atualização dos atributos para
     * cada recurso do GraphList (nodesGraph):
     * <ul>
     * <li>É verificado se existem atributos a serem atualizados
     * (request.getPayLoad().getAttributes() não é nulo e não está vazio).</li>
     * <li>Caso existam atributos a serem atualizados, os atributos dos recursos
     * são mesclados com os novos valores fornecidos no objeto
     * request.getPayLoad().getAttributes().</li>
     * <li>O recurso é atualizado no banco de dados por meio do método update do
     * managedResourceManager.</li>
     * <li>Se a atualização for bem-sucedida, o objeto updateRequestDTO é
     * atualizado com o contador de objetos atualizados
     * (addUpdatedObject()).</li>
     * <li>Se ocorrer algum erro durante o processo de atualização, o objeto
     * updateRequestDTO é atualizado com o contador de erros (addError()).</li>
     * </ul>
     * </li>
     * </ol>
     * </p>
     *
     * Exceptions:
     * <ul>
     * <li>InvalidRequestException: Lançada quando o filtro fornecido no objeto
     * UpdateBatchAttributeRequest é nulo.</li>
     * <li>DomainNotFoundException: Lançada se o domínio do recurso não for
     * encontrado durante o processo de obtenção dos recursos.</li>
     * <li>ResourceNotFoundException: Lançada se algum recurso original não for
     * encontrado com base nas informações de filtro.</li>
     * <li>ArangoDaoException: Lançada se ocorrer algum erro durante a interação
     * com o banco de dados ArangoDB.</li>
     * <li>GenericException: Lançada se ocorrer algum outro erro não
     * especificado durante a execução do método.</li>
     * </ul>
     *
     * @param request Objeto UpdateBatchAttributeRequest contendo os atributos a
     *                serem atualizados e as condições de filtro para selecionar os
     *                recursos a
     *                serem atualizados.
     * @return Objeto UpdateBatchAttributeResponse contendo informações sobre a
     *         atualização em lote.
     * @throws InvalidRequestException   Se o filtro fornecido for nulo.
     * @throws DomainNotFoundException   Se o domínio do recurso não for
     *                                   encontrado durante o processo de obtenção
     *                                   dos recursos.
     * @throws ResourceNotFoundException Se algum recurso original não for
     *                                   encontrado com base nas informações de
     *                                   filtro.
     * @throws ArangoDaoException        Se ocorrer algum erro ao interagir com o
     *                                   banco
     *                                   de dados ArangoDB.
     * @throws GenericException          Se ocorrer algum outro erro não
     *                                   especificado
     *                                   durante a execução do método.
     */
    public UpdateBatchAttributeResponse updateBatchAttribute(UpdateBatchAttributeRequest request)
            throws InvalidRequestException, DomainNotFoundException, ResourceNotFoundException, ArangoDaoException,
            GenericException {
        BatchAttributeUpdateDTO updateRequestDTO = request.getPayLoad();
        FilterDTO filter = request.getPayLoad().getFilter();
        if (filter != null) {
            //
            // Vamos tentar resolver os filtros
            //
            try (GraphList<ManagedResource> nodesGraph = managedResourceManager.getNodesByFilter(filter,
                    request.getRequestDomain())) {

                /**
                 * Seta o total de objetos a serem atualizados
                 */
                updateRequestDTO.setTotalObjects(nodesGraph.size());

                //
                // Agora começa o merge, esse carinha aqui conhece todo o resultado a ser
                // atualizado
                //
                nodesGraph.forEach(resource -> {
                    if (request.getPayLoad().getAttributes() != null
                            && !request.getPayLoad().getAttributes().isEmpty()) {
                        //
                        // Faz o merge com os novos valores
                        //
                        resource.getAttributes().putAll(request.getPayLoad().getAttributes());
                        try {
                            this.managedResourceManager.update(resource);
                            updateRequestDTO.addUpdatedObject();
                        } catch (InvalidRequestException | ArangoDaoException | AttributeConstraintViolationException
                                | AttributeNotFoundException | GenericException
                                | ResourceNotFoundException | SchemaNotFoundException | ScriptRuleException ex) {
                            //
                            // Aconteceu um erro durante o processo de update... o que fazer
                            //
                            updateRequestDTO.addError();
                        }

                    }
                });

            }

        } else {
            throw new InvalidRequestException("Please provide a valid Filter");
        }
        return new UpdateBatchAttributeResponse(updateRequestDTO);
    }
}
