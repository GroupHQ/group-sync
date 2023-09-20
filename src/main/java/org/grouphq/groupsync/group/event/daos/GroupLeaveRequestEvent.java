package org.grouphq.groupsync.group.event.daos;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;

/**
 * Data class for the group leave request event.
 * <p>This class is used to request a member to leave a group.</p>
 */
public class GroupLeaveRequestEvent extends RequestEvent {

    @Getter
    @NotNull(message = "Member ID must be provided")
    @Positive(message = "Member ID must be a positive value")
    private final Long memberId;

    public GroupLeaveRequestEvent(
        UUID eventId,
        Long groupId,
        Long memberId,
        String websocketId,
        Instant createdDate
    ) {
        super(eventId, groupId, websocketId, createdDate);
        this.memberId = memberId;
    }
}
