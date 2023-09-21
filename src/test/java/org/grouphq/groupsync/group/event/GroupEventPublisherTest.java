package org.grouphq.groupsync.group.event;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import org.grouphq.groupsync.groupservice.domain.groups.GroupStatus;
import org.grouphq.groupsync.testutility.GroupTestUtility;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.stream.function.StreamBridge;

@ExtendWith(MockitoExtension.class)
@Tag("UnitTest")
public class GroupEventPublisherTest {

    @Mock
    private StreamBridge streamBridge;

    @InjectMocks
    private GroupEventPublisher groupEventPublisher;

    @Value("${spring.cloud.stream.bindings.groupCreateRequests-out-0.destination}")
    private String groupCreateRequestDestination;

    @Value("${spring.cloud.stream.bindings.groupStatusRequests-out-0.destination}")
    private String groupUpdateStatusRequestDestination;

    @Value("${spring.cloud.stream.bindings.groupJoinRequests-out-0.destination}")
    private String groupJoinRequestDestination;

    @Value("${spring.cloud.stream.bindings.groupLeaveRequests-out-0.destination}")
    private String groupLeaveRequestDestination;

    @Test
    @DisplayName("Publishes group create requests")
    void publishesGroupCreateRequests() {
        var groupCreateRequest = GroupTestUtility.generateGroupCreateRequestEvent();
        given(streamBridge.send(groupCreateRequestDestination, groupCreateRequest))
            .willReturn(true);

        groupEventPublisher.publishGroupCreateRequest(groupCreateRequest);

        verify(streamBridge).send(groupCreateRequestDestination, groupCreateRequest);
    }

    @Test
    @DisplayName("Publishes group update status requests")
    void publishesGroupUpdateStatusRequests() {
        var groupStatusRequestEvent =
            GroupTestUtility.generateGroupStatusRequestEvent(1L, GroupStatus.DISBANDED);
        given(streamBridge.send(groupUpdateStatusRequestDestination, groupStatusRequestEvent))
            .willReturn(true);

        groupEventPublisher.publishGroupUpdateStatusRequest(groupStatusRequestEvent);

        verify(streamBridge).send(groupUpdateStatusRequestDestination, groupStatusRequestEvent);
    }

    @Test
    @DisplayName("Publishes group join requests")
    void publishesGroupJoinRequests() {
        var groupJoinRequestEvent = GroupTestUtility.generateGroupJoinRequestEvent();
        given(streamBridge.send(groupJoinRequestDestination, groupJoinRequestEvent))
            .willReturn(true);

        groupEventPublisher.publishGroupJoinRequest(groupJoinRequestEvent);

        verify(streamBridge).send(groupJoinRequestDestination, groupJoinRequestEvent);
    }

    @Test
    @DisplayName("Publishes group leave requests")
    void publishesGroupLeaveRequests() {
        var groupLeaveRequestEvent = GroupTestUtility.generateGroupLeaveRequestEvent();
        given(streamBridge.send(groupLeaveRequestDestination, groupLeaveRequestEvent))
            .willReturn(true);

        groupEventPublisher.publishGroupLeaveRequest(groupLeaveRequestEvent);

        verify(streamBridge).send(groupLeaveRequestDestination, groupLeaveRequestEvent);
    }
}
