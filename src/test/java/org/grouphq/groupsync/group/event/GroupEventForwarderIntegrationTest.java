package org.grouphq.groupsync.group.event;

import java.time.Duration;
import org.grouphq.groupsync.GroupTestUtility;
import org.grouphq.groupsync.group.domain.PublicOutboxEvent;
import org.grouphq.groupsync.group.web.GroupSyncController;
import org.grouphq.groupsync.groupservice.domain.outbox.OutboxEvent;
import org.grouphq.groupsync.groupservice.domain.outbox.enums.EventStatus;
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
import org.springframework.security.test.context.support.WithMockUser;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

@SpringBootTest
@Import(TestChannelBinderConfiguration.class)
@Tag("IntegrationTest")
class GroupEventForwarderIntegrationTest {

    @Autowired
    private InputDestination inputDestination;

    @Autowired
    private GroupSyncController groupSyncController;

    @Value("${spring.cloud.stream.bindings.processedEvents-in-0.destination}")
    private String eventDestination;

    private static final String USER = "USER";

    @Test
    @DisplayName("Forwards events to the outbox event update successful sink")
    void forwardsEventsToTheOutboxEventUpdateSink() {
        final PublicOutboxEvent[] publicEvents = {
            PublicOutboxEvent.convertOutboxEvent(GroupTestUtility.generateOutboxEvent()),
            PublicOutboxEvent.convertOutboxEvent(GroupTestUtility.generateOutboxEvent()),
            PublicOutboxEvent.convertOutboxEvent(GroupTestUtility.generateOutboxEvent())
        };

        final Flux<PublicOutboxEvent> groupUpdatesStream =
            groupSyncController.getPublicUpdates()
            .doOnSubscribe(subscription -> {
                inputDestination.send(new GenericMessage<>(publicEvents[0]), eventDestination);
                inputDestination.send(new GenericMessage<>(publicEvents[1]), eventDestination);
                inputDestination.send(new GenericMessage<>(publicEvents[2]), eventDestination);
            });

        // Subscribe to updates sink
        StepVerifier.create(groupUpdatesStream)
            .expectNext(publicEvents[0], publicEvents[1], publicEvents[2])
            .thenCancel()
            .verify(Duration.ofSeconds(1));
    }

    @Test
    @WithMockUser(username = USER)
    @DisplayName("Forwards events to the outbox event update failed sink")
    void forwardsEventsToTheOutboxEventUpdateFailedSink() {
        final OutboxEvent[] outboxEvents = {
            GroupTestUtility.generateOutboxEvent(USER, EventStatus.FAILED),
            GroupTestUtility.generateOutboxEvent(USER, EventStatus.FAILED),
            GroupTestUtility.generateOutboxEvent(USER, EventStatus.FAILED)
        };

        final Flux<OutboxEvent> groupUpdatesStream =
            groupSyncController.getEventOwnerUpdates()
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
