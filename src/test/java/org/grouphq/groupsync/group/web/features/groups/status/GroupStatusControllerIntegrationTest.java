package org.grouphq.groupsync.group.web.features.groups.status;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.rsocket.metadata.WellKnownMimeType;
import java.io.IOException;
import java.net.URI;
import java.util.UUID;
import org.grouphq.groupsync.GroupTestUtility;
import org.grouphq.groupsync.groupservice.domain.groups.GroupStatus;
import org.grouphq.groupsync.groupservice.event.daos.requestevent.GroupStatusRequestEvent;
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
import org.springframework.test.context.TestPropertySource;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {"grouphq.features.groups.status=true"})
@Import(TestChannelBinderConfiguration.class)
@Tag("IntegrationTest")
class GroupStatusControllerIntegrationTest {
    private static RSocketRequester requester;

    @Autowired
    private OutputDestination outputDestination;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${spring.cloud.stream.bindings.groupStatusRequests-out-0.destination}")
    private String groupUpdateStatusRequestDestination;

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
    @DisplayName("Only accepts group Id and new group status for group status update requests")
    void testSocketIntegrationWithGroupStatusRequests() throws IOException {
        final var originalRequest =
            GroupTestUtility.generateGroupStatusRequestEvent(1L, GroupStatus.DISBANDED);

        final Mono<Void> groupStatusRequest = requester
            .route("groups.status")
            .data(originalRequest)
            .retrieveMono(Void.class);

        StepVerifier.create(groupStatusRequest)
            .verifyComplete();

        final Message<byte[]> message =
            outputDestination.receive(1000, groupUpdateStatusRequestDestination);

        assertThat(message).isNotNull();
        assertThat(
            objectMapper.readValue(message.getPayload(), GroupStatusRequestEvent.class))
            .satisfies(forwardedRequest -> {
                assertThat(forwardedRequest.getAggregateId()).isEqualTo(1L);
                assertThat(forwardedRequest.getNewStatus()).isEqualTo(GroupStatus.DISBANDED);
                assertThat(forwardedRequest.getEventId()).isEqualTo(originalRequest.getEventId());
                assertThat(forwardedRequest.getCreatedDate()).isAfter(originalRequest.getCreatedDate());
                assertThat(forwardedRequest.getWebsocketId()).isEqualTo(USER_ID);
            });
    }
}
