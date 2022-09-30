package com.osstelecom.db.inventory.manager.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfiguration {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.cors()
                .and()
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
                .anyRequest()
                .authenticated()
                .and()
                .oauth2ResourceServer()
                //                .jwt().and().authenticationEntryPoint(new BearerTokenAuthenticationEntryPoint());
                .jwt();

        return http.build();
    }

}
