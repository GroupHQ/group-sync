package org.grouphq.groupsync.cucumber.steps;

import com.github.javafaker.Faker;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.rsocket.metadata.WellKnownMimeType;
import org.grouphq.groupsync.GroupTestUtility;
import org.grouphq.groupsync.config.SecurityConfig;
import org.grouphq.groupsync.groupservice.domain.groups.Group;
import org.grouphq.groupsync.groupservice.domain.groups.GroupStatus;
import org.grouphq.groupsync.groupservice.domain.members.Member;
import org.grouphq.groupsync.groupservice.domain.outbox.OutboxEvent;
import org.grouphq.groupsync.groupservice.domain.outbox.enums.EventStatus;
import org.grouphq.groupsync.groupservice.domain.outbox.enums.EventType;
import org.grouphq.groupsync.groupservice.event.daos.requestevent.RequestEvent;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.security.rsocket.metadata.UsernamePasswordMetadata;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.net.URI;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@AutoConfigureWebTestClient
@Import({SecurityConfig.class})
@Tag("AcceptanceTest")
public class MemberPolicy {

    public static final String GROUPS_ENDPOINT = "/api/groups";
    public static final String AUTHORIZATION = "Authorization";
    public static final String HTTP_BASIC_PREFIX = "Basic ";
    private static List<Group> groups = new ArrayList<>();

    private static RSocketRequester requester;


    private static String username;
    private static Member member;
    private static Group group;
    private static RequestEvent requestEvent;

    private static final Deque<OutboxEvent> OUTBOX_EVENTS = new ArrayDeque<>();

    private static OutboxEvent event;

    private String userId;

    private String httpBasicCredentialsEncoded;

    @Autowired
    private RSocketRequester.Builder builder;

    @LocalServerPort
    private Integer port;

    @Autowired
    private WebTestClient webTestClient;

    @Before("@MemberPolicy")
    public void setupOnce() {
        final URI url = URI.create("ws://localhost:" + port + "/api/rsocket");

        userId = UUID.randomUUID().toString();

        final String httpBasicCredentialsRaw = userId + ":password";
        httpBasicCredentialsEncoded =
            Base64.getEncoder().encodeToString(httpBasicCredentialsRaw.getBytes());

        final UsernamePasswordMetadata credentials =
            new UsernamePasswordMetadata(userId, "password");

        final MimeType authenticationMimeType =
            MimeTypeUtils.parseMimeType(WellKnownMimeType.MESSAGE_RSOCKET_AUTHENTICATION.getString());

        requester = builder
            .setupMetadata(credentials, authenticationMimeType)
            .websocket(url);

        requester
            .route("groups.updates.user")
            .retrieveFlux(OutboxEvent.class)
            .doOnNext(OUTBOX_EVENTS::add)
            .subscribe();

        webTestClient
            .get()
            .uri( "/api/groups")
            .header(AUTHORIZATION, HTTP_BASIC_PREFIX + httpBasicCredentialsEncoded)
            .exchange()
            .expectStatus().is2xxSuccessful()
            .expectBodyList(Group.class)
            .value(groups -> MemberPolicy.groups = groups);

        username = new Faker().name().firstName();
    }

    @After("@MemberPolicy")
    public static void tearDownOnce() {
        requester.rsocketClient().dispose();
    }

    @Given("there is an active group")
    public void thereIsAnActiveGroup() {
        group = groups.stream()
            .filter(g -> g.status().equals(GroupStatus.ACTIVE))
            .findFirst()
            .orElseThrow();
    }

    @When("I try to join the group")
    public void iTryToJoinTheGroup() {
        requestEvent = GroupTestUtility.generateGroupJoinRequestEvent(userId, username, group.id());

        OUTBOX_EVENTS.clear();

        final Mono<Void> groupJoinRequest = requester
            .route("groups.join")
            .data(requestEvent)
            .retrieveMono(Void.class);

        StepVerifier.create(groupJoinRequest)
            .expectComplete()
            .log()
            .verify(Duration.ofSeconds(1));

        await().atMost(5, TimeUnit.SECONDS).until(() -> !OUTBOX_EVENTS.isEmpty());
    }

    @Then("I should receive an event confirming my membership")
    public void iShouldBeAMemberOfTheGroup() {
        event = OUTBOX_EVENTS.getLast();

        assertThat(event).isNotNull();

        assertThat(event).satisfies(joinEvent -> {
            assertThat(joinEvent.getEventId()).isEqualTo(requestEvent.getEventId());
            assertThat(joinEvent.getAggregateId()).isEqualTo(requestEvent.getAggregateId());
            assertThat(joinEvent.getWebsocketId()).isEqualTo(requestEvent.getWebsocketId());
            assertThat(joinEvent.getEventType()).isEqualTo(EventType.MEMBER_JOINED);
            assertThat(joinEvent.getEventStatus()).isEqualTo(EventStatus.SUCCESSFUL);
        });
    }

    @And("the group's current member size should increase by one")
    public void theGroupSCurrentMemberSizeShouldIncreaseByOne() {
        webTestClient
            .get()
            .uri(GROUPS_ENDPOINT)
            .header(AUTHORIZATION, HTTP_BASIC_PREFIX + httpBasicCredentialsEncoded)
            .exchange()
            .expectStatus().is2xxSuccessful()
            .expectBodyList(Group.class)
            .value(groups -> {
                final var targetGroup =
                    groups.stream().filter(g -> g.id().equals(group.id())).findFirst().orElseThrow();
                assertThat(targetGroup.members().size()).isEqualTo(group.members().size() + 1);
            });
    }

