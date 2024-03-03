package org.grouphq.groupsync.group.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import io.rsocket.metadata.WellKnownMimeType;
import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.grouphq.groupsync.GroupTestUtility;
import org.grouphq.groupsync.config.ClientProperties;
import org.grouphq.groupsync.group.domain.PublicOutboxEvent;
import org.grouphq.groupsync.group.sync.GroupFetchService;
import org.grouphq.groupsync.groupservice.domain.members.Member;
import org.grouphq.groupsync.groupservice.domain.outbox.OutboxEvent;
import org.grouphq.groupsync.groupservice.domain.outbox.enums.EventStatus;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.cloud.stream.binder.test.InputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.security.rsocket.metadata.UsernamePasswordMetadata;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestChannelBinderConfiguration.class)
@Tag("IntegrationTest")
class GroupSyncSocketIntegrationTest {

    @SpyBean
    private ClientProperties clientProperties;

    @MockBean
    private GroupFetchService groupFetchService;

    private static RSocketRequester requester;

    @Value("${spring.cloud.stream.bindings.processedEvents-in-0.destination}")
    private String eventDestination;

    private static final String USER_ID = UUID.randomUUID().toString();

    @BeforeEach
    public void setup(@Autowired RSocketRequester.Builder builder,
                      @LocalServerPort Integer port) {
        final URI url = URI.create("ws://localhost:" + port + "/api/rsocket");

        final UsernamePasswordMetadata credentials =
            new UsernamePasswordMetadata(USER_ID, "password");

        final MimeType authenticationMimeType = MimeTypeUtils.parseMimeType(
                WellKnownMimeType.MESSAGE_RSOCKET_AUTHENTICATION.getString());

        requester = builder
            .setupMetadata(credentials, authenticationMimeType)
            .websocket(url);
    }

    @AfterAll
    public static void tearDownOnce() {
        requester.rsocketClient().dispose();
    }

    @Test
    @DisplayName("Test RSocket integration for pings")
    void testPing() {
        StepVerifier.create(requester.route("groups.ping").retrieveMono(Boolean.class))
            .expectNext(true)
            .verifyComplete();
    }

    /**
     * Note the doOnSubscribe hook used here to cause the sink to emit events for the flux to
     * consume. We cannot use the then() operator to create the signals, since that is not
     * guaranteed to run in the order specified when used in a StepVerifier.
     *
     * @see <a href="https://github.com/reactor/reactor-core/issues/2139#issuecomment-624654710">
     *     Related Issue</a>
     */
    @Test
    @DisplayName("Test RSocket integration for streaming successful outbox events to all users")
    void testGetPublicOutboxEventUpdates(@Autowired InputDestination inputDestination) {
        given(groupFetchService.getGroupsAsEvents()).willReturn(Flux.empty());

        final List<OutboxEvent> outboxEvents = List.of(
            GroupTestUtility.generateOutboxEvent(USER_ID, EventStatus.SUCCESSFUL),
            GroupTestUtility.generateOutboxEvent(USER_ID, EventStatus.SUCCESSFUL),
            GroupTestUtility.generateOutboxEvent("Some other user 1", EventStatus.SUCCESSFUL),
            GroupTestUtility.generateOutboxEvent(USER_ID, EventStatus.FAILED),
            GroupTestUtility.generateOutboxEvent(USER_ID, EventStatus.FAILED),
            GroupTestUtility.generateOutboxEvent("Some other user 2", EventStatus.FAILED)
        );

        final Set<UUID> outboxEventIds = outboxEvents.stream()
            .map(OutboxEvent::getEventId)
            .collect(Collectors.toSet());

        final Flux<PublicOutboxEvent> groupUpdatesStream = requester
            .route("groups.updates.all")
            .retrieveFlux(PublicOutboxEvent.class)
            .doOnSubscribe(subscription -> {
                for (final OutboxEvent outboxEvent : outboxEvents) {
                    inputDestination.send(new GenericMessage<>(outboxEvent), eventDestination);
                }
            });

        // Events we expect to receive back as public
        final List<PublicOutboxEvent> successfulPublicEvents = outboxEvents.stream()
            .filter(event -> event.getEventStatus() == EventStatus.SUCCESSFUL)
            .map(PublicOutboxEvent::convertOutboxEvent)
            .toList();

        final List<PublicOutboxEvent> failedPublicEvents = outboxEvents.stream()
            .filter(event -> event.getEventStatus() == EventStatus.FAILED)
            .map(PublicOutboxEvent::convertOutboxEvent)
            .toList();

        StepVerifier.create(groupUpdatesStream.filter(event -> outboxEventIds.contains(event.eventId())))
            .recordWith(ArrayList::new)
            .expectNextCount(successfulPublicEvents.size())
            .consumeRecordedWith(received -> {
                assertThat(received).containsAll(successfulPublicEvents);
                assertThat(received).doesNotContainAnyElementsOf(failedPublicEvents);
            })
            .thenCancel()
            .verify();
    }

