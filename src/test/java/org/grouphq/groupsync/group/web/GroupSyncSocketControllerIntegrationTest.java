package org.grouphq.groupsync.group.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import org.grouphq.groupsync.groupservice.domain.groups.GroupStatus;
import org.grouphq.groupsync.groupservice.domain.outbox.OutboxEvent;
import org.grouphq.groupsync.groupservice.event.daos.GroupCreateRequestEvent;
import org.grouphq.groupsync.groupservice.event.daos.GroupJoinRequestEvent;
import org.grouphq.groupsync.groupservice.event.daos.GroupLeaveRequestEvent;
import org.grouphq.groupsync.groupservice.event.daos.GroupStatusRequestEvent;
import org.grouphq.groupsync.testutility.GroupTestUtility;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.cloud.stream.binder.test.InputDestination;
import org.springframework.cloud.stream.binder.test.OutputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.Message;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.messaging.support.GenericMessage;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestChannelBinderConfiguration.class)
@Tag("IntegrationTest")
class GroupSyncSocketControllerIntegrationTest {

    private static RSocketRequester requester;

    @Autowired
    private OutputDestination outputDestination;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${spring.cloud.stream.bindings.processedEvents-in-0.destination}")
    private String eventDestination;

    @Value("${spring.cloud.stream.bindings.groupCreateRequests-out-0.destination}")
    private String groupCreateRequestDestination;

    @Value("${spring.cloud.stream.bindings.groupStatusRequests-out-0.destination}")
    private String groupUpdateStatusRequestDestination;

    @Value("${spring.cloud.stream.bindings.groupJoinRequests-out-0.destination}")
    private String groupJoinRequestDestination;

    @Value("${spring.cloud.stream.bindings.groupLeaveRequests-out-0.destination}")
    private String groupLeaveRequestDestination;

    @BeforeAll
    public static void setupOnce(@Autowired RSocketRequester.Builder builder,
                                 @LocalServerPort Integer port) {
        final URI url = URI.create("ws://localhost:" + port + "/rsocket");
        requester = builder.websocket(url);
    }

    @AfterAll
    public static void tearDownOnce() {
        requester.rsocketClient().dispose();
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
    @DisplayName("Test RSocket integration for streaming outbox events")
    void testGetOutboxEventUpdates(@Autowired InputDestination inputDestination) {
        final OutboxEvent[] outboxEvents = {
            GroupTestUtility.generateOutboxEvent(),
            GroupTestUtility.generateOutboxEvent(),
            GroupTestUtility.generateOutboxEvent()
        };

        final Flux<OutboxEvent> groupUpdatesStream = requester
            .route("groups.updates")
            .retrieveFlux(OutboxEvent.class)
            .doOnSubscribe(subscription -> {
                inputDestination.send(new GenericMessage<>(outboxEvents[0]), eventDestination);
                inputDestination.send(new GenericMessage<>(outboxEvents[1]), eventDestination);
                inputDestination.send(new GenericMessage<>(outboxEvents[2]), eventDestination);
            });

        StepVerifier.create(groupUpdatesStream)
            .recordWith(ArrayList::new)
            .expectNextCount(3)
            .consumeRecordedWith(received -> {
                assertThat(received).hasSize(3);
                assertThat(received).containsExactlyInAnyOrder(outboxEvents);
            })
            .thenCancel()
            .verify(Duration.ofSeconds(5));
    }

    @Test
    @DisplayName("Test RSocket integration with group creation requests")
    void testSocketIntegrationWithGroupCreationRequests() {
        final var createRequestEvent = GroupTestUtility.generateGroupCreateRequestEvent();

        final Mono<Void> groupCreateRequest = requester
            .route("groups.create")
            .data(createRequestEvent)
            .retrieveMono(Void.class);

        StepVerifier.create(groupCreateRequest)
            .verifyComplete();

        final Message<byte[]> message =
            outputDestination.receive(1000, groupCreateRequestDestination);

        assertThat(message).isNotNull();
        assertThat(
            objectMapper.convertValue(createRequestEvent, GroupCreateRequestEvent.class))
            .isEqualTo(createRequestEvent);
    }

    @Test
    @DisplayName("Test RSocket integration with group status update requests")
    void testSocketIntegrationWithGroupStatusRequests() {
        final var groupStatusRequestEvent =
            GroupTestUtility.generateGroupStatusRequestEvent(1L, GroupStatus.DISBANDED);

        final Mono<Void> groupStatusRequest = requester
            .route("groups.status")
            .data(groupStatusRequestEvent)
            .retrieveMono(Void.class);

        StepVerifier.create(groupStatusRequest)
            .verifyComplete();

        final Message<byte[]> message =
            outputDestination.receive(1000, groupUpdateStatusRequestDestination);

        assertThat(message).isNotNull();
        assertThat(
            objectMapper.convertValue(groupStatusRequestEvent, GroupStatusRequestEvent.class))
            .isEqualTo(groupStatusRequestEvent);
    }

    @Test
    @DisplayName("Test RSocket integration with group join requests")
    void testSocketIntegrationWithGroupJoinRequests() {
        final var groupJoinRequestEvent = GroupTestUtility.generateGroupJoinRequestEvent();

        final Mono<Void> groupJoinRequest = requester
            .route("groups.join")
            .data(groupJoinRequestEvent)
            .retrieveMono(Void.class);

        StepVerifier.create(groupJoinRequest)
            .verifyComplete();

        final Message<byte[]> message =
            outputDestination.receive(1000, groupJoinRequestDestination);

        assertThat(message).isNotNull();
        assertThat(
            objectMapper.convertValue(groupJoinRequestEvent, GroupJoinRequestEvent.class))
            .isEqualTo(groupJoinRequestEvent);
    }

    @Test
    @DisplayName("Test RSocket integration with group leave requests")
    void testSocketIntegrationWithGroupLeaveRequests() {
        final var groupLeaveRequestEvent = GroupTestUtility.generateGroupLeaveRequestEvent();

        final Mono<Void> groupLeaveRequest = requester
            .route("groups.leave")
            .data(groupLeaveRequestEvent)
            .retrieveMono(Void.class);

        StepVerifier.create(groupLeaveRequest)
            .verifyComplete();

        final Message<byte[]> message =
            outputDestination.receive(1000, groupLeaveRequestDestination);

        assertThat(message).isNotNull();
        assertThat(objectMapper.convertValue(groupLeaveRequestEvent, GroupLeaveRequestEvent.class))
            .isEqualTo(groupLeaveRequestEvent);
    }
}
