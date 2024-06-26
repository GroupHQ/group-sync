package org.grouphq.groupsync;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.datafaker.Faker;
import org.grouphq.groupsync.groupservice.domain.groups.Group;
import org.grouphq.groupsync.groupservice.domain.groups.GroupStatus;
import org.grouphq.groupsync.groupservice.domain.members.Member;
import org.grouphq.groupsync.groupservice.domain.members.MemberStatus;
import org.grouphq.groupsync.groupservice.domain.outbox.ErrorData;
import org.grouphq.groupsync.groupservice.domain.outbox.EventDataModel;
import org.grouphq.groupsync.groupservice.domain.outbox.OutboxEvent;
import org.grouphq.groupsync.groupservice.domain.outbox.enums.AggregateType;
import org.grouphq.groupsync.groupservice.domain.outbox.enums.EventStatus;
import org.grouphq.groupsync.groupservice.domain.outbox.enums.EventType;
import org.grouphq.groupsync.groupservice.event.daos.requestevent.GroupCreateRequestEvent;
import org.grouphq.groupsync.groupservice.event.daos.requestevent.GroupJoinRequestEvent;
import org.grouphq.groupsync.groupservice.event.daos.requestevent.GroupLeaveRequestEvent;
import org.grouphq.groupsync.groupservice.event.daos.requestevent.GroupStatusRequestEvent;
import org.grouphq.groupsync.groupservice.web.objects.egress.PublicMember;

/**
 * Utility class for common functionality needed by multiple tests.
 */
public final class GroupTestUtility {

    private static final Faker FAKER = new Faker();
    static final String OWNER = "system";

    private GroupTestUtility() {}

    /**
     * Generates a group that would be found in the database.
     * Note that IDs are intentionally 12 digits so that they'll always be considered a
     * {@code long} type. Otherwise, it will introduce flakiness to the JsonTests as they
     * dynamically assign an {@code int} or {@code long} type based on if the number is at
     * or below {@code Integer.MAX_VALUE}.
     *
     * @param status the status of the group.
     *
     * @return A group object with all details.
     */
    public static Group generateFullGroupDetails(GroupStatus status) {
        final Faker faker = new Faker();

        // Generate capacities and ensure space for at least 100 members to join
        final int maxCapacity = faker.number().numberBetween(100, 150);

        return new Group(
            faker.number().randomNumber(12, true),
            faker.lorem().sentence(),
            faker.lorem().sentence(20),
            maxCapacity,
            status,
            null,
            Instant.now(),
            Instant.now(),
            OWNER,
            OWNER,
            0,
            Set.of()
        );
    }

    /**
     * Overloaded method for {@link #generateFullGroupDetails(GroupStatus status)}.
     *
     * @param groupId the group's id.
     * @param status the status of the group.
     *
     * @return a Group object with all details.
     */
    public static Group generateFullGroupDetails(Long groupId, GroupStatus status) {

        final int maxCapacity = FAKER.number().numberBetween(100, 150);

        return new Group(
            groupId,
            FAKER.lorem().sentence(),
            FAKER.lorem().sentence(20),
            maxCapacity,
            status,
            null,
            Instant.now(),
            Instant.now(),
            OWNER,
            OWNER,
            0,
            Set.of()
        );
    }

    /**
     * Overloaded method for {@link #generateFullGroupDetails(GroupStatus status)}.
     *
     * @param maxGroupSize maximum number of users that can belong to the group.
     * @param status the status of the group.
     *
     * @return a group object with all details.
     */
    public static Group generateFullGroupDetails(
        int maxGroupSize,
        GroupStatus status) {

        return new Group(
            FAKER.number().randomNumber(12, true),
            FAKER.lorem().sentence(),
            FAKER.lorem().sentence(20),
            maxGroupSize,
            status,
            null,
            Instant.now(),
            Instant.now(),
            OWNER,
            OWNER,
            0,
            Set.of()
        );
    }

