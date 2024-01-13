package org.grouphq.groupsync.group.web.features;

import java.security.Principal;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.grouphq.groupsync.group.event.GroupEventPublisher;
import org.grouphq.groupsync.group.security.UserService;
import org.grouphq.groupsync.groupservice.domain.exceptions.InternalServerError;
import org.grouphq.groupsync.groupservice.event.daos.requestevent.GroupJoinRequestEvent;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * RSocket handler for sending group join requests.
 */
@Slf4j
@RequiredArgsConstructor
@RestController
@ConditionalOnProperty(name = "grouphq.features.groups.join", havingValue = "true")
public class GroupJoinController {

    private final UserService userService;

    private final GroupEventPublisher groupEventPublisher;

    @MessageMapping("groups.join")
    public Mono<Void> joinGroup(GroupJoinRequestEvent groupJoinRequestEvent) {
        return createJoinRequestWithCurrentAuthorization(groupJoinRequestEvent)
            .flatMap(eventWithAuth -> groupEventPublisher.publishGroupJoinRequest(eventWithAuth)
                .doOnSuccess(unused -> log.debug("Sent join request: {}", eventWithAuth))
                .doOnError(throwable -> log.error("Error while joining group.", throwable))
                .onErrorMap(unusedThrowable -> new InternalServerError("Cannot join group"))
            );
    }

    private Mono<GroupJoinRequestEvent> createJoinRequestWithCurrentAuthorization(
        GroupJoinRequestEvent groupJoinRequestEvent) {
        return userService.getUserAuthentication()
            .map(Principal::getName)
            .flatMap(websocketId -> Mono.just(new GroupJoinRequestEvent(
                UUID.randomUUID(),
                groupJoinRequestEvent.getAggregateId(),
                groupJoinRequestEvent.getUsername(),
                websocketId,
                Instant.now()
            )));
    }
}
