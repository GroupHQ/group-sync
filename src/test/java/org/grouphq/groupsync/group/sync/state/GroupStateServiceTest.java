package org.grouphq.groupsync.group.sync.state;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.UUID;
import org.grouphq.groupsync.GroupTestUtility;
import org.grouphq.groupsync.group.domain.PublicOutboxEvent;
import org.grouphq.groupsync.groupservice.domain.groups.Group;
import org.grouphq.groupsync.groupservice.domain.groups.GroupStatus;
import org.grouphq.groupsync.groupservice.domain.members.Member;
import org.grouphq.groupsync.groupservice.domain.outbox.OutboxEvent;
import org.grouphq.groupsync.groupservice.domain.outbox.enums.AggregateType;
import org.grouphq.groupsync.groupservice.domain.outbox.enums.EventStatus;
import org.grouphq.groupsync.groupservice.domain.outbox.enums.EventType;
import org.grouphq.groupsync.groupservice.web.objects.egress.PublicMember;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

@Tag("UnitTest")
class GroupStateServiceTest {

    GroupStateService groupStateService;

    public GroupStateServiceTest() {
        this.groupStateService = new GroupStateService();
    }

    @Test
    @DisplayName("Saves group events")
    void testGetCurrentGroupEvents() {
        final Group group =
            GroupTestUtility.generateFullGroupDetailsWithMembers(GroupStatus.ACTIVE);
        final OutboxEvent outboxEvent =
            GroupTestUtility.generateOutboxEvent(group.id(), group, EventType.GROUP_CREATED);
        final PublicOutboxEvent publicOutboxEvent =
            PublicOutboxEvent.convertOutboxEvent(outboxEvent);

        assertThat(publicOutboxEvent.eventData()).isExactlyInstanceOf(Group.class);
        assertThat(publicOutboxEvent.eventType()).isEqualTo(EventType.GROUP_CREATED);

        StepVerifier.create(
            groupStateService.saveGroupEvent(publicOutboxEvent)
                .thenMany(groupStateService.getCurrentGroupEvents())
            )
            .assertNext(savedEvent -> assertThat(savedEvent).isEqualTo(publicOutboxEvent))
            .verifyComplete();
    }

    @Test
    @DisplayName("Test resetState, should clear all events and members from state")
    void testResetState() {
        final Group group =
            GroupTestUtility.generateFullGroupDetailsWithMembers(GroupStatus.ACTIVE);
        final OutboxEvent outboxEvent =
            GroupTestUtility.generateOutboxEvent(group.id(), group, EventType.GROUP_CREATED);
        final PublicOutboxEvent publicOutboxEvent = PublicOutboxEvent.convertOutboxEvent(outboxEvent);

        assertThat(publicOutboxEvent.eventData()).isExactlyInstanceOf(Group.class);
        assertThat(publicOutboxEvent.eventType()).isEqualTo(EventType.GROUP_CREATED);

        StepVerifier.create(
                groupStateService.saveGroupEvent(publicOutboxEvent)
                    .then(groupStateService.resetState())
                    .thenMany(groupStateService.getCurrentGroupEvents())
            )
            .verifyComplete();
    }

    @Test
    @DisplayName("Saves members for new groups")
    void testGetMembersForGroup() {
        final Group group =
            GroupTestUtility.generateFullGroupDetailsWithMembers(GroupStatus.ACTIVE);
        final OutboxEvent outboxEvent =
            GroupTestUtility.generateOutboxEvent(group.id(), group, EventType.GROUP_CREATED);
        final PublicOutboxEvent publicOutboxEvent =
            PublicOutboxEvent.convertOutboxEvent(outboxEvent);

        assertThat(publicOutboxEvent.eventData()).isExactlyInstanceOf(Group.class);
        assertThat(publicOutboxEvent.eventType()).isEqualTo(EventType.GROUP_CREATED);

        StepVerifier.create(
            groupStateService.saveGroupEvent(publicOutboxEvent)
                .thenMany(groupStateService.getMembersForGroup(group.id()))
                .collectList()
            )
            .assertNext(publicMembers ->
                assertThat(publicMembers)
                    .containsExactlyInAnyOrderElementsOf(new ArrayList<>(group.members()))
            )
            .verifyComplete();
    }

