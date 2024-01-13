package org.grouphq.groupsync.group.web.features;

import java.security.Principal;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.grouphq.groupsync.group.event.GroupEventPublisher;
import org.grouphq.groupsync.group.security.UserService;
import org.grouphq.groupsync.groupservice.domain.exceptions.InternalServerError;
import org.grouphq.groupsync.groupservice.event.daos.requestevent.GroupLeaveRequestEvent;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * RSocket handler for sending group leave requests.
 */
@Slf4j
@RequiredArgsConstructor
@RestController
@ConditionalOnProperty(name = "grouphq.features.groups.leave", havingValue = "true")
public class GroupLeaveController {

    private final UserService userService;

    private final GroupEventPublisher groupEventPublisher;

    @MessageMapping("groups.leave")
    public Mono<Void> leaveGroup(GroupLeaveRequestEvent groupLeaveRequestEvent) {
        return createLeaveRequestWithCurrentAuthorization(groupLeaveRequestEvent)
            .flatMap(eventWithAuth -> groupEventPublisher.publishGroupLeaveRequest(eventWithAuth)
                .doOnSuccess(unused -> log.debug("Sent leave request: {}", eventWithAuth))
                .doOnError(throwable -> log.error("Error while leaving group.", throwable))
                .onErrorMap(unusedThrowable -> new InternalServerError("Cannot leave group"))
            );
    }

    private Mono<GroupLeaveRequestEvent> createLeaveRequestWithCurrentAuthorization(
        GroupLeaveRequestEvent groupLeaveRequestEvent) {
        return userService.getUserAuthentication()
            .map(Principal::getName)
            .flatMap(websocketId -> Mono.just(new GroupLeaveRequestEvent(
                UUID.randomUUID(),
                groupLeaveRequestEvent.getAggregateId(),
                groupLeaveRequestEvent.getMemberId(),
                websocketId,
                Instant.now()
            )));
    }
}
