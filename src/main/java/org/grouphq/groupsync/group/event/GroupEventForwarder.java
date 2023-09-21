package org.grouphq.groupsync.group.event;

import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.grouphq.groupsync.group.sync.GroupSyncService;
import org.grouphq.groupsync.groupservice.domain.outbox.OutboxEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Flux;

/**
 * Forwards outbox events from the event broker to a flux managed by GroupSyncService.
 */
@Slf4j
@Configuration
public class GroupEventForwarder {

    private final GroupSyncService groupSyncService;

    public GroupEventForwarder(GroupSyncService groupSyncService) {
        this.groupSyncService = groupSyncService;
    }

    @Bean
    public Consumer<Flux<OutboxEvent>> processedEvents() {
        return outboxEvents ->
            outboxEvents.doOnNext(groupSyncService::sendOutboxEventUpdate)
                .doOnError(throwable -> log.error("Error while forwarding events. "
                                                  + "Attempting to resume.", throwable))
                .onErrorResume(throwable -> Flux.empty())
                .subscribe();
    }
}