    /**
     * Generates a group with members.
     *
     * @return A group object with all details.
     */
    public static Group generateFullGroupDetailsWithMembers(GroupStatus status) {
        final Faker faker = new Faker();

        final Long groupId = faker.number().randomNumber(12, true);

        // Generate capacities and ensure space for at least 10 members to join
        final int maxCapacity = faker.number().numberBetween(10, 25);

        final Set<PublicMember> members = new HashSet<>();
        for (int i = 0; i < maxCapacity / 2; i++) {
            final Member member = generateFullMemberDetails(faker.name().firstName(), groupId);
            final PublicMember publicMember = Member.toPublicMember(member);
            members.add(publicMember);
        }

        return new Group(
            faker.number().randomNumber(12, true),
            faker.lorem().sentence(),
            faker.lorem().sentence(20),
            maxCapacity,
            status,
            Instant.now(),
            Instant.now(),
            Instant.now(),
            OWNER,
            OWNER,
            0,
            members
        );
    }

    /**
     * Generates a member that would be found in the database. Note that IDs are
     * intentionally 12 digits so that they'll always be considered a {@code long} type.
     *
     * @see #generateFullGroupDetails(GroupStatus status) for more info.
     *
     * @return A group object with all details.
     */
    public static Member generateFullMemberDetails() {

        return new Member(
            FAKER.number().randomNumber(12, true),
            UUID.randomUUID(),
            FAKER.name().firstName(),
            FAKER.number().randomNumber(12, true),
            MemberStatus.ACTIVE,
            null,
            Instant.now(),
            Instant.now(),
            OWNER,
            OWNER,
            0
        );
    }

    /**
     * Generates a member that would be found in the database. Note that IDs are
     * intentionally 12 digits so that they'll always be considered a {@code long} type.
     *
     * @see #generateFullGroupDetails(GroupStatus status) for more info.
     *
     * @param username the username of the member.
     * @param groupId the group ID the member belongs to.
     *
     * @return A group object with all details.
     */
    public static Member generateFullMemberDetails(String username, Long groupId) {

        return new Member(
            FAKER.number().randomNumber(12, true),
            UUID.randomUUID(),
            username == null ? FAKER.name().firstName() : username,
            groupId,
            MemberStatus.ACTIVE,
            null,
            Instant.now(),
            Instant.now(),
            OWNER,
            OWNER,
            0
        );
    }

    /**
     * Overloaded method for {@link #generateFullMemberDetails()}.
     *
     * @see #generateFullMemberDetails(String username, Long groupId) for more info.
     *
     * @param websocketId the user's unique connection id
     * @param username the username of the member.
     * @param groupId the group ID the member belongs to.
     *
     * @return A group object with all details.
     */
    public static Member generateFullMemberDetails(UUID websocketId, String username, Long groupId) {

        return new Member(
            FAKER.number().randomNumber(12, true),
            websocketId,
            username == null ? FAKER.name().firstName() : username,
            groupId,
            MemberStatus.ACTIVE,
            null,
            Instant.now(),
            Instant.now(),
            OWNER,
            OWNER,
            0
        );
    }


    /**
     * Generates a random group join request object.
     *
     * @return a GroupJoinRequestEvent object with all details.
     */
    public static GroupJoinRequestEvent generateGroupJoinRequestEvent() {

        return new GroupJoinRequestEvent(
            UUID.randomUUID(),
            FAKER.number().randomNumber(12, true),
            FAKER.name().firstName(),
            UUID.randomUUID().toString(),
            Instant.now()
        );
    }

    /**
     * Overloaded method for {@link #generateGroupJoinRequestEvent()}.
     *
     * @param groupId the group ID the member is requesting to join.
     *
     * @return a GroupJoinRequestEvent object with all details.
     */
    public static GroupJoinRequestEvent generateGroupJoinRequestEvent(Long groupId) {

        return new GroupJoinRequestEvent(
            UUID.randomUUID(),
            groupId,
            FAKER.name().firstName(),
            UUID.randomUUID().toString(),
            Instant.now()
        );
    }

