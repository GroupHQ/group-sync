package org.grouphq.groupsync.group.event;

import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.grouphq.groupsync.group.domain.PublicOutboxEvent;
import org.grouphq.groupsync.group.sync.GroupUpdateService;
import org.grouphq.groupsync.groupservice.domain.outbox.OutboxEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Forwards outbox events from the event broker to a flux managed by GroupUpdateService.
 */
@Slf4j
@Configuration
public class GroupEventForwarder {

    private final GroupUpdateService groupUpdateService;

    public GroupEventForwarder(GroupUpdateService groupUpdateService) {
        this.groupUpdateService = groupUpdateService;
    }

    @Bean
    public Consumer<Flux<OutboxEvent>> processedEvents() {
        return outboxEvents ->
            outboxEvents.flatMap(this::forwardUpdate)
                .doOnError(throwable -> log.error("Error while forwarding events. "
                                                  + "Attempting to resume.", throwable))
                .onErrorResume(throwable -> Flux.empty())
                .subscribe();
    }

    private Mono<Void> forwardUpdate(OutboxEvent outboxEvent) {
        switch (outboxEvent.getEventStatus()) {
            case SUCCESSFUL -> {
                groupUpdateService.sendPublicOutboxEventToAll(
                    PublicOutboxEvent.convertOutboxEvent(outboxEvent));
                groupUpdateService.sendOutboxEventToEventOwner(outboxEvent);
            }
            case FAILED -> groupUpdateService.sendOutboxEventToEventOwner(outboxEvent);
            default -> log.error("Unknown event status: {}", outboxEvent.getEventStatus());
        }

        return Mono.empty();
    }
}
