package org.grouphq.groupsync.group.domain;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.time.Instant;
import java.util.UUID;
import org.grouphq.groupsync.GroupTestUtility;
import org.grouphq.groupsync.groupservice.domain.outbox.OutboxEvent;
import org.grouphq.groupsync.groupservice.domain.outbox.enums.AggregateType;
import org.grouphq.groupsync.groupservice.domain.outbox.enums.EventType;
import org.grouphq.groupsync.groupservice.web.objects.egress.PublicMember;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("UnitTest")
class PublicOutboxEventTest {

    @Test
    @DisplayName("Convert MEMBER_JOINED outbox events to public outbox event with public member")
    void convertMemberJoinedOutboxEventToPublicOutboxEventWithPublicMember() {
        final OutboxEvent outboxEvent = OutboxEvent.builder()
            .eventId(UUID.randomUUID())
            .aggregateId(1L)
            .websocketId(UUID.randomUUID().toString())
            .aggregateType(AggregateType.GROUP)
            .eventType(EventType.MEMBER_JOINED)
            .eventData(GroupTestUtility.generateFullMemberDetails("User", 1L))
            .createdDate(Instant.now())
            .build();

        final PublicOutboxEvent publicOutboxEvent = PublicOutboxEvent.convertOutboxEvent(outboxEvent);

        assertThat(publicOutboxEvent.eventData()).isExactlyInstanceOf(PublicMember.class);
    }

    @Test
    @DisplayName("Convert MEMBER_LEFT outbox events to public outbox event with public member")
    void convertMemberLeftOutboxEventToPublicOutboxEventWithPublicMember() {
        final OutboxEvent outboxEvent = OutboxEvent.builder()
            .eventId(UUID.randomUUID())
            .aggregateId(1L)
            .websocketId(UUID.randomUUID().toString())
            .aggregateType(AggregateType.GROUP)
            .eventType(EventType.MEMBER_LEFT)
            .eventData(GroupTestUtility.generateFullMemberDetails("User", 1L))
            .createdDate(Instant.now())
            .build();

        final PublicOutboxEvent publicOutboxEvent = PublicOutboxEvent.convertOutboxEvent(outboxEvent);

        assertThat(publicOutboxEvent.eventData()).isExactlyInstanceOf(PublicMember.class);
    }
}
