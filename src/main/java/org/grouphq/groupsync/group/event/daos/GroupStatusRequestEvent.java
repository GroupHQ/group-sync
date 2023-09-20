package org.grouphq.groupsync.group.event.daos;

import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import org.grouphq.groupsync.group.domain.groups.GroupStatus;

/**
 * Data class for the group status request event.
 * <p>This class is used to request a change in the status of a group.</p>
 */
public class GroupStatusRequestEvent extends RequestEvent {

    @Getter
    @NotNull(message = "New status must be provided")
    private final GroupStatus newStatus;

    public GroupStatusRequestEvent(
        UUID eventId,
        Long groupId,
        GroupStatus newStatus,
        String websocketId,
        Instant createdDate
    ) {
        super(eventId, groupId, websocketId, createdDate);
        this.newStatus = newStatus;
    }
}