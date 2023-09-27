package org.grouphq.groupsync.group.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.rsocket.metadata.WellKnownMimeType;
import java.net.URI;
import java.util.UUID;
import org.grouphq.groupsync.GroupTestUtility;
import org.grouphq.groupsync.groupservice.domain.groups.GroupStatus;
import org.grouphq.groupsync.groupservice.event.daos.GroupCreateRequestEvent;
import org.grouphq.groupsync.groupservice.event.daos.GroupJoinRequestEvent;
import org.grouphq.groupsync.groupservice.event.daos.GroupLeaveRequestEvent;
import org.grouphq.groupsync.groupservice.event.daos.GroupStatusRequestEvent;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.cloud.stream.binder.test.OutputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.Message;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.security.rsocket.metadata.UsernamePasswordMetadata;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
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

    @Value("${spring.cloud.stream.bindings.groupCreateRequests-out-0.destination}")
    private String groupCreateRequestDestination;

    @Value("${spring.cloud.stream.bindings.groupStatusRequests-out-0.destination}")
    private String groupUpdateStatusRequestDestination;

    @Value("${spring.cloud.stream.bindings.groupJoinRequests-out-0.destination}")
    private String groupJoinRequestDestination;

    @Value("${spring.cloud.stream.bindings.groupLeaveRequests-out-0.destination}")
    private String groupLeaveRequestDestination;

    private static final String USER_ID = UUID.randomUUID().toString();

    @BeforeEach
    public void setup(@Autowired RSocketRequester.Builder builder,
                                 @LocalServerPort Integer port) {
        final URI url = URI.create("ws://localhost:" + port + "/rsocket");

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