    @When("I try to leave the group")
    public void iTryToLeaveTheGroup() {
        requestEvent = GroupTestUtility.generateGroupLeaveRequestEvent(userId, group.id(), member.id());

        OUTBOX_EVENTS.clear();

        final Mono<Void> groupLeaveRequest = requester
            .route("groups.leave")
            .data(requestEvent)
            .retrieveMono(Void.class);

        StepVerifier.create(groupLeaveRequest)
            .expectComplete()
            .log()
            .verify(Duration.ofSeconds(1));

        await().atMost(5, TimeUnit.SECONDS).until(() -> !OUTBOX_EVENTS.isEmpty());
    }

    @Then("I should no longer be an active member of that group")
    public void iShouldNoLongerBeAnActiveMemberOfThatGroup() {
        webTestClient
            .get()
            .uri(GROUPS_ENDPOINT)
            .header(AUTHORIZATION, HTTP_BASIC_PREFIX + httpBasicCredentialsEncoded)
            .exchange()
            .expectStatus().is2xxSuccessful()
            .expectBodyList(Group.class)
            .value(groups -> {
                final var targetGroup =
                    groups.stream().filter(g -> g.id().equals(group.id())).findFirst().orElseThrow();
                assertThat(targetGroup.members())
                    .noneMatch(memberInGroup -> memberInGroup.getGroupId().equals(member.id()));
            });
    }

    @And("I am a member of the group")
    public void iAmAMemberOfTheGroup() {
        requestEvent = GroupTestUtility.generateGroupJoinRequestEvent(userId, username, group.id());

        OUTBOX_EVENTS.clear();

        final Mono<Void> groupJoinRequest = requester
            .route("groups.join")
            .data(requestEvent)
            .retrieveMono(Void.class);

        StepVerifier.create(groupJoinRequest)
            .expectComplete()
            .log()
            .verify(Duration.ofSeconds(1));

        await().atMost(5, TimeUnit.SECONDS).until(() -> !OUTBOX_EVENTS.isEmpty());

        event = OUTBOX_EVENTS.getLast();
        member = (Member) event.getEventData();

        webTestClient
            .get()
            .uri(GROUPS_ENDPOINT)
            .header(AUTHORIZATION, HTTP_BASIC_PREFIX + httpBasicCredentialsEncoded)
            .exchange()
            .expectStatus()
            .is2xxSuccessful()
            .expectBodyList(Group.class)
            .value(groups -> {
                final var targetGroup =
                    groups.stream().filter(g -> g.id().equals(group.id())).findFirst().orElseThrow();
                assertThat(targetGroup.members().size()).isEqualTo(group.members().size() + 1);
            });
    }

    @Then("I should not be added to the group again")
    public void iShouldNotBeAddedToTheGroupAgain() {
        webTestClient
            .get()
            .uri(GROUPS_ENDPOINT)
            .header(AUTHORIZATION, HTTP_BASIC_PREFIX + httpBasicCredentialsEncoded)
            .exchange()
            .expectStatus().is2xxSuccessful()
            .expectBodyList(Group.class)
            .value(groups -> {
                final var targetGroup =
                    groups.stream().filter(g -> g.id().equals(group.id())).findFirst().orElseThrow();
                assertThat(targetGroup.members()).satisfiesOnlyOnce(
                    memberInList -> assertThat(memberInList.getGroupId()).isEqualTo(member.id()));
            });
    }

    @And("there is a second active group")
    public void iAmAMemberOfOneGroup() {
        assertThat(groups).hasSizeGreaterThanOrEqualTo(2);
        group = groups.stream()
            .filter(secondGroup ->
                    secondGroup.status().equals(GroupStatus.ACTIVE)
                            && !secondGroup.id().equals(group.id()))
            .findFirst()
            .orElseThrow();
    }

    @Then("I should not be added to the second group")
    public void iShouldNotBeAddedToTheSecondGroup() {
        webTestClient
            .get()
            .uri(GROUPS_ENDPOINT)
            .header(AUTHORIZATION, HTTP_BASIC_PREFIX + httpBasicCredentialsEncoded)
            .exchange()
            .expectStatus().is2xxSuccessful()
            .expectBodyList(Group.class)
            .value(groups -> {
                final Group targetGroup =
                    groups.stream().filter(g -> g.id().equals(group.id())).findFirst().orElseThrow();
                assertThat(targetGroup.members()).noneSatisfy(
                    memberInList -> assertThat(memberInList.getUsername()).isEqualTo(member.username()));
            });
    }

    @When("another user tries to remove me from the group")
    public void anotherUserTriesToRemoveMeFromTheGroup() {
        final String otherUsersId = UUID.randomUUID().toString();
        requestEvent = GroupTestUtility.generateGroupLeaveRequestEvent(
                otherUsersId, group.id(), member.id());

        final Mono<Void> groupLeaveRequest = requester
            .route("groups.leave")
            .data(requestEvent)
            .retrieveMono(Void.class);

        StepVerifier.create(groupLeaveRequest)
            .expectComplete()
            .log()
            .verify(Duration.ofSeconds(1));
    }

    @Then("I should be in the group only once")
    public void iShouldStillBeInTheGroup() {
        webTestClient
            .get()
            .uri(GROUPS_ENDPOINT)
            .header(AUTHORIZATION, HTTP_BASIC_PREFIX + httpBasicCredentialsEncoded)
            .exchange()
            .expectStatus().is2xxSuccessful()
            .expectBodyList(Group.class)
            .value(groups -> {
                final Group targetGroup =
                    groups.stream().filter(g -> g.id().equals(group.id())).findFirst().orElseThrow();
                assertThat(targetGroup.members()).satisfiesOnlyOnce(
                    memberInList -> assertThat(memberInList.getGroupId()).isEqualTo(member.id()));
            });
    }

}
