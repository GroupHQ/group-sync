package org.grouphq.groupsync.group.sync;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.grouphq.groupsync.groupservice.domain.outbox.OutboxEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

@Tag("UnitTest")
class GroupSyncServiceTest {

    private final GroupSyncService groupSyncService;

    public GroupSyncServiceTest() {
        groupSyncService = new GroupSyncService();
    }

    @Test
    @DisplayName("Returns a flux for updates")
    void returnsSinkForUpdates() {
        assertThat(groupSyncService.outboxEventUpdateStream()).isNotNull();
        assertThat(groupSyncService.outboxEventUpdateStream()).isInstanceOfAny(Flux.class);
    }

    @Test
    @DisplayName("Updates sink with new outbox events and emits them")
    void updatesSinkWithNewOutboxEventsAndEmitsThem() {
        final OutboxEvent outboxEvent = new OutboxEvent();

        StepVerifier.create(groupSyncService.outboxEventUpdateStream())
            .then(() -> groupSyncService.sendOutboxEventUpdate(outboxEvent))
            .expectNext(outboxEvent)
            .thenCancel()
            .verify(Duration.ofSeconds(1));
    }
}
