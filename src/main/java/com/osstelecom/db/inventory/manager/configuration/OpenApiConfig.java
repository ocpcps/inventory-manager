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
package com.osstelecom.db.inventory.manager.configuration;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;

import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.OAuthFlow;
import io.swagger.v3.oas.annotations.security.OAuthFlows;
import io.swagger.v3.oas.annotations.security.OAuthScope;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.HandlerMethod;

/**
 * Configurações do OPEN API
 * @author Lucas Nishimura
 * @created 14.01.2023
 */
@Configuration
@SecurityScheme(name = "SecuredAPI", type = SecuritySchemeType.OAUTH2,
        flows = @OAuthFlows(authorizationCode = @OAuthFlow(
                authorizationUrl = "${springdoc.oAuthFlow.authorizationUrl}",
                tokenUrl = "${springdoc.oAuthFlow.tokenUrl}", scopes = {
                    @OAuthScope(name = "read", description = "IdentityPortal.API")}),
                password = @OAuthFlow(
                        tokenUrl = "${springdoc.oAuthFlow.tokenUrl}", scopes = {
                            @OAuthScope(name = "read", description = "default.scope")})))
@OpenAPIDefinition(info = @Info(
        title = "Netcompass OpenAPI",
        description = "Netcompass Open API",
        version = "0.0.2",
        contact = @Contact(name = "Lucas Nishimura", email = "lucas.nishimura@telefonica.com")),
        servers = {
            @Server(url = "${inventory-manager.api-server}"),
            @Server(url = "https://localhost:9000"),
        })
public class OpenApiConfig {

    /**
     * Adciona o Header Opcional para mostrar informações uteis para debug
     *
     * @return
     */
    @Bean
    public OperationCustomizer customGlobalHeaders() {
        return (Operation operation, HandlerMethod handlerMethod) -> {
            Parameter xshowErrors = new Parameter()
                    .in(ParameterIn.HEADER.toString())
                    .schema(new StringSchema())
                    .name("x-show-errors")
                    .description("Enable Show Errors In Response with request for debug accetp any value")
                    .required(false);
            operation.addParametersItem(xshowErrors);
            return operation;
        };
    }
}
