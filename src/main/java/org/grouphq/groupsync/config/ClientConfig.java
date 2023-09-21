package org.grouphq.groupsync.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Configures a web client bean to communicate with group service.
 */
@Configuration
public class ClientConfig {

    @Bean
    WebClient webClient(
        ClientProperties clientProperties,
        WebClient.Builder webClientBuilder
    ) {
        return webClientBuilder
            .baseUrl(clientProperties.groupServiceUri().toString())
            .build();
    }
}
