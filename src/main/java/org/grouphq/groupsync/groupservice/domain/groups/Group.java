package org.grouphq.groupsync.groupservice.domain.groups;

import java.time.Instant;
import java.util.List;
import org.grouphq.groupsync.groupservice.web.objects.egress.PublicMember;

/**
 * A group model.
 *
 * @param id A unique ID belonging to a group
 * @param title Group's title or name
 * @param description Information about the group
 * @param maxGroupSize Maximum number of users that can belong to the group
 * @param status Status of group {@link GroupStatus}
 * @param lastMemberActivity Last action done in the group by some group member
 * @param createdDate Time group was created
 * @param lastModifiedDate Time group was last modified / updated
 * @param createdBy Creator of group (e.g. system or user)
 * @param lastModifiedBy Who last modified the group
 * @param version Unique number on group state (used by Spring Data for optimistic locking)
 */
public record Group(
        Long id,
        String title,
        String description,
        int maxGroupSize,
        GroupStatus status,
        Instant lastMemberActivity,
        Instant createdDate,
        Instant lastModifiedDate,
        String createdBy,
        String lastModifiedBy,
        int version,
        List<PublicMember> members
) {
    public static Group of(String title, String description,
                           int maxGroupSize, GroupStatus status) {
        return new Group(null, title, description, maxGroupSize, status,
            Instant.now(), null, null, null, null, 0, null);
    }
}
