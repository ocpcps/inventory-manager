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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.osstelecom.db.inventory.manager.exception.GenericException;
import com.osstelecom.db.inventory.manager.exception.InvalidRequestException;
import com.osstelecom.db.inventory.manager.exception.SchemaNotFoundException;
import com.osstelecom.db.inventory.manager.request.CreateResourceSchemaModelRequest;
import com.osstelecom.db.inventory.manager.request.PatchResourceSchemaModelRequest;
import com.osstelecom.db.inventory.manager.response.CreateResourceSchemaModelResponse;
import com.osstelecom.db.inventory.manager.response.EmptyOkResponse;
import com.osstelecom.db.inventory.manager.response.GetSchemasResponse;
import com.osstelecom.db.inventory.manager.response.ListSchemasResponse;
import com.osstelecom.db.inventory.manager.response.PatchResourceSchemaModelResponse;
import com.osstelecom.db.inventory.manager.response.ResourceSchemaResponse;
import com.osstelecom.db.inventory.manager.response.TypedListResponse;
import com.osstelecom.db.inventory.manager.response.TypedMapResponse;
import com.osstelecom.db.inventory.manager.security.model.AuthenticatedCall;
import com.osstelecom.db.inventory.manager.session.SchemaSession;
import io.swagger.v3.oas.annotations.Operation;
import javax.servlet.http.HttpServletRequest;

import org.springframework.web.bind.annotation.RequestParam;

/**
 * Handles the Dynamic Schema Mapping and Operations
 *
 * @author Lucas Nishimura
 * @created 21.07.2022
 * @Changelog: 02-08-2022: Removed GSON fromJson
 */
@RestController
@RequestMapping("inventory/v1/schema")
public class SchemaApi extends BaseApi {

    @Autowired
    private SchemaSession schemaSession;

    @AuthenticatedCall(role = {"user"})
    @GetMapping(path = "/detail/{filter}", produces = "application/json")
    @Operation(
            summary = "Lista esquemas de recursos disponíveis",
            description = "<p>Este endpoint permite aos usuários listar esquemas de recursos armazenados, com opções adicionais para paginação, ordenação e filtragem.</p>"
            + "<ul>"
            + "<li><strong>Paginação:</strong> Os esquemas podem ser paginados fornecendo números de página e tamanho de página.</li>"
            + "<li><strong>Ordenação:</strong> Os esquemas podem ser ordenados com base em um campo específico e direção (ascendente ou descendente).</li>"
            + "<li><strong>Filtragem:</strong> Os esquemas podem ser filtrados por nome usando um texto de filtro fornecido.</li>"
            + "</ul>"
            + "<p><strong>Cache:</strong> Este método faz uso do cache para melhorar o desempenho e reduzir a carga no sistema de arquivos.</p>"
            + "<p><strong>Exceções:</strong></p>"
            + "<ul>"
            + "<li><code>SchemaNotFoundException</code>: Lançada quando um esquema específico não é encontrado.</li>"
            + "<li><code>GenericException</code>: Exceção genérica lançada para erros não especificados.</li>"
            + "</ul>"
    )
    public ListSchemasResponse listSchemas(
            @RequestParam(
                    value = "page",
                    required = false,
                    defaultValue = "0") int page,
            @RequestParam(
                    value = "size",
                    required = false,
                    defaultValue = "10") int size,
            @RequestParam(value = "sort", required = false, defaultValue = "id") String sort,
            @RequestParam(value = "direction", required = false, defaultValue = "asc") String sortDirection,
            @PathVariable("filter") String filter) throws SchemaNotFoundException, GenericException {
        return schemaSession.listSchemas(page, size, sort, sortDirection, filter);
    }

    /**
     * Retrieves the schema representation
     *
     * @param schema
     * @return
     */
    @AuthenticatedCall(role = {"user"})
    @GetMapping(produces = "application/json")
    @Operation(
            summary = "Lista esquemas de recursos disponíveis, apenas nomes")
    public GetSchemasResponse getSchemasDefinition() throws SchemaNotFoundException, GenericException {
        return schemaSession.loadSchemas();
    }

    /**
     * Retrieves the schema representation
     *
     * @param schema
     * @return
     */
    @AuthenticatedCall(role = {"user"})
    @GetMapping(path = "/{schema}", produces = "application/json")
    @Operation(
            summary = "Carrega um esquema de recurso específico",
            description = "<p>Este endpoint permite aos usuários carregar um esquema de recurso específico.</p>"
            + "<p>Primeiro, o método tenta recuperar o esquema do cache interno. Se o esquema for encontrado no cache, ele é retornado imediatamente e uma mensagem de log é gerada indicando o acerto no cache. Caso contrário, o esquema é carregado do disco e, posteriormente, armazenado no cache para consultas futuras.</p>"
            + "<p><strong>Parâmetros:</strong></p>"
            + "<ul>"
            + "<li><strong>schemaName:</strong> O nome do esquema de recurso a ser carregado.</li>"
            + "</ul>"
            + "<p><strong>Exceções:</strong></p>"
            + "<ul>"
            + "<li><code>SchemaNotFoundException</code>: Lançada se o esquema especificado não for encontrado no disco.</li>"
            + "<li><code>GenericException</code>: Exceção genérica para outros possíveis erros.</li>"
            + "</ul>"
    )
    public ResourceSchemaResponse getSchemaDefinition(@PathVariable("schema") String schema, HttpServletRequest httpRequest)
            throws GenericException, SchemaNotFoundException {
        httpRequest.setAttribute("request", schema);
        return schemaSession.loadSchemaByName(schema);
    }

