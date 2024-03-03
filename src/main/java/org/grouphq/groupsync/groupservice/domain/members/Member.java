package org.grouphq.groupsync.groupservice.domain.members;

import java.time.Instant;
import java.util.UUID;
import org.grouphq.groupsync.groupservice.domain.outbox.EventDataModel;
import org.grouphq.groupsync.groupservice.web.objects.egress.PublicMember;

/**
 * A member model. The @type annotation is temporarily being ignored until GROUP-89 is resolved.
 *
 * @param id A unique ID belonging to a member
 * @param websocketId The user's websocket ID for the request
 * @param username Member's username
 * @param groupId Group ID identifying the group the member belongs to
 * @param exitedDate Time user left the group. Initially null.
 * @param createdDate Time group was created
 * @param lastModifiedDate Time group was last modified / updated
 * @param createdBy Creator of group (e.g. system or user)
 * @param lastModifiedBy Who last modified the group
 * @param version Unique number on group state (used by Spring Data for optimistic locking)
 */
public record Member(

    Long id,
    UUID websocketId,

    String username,
    Long groupId,
    MemberStatus memberStatus,

    Instant exitedDate,

    Instant createdDate,

    Instant lastModifiedDate,

    String createdBy,

    String lastModifiedBy,

    int version
) implements EventDataModel {
    public static Member of(String username, Long groupId) {
        return new Member(null, UUID.randomUUID(), username, groupId, MemberStatus.ACTIVE, null,
            null, null, null, null, 0);
    }

    public static Member of(UUID websocketId, String username, Long groupId) {
        return new Member(null, websocketId, username, groupId,
            MemberStatus.ACTIVE, null, null, null,
            null, null, 0);
    }

    public static PublicMember toPublicMember(Member member) {
        return PublicMember.builder()
            .id(member.id())
            .username(member.username())
            .groupId(member.groupId())
            .memberStatus(member.memberStatus())
            .joinedDate(member.createdDate().toString())
            .exitedDate(member.exitedDate() == null ? null : member.exitedDate().toString())
            .build();
    }
}