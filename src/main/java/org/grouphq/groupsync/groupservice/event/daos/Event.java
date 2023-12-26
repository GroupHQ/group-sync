package org.grouphq.groupsync.groupservice.event.daos;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data class for event objects.
 * <p>This class is abstract and should be extended by all event classes.
 * This class contains the common fields for all events.
 * For request events, see {@link org.grouphq.groupsync.groupservice.event.daos.requestevent.RequestEvent}</p>
 *
 */
@AllArgsConstructor
@NoArgsConstructor(force = true)
@Data
public abstract class Event {
    @NotNull(message = "Event ID must be provided")
    private final UUID eventId;

    @Positive(message = "Aggregate ID must be a positive value")
    private final Long aggregateId;

    @NotNull(message = "Created date must be provided")
    @PastOrPresent(message = "Created date must be in the past or present")
    private final Instant createdDate;
}
