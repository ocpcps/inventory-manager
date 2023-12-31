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
package com.osstelecom.db.inventory.manager.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Configuração da Autenticação da API
 *
 * @author Lucas Nishimura
 */
@Configuration
public class SecurityConfiguration {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.cors()
                .and().csrf().disable()
                .authorizeRequests()
                .antMatchers(HttpMethod.GET, "/inventory/v1/**")
                .hasAuthority("SCOPE_read")
                .antMatchers(HttpMethod.PUT, "/inventory/v1/**")
                .hasAuthority("SCOPE_write")
                .antMatchers(HttpMethod.PATCH, "/inventory/v1/**")
                .hasAuthority("SCOPE_write")
                .antMatchers(HttpMethod.POST, "/inventory/v1/**")
                .hasAuthority("SCOPE_write")
                .antMatchers(HttpMethod.DELETE, "/inventory/v1/**")
                .hasAuthority("SCOPE_write")
                .antMatchers(HttpMethod.POST, "/topology/v1/**")
                .hasAuthority("SCOPE_write")
                .antMatchers(HttpMethod.GET, "/topology/v1/**")
                .hasAuthority("SCOPE_write")
                .antMatchers(HttpMethod.PATCH, "/topology/v1/**")
                .hasAuthority("SCOPE_write")
                .antMatchers(HttpMethod.DELETE, "/topology/v1/**")
                .hasAuthority("SCOPE_write")
                .anyRequest()
                .anonymous()
                .and()
                .oauth2ResourceServer()
                .jwt();

        return http.build();
    }

}
