package org.grouphq.groupsync.group.web.features;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.grouphq.groupsync.group.event.GroupEventPublisher;
import org.grouphq.groupsync.groupservice.domain.exceptions.InternalServerError;
import org.grouphq.groupsync.groupservice.event.daos.GroupJoinRequestEvent;
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

    private final GroupEventPublisher groupEventPublisher;

    @MessageMapping("groups.join")
    public Mono<Void> joinGroup(GroupJoinRequestEvent groupJoinRequestEvent) {
        return groupEventPublisher.publishGroupJoinRequest(groupJoinRequestEvent)
            .doOnSuccess(unused -> log.debug("Sent join request"))
            .doOnError(throwable -> log.error("Error while joining group.", throwable))
            .onErrorMap(unusedThrowable -> new InternalServerError("Cannot join group"));
    }
}
