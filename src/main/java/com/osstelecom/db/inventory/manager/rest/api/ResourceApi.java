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
package com.osstelecom.db.inventory.manager.rest.api;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.arangodb.ArangoDBException;
import com.osstelecom.db.inventory.manager.exception.ArangoDaoException;
import com.osstelecom.db.inventory.manager.exception.AttributeNotFoundException;
import com.osstelecom.db.inventory.manager.exception.DomainNotFoundException;
import com.osstelecom.db.inventory.manager.exception.GenericException;
import com.osstelecom.db.inventory.manager.exception.InvalidRequestException;
import com.osstelecom.db.inventory.manager.exception.ResourceNotFoundException;
import com.osstelecom.db.inventory.manager.exception.SchemaNotFoundException;
import com.osstelecom.db.inventory.manager.exception.ScriptRuleException;
import com.osstelecom.db.inventory.manager.request.CreateManagedResourceRequest;
import com.osstelecom.db.inventory.manager.request.DeleteManagedResourceRequest;
import com.osstelecom.db.inventory.manager.request.FilterRequest;
import com.osstelecom.db.inventory.manager.request.FindManagedResourceRequest;
import com.osstelecom.db.inventory.manager.request.ListManagedResourceRequest;
import com.osstelecom.db.inventory.manager.request.PatchManagedResourceRequest;
import com.osstelecom.db.inventory.manager.resources.exception.AttributeConstraintViolationException;
import com.osstelecom.db.inventory.manager.response.CreateManagedResourceResponse;
import com.osstelecom.db.inventory.manager.response.DeleteManagedResourceResponse;
import com.osstelecom.db.inventory.manager.response.FilterResponse;
import com.osstelecom.db.inventory.manager.response.FindManagedResourceResponse;
import com.osstelecom.db.inventory.manager.response.PatchManagedResourceResponse;
import com.osstelecom.db.inventory.manager.response.TypedListResponse;
import com.osstelecom.db.inventory.manager.security.model.AuthenticatedCall;
import com.osstelecom.db.inventory.manager.session.ResourceSession;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Deals with resource API
 *
 * @author Lucas Nishimura
 * @created 25.11.2022
 */
@RestController
@RequestMapping("inventory/v1")
public class ResourceApi extends BaseApi {

    @Autowired
    private ResourceSession resourceSession;

    /**
     * Cria um Managed Resource
     *
     * @param requestBody
     * @param domain
     * @return
     * @throws GenericException
     * @throws SchemaNotFoundException
     * @throws AttributeConstraintViolationException
     * @throws ScriptRuleException
     * @throws InvalidRequestException
     * @throws DomainNotFoundException
     */
    @AuthenticatedCall(role = {"user"})
    @PutMapping(path = "/{domain}/resource", produces = "application/json", consumes = "application/json")
    @Operation(
            summary = "Criar Recurso Gerenciado",
            description = "<p>Cria um recurso gerenciado com base na solicitação fornecida.</p>"
            + "<h3>Fluxo:</h3>"
            + "<ol>"
            + "<li>Valida a requisição e o payload.</li>"
            + "<li>Define o domínio do recurso.</li>"
            + "<li>Valida o nome do recurso e atribui um nome se não fornecido.</li>"
            + "<li>Define esquemas, classes e status padrões se não fornecidos.</li>"
            + "<li>Valida e define o endereço do nó.</li>"
            + "<li>Valida e define o ID da estrutura se fornecido.</li>"
            + "<li>Define o proprietário e a data de inserção.</li>"
            + "<li>Cria o recurso e retorna a resposta.</li>"
            + "</ol>"
            + "<h3>Exceções possíveis:</h3>"
            + "<ul>"
            + "<li><b>SchemaNotFoundException:</b> Lançada se o esquema não for encontrado.</li>"
            + "<li><b>AttributeConstraintViolationException:</b> Lançada em caso de violação nas restrições de atributo.</li>"
            + "<li><b>GenericException:</b> Exceção geral.</li>"
            + "<li><b>ScriptRuleException:</b> Lançada em caso de violação nas regras de script.</li>"
            + "<li><b>InvalidRequestException:</b> Lançada para solicitações nulas ou outros problemas relacionados à solicitação.</li>"
            + "<li><b>DomainNotFoundException:</b> Lançada se o domínio na solicitação não for encontrado.</li>"
            + "<li><b>ArangoDaoException:</b> Exceção relacionada às operações do ArangoDB.</li>"
            + "<li><b>ResourceNotFoundException:</b> Lançada se o recurso não for encontrado.</li>"
            + "<li><b>AttributeNotFoundException:</b> Lançada se o atributo na solicitação não for encontrado.</li>"
            + "</ul>"
    )
    public CreateManagedResourceResponse createManagedResource(@RequestBody CreateManagedResourceRequest request, @PathVariable("domain") String domain, HttpServletRequest httpRequest) throws GenericException, SchemaNotFoundException, AttributeConstraintViolationException, ScriptRuleException, InvalidRequestException, DomainNotFoundException, ArangoDaoException, ResourceNotFoundException, AttributeNotFoundException {
        try {
            this.setUserDetails(request);
            httpRequest.setAttribute("request", request);
            request.setRequestDomain(domain);
            return resourceSession.createManagedResource(request);
        } catch (ArangoDBException ex) {
            GenericException exa = new GenericException(ex.getMessage());
            exa.setStatusCode(ex.getResponseCode());
            throw exa;
        }
    }