    /**
     * Overloaded method for {@link #generateGroupJoinRequestEvent()}.
     *
     * @param username the username of the member.
     * @param groupId the group ID the member is requesting to join.
     * @param websocketId the websocket ID of the member.
     *
     * @return a GroupJoinRequestEvent object with all details.
     */
    public static GroupJoinRequestEvent generateGroupJoinRequestEvent(
        String websocketId, String username, Long groupId) {

        return new GroupJoinRequestEvent(
            UUID.randomUUID(),
            groupId,
            username,
            websocketId,
            Instant.now()
        );
    }

    /**
     * Generates a group leave request event.
     * Note that IDs are intentionally 12 digits so that
     * they'll always be considered a {@code long} type.
     *
     * @see #generateFullGroupDetails(GroupStatus status) for more info.
     *
     * @return a GroupLeaveRequestEvent object with all details.
     */
    public static GroupLeaveRequestEvent generateGroupLeaveRequestEvent() {

        return new GroupLeaveRequestEvent(
            UUID.randomUUID(),
            FAKER.number().randomNumber(12, true),
            FAKER.number().randomNumber(12, true),
            UUID.randomUUID().toString(),
            Instant.now()
        );
    }

    /**
     * Overloaded method for {@link #generateGroupLeaveRequestEvent()}.
     *
     * @param groupId the group ID the member is requesting to leave.
     * @param memberId the member ID that is requesting to leave the group.
     *
     * @return a GroupLeaveRequestEvent object with all details.
     */
    public static GroupLeaveRequestEvent generateGroupLeaveRequestEvent(
        Long groupId, Long memberId) {

        return new GroupLeaveRequestEvent(
            UUID.randomUUID(),
            groupId,
            memberId,
            UUID.randomUUID().toString(),
            Instant.now()
        );
    }

    /**
     * Overloaded method for {@link #generateGroupLeaveRequestEvent()}.
     *
     * @param groupId the group ID the member is requesting to leave.
     * @param memberId the member ID that is requesting to leave the group.
     * @param websocketId the websocket ID of the member.
     *
     * @return a GroupLeaveRequestEvent object with all details.
     */
    public static GroupLeaveRequestEvent generateGroupLeaveRequestEvent(
        String websocketId, Long groupId, Long memberId) {

        return new GroupLeaveRequestEvent(
            UUID.randomUUID(),
            groupId,
            memberId,
            websocketId,
            Instant.now()
        );
    }

    /**
     * Generates a group create request event.
     *
     * @return a GroupCreateRequestEvent object with all details.
     */
    public static GroupCreateRequestEvent generateGroupCreateRequestEvent() {

        final int maxCapacity = FAKER.number().numberBetween(100, 150);

        return new GroupCreateRequestEvent(
            UUID.randomUUID(),
            FAKER.lorem().sentence(),
            FAKER.lorem().sentence(20),
            maxCapacity,
            OWNER,
            UUID.randomUUID().toString(),
            Instant.now()
        );
    }

    /**
     * Overloaded method for {@link #generateGroupCreateRequestEvent()}.
     *
     * @param maxCapacity the maximum number of members that can join the group.
     *
     * @return a GroupCreateRequestEvent object with all details.
     */
    public static GroupCreateRequestEvent generateGroupCreateRequestEvent(
        int maxCapacity) {

        return new GroupCreateRequestEvent(
            UUID.randomUUID(),
            FAKER.lorem().sentence(),
            FAKER.lorem().sentence(20),
            maxCapacity,
            OWNER,
            UUID.randomUUID().toString(),
            Instant.now()
        );
    }

