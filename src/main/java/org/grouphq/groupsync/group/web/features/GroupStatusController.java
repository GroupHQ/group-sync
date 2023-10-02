package org.grouphq.groupsync.group.web.features;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.grouphq.groupsync.group.event.GroupEventPublisher;
import org.grouphq.groupsync.groupservice.domain.exceptions.InternalServerError;
import org.grouphq.groupsync.groupservice.event.daos.GroupStatusRequestEvent;
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

    private final GroupEventPublisher groupEventPublisher;

    @MessageMapping("groups.status")
    public Mono<Void> updateGroupStatus(GroupStatusRequestEvent groupStatusRequestEvent) {
        return groupEventPublisher.publishGroupUpdateStatusRequest(groupStatusRequestEvent)
            .doOnSuccess(unused -> log.debug("Sent update group status request."))
            .doOnError(throwable -> log.error("Error while updating group status.", throwable))
            .onErrorMap(unusedThrowable -> new InternalServerError("Cannot update group status"));
    }
}