    /**
     * Find Managed Resource By ID
     *
     * @param domain
     * @param resourceId
     * @return
     * @throws InvalidRequestException
     * @throws DomainNotFoundException
     * @throws ResourceNotFoundException
     * @throws ArangoDaoException
     */
    @AuthenticatedCall(role = {"user"})
    @GetMapping(path = "/{domain}/resource/{resourceId}", produces = "application/json")
    @Operation(description = "Busca um recurso gerenciado (ManagedResource) pelo seu ID no sistema Netcompass. Este método permite aos usuários realizarem uma consulta pelo ID do recurso (resourceId) no domínio especificado na URL da requisição (domain). Caso o recurso seja encontrado, será retornado um objeto FindManagedResourceResponse contendo as informações do recurso. Caso o recurso não seja encontrado ou ocorra algum erro durante a busca, serão lançadas exceções específicas, tais como InvalidRequestException, DomainNotFoundException, ResourceNotFoundException e ArangoDaoException. Os detalhes da requisição podem ser obtidos através do objeto HttpServletRequest passado como parâmetro. O objeto FindManagedResourceResponse contém os detalhes do recurso encontrado, incluindo atributos, status e informações do domínio associado.")
    public FindManagedResourceResponse findManagedResourceById(@PathVariable("domain") String domain, @PathVariable("resourceId") String resourceId, HttpServletRequest httpRequest) throws InvalidRequestException, DomainNotFoundException, ResourceNotFoundException, ArangoDaoException {
        FindManagedResourceRequest findRequest = new FindManagedResourceRequest(resourceId, domain);
        this.setUserDetails(findRequest);
        httpRequest.setAttribute("request", findRequest);
        return resourceSession.findManagedResourceById(findRequest);
    }

    /**
     * Delete managed resource by id
     *
     * @param domain
     * @param resourceId
     * @return
     * @throws InvalidRequestException
     * @throws DomainNotFoundException
     * @throws ResourceNotFoundException
     * @throws ArangoDaoException
     */
    @AuthenticatedCall(role = {"user"})
    @DeleteMapping(path = "/{domain}/resource/{resourceId}", produces = "application/json")
    @Operation(description = "Remove um recurso gerenciado (ManagedResource) pelo seu ID no sistema Netcompass. Este método permite aos usuários realizarem a exclusão de um recurso pelo seu ID (resourceId) no domínio especificado na URL da requisição (domain). Caso o recurso seja encontrado e removido com sucesso, será retornado um objeto DeleteManagedResourceResponse contendo informações sobre a operação de exclusão. Caso o recurso não seja encontrado ou ocorra algum erro durante a exclusão, serão lançadas exceções específicas, tais como InvalidRequestException, DomainNotFoundException, ResourceNotFoundException e ArangoDaoException. Os detalhes da requisição podem ser obtidos através do objeto HttpServletRequest passado como parâmetro. O objeto DeleteManagedResourceResponse contém informações sobre a remoção bem-sucedida do recurso, como o status da operação e possíveis mensagens adicionais.")
    public DeleteManagedResourceResponse deleteManagedResourceById(@PathVariable("domain") String domain, @PathVariable("resourceId") String resourceId, HttpServletRequest httpRequest) throws InvalidRequestException, DomainNotFoundException, ResourceNotFoundException, ArangoDaoException {
        DeleteManagedResourceRequest deleteRequest = new DeleteManagedResourceRequest(resourceId, domain);
        this.setUserDetails(deleteRequest);
        httpRequest.setAttribute("request", deleteRequest);
        return resourceSession.deleteManagedResource(deleteRequest);
    }

