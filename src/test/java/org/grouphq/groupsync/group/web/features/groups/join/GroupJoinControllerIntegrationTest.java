package org.grouphq.groupsync.group.web.features.groups.join;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.rsocket.metadata.WellKnownMimeType;
import java.io.IOException;
import java.net.URI;
import java.util.UUID;
import org.grouphq.groupsync.GroupTestUtility;
import org.grouphq.groupsync.groupservice.event.daos.requestevent.GroupJoinRequestEvent;
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
@TestPropertySource(properties = {"grouphq.features.groups.join=true"})
@Import(TestChannelBinderConfiguration.class)
@Tag("IntegrationTest")
class GroupJoinControllerIntegrationTest {
    private static RSocketRequester requester;

    @Autowired
    private OutputDestination outputDestination;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${spring.cloud.stream.bindings.groupJoinRequests-out-0.destination}")
    private String groupJoinRequestDestination;

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
    @DisplayName("Only accepts username and group Id for group join requests")
    void testSocketIntegrationWithGroupJoinRequests() throws IOException {
        final var originalRequest = GroupTestUtility.generateGroupJoinRequestEvent();

        final Mono<Void> groupJoinRequest = requester
            .route("groups.join")
            .data(originalRequest)
            .retrieveMono(Void.class);

        StepVerifier.create(groupJoinRequest)
            .verifyComplete();

        final Message<byte[]> message =
            outputDestination.receive(1000, groupJoinRequestDestination);

        assertThat(message).isNotNull();
        assertThat(
            objectMapper.readValue(message.getPayload(), GroupJoinRequestEvent.class))
            .satisfies(forwardedRequest -> {
                assertThat(forwardedRequest.getAggregateId()).isEqualTo(originalRequest.getAggregateId());
                assertThat(forwardedRequest.getUsername()).isEqualTo(originalRequest.getUsername());
                assertThat(forwardedRequest.getEventId()).isEqualTo(originalRequest.getEventId());
                assertThat(forwardedRequest.getCreatedDate()).isAfter(originalRequest.getCreatedDate());
                assertThat(forwardedRequest.getWebsocketId()).isEqualTo(USER_ID);
            });
    }
}
