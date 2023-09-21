package org.grouphq.groupsync.group.event;

import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.grouphq.groupsync.group.sync.GroupUpdateService;
import org.grouphq.groupsync.groupservice.domain.outbox.OutboxEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Flux;

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
            outboxEvents.doOnNext(groupUpdateService::sendOutboxEventUpdate)
                .doOnError(throwable -> log.error("Error while forwarding events. "
                                                  + "Attempting to resume.", throwable))
                .onErrorResume(throwable -> Flux.empty())
                .subscribe();
    }
}
