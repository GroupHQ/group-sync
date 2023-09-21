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
class GroupUpdateServiceTest {

    private final GroupUpdateService groupUpdateService;

    public GroupUpdateServiceTest() {
        groupUpdateService = new GroupUpdateService();
    }

    @Test
    @DisplayName("Returns a flux for updates")
    void returnsSinkForUpdates() {
        assertThat(groupUpdateService.outboxEventUpdateStream()).isNotNull();
        assertThat(groupUpdateService.outboxEventUpdateStream()).isInstanceOfAny(Flux.class);
    }

    @Test
    @DisplayName("Updates sink with new outbox events and emits them")
    void updatesSinkWithNewOutboxEventsAndEmitsThem() {
        final OutboxEvent outboxEvent = new OutboxEvent();

        StepVerifier.create(groupUpdateService.outboxEventUpdateStream())
            .then(() -> groupUpdateService.sendOutboxEventUpdate(outboxEvent))
            .expectNext(outboxEvent)
            .thenCancel()
            .verify(Duration.ofSeconds(1));
    }
}
