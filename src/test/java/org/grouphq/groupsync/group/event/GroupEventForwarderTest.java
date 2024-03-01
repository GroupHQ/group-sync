package org.grouphq.groupsync.group.event;

import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.Mockito.verify;

import org.grouphq.groupsync.GroupTestUtility;
import org.grouphq.groupsync.group.domain.PublicOutboxEvent;
import org.grouphq.groupsync.group.sync.GroupUpdateService;
import org.grouphq.groupsync.groupservice.domain.outbox.OutboxEventJson;
import org.grouphq.groupsync.groupservice.domain.outbox.enums.EventStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;

@Tag("UnitTest")
@ExtendWith(MockitoExtension.class)
class GroupEventForwarderTest {

    @Mock
    private GroupUpdateService groupUpdateService;

    @InjectMocks
    private GroupEventForwarder groupEventForwarder;

    @Test
    @DisplayName("Forwards successful events to the to all users sink and the event owner sink")
    void forwardsSuccessfulEventsToTheUpdatesSink() {
        final OutboxEventJson outboxEvent =
            GroupTestUtility.generateOutboxEventJson("ID", EventStatus.SUCCESSFUL);
        final PublicOutboxEvent publicOutboxEvent =
            PublicOutboxEvent.convertOutboxEvent(outboxEvent);

        willDoNothing().given(groupUpdateService).sendPublicOutboxEventToAll(publicOutboxEvent);
        willDoNothing().given(groupUpdateService).sendOutboxEventToEventOwner(outboxEvent);

        groupEventForwarder.processedEvents().accept(Flux.just(outboxEvent));

        verify(groupUpdateService).sendPublicOutboxEventToAll(publicOutboxEvent);
        verify(groupUpdateService).sendOutboxEventToEventOwner(outboxEvent);
    }

    @Test
    @DisplayName("Forwards failed events to only the event owner sink")
    void forwardsFailedEventsToTheUpdatesFailedSink() {
        final OutboxEventJson outboxEvent =
            GroupTestUtility.generateOutboxEventJson("ID", EventStatus.FAILED);

        willDoNothing().given(groupUpdateService).sendOutboxEventToEventOwner(outboxEvent);

        groupEventForwarder.processedEvents().accept(Flux.just(outboxEvent));

        verify(groupUpdateService).sendOutboxEventToEventOwner(outboxEvent);
    }

}
