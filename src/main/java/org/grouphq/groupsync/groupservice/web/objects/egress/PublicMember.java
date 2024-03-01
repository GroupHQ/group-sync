package org.grouphq.groupsync.groupservice.web.objects.egress;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.grouphq.groupsync.groupservice.domain.members.MemberStatus;

/**
 * A data-access-object representing a member model containing
 * only necessary and insensitive attributes for client.
 * The @type annotation is temporarily being ignored until GROUP-89 is resolved.
 *
 * @param id Member's ID
 * @param username Member's username
 * @param groupId Group ID identifying the group the member belongs to
 * @param joinedDate Time user joined the group. Same time as createdDate
 * @param exitedDate Time user left the group. Initially null.
 */
@JsonIgnoreProperties(value = {"@type"})
public record PublicMember(
    Long id,
    String username,
    Long groupId,
    MemberStatus memberStatus,

    String joinedDate,

    String exitedDate
) {
}