    /**
     * Generates a group status request event.
     *
     * @param groupId the group ID to update status for.
     * @param status the status of the group.
     *
     * @return a GroupStatusRequestEvent object with all details.
     */
    public static GroupStatusRequestEvent generateGroupStatusRequestEvent(
        Long groupId, GroupStatus status) {

        return new GroupStatusRequestEvent(
            UUID.randomUUID(),
            groupId,
            status,
            UUID.randomUUID().toString(),
            Instant.now()
        );
    }

    /**
     * Generates an outbox event.
     *
     * @return an OutboxEvent object with all details.
     */
    public static OutboxEvent generateOutboxEvent() {
        final EventDataModel eventData = GroupTestUtility.generateFullGroupDetails(GroupStatus.ACTIVE);

        return new OutboxEvent(
            UUID.randomUUID(),
            FAKER.number().randomNumber(12, true),
            UUID.randomUUID().toString(),
            AggregateType.GROUP,
            EventType.GROUP_CREATED,
            eventData,
            EventStatus.SUCCESSFUL,
            Instant.now()
        );
    }

    /**
     * Generates an outbox event with the given parameters.
     *
     * @param aggregateId the aggregate ID to use
     * @param eventData the event data to use
     * @param eventType the event type to use
     *
     * @return an OutboxEvent object with all details.
     */
    public static OutboxEvent generateOutboxEvent(
        Long aggregateId, EventDataModel eventData, EventType eventType) {

        return new OutboxEvent(
            UUID.randomUUID(),
            aggregateId,
            UUID.randomUUID().toString(),
            AggregateType.GROUP,
            eventType,
            eventData,
            EventStatus.SUCCESSFUL,
            Instant.now()
        );
    }

    /**
     * Generates an outbox event with the given parameters.
     *
     * @param aggregateId the aggregate ID to use
     * @param eventData the event data to use
     * @param eventType the event type to use
     * @param eventStatus the event status to use
     *
     * @return an OutboxEvent object with all details.
     */
    public static OutboxEvent generateOutboxEvent(
        Long aggregateId, EventDataModel eventData, EventType eventType, EventStatus eventStatus) {

        return new OutboxEvent(
            UUID.randomUUID(),
            aggregateId,
            UUID.randomUUID().toString(),
            AggregateType.GROUP,
            eventType,
            eventData,
            eventStatus,
            Instant.now()
        );
    }

    /**
     * Overloaded method for {@link #generateOutboxEvent()} ()}.
     */
    public static OutboxEvent generateOutboxEvent(String webSocketId, EventStatus eventStatus) {
        EventDataModel eventData;

        if (eventStatus == EventStatus.FAILED) {
            eventData = new ErrorData("Error message");
        } else {
            eventData = GroupTestUtility.generateFullGroupDetails(GroupStatus.ACTIVE);
        }

        return new OutboxEvent(
            UUID.randomUUID(),
            FAKER.number().randomNumber(12, true),
            webSocketId,
            AggregateType.GROUP,
            EventType.GROUP_CREATED,
            eventData,
            eventStatus,
            Instant.now()
        );
    }

    /**
     * Overloaded method for {@link #generateOutboxEvent()} ()}.
     */
    public static OutboxEvent generateOutboxEvent(String webSocketId, EventStatus eventStatus, EventType eventType) {
        EventDataModel eventData;

        if (eventStatus == EventStatus.FAILED) {
            eventData = new ErrorData("Error message");
        } else {
            eventData = switch (eventType) {
                case GROUP_CREATED -> GroupTestUtility.generateFullGroupDetails(GroupStatus.ACTIVE);
                case GROUP_UPDATED, GROUP_DISBANDED -> GroupTestUtility.generateFullGroupDetails(GroupStatus.DISBANDED);
                case MEMBER_JOINED, MEMBER_LEFT -> GroupTestUtility.generateFullMemberDetails();
                case NOTHING -> null;
            };
        }

        return new OutboxEvent(
            UUID.randomUUID(),
            FAKER.number().randomNumber(12, true),
            webSocketId,
            AggregateType.GROUP,
            eventType,
            eventData,
            eventStatus,
            Instant.now()
        );
    }
}
