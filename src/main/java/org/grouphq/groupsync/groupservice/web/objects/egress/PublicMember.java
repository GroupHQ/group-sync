package org.grouphq.groupsync.groupservice.web.objects.egress;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.jackson.Jacksonized;
import org.grouphq.groupsync.groupservice.domain.members.MemberStatus;
import org.grouphq.groupsync.groupservice.domain.outbox.EventDataModel;

/**
 * A data-access-object representing a member model containing
 * only necessary and insensitive attributes for client.
 *
 * @param id Member's ID
 * @param username Member's username
 * @param groupId Group ID identifying the group the member belongs to
 * @param joinedDate Time user joined the group. Same time as createdDate
 * @param exitedDate Time user left the group. Initially null.
 */
@Getter
@Builder
@Jacksonized
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(callSuper = true)
public class PublicMember implements EventDataModel {
    @EqualsAndHashCode.Include
    private final Long id;
    private final String username;
    private final Long groupId;
    private final MemberStatus memberStatus;
    private final String joinedDate;
    private final String exitedDate;
}