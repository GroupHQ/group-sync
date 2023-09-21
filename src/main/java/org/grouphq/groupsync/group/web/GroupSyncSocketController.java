package org.grouphq.groupsync.group.web;

import lombok.extern.slf4j.Slf4j;
import org.grouphq.groupsync.group.sync.GroupSyncService;
import org.grouphq.groupsync.groupservice.domain.outbox.OutboxEvent;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

/**
 * Provides RSocket endpoints for streaming outbox events and handling request events.
 */
@Slf4j
@RestController
public class GroupSyncSocketController {
    private final GroupSyncService groupSyncService;

    public GroupSyncSocketController(GroupSyncService groupSyncService) {
        this.groupSyncService = groupSyncService;
    }

    @MessageMapping("groups.updates")
    public Flux<OutboxEvent> getOutboxEventUpdates() {
        return groupSyncService.outboxEventUpdateStream()
            .doOnError(throwable -> log.error("Error while streaming outbox events. "
                                              + "Attempting to resume.", throwable))
            .onErrorResume(throwable -> Flux.empty());
    }
}
