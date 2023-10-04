package org.grouphq.groupsync.cucumber;

import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.TestPropertySource;

/**
 * Configuration for linking Cucumber with the Spring context.
 */
@CucumberContextConfiguration
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Profile("production")
@TestPropertySource(properties =
    {"grouphq.group-service.url=http://localhost:9001", "spring.cloud.config.fail-fast=true"})
@AutoConfigureWebTestClient
public class CucumberSpringConfiguration {
}
