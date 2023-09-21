package org.grouphq.groupsync.group.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import org.grouphq.groupsync.groupservice.domain.outbox.OutboxEvent;
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
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.messaging.support.GenericMessage;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestChannelBinderConfiguration.class)
@Tag("IntegrationTest")
class GroupSyncSocketControllerTest {

    private static RSocketRequester requester;

    @Autowired
    private InputDestination inputDestination;

    @Value("${spring.cloud.stream.bindings.processedEvents-in-0.destination}")
    private String eventDestination;

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
    @DisplayName("Test RSocket integration with group updates")
    void testSocketIntegrationWithGroupUpdates() {
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
}
