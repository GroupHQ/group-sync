package org.grouphq.groupsync.group.sync.state;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.grouphq.groupsync.group.domain.PublicOutboxEvent;
import org.grouphq.groupsync.groupservice.domain.groups.Group;
import org.grouphq.groupsync.groupservice.domain.groups.GroupStatus;
import org.grouphq.groupsync.groupservice.domain.outbox.enums.EventStatus;
import org.grouphq.groupsync.groupservice.domain.outbox.enums.EventType;
import org.grouphq.groupsync.groupservice.web.objects.egress.PublicMember;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Maintains the current relevant events to enable clients
 * to build the current state of groups and their members.
 * Relies on Java's Standard Library Concurrency Utilities
 */
@Service
@Slf4j
public class GroupStateService {
    private final ConcurrentHashMap<Long, PublicOutboxEvent> groupToEventMap = new ConcurrentHashMap<>(50);
    private final ConcurrentHashMap<Long, Set<PublicMember>> groupToMembersMap = new ConcurrentHashMap<>(250);

    public Mono<Void> resetState() {
        return Mono.fromRunnable(() -> {
            groupToEventMap.clear();
            groupToMembersMap.clear();
        });
    }

    public Flux<PublicOutboxEvent> getCurrentGroupEvents() {
        return Flux.fromIterable(groupToEventMap.values());
    }

    public Flux<PublicMember> getMembersForGroup(Long groupId) {
        final Set<PublicMember> currentMembers = groupToMembersMap.getOrDefault(groupId, Set.of());
        return Flux.fromIterable(new HashSet<>(currentMembers));
    }

    public Mono<PublicOutboxEvent> saveGroupEvent(PublicOutboxEvent publicOutboxEvent) {
        if (!(publicOutboxEvent.eventData() instanceof Group group)) {
            log.warn("Expected received event to have its event data containing a group: {}", publicOutboxEvent);
            return Mono.empty();
        }

        log.info("Saving event {} to initial state", publicOutboxEvent);

        groupToEventMap.put(group.id(), publicOutboxEvent);
        groupToMembersMap.put(group.id(), createConcurrentMemberSet(group.members()));

        return Mono.just(publicOutboxEvent);
    }

    private Set<PublicMember> createConcurrentMemberSet(Set<PublicMember> currentMembers) {
        final Set<PublicMember> publicMemberSet = Collections.newSetFromMap(new ConcurrentHashMap<>());
        publicMemberSet.addAll(currentMembers);
        return publicMemberSet;
    }

    public Mono<PublicOutboxEvent> handleEventUpdate(PublicOutboxEvent event) {
        if (event.eventStatus().equals(EventStatus.FAILED)) {
            return Mono.empty();
        }

        return switch (event.eventType()) {
            case GROUP_CREATED, GROUP_UPDATED -> handleGroupUpdate(event);
            case MEMBER_JOINED, MEMBER_LEFT -> handleMemberUpdate(event);
            default -> Mono.empty();
        };
    }

    private Mono<PublicOutboxEvent> handleGroupUpdate(PublicOutboxEvent event) {
        if (!(event.eventData() instanceof Group newGroup)) {
            log.warn("Group update requested with non-group event data from event {}", event);
            return Mono.empty();
        }

        log.info("Handling group update for event: {}", event);

        final Long groupId = event.aggregateId();
        if (Objects.requireNonNull(event.eventType()) == EventType.GROUP_CREATED) {
            return saveGroupEvent(event);
        } else if (event.eventType() == EventType.GROUP_UPDATED) {
            if (newGroup.status().equals(GroupStatus.ACTIVE)) {
                groupToEventMap.put(groupId, event);
            } else {
                groupToEventMap.remove(groupId);
                groupToMembersMap.remove(groupId);
            }
        }

        return Mono.just(event);
    }

    private Mono<PublicOutboxEvent> handleMemberUpdate(PublicOutboxEvent event) {
        if (!(event.eventData() instanceof PublicMember newMemberState)) {
            log.warn("Member update requested on non-member event data {}", event);
            return Mono.empty();
        }

        final Long groupId = event.aggregateId();

        if (!groupToMembersMap.containsKey(groupId)) {
            log.warn("Asked to update member from event {}, but member set not found", event);
            return Mono.empty();
        }

        log.info("Handling member update for event: {}", event);

        final Set<PublicMember> groupMembers = groupToMembersMap.get(groupId);

        if (Objects.requireNonNull(event.eventType()) == EventType.MEMBER_JOINED) {
            groupMembers.add(newMemberState);
        } else if (event.eventType() == EventType.MEMBER_LEFT) {
            groupMembers.remove(newMemberState);
        }

        return Mono.just(event);
    }
}
