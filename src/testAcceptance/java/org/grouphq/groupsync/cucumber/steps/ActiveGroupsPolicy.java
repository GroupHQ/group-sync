package org.grouphq.groupsync.cucumber.steps;

import static org.assertj.core.api.Assertions.assertThat;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.grouphq.groupsync.groupservice.domain.groups.Group;
import org.grouphq.groupsync.groupservice.domain.groups.GroupStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest
@AutoConfigureWebTestClient
public class ActiveGroupsPolicy {

    @Autowired
    private WebTestClient webTestClient;

    private WebTestClient.ListBodySpec<Group> groupResponse;

    @Given("any time")
    public void anyTime() {
        // such as now
    }

    @When("I request groups")
    public void iRequestGroups() {
        groupResponse = webTestClient
            .get()
            .uri("/groups")
            .exchange()
            .expectStatus().is2xxSuccessful()
            .expectBodyList(Group.class);
    }

    @Then("I should be given a list of at least {int} active groups")
    public void iShouldBeGivenAListOfAtLeastActiveGroups(int activeGroupsNeeded) {
        groupResponse.value(groups -> {
            assertThat(groups).isNotEmpty();
            assertThat(groups).hasSizeGreaterThanOrEqualTo(activeGroupsNeeded);
            assertThat(groups).allMatch(group ->
                    group.status().equals(GroupStatus.ACTIVE),
                "All groups received should be active");
        });
    }
}
