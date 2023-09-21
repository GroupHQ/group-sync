package org.grouphq.groupsync.group.event;

import java.time.Duration;
import org.grouphq.groupsync.group.web.GroupSyncSocketController;
import org.grouphq.groupsync.groupservice.domain.outbox.OutboxEvent;
import org.grouphq.groupsync.testutility.GroupTestUtility;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.binder.test.InputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.support.GenericMessage;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

@SpringBootTest
@Import(TestChannelBinderConfiguration.class)
@Tag("IntegrationTest")
class GroupEventForwarderIntegrationTest {

    @Autowired
    private InputDestination inputDestination;

    @Autowired
    private GroupSyncSocketController groupSyncSocketController;

    @Value("${spring.cloud.stream.bindings.forwardProcessedEvents-in-0.destination}")
    private String eventDestination;

    @Test
    @DisplayName("Forwards events to the outbox event update sink")
    void forwardsEventsToTheOutboxEventUpdateSink() {
        final OutboxEvent[] outboxEvents = {
            GroupTestUtility.generateOutboxEvent(),
            GroupTestUtility.generateOutboxEvent(),
            GroupTestUtility.generateOutboxEvent()
        };

        final Flux<OutboxEvent> groupUpdatesStream = groupSyncSocketController.getOutboxEventUpdates()
            .doOnSubscribe(subscription -> {
                inputDestination.send(new GenericMessage<>(outboxEvents[0]), eventDestination);
                inputDestination.send(new GenericMessage<>(outboxEvents[1]), eventDestination);
                inputDestination.send(new GenericMessage<>(outboxEvents[2]), eventDestination);
            });

        // Subscribe to updates sink
        StepVerifier.create(groupUpdatesStream)
            .expectNext(outboxEvents[0], outboxEvents[1], outboxEvents[2])
            .thenCancel()
            .verify(Duration.ofSeconds(1));
    }
}
