package org.grouphq.groupsync.cucumber;

import io.cucumber.spring.CucumberContextConfiguration;
import org.grouphq.groupsync.group.sync.GroupFetchService;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

/**
 * Configuration for linking Cucumber with the Spring context.
 */
@CucumberContextConfiguration
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
public class CucumberSpringConfiguration {

    /**
     * The MockBean annotation only works in the @CucumberContextConfiguration annotated class.
     *
     * @see <a href="https://stackoverflow.com/a/75922258">See this link for more details.</a>
     */
    @MockBean
    private GroupFetchService groupFetchService;
}
