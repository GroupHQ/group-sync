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
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
@Tag("UnitTest")
class GroupEventPublisherTest {

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
        final var groupCreateRequest = GroupTestUtility.generateGroupCreateRequestEvent();
        given(streamBridge.send(groupCreateRequestDestination, groupCreateRequest))
            .willReturn(true);

        StepVerifier.create(groupEventPublisher.publishGroupCreateRequest(groupCreateRequest))
            .verifyComplete();

        verify(streamBridge).send(groupCreateRequestDestination, groupCreateRequest);
    }

    @Test
    @DisplayName("Publishes group update status requests")
    void publishesGroupUpdateStatusRequests() {
        final var groupStatusRequestEvent =
            GroupTestUtility.generateGroupStatusRequestEvent(1L, GroupStatus.DISBANDED);
        given(streamBridge.send(groupUpdateStatusRequestDestination, groupStatusRequestEvent))
            .willReturn(true);

        StepVerifier
            .create(groupEventPublisher.publishGroupUpdateStatusRequest(groupStatusRequestEvent))
            .verifyComplete();

        verify(streamBridge).send(groupUpdateStatusRequestDestination, groupStatusRequestEvent);
    }

    @Test
    @DisplayName("Publishes group join requests")
    void publishesGroupJoinRequests() {
        final var groupJoinRequestEvent = GroupTestUtility.generateGroupJoinRequestEvent();
        given(streamBridge.send(groupJoinRequestDestination, groupJoinRequestEvent))
            .willReturn(true);

        StepVerifier
            .create(groupEventPublisher.publishGroupJoinRequest(groupJoinRequestEvent))
            .verifyComplete();

        verify(streamBridge).send(groupJoinRequestDestination, groupJoinRequestEvent);
    }

    @Test
    @DisplayName("Publishes group leave requests")
    void publishesGroupLeaveRequests() {
        final var groupLeaveRequestEvent = GroupTestUtility.generateGroupLeaveRequestEvent();
        given(streamBridge.send(groupLeaveRequestDestination, groupLeaveRequestEvent))
            .willReturn(true);

        StepVerifier
            .create(groupEventPublisher.publishGroupLeaveRequest(groupLeaveRequestEvent))
            .verifyComplete();

        verify(streamBridge).send(groupLeaveRequestDestination, groupLeaveRequestEvent);
    }
}
