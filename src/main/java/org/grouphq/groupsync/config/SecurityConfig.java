package org.grouphq.groupsync.config;

import java.util.Collections;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.rsocket.messaging.RSocketStrategiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.rsocket.EnableRSocketSecurity;
import org.springframework.security.config.annotation.rsocket.RSocketSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.rsocket.core.PayloadSocketAcceptorInterceptor;
import org.springframework.security.rsocket.metadata.SimpleAuthenticationEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import reactor.core.publisher.Mono;

/**
 * Configuration for Spring Security.
 */
@Configuration
@Slf4j
@EnableRSocketSecurity
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    SecurityWebFilterChain filterChain(ServerHttpSecurity httpSecurity) {
        return httpSecurity
            .authorizeExchange(authorizeExchangeSpec -> authorizeExchangeSpec
                .anyExchange().permitAll())
            .httpBasic(Customizer.withDefaults())
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            .build();
    }

    @Bean
    public ReactiveAuthenticationManager reactiveAuthenticationManager() {
        return authentication -> {
            final String username = authentication.getName();
            /* after testing, check that all usernames are UUIDs
            try {
                UUID tryConversion = UUID.fromString(username);
            } catch (IllegalArgumentException e) {
                return Mono.error(new IllegalArgumentException("Invalid username: " + username));
            }

            String uuid = UUID.randomUUID().toString();
            */
            log.info("Creating Authentication Token with Username: {}", username);

            return Mono.just(new UsernamePasswordAuthenticationToken(
                username,
                "dummy",
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))
            ));
        };
    }

    @Bean
    RSocketStrategiesCustomizer strategiesCustomizer() {
        return strategies -> strategies.encoder(new SimpleAuthenticationEncoder());
    }

    @Bean
    PayloadSocketAcceptorInterceptor rsocketInterceptor(RSocketSecurity socketSecurity) {
        socketSecurity.authorizePayload(authorize -> authorize
            .anyExchange().authenticated())
            .simpleAuthentication(Customizer.withDefaults());

        return socketSecurity.build();
    }
}
