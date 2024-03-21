package org.grouphq.groupsync.group.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.grouphq.groupsync.GroupTestUtility;
import org.grouphq.groupsync.groupservice.domain.members.Member;
import org.grouphq.groupsync.groupservice.domain.outbox.ErrorData;
import org.grouphq.groupsync.groupservice.domain.outbox.OutboxEvent;
import org.grouphq.groupsync.groupservice.domain.outbox.enums.EventStatus;
import org.grouphq.groupsync.groupservice.domain.outbox.enums.EventType;
import org.grouphq.groupsync.groupservice.web.objects.egress.PublicMember;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@Tag("UnitTest")
@ExtendWith(MockitoExtension.class)
class OutboxEventTest {
    @Test
    @DisplayName("Converts OutboxEvent member event data for MEMBER_JOINED events to public type")
    void convertsOutboxEventMemberEventDataForMemberJoinedToPublicType() {
        final Member member = GroupTestUtility.generateFullMemberDetails();
        final OutboxEvent outboxEvent = GroupTestUtility.generateOutboxEvent(1L, member, EventType.MEMBER_JOINED);
        final OutboxEvent convertedOutboxEvent = OutboxEvent.convertEventDataToPublic(outboxEvent);

        assertThat(convertedOutboxEvent.getEventData()).isExactlyInstanceOf(PublicMember.class);
    }

    @Test
    @DisplayName("Converts OutboxEvent member event data for MEMBER_LEFT events to public type")
    void convertsOutboxEventMemberEventDataForMemberLeftToPublicType() {
        final Member member = GroupTestUtility.generateFullMemberDetails();
        final OutboxEvent outboxEvent = GroupTestUtility.generateOutboxEvent(1L, member, EventType.MEMBER_LEFT);
        final OutboxEvent convertedOutboxEvent = OutboxEvent.convertEventDataToPublic(outboxEvent);

        assertThat(convertedOutboxEvent.getEventData()).isExactlyInstanceOf(PublicMember.class);
    }

    @Test
    @DisplayName("Does not perform any conversion for failed events")
    void doesNotPerformAnyConversionForFailedEvents() {
        final ErrorData errorData = new ErrorData("Error message");
        final OutboxEvent outboxEvent = GroupTestUtility.generateOutboxEvent(
            1L, errorData, EventType.MEMBER_JOINED, EventStatus.FAILED);

        final OutboxEvent nonConvertedEvent = OutboxEvent.convertEventDataToPublic(outboxEvent);

        assertThat(nonConvertedEvent).isEqualTo(outboxEvent);
    }
}
