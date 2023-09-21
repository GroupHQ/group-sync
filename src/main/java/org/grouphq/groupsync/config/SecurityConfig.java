package org.grouphq.groupsync.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.rsocket.EnableRSocketSecurity;
import org.springframework.security.config.annotation.rsocket.RSocketSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.rsocket.core.PayloadSocketAcceptorInterceptor;
import org.springframework.security.web.server.SecurityWebFilterChain;

/**
 * Configuration for Spring Security.
 */
@Configuration
@EnableRSocketSecurity
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    SecurityWebFilterChain filterChain(ServerHttpSecurity httpSecurity) {
        return httpSecurity
            .authorizeExchange(authorizeExchangeSpec -> authorizeExchangeSpec
                .anyExchange().permitAll())
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            .build();
    }

    @Bean
    PayloadSocketAcceptorInterceptor rsocketInterceptor(RSocketSecurity socketSecurity) {
        socketSecurity.authorizePayload(authorize -> authorize
            .anyRequest().permitAll()
            .anyExchange().permitAll()
        ).simpleAuthentication(Customizer.withDefaults());

        return socketSecurity.build();
    }
}
