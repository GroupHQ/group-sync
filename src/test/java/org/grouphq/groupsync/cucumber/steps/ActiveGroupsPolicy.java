package org.grouphq.groupsync.cucumber.steps;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.time.Duration;
import org.grouphq.groupsync.group.sync.GroupFetchService;
import org.grouphq.groupsync.groupservice.domain.groups.Group;
import org.grouphq.groupsync.groupservice.domain.groups.GroupStatus;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@SpringBootTest
@AutoConfigureWebTestClient
public class ActiveGroupsPolicy {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private GroupFetchService groupFetchService;

    private WebTestClient.ListBodySpec<Group> groupResponse;

    private Group[] groups;

    @Given("there are active groups")
    public void thereAreActiveGroups() {
        groups = new Group[] {
            Group.of("Example Title", "Example Description", 10,
                1, GroupStatus.ACTIVE),
            Group.of("Example Title", "Example Description", 5,
                2, GroupStatus.ACTIVE)
        };

        given(groupFetchService.getGroups()).willReturn(Flux.just(groups));
    }

    @When("I request groups")
    public void iRequestGroups() {
        groupResponse = webTestClient
            .get()
            .uri("/groups")
            .exchange()
            .expectStatus().is2xxSuccessful()
            .expectBodyList(Group.class);

        verify(groupFetchService).getGroups();
    }

    @Then("I should be given a list of active groups")
    public void iShouldBeGivenAListOfActiveGroups() {
        groupResponse.value(groups -> {
            assertThat(groups).isNotEmpty();
            assertThat(groups).allMatch(group ->
                    group.status().equals(GroupStatus.ACTIVE),
                "All groups received should be active");
        });
    }

    @Given("any time")
    public void anyTime() {
        // such as now
    }

    @Then("I should be given a list of at least {int} active groups")
    public void iShouldBeGivenAListOfAtLeastActiveGroups(int activeGroupsNeeded) {
    }
}
