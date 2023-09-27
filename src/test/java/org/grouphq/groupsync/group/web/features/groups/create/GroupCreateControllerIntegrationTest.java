package org.grouphq.groupsync.group.web.features.groups.create;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.rsocket.metadata.WellKnownMimeType;
import java.net.URI;
import java.util.UUID;
import org.grouphq.groupsync.GroupTestUtility;
import org.grouphq.groupsync.groupservice.event.daos.GroupCreateRequestEvent;
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
@TestPropertySource(properties = {"grouphq.features.groups.create=true"})
@Import(TestChannelBinderConfiguration.class)
@Tag("IntegrationTest")
class GroupCreateControllerIntegrationTest {
    private static RSocketRequester requester;

    @Autowired
    private OutputDestination outputDestination;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${spring.cloud.stream.bindings.groupCreateRequests-out-0.destination}")
    private String groupCreateRequestDestination;

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
}