    @Test
    @DisplayName("Handles group created updates")
    void testHandleGroupUpdate() {
        final Group group =
            GroupTestUtility.generateFullGroupDetails(GroupStatus.ACTIVE);
        final OutboxEvent outboxEvent =
            GroupTestUtility.generateOutboxEvent(group.id(), group, EventType.GROUP_CREATED);

        final PublicOutboxEvent publicOutboxEvent = PublicOutboxEvent.convertOutboxEvent(outboxEvent);
        assertThat(publicOutboxEvent.eventData()).isExactlyInstanceOf(Group.class);
        assertThat(publicOutboxEvent.eventType()).isEqualTo(EventType.GROUP_CREATED);

        StepVerifier.create(
            groupStateService.handleEventUpdate(publicOutboxEvent)
                .thenMany(groupStateService.getCurrentGroupEvents())
            )
            .assertNext(savedEvent -> assertThat(savedEvent).isEqualTo(publicOutboxEvent))
            .verifyComplete();
    }

    @Test
    @DisplayName("Handle group updated updates by updating group when a group is still active")
    void testHandleGroupUpdateWhenGroupActive() {
        final Group group =
            GroupTestUtility.generateFullGroupDetails(GroupStatus.ACTIVE);
        final OutboxEvent outboxEvent =
            GroupTestUtility.generateOutboxEvent(group.id(), group, EventType.GROUP_UPDATED);

        final PublicOutboxEvent publicOutboxEvent = PublicOutboxEvent.convertOutboxEvent(outboxEvent);
        assertThat(publicOutboxEvent.eventData()).isExactlyInstanceOf(Group.class);
        assertThat(publicOutboxEvent.eventType()).isEqualTo(EventType.GROUP_UPDATED);

        StepVerifier.create(
            groupStateService.handleEventUpdate(publicOutboxEvent)
                .thenMany(groupStateService.getCurrentGroupEvents())
            )
            .assertNext(savedEvent -> assertThat(savedEvent).isEqualTo(publicOutboxEvent))
            .verifyComplete();
    }

    @Test
    @DisplayName("Handle group updated updates by removing group when a group becomes inactive")
    void testHandleGroupUpdateWhenGroupInactive() {
        final Group group =
            GroupTestUtility.generateFullGroupDetails(GroupStatus.AUTO_DISBANDED);
        final OutboxEvent outboxEvent =
            GroupTestUtility.generateOutboxEvent(group.id(), group, EventType.GROUP_UPDATED);

        final PublicOutboxEvent publicOutboxEvent = PublicOutboxEvent.convertOutboxEvent(outboxEvent);
        assertThat(publicOutboxEvent.eventData()).isExactlyInstanceOf(Group.class);
        assertThat(publicOutboxEvent.eventType()).isEqualTo(EventType.GROUP_UPDATED);

        StepVerifier.create(
            groupStateService.handleEventUpdate(publicOutboxEvent)
                .thenMany(groupStateService.getCurrentGroupEvents())
            )
            .verifyComplete();
    }

    @Test
    @DisplayName("Removes members belonging to a group when a group is removed")
    void testRemoveMembersForGroup() {
        final Group group =
            GroupTestUtility.generateFullGroupDetailsWithMembers(GroupStatus.AUTO_DISBANDED);
        final OutboxEvent outboxEvent =
            GroupTestUtility.generateOutboxEvent(group.id(), group, EventType.GROUP_UPDATED);
        final PublicOutboxEvent publicOutboxEvent =
            PublicOutboxEvent.convertOutboxEvent(outboxEvent);

        assertThat(publicOutboxEvent.eventData()).isExactlyInstanceOf(Group.class);
        assertThat(publicOutboxEvent.eventType()).isEqualTo(EventType.GROUP_UPDATED);

        StepVerifier.create(
                groupStateService.handleEventUpdate(publicOutboxEvent)
                    .thenMany(groupStateService.getMembersForGroup(group.id()))
                    .collectList()
            )
            .assertNext(publicMembers -> assertThat(publicMembers).isEmpty())
            .verifyComplete();
    }

    @Test
    @DisplayName("Handles member joined updates")
    void testHandleMemberUpdate() {
        final Group group =
            GroupTestUtility.generateFullGroupDetails(GroupStatus.ACTIVE);
        final OutboxEvent outboxEvent =
            GroupTestUtility.generateOutboxEvent(group.id(), group, EventType.GROUP_CREATED);

        final PublicOutboxEvent groupPublicOutboxEvent = PublicOutboxEvent.convertOutboxEvent(outboxEvent);
        assertThat(groupPublicOutboxEvent.eventData()).isExactlyInstanceOf(Group.class);
        assertThat(groupPublicOutboxEvent.eventType()).isEqualTo(EventType.GROUP_CREATED);

        final Member member = GroupTestUtility.generateFullMemberDetails(null, group.id());
        final PublicMember publicMember = Member.toPublicMember(member);
        final OutboxEvent memberOutboxEvent =
            GroupTestUtility.generateOutboxEvent(group.id(), member, EventType.MEMBER_JOINED);

        final PublicOutboxEvent memberPublicOutboxEvent =
            PublicOutboxEvent.convertOutboxEvent(memberOutboxEvent);
        assertThat(memberPublicOutboxEvent.eventData()).isExactlyInstanceOf(PublicMember.class);
        assertThat(memberPublicOutboxEvent.eventType()).isEqualTo(EventType.MEMBER_JOINED);

        StepVerifier.create(
            groupStateService.handleEventUpdate(groupPublicOutboxEvent)
                .then(groupStateService.handleEventUpdate(memberPublicOutboxEvent))
                .thenMany(groupStateService.getMembersForGroup(group.id()))
            )
            .assertNext(savedMember -> assertThat(savedMember).isEqualTo(publicMember))
            .verifyComplete();
    }

