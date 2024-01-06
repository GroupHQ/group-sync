package org.grouphq.groupsync.group.web.features;

import java.security.Principal;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.grouphq.groupsync.group.event.GroupEventPublisher;
import org.grouphq.groupsync.group.security.UserService;
import org.grouphq.groupsync.groupservice.domain.exceptions.InternalServerError;
import org.grouphq.groupsync.groupservice.event.daos.requestevent.GroupStatusRequestEvent;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * RSocket handler for sending group status update requests.
 */
@Slf4j
@RequiredArgsConstructor
@RestController
@ConditionalOnProperty(name = "grouphq.features.groups.status", havingValue = "true")
public class GroupStatusController {

    private final UserService userService;

    private final GroupEventPublisher groupEventPublisher;

    @MessageMapping("groups.status")
    public Mono<Void> updateGroupStatus(GroupStatusRequestEvent groupStatusRequestEvent) {
        return createUpdateStatusRequestWithCurrentAuthorization(groupStatusRequestEvent)
            .flatMap(eventWithAuth -> groupEventPublisher.publishGroupUpdateStatusRequest(eventWithAuth)
                .doOnSuccess(unused -> log.debug("Sent update group status request: {}", eventWithAuth))
                .doOnError(throwable -> log.error("Error while updating group status.", throwable))
                .onErrorMap(unusedThrowable -> new InternalServerError("Cannot update group status"))
            );
    }

    private Mono<GroupStatusRequestEvent> createUpdateStatusRequestWithCurrentAuthorization(
        GroupStatusRequestEvent groupStatusRequestEvent) {
        return userService.getUserAuthentication()
            .map(Principal::getName)
            .flatMap(websocketId -> Mono.just(new GroupStatusRequestEvent(
                UUID.randomUUID(),
                groupStatusRequestEvent.getAggregateId(),
                groupStatusRequestEvent.getNewStatus(),
                websocketId,
                Instant.now()
            )));
    }
}
