package org.grouphq.groupsync.group.event;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.grouphq.groupsync.groupservice.domain.groups.GroupStatus;
import org.grouphq.groupsync.groupservice.event.daos.GroupCreateRequestEvent;
import org.grouphq.groupsync.groupservice.event.daos.GroupJoinRequestEvent;
import org.grouphq.groupsync.groupservice.event.daos.GroupLeaveRequestEvent;
import org.grouphq.groupsync.groupservice.event.daos.GroupStatusRequestEvent;
import org.grouphq.groupsync.testutility.GroupTestUtility;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.binder.test.OutputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.Message;

@SpringBootTest
@Import(TestChannelBinderConfiguration.class)
@Tag("IntegrationTest")
public class GroupEventPublisherIntegrationTest {

    @Autowired
    private OutputDestination outputDestination;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
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

        groupEventPublisher.publishGroupCreateRequest(groupCreateRequest);

        Message<byte[]> message = outputDestination.receive(1000, groupCreateRequestDestination);

        assertThat(message).isNotNull();
        assertThat(objectMapper.convertValue(groupCreateRequest, GroupCreateRequestEvent.class))
            .isEqualTo(groupCreateRequest);
    }

    @Test
    @DisplayName("Publishes group update status requests")
    void publishesGroupUpdateStatusRequests() {
        var groupStatusRequestEvent =
            GroupTestUtility.generateGroupStatusRequestEvent(1L, GroupStatus.DISBANDED);

        groupEventPublisher.publishGroupUpdateStatusRequest(groupStatusRequestEvent);

        Message<byte[]> message = outputDestination.receive(1000, groupUpdateStatusRequestDestination);

        assertThat(message).isNotNull();
        assertThat(objectMapper.convertValue(groupStatusRequestEvent, GroupStatusRequestEvent.class))
            .isEqualTo(groupStatusRequestEvent);
    }

    @Test
    @DisplayName("Publishes group join requests")
    void publishesGroupJoinRequests() {
        var groupJoinRequestEvent = GroupTestUtility.generateGroupJoinRequestEvent();

        groupEventPublisher.publishGroupJoinRequest(groupJoinRequestEvent);

        Message<byte[]> message = outputDestination.receive(1000, groupJoinRequestDestination);

        assertThat(message).isNotNull();
        assertThat(objectMapper.convertValue(groupJoinRequestEvent, GroupJoinRequestEvent.class))
            .isEqualTo(groupJoinRequestEvent);
    }

    @Test
    @DisplayName("Publishes group leave requests")
    void publishesGroupLeaveRequests() {
        var groupLeaveRequestEvent = GroupTestUtility.generateGroupLeaveRequestEvent();

        groupEventPublisher.publishGroupLeaveRequest(groupLeaveRequestEvent);

        Message<byte[]> message = outputDestination.receive(1000, groupLeaveRequestDestination);

        assertThat(message).isNotNull();
        assertThat(objectMapper.convertValue(groupLeaveRequestEvent, GroupLeaveRequestEvent.class))
            .isEqualTo(groupLeaveRequestEvent);
    }
}
