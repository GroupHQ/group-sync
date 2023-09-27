package org.grouphq.groupsync.group.event;

import org.grouphq.groupsync.groupservice.event.daos.GroupCreateRequestEvent;
import org.grouphq.groupsync.groupservice.event.daos.GroupJoinRequestEvent;
import org.grouphq.groupsync.groupservice.event.daos.GroupLeaveRequestEvent;
import org.grouphq.groupsync.groupservice.event.daos.GroupStatusRequestEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Publishes group events to the group event streams.
 */
@Component
public class GroupEventPublisher {
    private final StreamBridge streamBridge;

    @Value("${spring.cloud.stream.bindings.groupCreateRequests-out-0.destination}")
    private String groupCreateRequestDestination;

    @Value("${spring.cloud.stream.bindings.groupStatusRequests-out-0.destination}")
    private String groupUpdateStatusRequestDestination;

    @Value("${spring.cloud.stream.bindings.groupJoinRequests-out-0.destination}")
    private String groupJoinRequestDestination;

    @Value("${spring.cloud.stream.bindings.groupLeaveRequests-out-0.destination}")
    private String groupLeaveRequestDestination;

    public GroupEventPublisher(StreamBridge streamBridge) {
        this.streamBridge = streamBridge;
    }

    public Mono<Void> publishGroupCreateRequest(GroupCreateRequestEvent groupCreateRequest) {
        return Mono.fromRunnable(() ->
            streamBridge.send(groupCreateRequestDestination, groupCreateRequest));
    }

    public Mono<Void> publishGroupUpdateStatusRequest(
        GroupStatusRequestEvent groupUpdateStatusRequest) {
        return Mono.fromRunnable(() ->
            streamBridge.send(groupUpdateStatusRequestDestination, groupUpdateStatusRequest));
    }

    public Mono<Void> publishGroupJoinRequest(GroupJoinRequestEvent groupJoinRequest) {
        return Mono.fromRunnable(() ->
           streamBridge.send(groupJoinRequestDestination, groupJoinRequest));
    }

    public Mono<Void> publishGroupLeaveRequest(GroupLeaveRequestEvent groupLeaveRequest) {
        return Mono.fromRunnable(() ->
            streamBridge.send(groupLeaveRequestDestination, groupLeaveRequest));
    }
}
