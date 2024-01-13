package org.grouphq.groupsync.group.web.features;

import java.security.Principal;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.grouphq.groupsync.group.event.GroupEventPublisher;
import org.grouphq.groupsync.group.security.UserService;
import org.grouphq.groupsync.groupservice.domain.exceptions.InternalServerError;
import org.grouphq.groupsync.groupservice.event.daos.requestevent.GroupCreateRequestEvent;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * RSocket handler for sending group create requests.
 */
@Slf4j
@RequiredArgsConstructor
@RestController
@ConditionalOnProperty(name = "grouphq.features.groups.create", havingValue = "true")
public class GroupCreateController {

    private final UserService userService;

    private final GroupEventPublisher groupEventPublisher;

    @MessageMapping("groups.create")
    public Mono<Void> createGroup(GroupCreateRequestEvent groupCreateRequestEvent) {
        return createCreateRequestWithCurrentAuthorization(groupCreateRequestEvent)
            .flatMap(eventWithAuth -> groupEventPublisher.publishGroupCreateRequest(eventWithAuth)
                .doOnSuccess(unused -> log.debug("Sent create request: {}", eventWithAuth))
                .doOnError(throwable -> log.error("Error while creating group.", throwable))
                .onErrorMap(unusedThrowable -> new InternalServerError("Cannot create group"))
            );
    }

    private Mono<GroupCreateRequestEvent> createCreateRequestWithCurrentAuthorization(
        GroupCreateRequestEvent groupCreateRequestEvent) {
        return userService.getUserAuthentication()
            .map(Principal::getName)
            .flatMap(websocketId -> Mono.just(new GroupCreateRequestEvent(
                UUID.randomUUID(),
                groupCreateRequestEvent.getTitle(),
                groupCreateRequestEvent.getDescription(),
                groupCreateRequestEvent.getMaxGroupSize(),
                websocketId,
                websocketId,
                Instant.now()
            )));
    }
}
