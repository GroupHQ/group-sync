package org.grouphq.groupsync.group.web;

import lombok.extern.slf4j.Slf4j;
import org.grouphq.groupsync.group.domain.PublicOutboxEvent;
import org.grouphq.groupsync.group.sync.GroupUpdateService;
import org.grouphq.groupsync.groupservice.domain.exceptions.InternalServerError;
import org.grouphq.groupsync.groupservice.domain.outbox.OutboxEvent;
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
public class GroupSyncController {
    private final GroupUpdateService groupUpdateService;

    public GroupSyncController(GroupUpdateService groupUpdateService) {
        this.groupUpdateService = groupUpdateService;
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
            .flatMap(outboxEvent -> monoIsUserEventOwner(outboxEvent)
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

    private Mono<Boolean> monoIsUserEventOwner(OutboxEvent outboxEvent) {
        return ReactiveSecurityContextHolder.getContext()
            .map(SecurityContext::getAuthentication)
            .flatMap(authentication -> {
                final String username = authentication.getName();
                final String eventOwner = outboxEvent.getWebsocketId();
                log.info("User ID is {}. Event Owner is {}", username, eventOwner);
                if (username.equals(eventOwner)) {
                    log.info("User matches failed event, sending event...");
                    return Mono.just(true);
                }
                log.info("User does not match failed event, not sending event...");
                return Mono.just(false);
            });
    }
}
