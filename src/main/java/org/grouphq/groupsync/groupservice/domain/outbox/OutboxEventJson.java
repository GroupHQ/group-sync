package org.grouphq.groupsync.groupservice.domain.outbox;

import java.time.Instant;
import java.util.UUID;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.grouphq.groupsync.groupservice.domain.outbox.enums.AggregateType;
import org.grouphq.groupsync.groupservice.domain.outbox.enums.EventStatus;
import org.grouphq.groupsync.groupservice.domain.outbox.enums.EventType;

/**
 * A type of OutboxEvent with its event data serialized to the appropriate object.
 * This variation of OutboxEvent is used when sending events to clients, simplifying the steps
 * needed for them to read received events. Ideally, we would use this type of class for both
 * sending to clients and saving to the database, but the current database integration
 * via Spring Data doesn't allow objects to be saved directly--they must first be converted to
 * a string when saving to the database. When retrieving from the database, they should be
 * converted to this object before sending to clients.
 *
 * @see OutboxEvent
 * @since 2/29/2024
 */
@RequiredArgsConstructor
@Data
@Slf4j
public class OutboxEventJson {
    private final UUID eventId;
    private final Long aggregateId;
    private final String websocketId;
    private final AggregateType aggregateType;
    private final EventType eventType;
    private final EventDataModel eventData;
    private final EventStatus eventStatus;
    private final Instant createdDate;
}