package com.osstelecom.db.inventory.manager.configuration;

import java.util.Collections;
import java.util.Map;

import org.springframework.boot.autoconfigure.security.oauth2.resource.OAuth2ResourceServerProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.oauth2.jwt.MappedJwtClaimSetConverter;

@Configuration
public class SecurityConfiguration extends WebSecurityConfigurerAdapter {

	@Override
	protected void configure(HttpSecurity http) throws Exception {// @formatter:off
		http.cors()
        .and()
          .authorizeRequests()
            .antMatchers(HttpMethod.GET, "/inventory/v1/**")
              .hasAuthority("SCOPE_read")
            .antMatchers(HttpMethod.PUT, "/inventory/v1/**")
              .hasAuthority("SCOPE_write")
            .antMatchers(HttpMethod.PATCH, "/inventory/v1/**")
              .hasAuthority("SCOPE_write")
            .anyRequest()
              .authenticated()
        .and()
          .oauth2ResourceServer()
            .jwt();
	}// @formatter:on

	@Bean
	JwtDecoder jwtDecoder(OAuth2ResourceServerProperties properties) {
		NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder.withJwkSetUri(properties.getJwt().getJwkSetUri()).build();
		jwtDecoder.setClaimSetConverter(new OrganizationSubClaimAdapter());
		return jwtDecoder;
	}

    public class OrganizationSubClaimAdapter implements Converter<Map<String, Object>, Map<String, Object>> {
	
        private final MappedJwtClaimSetConverter delegate = MappedJwtClaimSetConverter.withDefaults(Collections.emptyMap());
    
        public Map<String, Object> convert(Map<String, Object> claims) {
            Map<String, Object> convertedClaims = this.delegate.convert(claims);
    
            String organization = convertedClaims.get("organization") != null ? (String) convertedClaims.get("organization")
                    : "unknown";
            convertedClaims.put("organization", organization.toUpperCase());
    
            return convertedClaims;
        }
    }
}