    /**
     * Retrieves the schema representation
     *
     * @param schema
     * @return
     */
    @AuthenticatedCall(role = {"user"})
    @GetMapping(path = "/filter/{filter}", produces = "application/json")
    public GetSchemasResponse getSchemaByFilter(@PathVariable("filter") String filter, HttpServletRequest httpRequest)
            throws GenericException, SchemaNotFoundException {
        httpRequest.setAttribute("request", filter);
        return schemaSession.getSchemaByFilter(filter);
    }

    @AuthenticatedCall(role = {"user"})
    @GetMapping(path = "/types", produces = "application/json")
    public TypedListResponse getSupportedTypes()
            throws GenericException, SchemaNotFoundException {
        TypedListResponse response = new TypedListResponse(schemaSession.validAttributesType());
        return response;
    }

    /**
     * Update the schema representation
     *
     * @param schemaName
     * @return
     */
    @AuthenticatedCall(role = {"user"})
    @PatchMapping(path = "/{schema}", produces = "application/json")
    @Operation(
            summary = "Atualiza parcialmente um esquema de recurso existente",
            description = "<p>Este endpoint permite realizar atualizações parciais em um esquema de recurso existente.</p>"
            + "<p><strong>Campos atualizáveis:</strong></p>"
            + "<ul>"
            + "<li><strong>fromSchema:</strong> A classe pai do esquema. Se houver uma mudança, a classe pai é atualizada.</li>"
            + "<li><strong>allowAll:</strong> Determina se todas as operações são permitidas.</li>"
            + "<li><strong>graphItemColor:</strong> Cor do item no gráfico.</li>"
            + "<li><strong>attributes:</strong> Uma lista de atributos que podem ser removidos, atualizados ou adicionados.</li>"
            + "</ul>"
            + "<p><strong>Fluxo de Processamento:</strong></p>"
            + "<ol>"
            + "<li>Verificação da carga útil da requisição.</li>"
            + "<li>Carregamento do esquema original com base no nome do esquema.</li>"
            + "<li>Atualização de campos individuais e atributos.</li>"
            + "<li>Persistência do esquema atualizado e limpeza do cache.</li>"
            + "<li>Envio de notificações sobre a atualização.</li>"
            + "<li>Retorno do esquema atualizado como resposta.</li>"
            + "</ol>"
            + "<p><strong>Exceções:</strong></p>"
            + "<ul>"
            + "<li><code>InvalidRequestException</code>: Lançada quando a requisição contém dados inválidos ou insuficientes.</li>"
            + "<li><code>GenericException</code>: Exceção genérica para outros possíveis erros.</li>"
            + "<li><code>SchemaNotFoundException</code>: Lançada quando o esquema solicitado não é encontrado.</li>"
            + "</ul>")
    public PatchResourceSchemaModelResponse patchSchameDefinition(@PathVariable("schema") String schemaName,
            @RequestBody PatchResourceSchemaModelRequest request, HttpServletRequest httpRequest)
            throws GenericException, SchemaNotFoundException, InvalidRequestException {
        request.getPayLoad().setSchemaName(schemaName);
        this.setUserDetails(request);
        httpRequest.setAttribute("request", request);
        return schemaSession.patchSchemaModel(request);
    }

    /**
     * Creates a new ResourceSchemaModel
     *
     * @param reqBody
     * @return
     */
    @AuthenticatedCall(role = {"user", "operator"})
    @PostMapping(path = "/", produces = "application/json", consumes = "application/json")
    @Operation(
            summary = "Cria um novo modelo de esquema de recurso",
            description = "<p>Este endpoint é responsável por criar um novo modelo de esquema de recurso, garantindo que ele siga certas validações e regras específicas.</p>"
            + "<ul>"
            + "<li>Realiza várias validações, incluindo nome do esquema, existência prévia, formatação dos atributos, entre outras.</li>"
            + "<li>Após as validações, o modelo é salvo no disco.</li>"
            + "<li>Retorna o modelo de esquema de recurso criado e validado como resposta.</li>"
            + "</ul>"
            + "<p><strong>Exceções:</strong></p>"
            + "<ul>"
            + "<li><code>GenericException</code>: Exceção genérica não especificada no método.</li>"
            + "<li><code>SchemaNotFoundException</code>: Lançada quando um esquema específico não é encontrado.</li>"
            + "<li><code>InvalidRequestException</code>: Lançada quando uma ou mais validações no pedido falham.</li>"
            + "</ul>"
    )
    public CreateResourceSchemaModelResponse createSchema(@RequestBody CreateResourceSchemaModelRequest request, HttpServletRequest httpRequest)
            throws GenericException, SchemaNotFoundException, InvalidRequestException {
        this.setUserDetails(request);
        httpRequest.setAttribute("request", request);
        return this.schemaSession.createResourceSchemaModel(request);
    }

    /**
     * clears the schema cache
     *
     * @return
     */
    @AuthenticatedCall(role = {"user", "operator"})
    @PostMapping(path = "/cache/clear", produces = "application/json")
    public EmptyOkResponse clearCachedSchema() {
        this.schemaSession.clearSchemaCache();
        return new EmptyOkResponse();
    }

    /**
     * list cached entries
     *
     * @return
     */
    @AuthenticatedCall(role = {"user", "operator"})
    @GetMapping(path = "/cache", produces = "application/json")
    public TypedMapResponse getCachedSchemas() {
        return new TypedMapResponse(this.schemaSession.getCachedSchemas());
    }

}