    @Test
    @DisplayName("Times out and retries appropriately when group service does not respond with groups")
    void whenGroupServiceTimesOutOnGroupsFetchThenReturnException() {
        given(clientProperties.getGroupsTimeoutMilliseconds()).willReturn(1L);
        given(groupFetchService.getGroupsAsEvents()).willReturn(Flux.never());

        final Flux<PublicOutboxEvent> groupUpdatesStream = requester
            .route("groups.updates.all")
            .retrieveFlux(PublicOutboxEvent.class);

        StepVerifier.create(groupUpdatesStream)
            .expectError()
            .verify(Duration.ofSeconds(5));
    }

    @Test
    @DisplayName("Test RSocket integration for streaming outbox events belonging to a user")
    void testGetEventOwnerOutboxEventUpdates(@Autowired InputDestination inputDestination) {
        final List<OutboxEvent> outboxEvents = List.of(
            GroupTestUtility.generateOutboxEvent(USER_ID, EventStatus.SUCCESSFUL),
            GroupTestUtility.generateOutboxEvent(USER_ID, EventStatus.SUCCESSFUL),
            GroupTestUtility.generateOutboxEvent("Some other user", EventStatus.SUCCESSFUL),
            GroupTestUtility.generateOutboxEvent(USER_ID, EventStatus.FAILED),
            GroupTestUtility.generateOutboxEvent(USER_ID, EventStatus.FAILED),
            GroupTestUtility.generateOutboxEvent("Some other user", EventStatus.FAILED)
        );

        final Flux<OutboxEvent> groupUpdatesFailedStream = requester
            .route("groups.updates.user")
            .retrieveFlux(OutboxEvent.class)
            .doOnSubscribe(subscription -> {
                for (final OutboxEvent outboxEvent : outboxEvents) {
                    inputDestination.send(new GenericMessage<>(outboxEvent), eventDestination);
                }
            });

        final Set<UUID> outboxEventIds = outboxEvents.stream()
            .map(OutboxEvent::getEventId)
            .collect(Collectors.toSet());

        final List<OutboxEvent> userEvents = outboxEvents.stream()
            .filter(event -> USER_ID.equals(event.getWebsocketId()))
            .toList();

        final List<OutboxEvent> nonUserEvents = outboxEvents.stream()
            .filter(event -> !USER_ID.equals(event.getWebsocketId()))
            .toList();

        StepVerifier.create(groupUpdatesFailedStream.filter(event -> outboxEventIds.contains(event.getEventId())))
            .recordWith(ArrayList::new)
            .expectNextCount(userEvents.size())
            .consumeRecordedWith(received -> {
                assertThat(received).containsAll(userEvents);
                assertThat(received).doesNotContainAnyElementsOf(nonUserEvents);
            })
            .thenCancel()
            .verify();
    }

    @Test
    @DisplayName("Test RSocket integration for getting a user's member details")
    void testGetUsersMemberDetails() {
        final Member member = Member.of(UUID.fromString(USER_ID), "user", 1L);
        given(groupFetchService.getMyMember(member.websocketId().toString())).willReturn(Mono.just(member));

        final Mono<Member> memberDetails = requester
            .route("groups.user.member")
            .retrieveMono(Member.class);

        StepVerifier.create(memberDetails)
            .expectNext(member)
            .verifyComplete();
    }
}
