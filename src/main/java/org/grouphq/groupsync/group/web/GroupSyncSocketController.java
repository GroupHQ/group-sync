package org.grouphq.groupsync.group.web;

import lombok.extern.slf4j.Slf4j;
import org.grouphq.groupsync.group.domain.PublicOutboxEvent;
import org.grouphq.groupsync.group.event.GroupEventPublisher;
import org.grouphq.groupsync.group.sync.GroupUpdateService;
import org.grouphq.groupsync.groupservice.domain.exceptions.InternalServerError;
import org.grouphq.groupsync.groupservice.domain.outbox.OutboxEvent;
import org.grouphq.groupsync.groupservice.event.daos.GroupCreateRequestEvent;
import org.grouphq.groupsync.groupservice.event.daos.GroupJoinRequestEvent;
import org.grouphq.groupsync.groupservice.event.daos.GroupLeaveRequestEvent;
import org.grouphq.groupsync.groupservice.event.daos.GroupStatusRequestEvent;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
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

    @MessageMapping("connect")
    public Mono<String> connect() {
        return ReactiveSecurityContextHolder.getContext()
            .map(SecurityContext::getAuthentication)
            .map(authentication -> {
                String username = authentication.getName();
                log.info("User {} connected.", username);
                log.info("Authentication is {}.", authentication);
                return username;
            });
    }

    @MessageMapping("groups.updates.all")
    public Flux<PublicOutboxEvent> getPublicUpdates() {
        return groupUpdateService.publicUpdatesStream()
            .doOnCancel(() -> log.info("Cancelled streaming outbox events."))
            .doOnComplete(() -> log.info("Stopped streaming outbox events."))
            .doOnError(throwable -> log.error("Error while streaming outbox events. "
                                              + "Stream will be terminated.", throwable))
            .onErrorMap(unusedThrowable -> new InternalServerError("Update stream closed"));
    }

    @MessageMapping("groups.updates.user")
    public Flux<OutboxEvent> getEventOwnerUpdates() {
        return groupUpdateService.eventOwnerUpdateStream()
            .flatMap(outboxEvent -> isUserEventOwner(outboxEvent)
                .flatMap(isOwner -> isOwner ? Mono.just(outboxEvent) : Mono.empty())
                .doOnError(throwable -> log.error(
                    "Error while verifying user ownership on event: {}", outboxEvent, throwable))
                .onErrorResume(throwable -> Mono.empty())
            )
            .doOnComplete(() -> log.info("Stopped streaming outbox events to users."))
            .doOnError(throwable -> log.error("Error while streaming user outbox events. "
                                              + "Stream will be terminated.", throwable))
            .onErrorMap(unusedThrowable -> new InternalServerError("User update stream closed"));
    }

    private Mono<Boolean> isUserEventOwner(OutboxEvent outboxEvent) {
        return ReactiveSecurityContextHolder.getContext()
            .map(SecurityContext::getAuthentication)
            .flatMap(authentication -> {
                String username = authentication.getName();
                String eventOwner = outboxEvent.getWebsocketId();
                log.info("User ID is {}. Event Owner is {}", username, eventOwner);
                if (username.equals(eventOwner)) {
                    log.info("User matches failed event, sending event...");
                    return Mono.just(true);
                }
                log.info("User does not match failed event, not sending event...");
                return Mono.just(false);
            });
    }

    @MessageMapping("groups.create")
    public Mono<Void> createGroup(GroupCreateRequestEvent groupCreateRequestEvent) {
        return groupEventPublisher.publishGroupCreateRequest(groupCreateRequestEvent)
            .doOnSuccess(unused -> log.debug("Sent create group request."))
            .doOnError(throwable -> log.error("Error while creating group.", throwable))
            .onErrorMap(unusedThrowable -> new InternalServerError("Cannot create group"));
    }

    @MessageMapping("groups.status")
    public Mono<Void> updateGroupStatus(GroupStatusRequestEvent groupStatusRequestEvent) {
        return groupEventPublisher.publishGroupUpdateStatusRequest(groupStatusRequestEvent)
            .doOnSuccess(unused -> log.debug("Sent update group status request."))
            .doOnError(throwable -> log.error("Error while updating group status.", throwable))
            .onErrorMap(unusedThrowable -> new InternalServerError("Cannot update group status"));
    }

    @MessageMapping("groups.join")
    public Mono<Void> joinGroup(GroupJoinRequestEvent groupJoinRequestEvent) {
        return groupEventPublisher.publishGroupJoinRequest(groupJoinRequestEvent)
            .doOnSuccess(unused -> log.debug("Sent join request"))
            .doOnError(throwable -> log.error("Error while joining group.", throwable))
            .onErrorMap(unusedThrowable -> new InternalServerError("Cannot join group"));
    }

    @MessageMapping("groups.leave")
    public Mono<Void> leaveGroup(GroupLeaveRequestEvent groupLeaveRequestEvent) {
        return groupEventPublisher.publishGroupLeaveRequest(groupLeaveRequestEvent)
            .doOnSuccess(unused -> log.debug("Sent leave request"))
            .doOnError(throwable -> log.error("Error while leaving group.", throwable))
            .onErrorMap(unusedThrowable -> new InternalServerError("Cannot leave group"));
    }
}
