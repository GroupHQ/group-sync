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
    @DisplayName("Returns a flux for successful events")
    void returnsSinkForUpdates() {
        assertThat(groupUpdateService.outboxEventUpdateStream()).isNotNull();
        assertThat(groupUpdateService.outboxEventUpdateStream()).isInstanceOfAny(Flux.class);
    }

    @Test
    @DisplayName("Updates sink with successful events and emits them")
    void updatesSinkWithNewOutboxEventsAndEmitsThem() {
        final OutboxEvent outboxEvent = new OutboxEvent();

        StepVerifier.create(groupUpdateService.outboxEventUpdateStream())
            .then(() -> groupUpdateService.sendOutboxEventUpdate(outboxEvent))
            .expectNext(outboxEvent)
            .thenCancel()
            .verify(Duration.ofSeconds(1));
    }

    @Test
    @DisplayName("Returns a flux for failed events")
    void returnsSinkForFailedUpdates() {
        assertThat(groupUpdateService.outboxEventFailedUpdateStream()).isNotNull();
        assertThat(groupUpdateService.outboxEventFailedUpdateStream()).isInstanceOfAny(Flux.class);
    }

    @Test
    @DisplayName("Updates sink with failed events and emits them")
    void updatesSinkWithFailedOutboxEventsAndEmitsThem() {
        final OutboxEvent outboxEvent = new OutboxEvent();

        StepVerifier.create(groupUpdateService.outboxEventFailedUpdateStream())
            .then(() -> groupUpdateService.sendOutboxEventUpdateFailed(outboxEvent))
            .expectNext(outboxEvent)
            .thenCancel()
            .verify(Duration.ofSeconds(1));
    }
}