    /**
     * Lista os recursos mas como é muito esse método vai ser deprecado e
     * trocado pelo filtro que suporta paginação
     *
     * @deprecated
     * @param domain
     * @return
     * @throws InvalidRequestException
     * @throws DomainNotFoundException
     * @throws ResourceNotFoundException
     * @throws ArangoDaoException
     */
    @AuthenticatedCall(role = {"user"})
    @GetMapping(path = "/{domain}/resource", produces = "application/json")
    public TypedListResponse listManagedResource(@PathVariable("domain") String domain, HttpServletRequest httpRequest) throws InvalidRequestException, DomainNotFoundException, ResourceNotFoundException, ArangoDaoException {
        //
        // This is a Find ALL Query
        //
        ListManagedResourceRequest listRequest = new ListManagedResourceRequest(domain);
        this.setUserDetails(listRequest);
        httpRequest.setAttribute("request", listRequest);
        return resourceSession.listManagedResources(listRequest);
    }

    /**
     * Aplica um filtro
     *
     * @param filter
     * @param domain
     * @return
     * @throws ResourceNotFoundException
     * @throws GenericException
     * @throws SchemaNotFoundException
     * @throws AttributeConstraintViolationException
     * @throws ScriptRuleException
     * @throws AttributeConstraintViolationException
     * @throws DomainNotFoundException
     */
    @AuthenticatedCall(role = {"user"})
    @PostMapping(path = {"/{domain}/filter", "/{domain}/resource/filter"}, produces = "application/json", consumes = "application/json")
    @Operation(description = "Realiza uma busca por recursos gerenciados (ManagedResource) no sistema Netcompass com base em um filtro específico. Este método permite aos usuários filtrarem os recursos com critérios definidos no objeto FilterRequest passado no corpo da requisição. A busca é realizada no domínio especificado na URL da requisição (domain). O método configura o detalhe do usuário (user details) com base nas informações fornecidas no objeto FilterRequest antes de prosseguir com a busca dos recursos. O objeto FilterResponse contém a resposta da busca, incluindo uma lista de recursos que correspondem ao filtro definido. Caso a busca não retorne resultados ou ocorra algum erro durante o processo, serão lançadas exceções específicas, tais como ArangoDaoException, ResourceNotFoundException, DomainNotFoundException e InvalidRequestException. Os detalhes da requisição podem ser obtidos através do objeto HttpServletRequest passado como parâmetro.")
    public FilterResponse findManagedResourceByFilter(@RequestBody FilterRequest filter, @PathVariable("domain") String domain, HttpServletRequest httpRequest) throws ArangoDaoException, ResourceNotFoundException, DomainNotFoundException, InvalidRequestException {
        this.setUserDetails(filter);
        filter.setRequestDomain(domain);
        httpRequest.setAttribute("request", filter);
        return resourceSession.findManagedResourceByFilter(filter);
    }

    /**
     *
     * @param filter
     * @param domain
     * @param httpRequest
     * @return
     */
    @AuthenticatedCall(role = {"user"})
    @PostMapping(path = {"/{domain}/query", "/{domain}/resource/query"}, produces = "application/json", consumes = "application/json")
    @Operation(description = "Realiza uma consulta por recursos no sistema Netcompass com base em um filtro específico. Este método permite aos usuários realizarem uma consulta de recursos utilizando os critérios definidos no objeto FilterRequest passado no corpo da requisição. A consulta é realizada no domínio especificado na URL da requisição (domain). O método configura o detalhe do usuário (user details) com base nas informações fornecidas no objeto FilterRequest antes de prosseguir com a consulta dos recursos. O resultado da consulta é uma representação no formato de string que pode conter os recursos encontrados ou informações sobre a consulta. Caso a consulta não retorne resultados ou ocorra algum erro durante o processo, nenhuma exceção específica é lançada, e a resposta pode conter mensagens de erro ou informações sobre o resultado da consulta. Os detalhes da requisição podem ser obtidos através do objeto HttpServletRequest passado como parâmetro.")
    public String queryResourceByFilter(@RequestBody FilterRequest filter, @PathVariable("domain") String domain, HttpServletRequest httpRequest) {
        this.setUserDetails(filter);
        filter.setRequestDomain(domain);
        httpRequest.setAttribute("request", filter);
        return resourceSession.findManagedResource(filter);
    }

