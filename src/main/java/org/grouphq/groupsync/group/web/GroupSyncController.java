package org.grouphq.groupsync.group.web;

import java.security.Principal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.grouphq.groupsync.group.domain.PublicOutboxEvent;
import org.grouphq.groupsync.group.security.UserService;
import org.grouphq.groupsync.group.sync.GroupFetchService;
import org.grouphq.groupsync.group.sync.GroupUpdateService;
import org.grouphq.groupsync.group.sync.state.GroupInitialStateService;
import org.grouphq.groupsync.groupservice.domain.exceptions.InternalServerError;
import org.grouphq.groupsync.groupservice.domain.members.Member;
import org.grouphq.groupsync.groupservice.domain.outbox.OutboxEvent;
import org.grouphq.groupsync.groupservice.web.objects.egress.PublicMember;
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
@RequiredArgsConstructor
@RestController
public class GroupSyncController {

    private final UserService userService;

    private final GroupInitialStateService groupInitialStateService;

    private final GroupUpdateService groupUpdateService;

    private final GroupFetchService groupFetchService;

    @MessageMapping("groups.ping")
    public Mono<Boolean> ping() {
        return Mono.just(true);
    }

    @MessageMapping("groups.updates.all")
    public Flux<PublicOutboxEvent> getPublicUpdates() {
        log.info("Subscribing connection to public updates stream.");
        return concatPublicUpdates()
            .doOnNext(outboxEvent -> log.info("Sending public outbox event: {}", outboxEvent))
            .doOnCancel(() -> log.info("User cancelled streaming outbox events."))
            .doOnComplete(() -> log.info("Stopped streaming outbox events."))
            .doOnError(throwable -> log.error("Error while streaming outbox events. "
                                              + "Stream will be terminated. Error: {}", throwable.getMessage()))
            .onErrorMap(unusedThrowable -> new InternalServerError("Update stream closed"));
    }

    private Flux<PublicOutboxEvent> concatPublicUpdates() {
        final Flux<PublicOutboxEvent> currentEvents = groupInitialStateService.requestCurrentEvents();
        final Mono<PublicOutboxEvent> emptyEvent = PublicOutboxEvent.getEmptyEvent();
        final Flux<PublicOutboxEvent> publicUpdates = groupUpdateService.publicUpdatesStream();

        return Flux.concat(currentEvents, emptyEvent, publicUpdates);
    }

    @MessageMapping("groups.updates.user")
    public Flux<OutboxEvent> getEventOwnerUpdates() {
        return groupUpdateService.eventOwnerUpdateStream()
            .flatMap(outboxEvent -> monoIsUserEventOwner(outboxEvent)
                .flatMap(isOwner -> isOwner ? Mono.just(outboxEvent) : Mono.empty())
                .doOnError(throwable -> log.error(
                    "Error while verifying user ownership. Error: {} with event: {}",
                    throwable.getMessage(), outboxEvent))
                .onErrorResume(throwable -> Mono.empty())
            )
            .doOnCancel(() -> log.info("User cancelled streaming user outbox events."))
            .doOnComplete(() -> log.info("Stopped streaming outbox events to users."))
            .doOnError(throwable -> log.error("Error while streaming user outbox events. "
                                              + "Stream will be terminated. Error: {}", throwable.getMessage()))
            .onErrorMap(unusedThrowable -> new InternalServerError("User update stream closed"));
    }

    @MessageMapping("groups.user.member")
    public Mono<PublicMember> getMyMember() {
        return ReactiveSecurityContextHolder.getContext()
            .map(SecurityContext::getAuthentication)
            .flatMap(authentication -> {
                final String websocketId = authentication.getName();
                return groupFetchService.getMyMember(websocketId)
                    .map(Member::toPublicMember);
            });
    }

    private Mono<Boolean> monoIsUserEventOwner(OutboxEvent outboxEvent) {
        return userService.getUserAuthentication()
            .map(Principal::getName)
            .flatMap(username -> {
                final String eventOwner = outboxEvent.getWebsocketId();
                return username.equals(eventOwner) ? Mono.just(true) : Mono.just(false);
            });
    }
}
