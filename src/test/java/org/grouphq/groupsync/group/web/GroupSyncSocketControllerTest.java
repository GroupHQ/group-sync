package org.grouphq.groupsync.group.web;

import static org.mockito.BDDMockito.given;

import java.time.Duration;
import org.grouphq.groupsync.group.event.GroupEventPublisher;
import org.grouphq.groupsync.group.sync.GroupUpdateService;
import org.grouphq.groupsync.groupservice.domain.exceptions.InternalServerError;
import org.grouphq.groupsync.groupservice.domain.groups.GroupStatus;
import org.grouphq.groupsync.groupservice.domain.outbox.OutboxEvent;
import org.grouphq.groupsync.groupservice.event.daos.GroupCreateRequestEvent;
import org.grouphq.groupsync.groupservice.event.daos.GroupJoinRequestEvent;
import org.grouphq.groupsync.groupservice.event.daos.GroupLeaveRequestEvent;
import org.grouphq.groupsync.groupservice.event.daos.GroupStatusRequestEvent;
import org.grouphq.groupsync.testutility.GroupTestUtility;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
@Tag("UnitTest")
class GroupSyncSocketControllerTest {

    @Mock
    private GroupUpdateService groupUpdateService;

    @Mock
    private GroupEventPublisher groupEventPublisher;

    @InjectMocks
    private GroupSyncSocketController groupSyncSocketController;

    private static final String INTERNAL_SERVER_ERROR_SUFFIX = """
             because the server has encountered an unexpected error.
            Rest assured, this will be investigated.
            """;
    private static final String DUMMY_MESSAGE = "This message should NOT be returned!";

    @Test
    @DisplayName("Test RSocket integration for streaming outbox events")
    void testGetOutboxEventUpdates() {
        final OutboxEvent event = GroupTestUtility.generateOutboxEvent();

        // Mimic a stream of events, followed by an error that should be ignored
        // The stream should continue after the error
        final Flux<OutboxEvent> eventEmitter = Flux.concat(
            Flux.interval(Duration.ofMillis(100))
                .map(tick -> event)
                .take(2),
            Mono.error(new RuntimeException("This message should not cause the stream to stop")),
            Flux.interval(Duration.ofMillis(100))
                .map(tick -> event)
        );

        given(groupUpdateService.outboxEventUpdateStream()).willReturn(eventEmitter);

        StepVerifier.create(groupSyncSocketController.getOutboxEventUpdates())
            .expectNext(event)
            .expectNext(event)
            .expectNext(event)
            .thenCancel()
            .verify();
    }

    @Test
    @DisplayName("Test RSocket integration for creating a group")
    void testCreateGroup() {
        final GroupCreateRequestEvent event = GroupTestUtility.generateGroupCreateRequestEvent();

        given(groupEventPublisher.publishGroupCreateRequest(event)).willReturn(Mono.empty());

        StepVerifier.create(groupSyncSocketController.createGroup(event))
            .expectComplete()
            .verify();
    }

    @Test
    @DisplayName("Test RSocket integration for updating a group status")
    void testUpdateGroupStatus() {
        final GroupStatusRequestEvent event =
            GroupTestUtility.generateGroupStatusRequestEvent(1L, GroupStatus.DISBANDED);

        given(groupEventPublisher.publishGroupUpdateStatusRequest(event)).willReturn(Mono.empty());

        StepVerifier.create(groupSyncSocketController.updateGroupStatus(event))
            .expectComplete()
            .verify();
    }

    @Test
    @DisplayName("Test RSocket integration for joining a group")
    void testJoinGroup() {
        final GroupJoinRequestEvent event = GroupTestUtility.generateGroupJoinRequestEvent();

        given(groupEventPublisher.publishGroupJoinRequest(event)).willReturn(Mono.empty());

        StepVerifier.create(groupSyncSocketController.joinGroup(event))
            .expectComplete()
            .verify();
    }

    @Test
    @DisplayName("Test RSocket integration for leaving a group")
    void testLeaveGroup() {
        final GroupLeaveRequestEvent event = GroupTestUtility.generateGroupLeaveRequestEvent();

        given(groupEventPublisher.publishGroupLeaveRequest(event)).willReturn(Mono.empty());

        StepVerifier.create(groupSyncSocketController.leaveGroup(event))
            .expectComplete()
            .verify();
    }

    @Test
    @DisplayName("Test RSocket integration for returning an error when creating a group")
    void testCreateGroupError() {
        final GroupCreateRequestEvent event = GroupTestUtility.generateGroupCreateRequestEvent();

        given(groupEventPublisher.publishGroupCreateRequest(event))
            .willReturn(Mono.error(new RuntimeException(DUMMY_MESSAGE)));

        final String expectedErrorString = "Cannot create group" + INTERNAL_SERVER_ERROR_SUFFIX;

        StepVerifier.create(groupSyncSocketController.createGroup(event))
            .expectErrorMatches(throwable -> throwable instanceof InternalServerError
                && throwable.getMessage().equals(expectedErrorString))
            .verify();
    }

    @Test
    @DisplayName("Test RSocket integration for returning an error when updating a group status")
    void testUpdateGroupStatusError() {
        final GroupStatusRequestEvent event =
            GroupTestUtility.generateGroupStatusRequestEvent(1L, GroupStatus.DISBANDED);

        given(groupEventPublisher.publishGroupUpdateStatusRequest(event))
            .willReturn(Mono.error(new RuntimeException(DUMMY_MESSAGE)));

        final String expectedErrorString = "Cannot update group status" + INTERNAL_SERVER_ERROR_SUFFIX;

        StepVerifier.create(groupSyncSocketController.updateGroupStatus(event))
            .expectErrorMatches(throwable -> throwable instanceof InternalServerError
                && throwable.getMessage().equals(expectedErrorString))
            .verify();
    }

    @Test
    @DisplayName("Test RSocket integration for returning an error when joining a group")
    void testJoinGroupError() {
        final GroupJoinRequestEvent event = GroupTestUtility.generateGroupJoinRequestEvent();

        given(groupEventPublisher.publishGroupJoinRequest(event))
            .willReturn(Mono.error(new RuntimeException(DUMMY_MESSAGE)));

        final String expectedErrorString = "Cannot join group" + INTERNAL_SERVER_ERROR_SUFFIX;

        StepVerifier.create(groupSyncSocketController.joinGroup(event))
            .expectErrorMatches(throwable -> throwable instanceof InternalServerError
                && throwable.getMessage().equals(expectedErrorString))
            .verify();
    }

    @Test
    @DisplayName("Test RSocket integration for returning an error when leaving a group")
    void testLeaveGroupError() {
        final GroupLeaveRequestEvent event = GroupTestUtility.generateGroupLeaveRequestEvent();

        given(groupEventPublisher.publishGroupLeaveRequest(event))
            .willReturn(Mono.error(new RuntimeException(DUMMY_MESSAGE)));

        final String expectedErrorString = "Cannot leave group" + INTERNAL_SERVER_ERROR_SUFFIX;

        StepVerifier.create(groupSyncSocketController.leaveGroup(event))
            .expectErrorMatches(throwable -> throwable instanceof InternalServerError
                && throwable.getMessage().equals(expectedErrorString))
            .verify();
    }
}
