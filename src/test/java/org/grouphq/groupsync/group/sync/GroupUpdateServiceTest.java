package org.grouphq.groupsync.group.sync;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.grouphq.groupsync.GroupTestUtility;
import org.grouphq.groupsync.group.domain.PublicOutboxEvent;
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
        assertThat(groupUpdateService.publicUpdatesStream()).isNotNull();
        assertThat(groupUpdateService.publicUpdatesStream()).isInstanceOfAny(Flux.class);
    }

    @Test
    @DisplayName("Updates sink with successful events and emits them")
    void updatesSinkWithNewOutboxEventsAndEmitsThem() {
        final OutboxEvent outboxEvent = GroupTestUtility.generateOutboxEvent();
        final PublicOutboxEvent publicOutboxEvent =
            PublicOutboxEvent.convertOutboxEvent(outboxEvent);

        StepVerifier.create(groupUpdateService.publicUpdatesStream())
            .then(() -> groupUpdateService.sendPublicOutboxEventToAll(publicOutboxEvent))
            .expectNext(publicOutboxEvent)
            .thenCancel()
            .verify(Duration.ofSeconds(1));
    }

    @Test
    @DisplayName("Returns a flux for failed events")
    void returnsSinkForFailedUpdates() {
        assertThat(groupUpdateService.eventOwnerUpdateStream()).isNotNull();
        assertThat(groupUpdateService.eventOwnerUpdateStream()).isInstanceOfAny(Flux.class);
    }

    @Test
    @DisplayName("Updates sink with failed events and emits them")
    void updatesSinkWithFailedOutboxEventsAndEmitsThem() {
        final OutboxEvent outboxEvent = new OutboxEvent();

        StepVerifier.create(groupUpdateService.eventOwnerUpdateStream())
            .then(() -> groupUpdateService.sendOutboxEventToEventOwner(outboxEvent))
            .expectNext(outboxEvent)
            .thenCancel()
            .verify(Duration.ofSeconds(1));
    }
}
