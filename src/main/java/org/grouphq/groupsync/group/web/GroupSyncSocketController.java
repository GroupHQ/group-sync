package org.grouphq.groupsync.group.web;

import lombok.extern.slf4j.Slf4j;
import org.grouphq.groupsync.group.event.GroupEventPublisher;
import org.grouphq.groupsync.group.sync.GroupUpdateService;
import org.grouphq.groupsync.groupservice.domain.exceptions.InternalServerError;
import org.grouphq.groupsync.groupservice.domain.outbox.OutboxEvent;
import org.grouphq.groupsync.groupservice.event.daos.GroupCreateRequestEvent;
import org.grouphq.groupsync.groupservice.event.daos.GroupJoinRequestEvent;
import org.grouphq.groupsync.groupservice.event.daos.GroupLeaveRequestEvent;
import org.grouphq.groupsync.groupservice.event.daos.GroupStatusRequestEvent;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Provides RSocket endpoints for streaming outbox events and handling request events.
 */
@Slf4j
@RestController
public class GroupSyncSocketController {

    private final GroupUpdateService groupUpdateService;
    private final GroupEventPublisher groupEventPublisher;

    public GroupSyncSocketController(
        GroupUpdateService groupUpdateService,
        GroupEventPublisher groupEventPublisher) {
        this.groupUpdateService = groupUpdateService;
        this.groupEventPublisher = groupEventPublisher;
    }

    @MessageMapping("groups.updates")
    public Flux<OutboxEvent> getOutboxEventUpdates() {
        return groupUpdateService.outboxEventUpdateStream()
            .doOnError(throwable -> log.error("Error while streaming outbox events. "
                                              + "Attempting to retry.", throwable))
            .retry();
    }

    @MessageMapping("groups.create")
    public Mono<Void> createGroup(GroupCreateRequestEvent groupCreateRequestEvent) {
        return groupEventPublisher.publishGroupCreateRequest(groupCreateRequestEvent)
            .doOnError(throwable -> log.error("Error while creating group.", throwable))
            .onErrorMap(unusedThrowable -> new InternalServerError("Cannot create group"));
    }

    @MessageMapping("groups.status")
    public Mono<Void> updateGroupStatus(GroupStatusRequestEvent groupStatusRequestEvent) {
        return groupEventPublisher.publishGroupUpdateStatusRequest(groupStatusRequestEvent)
            .doOnError(throwable -> log.error("Error while updating group status.", throwable))
            .onErrorMap(unusedThrowable -> new InternalServerError("Cannot update group status"));
    }

    @MessageMapping("groups.join")
    public Mono<Void> joinGroup(GroupJoinRequestEvent groupJoinRequestEvent) {
        return groupEventPublisher.publishGroupJoinRequest(groupJoinRequestEvent)
            .doOnError(throwable -> log.error("Error while joining group.", throwable))
            .onErrorMap(unusedThrowable -> new InternalServerError("Cannot join group"));
    }

    @MessageMapping("groups.leave")
    public Mono<Void> leaveGroup(GroupLeaveRequestEvent groupLeaveRequestEvent) {
        return groupEventPublisher.publishGroupLeaveRequest(groupLeaveRequestEvent)
            .doOnError(throwable -> log.error("Error while leaving group.", throwable))
            .onErrorMap(unusedThrowable -> new InternalServerError("Cannot leave group"));
    }
}