    @Test
    @DisplayName("Handles member left updates")
    void testHandleMemberLeft() {
        final Group group = GroupTestUtility.generateFullGroupDetails(GroupStatus.ACTIVE);
        final OutboxEvent outboxEvent =
            GroupTestUtility.generateOutboxEvent(group.id(), group, EventType.GROUP_CREATED);

        final PublicOutboxEvent groupPublicOutboxEvent = PublicOutboxEvent.convertOutboxEvent(outboxEvent);
        assertThat(groupPublicOutboxEvent.eventData()).isExactlyInstanceOf(Group.class);
        assertThat(groupPublicOutboxEvent.eventType()).isEqualTo(EventType.GROUP_CREATED);

        final Member member = GroupTestUtility.generateFullMemberDetails(null, group.id());
        final OutboxEvent memberJoinedOutboxEvent =
            GroupTestUtility.generateOutboxEvent(group.id(), member, EventType.MEMBER_JOINED);

        final PublicOutboxEvent memberJoinedPublicOutboxEvent =
            PublicOutboxEvent.convertOutboxEvent(memberJoinedOutboxEvent);
        assertThat(memberJoinedPublicOutboxEvent.eventData()).isExactlyInstanceOf(PublicMember.class);
        assertThat(memberJoinedPublicOutboxEvent.eventType()).isEqualTo(EventType.MEMBER_JOINED);

        final OutboxEvent memberLeftOutboxEvent =
            GroupTestUtility.generateOutboxEvent(group.id(), member, EventType.MEMBER_LEFT);
        final PublicOutboxEvent memberLeftPublicOutboxEvent =
            PublicOutboxEvent.convertOutboxEvent(memberLeftOutboxEvent);
        assertThat(memberLeftPublicOutboxEvent.eventData()).isExactlyInstanceOf(PublicMember.class);
        assertThat(memberLeftPublicOutboxEvent.eventType()).isEqualTo(EventType.MEMBER_LEFT);

        StepVerifier.create(
                groupStateService.handleEventUpdate(groupPublicOutboxEvent)
                    .then(groupStateService.handleEventUpdate(memberJoinedPublicOutboxEvent))
                    .then(groupStateService.handleEventUpdate(memberLeftPublicOutboxEvent))
                    .thenMany(groupStateService.getMembersForGroup(group.id()))
            )
            .verifyComplete();
    }

    @Test
    @DisplayName("Does not add member if group does not exist")
    void testHandleMemberUpdateWhenGroupDoesNotExist() {
        final Member member = GroupTestUtility.generateFullMemberDetails(null, 1L);
        final OutboxEvent memberJoinedOutboxEvent =
            GroupTestUtility.generateOutboxEvent(1L, member, EventType.MEMBER_JOINED);

        final PublicOutboxEvent memberJoinedPublicOutboxEvent =
            PublicOutboxEvent.convertOutboxEvent(memberJoinedOutboxEvent);
        assertThat(memberJoinedPublicOutboxEvent.eventData()).isExactlyInstanceOf(PublicMember.class);
        assertThat(memberJoinedPublicOutboxEvent.eventType()).isEqualTo(EventType.MEMBER_JOINED);

        StepVerifier.create(
            groupStateService.handleEventUpdate(memberJoinedPublicOutboxEvent)
                .thenMany(groupStateService.getMembersForGroup(1L))
            )
            .verifyComplete();
    }

    @Test
    @DisplayName("Does not process failed events")
    void testHandleFailedEvent() {
        final Group group = GroupTestUtility.generateFullGroupDetails(GroupStatus.ACTIVE);
        final PublicOutboxEvent publicOutboxEvent = new PublicOutboxEvent(
            UUID.randomUUID(),
            1L,
            AggregateType.GROUP,
            EventType.GROUP_CREATED,
            group,
            EventStatus.FAILED,
            null
        );

        StepVerifier.create(
            groupStateService.handleEventUpdate(publicOutboxEvent)
                .thenMany(groupStateService.getCurrentGroupEvents())
            )
            .verifyComplete();
    }
}
