package org.grouphq.groupsync.group.web.features;

import lombok.extern.slf4j.Slf4j;
import org.grouphq.groupsync.group.event.GroupEventPublisher;
import org.grouphq.groupsync.groupservice.domain.exceptions.InternalServerError;
import org.grouphq.groupsync.groupservice.event.daos.GroupCreateRequestEvent;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * RSocket handler for sending group create requests.
 */
@Slf4j
@RestController
@ConditionalOnProperty(name = "grouphq.features.groups.create", havingValue = "true")
public class GroupCreateController {
    private final GroupEventPublisher groupEventPublisher;

    public GroupCreateController(GroupEventPublisher groupEventPublisher) {
        this.groupEventPublisher = groupEventPublisher;
    }

    @MessageMapping("groups.create")
    public Mono<Void> createGroup(GroupCreateRequestEvent groupCreateRequestEvent) {
        return groupEventPublisher.publishGroupCreateRequest(groupCreateRequestEvent)
            .doOnSuccess(unused -> log.debug("Sent create group request."))
            .doOnError(throwable -> log.error("Error while creating group.", throwable))
            .onErrorMap(unusedThrowable -> new InternalServerError("Cannot create group"));
    }
}
