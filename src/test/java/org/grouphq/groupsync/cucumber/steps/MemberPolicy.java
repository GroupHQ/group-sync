package org.grouphq.groupsync.cucumber.steps;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.io.IOException;
import org.grouphq.groupsync.config.SecurityConfig;
import org.grouphq.groupsync.groupservice.domain.groups.Group;
import org.grouphq.groupsync.groupservice.domain.members.Member;
import org.grouphq.groupsync.groupservice.domain.outbox.OutboxEvent;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.binder.test.InputDestination;
import org.springframework.cloud.stream.binder.test.OutputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import({SecurityConfig.class, TestChannelBinderConfiguration.class})
@Tag("AcceptanceTest")
public class MemberPolicy {
    public static final String USERNAME = "username";
    private static Member member;
    private static Group group;
    private static OutboxEvent event;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private InputDestination inputDestination;

    @Autowired
    private OutputDestination outputDestination;

    @Value("${spring.cloud.stream.bindings.groupJoinRequests-in-0.destination}")
    private String joinHandlerDestination;

    @Value("${spring.cloud.stream.bindings.groupLeaveRequests-in-0.destination}")
    private String leaveHandlerDestination;

    @Value("${spring.cloud.stream.bindings.processedEvents-out-0.destination}")
    private String eventPublisherDestination;

    @Given("there is an active group")
    public void thereIsAnActiveGroup() {
    }

    @When("I try to join the group")
    public void iTryToJoinTheGroup() {
    }

    @Then("I should be a member of the group")
    public void iShouldBeAMemberOfTheGroup() {
    }

    @And("the group's current member size should increase by one")
    public void theGroupSCurrentMemberSizeShouldIncreaseByOne() {
    }

    @Given("I am in an active group")
    public void iAmInAnActiveGroup() throws IOException {
    }

    @When("I try to leave the group")
    public void iTryToLeaveTheGroup() throws IOException {
    }

    @Then("I should no longer be an active member of that group")
    public void iShouldNoLongerBeAnActiveMemberOfThatGroup() {
    }

    @And("the group's current member size should decrease by one")
    public void theGroupSCurrentMemberSizeShouldDecreaseByOne() {
    }

    @And("I am a member of the group")
    public void iAmAMemberOfTheGroup() {
    }

    @Then("I should not be added to the group again")
    public void iShouldNotBeAddedToTheGroupAgain() {
    }

    @Given("there are multiple active groups")
    public void thereAreMultipleActiveGroups() {
    }

    @And("I am a member of one group")
    public void iAmAMemberOfOneGroup() {
    }

    @When("I try to join a second group")
    public void iTryToJoinASecondGroup() {
    }

    @Then("I should not be added to the second group")
    public void iShouldNotBeAddedToTheSecondGroup() {
    }

    @Given("I know the ID of another member in an active group")
    public void iKnowTheIDOfAnotherMemberInAnActiveGroup() {
    }

    @When("I try to request that member leave the group")
    public void iTryToRequestThatMemberLeaveTheGroup() throws IOException {
    }

    @Then("that member should still be in the group")
    public void thatMemberShouldStillBeInTheGroup() {
    }
}