    /**
     * Atualiza um managed resource
     *
     * @param strReq
     * @param domainName
     * @param resourceId
     * @return
     * @throws DomainNotFoundException
     * @throws ResourceNotFoundException
     * @throws ArangoDaoException
     * @throws InvalidRequestException
     * @throws AttributeNotFoundException
     * @throws GenericException
     * @throws SchemaNotFoundException
     */
    @AuthenticatedCall(role = {"user"})
    @PatchMapping(path = "/{domain}/resource/{resourceId}", produces = "application/json", consumes = "application/json")
    @Operation(
            summary = "Realiza uma atualização parcial em um recurso gerenciado (ManagedResource) no sistema Netcompass.",
            description = "Este método permite aos usuários atualizarem apenas parte das informações de um recurso existente. O recurso a ser atualizado é identificado pelo seu ID (resourceId) no domínio especificado na URL da requisição (domainName). A atualização é baseada nas informações fornecidas no objeto PatchManagedResourceRequest passado no corpo da requisição. O método configura o detalhe do usuário (user details) com base nas informações fornecidas no objeto PatchManagedResourceRequest antes de prosseguir com a atualização do recurso.\n\nCampos Atualizáveis:\n- Name: Nome do recurso.\n- NodeAddress: Endereço do nó.\n- ClassName: Nome da classe do recurso.\n- OperationalStatus: Status operacional do recurso.\n- AdminStatus: Status administrativo do recurso.\n- Attributes: Atributos associados ao recurso.\n- Description: Descrição do recurso.\n- ResourceType: Tipo de recurso.\n- StructureId: ID da estrutura do recurso.\n- Category: Categoria do recurso.\n- BusinessStatus: Status comercial do recurso.\n- DiscoveryAttributes: Atributos de descoberta associados ao recurso.\n- DependentService: Serviço dependente associado ao recurso.\n- ConsumableMetric: Métrica consumível do recurso.\n- ConsumerMetric: Métrica consumidora do recurso.\n\nO objeto PatchManagedResourceResponse contém a resposta da atualização, incluindo as informações atualizadas do recurso após a operação. Caso o recurso não seja encontrado ou ocorra algum erro durante a atualização, serão lançadas exceções específicas, tais como DomainNotFoundException, ResourceNotFoundException, ArangoDaoException, InvalidRequestException, AttributeConstraintViolationException, ScriptRuleException, SchemaNotFoundException, GenericException e AttributeNotFoundException. Os detalhes da requisição podem ser obtidos através do objeto HttpServletRequest passado como parâmetro."
    )
    public PatchManagedResourceResponse patchManagedResource(@RequestBody PatchManagedResourceRequest request, @PathVariable("domain") String domainName, @PathVariable("resourceId") String resourceId, HttpServletRequest httpRequest) throws DomainNotFoundException, ResourceNotFoundException, ArangoDaoException, InvalidRequestException, AttributeConstraintViolationException, ScriptRuleException, SchemaNotFoundException, GenericException, AttributeNotFoundException {
        this.setUserDetails(request);
        request.setRequestDomain(domainName);
        request.getPayLoad().setId(resourceId);
        httpRequest.setAttribute("request", request);
        return this.resourceSession.patchManagedResource(request);
    }

    /**
     *
     * @param request
     * @param domainName
     * @param httpRequest
     * @return
     * @throws DomainNotFoundException
     * @throws ResourceNotFoundException
     * @throws ArangoDaoException
     * @throws InvalidRequestException
     * @throws AttributeConstraintViolationException
     * @throws ScriptRuleException
     * @throws SchemaNotFoundException
     * @throws GenericException
     * @throws AttributeNotFoundException
     */
    @AuthenticatedCall(role = {"user"})
    @PatchMapping(path = "/{domain}/resource", produces = "application/json", consumes = "application/json")
    @Operation(
            summary = "Realiza uma atualização parcial em um recurso gerenciado (ManagedResource) no sistema Netcompass.",
            description = "Este método permite aos usuários atualizarem apenas parte das informações de um recurso existente. O recurso a ser atualizado é identificado pelo domínio (domain) especificado na URL da requisição e pelas informações fornecidas no objeto PatchManagedResourceRequest passado no corpo da requisição. O método configura o detalhe do usuário (user details) com base nas informações fornecidas no objeto PatchManagedResourceRequest antes de prosseguir com a atualização do recurso. O objeto PatchManagedResourceResponse contém a resposta da atualização, incluindo as informações atualizadas do recurso após a operação. Caso o recurso não seja encontrado ou ocorra algum erro durante a atualização, serão lançadas exceções específicas, tais como DomainNotFoundException, ResourceNotFoundException, ArangoDaoException, InvalidRequestException, AttributeConstraintViolationException, ScriptRuleException, SchemaNotFoundException, GenericException e AttributeNotFoundException. Os detalhes da requisição podem ser obtidos através do objeto HttpServletRequest passado como parâmetro."
    )
    public PatchManagedResourceResponse patchManagedResource(@RequestBody PatchManagedResourceRequest request, @PathVariable("domain") String domainName, HttpServletRequest httpRequest) throws DomainNotFoundException, ResourceNotFoundException, ArangoDaoException, InvalidRequestException, AttributeConstraintViolationException, ScriptRuleException, SchemaNotFoundException, GenericException, AttributeNotFoundException {
        this.setUserDetails(request);
        request.setRequestDomain(domainName);
        httpRequest.setAttribute("request", request);
        return this.resourceSession.patchManagedResource(request);
    }

}
