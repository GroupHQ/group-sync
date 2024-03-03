package org.grouphq.groupsync.group.domain;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.Instant;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.grouphq.groupsync.groupservice.domain.members.Member;
import org.grouphq.groupsync.groupservice.domain.outbox.EventDataModel;
import org.grouphq.groupsync.groupservice.domain.outbox.OutboxEvent;
import org.grouphq.groupsync.groupservice.domain.outbox.enums.AggregateType;
import org.grouphq.groupsync.groupservice.domain.outbox.enums.EventStatus;
import org.grouphq.groupsync.groupservice.domain.outbox.enums.EventType;

/**
 * A data-access-object representing an outbox event containing
 * only necessary and insensitive attributes for client.
 *
 * @param aggregateId The ID of the aggregate that the event is for
 * @param aggregateType The type of aggregate that the event is for
 * @param eventType The type of event
 * @param eventData The data of the event
 * @param eventStatus The status of the event
 * @param createdDate The date the event was created
 */
@Slf4j
public record PublicOutboxEvent(UUID eventId, Long aggregateId, AggregateType aggregateType,
                                EventType eventType, EventDataModel eventData,
                                EventStatus eventStatus, Instant createdDate) {

    private static final ObjectMapper OBJECT_MAPPER;

    static {
        OBJECT_MAPPER = new ObjectMapper();
        OBJECT_MAPPER.registerModule(new JavaTimeModule());
    }

    public static PublicOutboxEvent convertOutboxEvent(OutboxEvent outboxEvent) {
        return switch (outboxEvent.getEventType()) {
            case GROUP_CREATED -> convertGroupCreated(outboxEvent);
            case GROUP_UPDATED -> convertGroupStatusUpdated(outboxEvent);
            case MEMBER_JOINED -> convertMemberJoined(outboxEvent);
            case MEMBER_LEFT -> convertMemberLeft(outboxEvent);
            default -> convertDefault(outboxEvent);
        };
    }

    private static PublicOutboxEvent convertGroupCreated(OutboxEvent outboxEvent) {
        return convertDefault(outboxEvent);
    }

    private static PublicOutboxEvent convertGroupStatusUpdated(OutboxEvent outboxEvent) {
        return convertDefault(outboxEvent);
    }

    private static PublicOutboxEvent convertMemberJoined(OutboxEvent outboxEvent) {
        PublicOutboxEvent publicOutboxEvent;

        publicOutboxEvent = new PublicOutboxEvent(
            outboxEvent.getEventId(),
            outboxEvent.getAggregateId(),
            outboxEvent.getAggregateType(),
            outboxEvent.getEventType(),
            Member.toPublicMember((Member) outboxEvent.getEventData()),
            outboxEvent.getEventStatus(),
            outboxEvent.getCreatedDate()
        );

        return publicOutboxEvent;
    }

    private static PublicOutboxEvent convertMemberLeft(OutboxEvent outboxEvent) {
        PublicOutboxEvent publicOutboxEvent;

        publicOutboxEvent = new PublicOutboxEvent(
            outboxEvent.getEventId(),
            outboxEvent.getAggregateId(),
            outboxEvent.getAggregateType(),
            outboxEvent.getEventType(),
            Member.toPublicMember((Member) outboxEvent.getEventData()),
            outboxEvent.getEventStatus(),
            outboxEvent.getCreatedDate()
        );

        return publicOutboxEvent;
    }

    private static PublicOutboxEvent convertDefault(OutboxEvent outboxEvent) {
        return new PublicOutboxEvent(
            outboxEvent.getEventId(),
            outboxEvent.getAggregateId(),
            outboxEvent.getAggregateType(),
            outboxEvent.getEventType(),
            outboxEvent.getEventData(),
            outboxEvent.getEventStatus(),
            outboxEvent.getCreatedDate()
        );
    }

    public PublicOutboxEvent withNewEventData(EventDataModel eventDataModel) {
        return new PublicOutboxEvent(
            this.eventId,
            this.aggregateId(),
            this.aggregateType(),
            this.eventType,
            eventDataModel,
            this.eventStatus,
            this.createdDate
        );
    }
}
