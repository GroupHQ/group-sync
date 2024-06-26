package org.grouphq.groupsync.groupservice.domain.outbox;

import java.time.Instant;
import java.util.UUID;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.grouphq.groupsync.groupservice.domain.members.Member;
import org.grouphq.groupsync.groupservice.domain.outbox.enums.AggregateType;
import org.grouphq.groupsync.groupservice.domain.outbox.enums.EventStatus;
import org.grouphq.groupsync.groupservice.domain.outbox.enums.EventType;

/**
 * Model for the outbox table.
 * <p>Note that both the NoArgsConstructor and the AllArgsConstructor are required.
 * The AllArgsConstructor is required for Spring Data to instantiate the object,
 * while the NoArgsConstructor is required for Jackson to deserialize the object.
 * While there is a lombok.anyconstructor.addconstructorproperties property that can
 * be enabled (it's disabled by default), the property won't apply for some reason.
 * Otherwise, we could just have the @AllArgsConstructor annotation.
 * Until that issue is fixed, we need the @AllArgsConstructor with the @PersistenceCreator
 * annotation to tell Spring Data to use that constructor. We also need the @NoArgsConstructor
 * annotation for Jackson, and the force = true parameter to tell Lombok to instantiate the
 * final fields to their default constructor.</p>
 *
 * @since 9/17/2023
 * @author makmn
 */
@NoArgsConstructor(force = true)
@RequiredArgsConstructor
@Builder
@Data
public class OutboxEvent {
    private final UUID eventId;
    private final Long aggregateId;
    private final String websocketId;
    private final AggregateType aggregateType;
    private final EventType eventType;
    private final EventDataModel eventData;
    private final EventStatus eventStatus;
    private final Instant createdDate;

    public static OutboxEvent of(UUID eventId, Long aggregateId, AggregateType aggregateType,
                                 EventType eventType, EventDataModel eventData, EventStatus eventStatus,
                                 String websocketId) {
        return new OutboxEvent(eventId, aggregateId, websocketId, aggregateType,
            eventType, eventData, eventStatus, Instant.now());
    }

    public static OutboxEvent convertEventDataToPublic(OutboxEvent outboxEvent) {
        if (outboxEvent.getEventStatus() == EventStatus.FAILED) {
            return outboxEvent;
        }

        return switch (outboxEvent.getEventType()) {
            case MEMBER_JOINED, MEMBER_LEFT -> convertMember(outboxEvent);
            default -> outboxEvent;
        };
    }

    private static OutboxEvent convertMember(OutboxEvent outboxEvent) {
        return new OutboxEventBuilder()
            .eventId(outboxEvent.getEventId())
            .aggregateId(outboxEvent.getAggregateId())
            .websocketId(outboxEvent.getWebsocketId())
            .aggregateType(outboxEvent.getAggregateType())
            .eventType(outboxEvent.getEventType())
            .eventData(Member.toPublicMember((Member) outboxEvent.getEventData()))
            .eventStatus(outboxEvent.getEventStatus())
            .createdDate(outboxEvent.getCreatedDate())
            .build();
    }

}