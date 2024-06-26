package org.grouphq.groupsync.group.event;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import org.grouphq.groupsync.GroupTestUtility;
import org.grouphq.groupsync.group.domain.PublicOutboxEvent;
import org.grouphq.groupsync.group.sync.GroupUpdateService;
import org.grouphq.groupsync.groupservice.domain.outbox.OutboxEvent;
import org.grouphq.groupsync.groupservice.domain.outbox.enums.EventStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

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
        final OutboxEvent outboxEvent =
            GroupTestUtility.generateOutboxEvent("ID", EventStatus.SUCCESSFUL);
        final PublicOutboxEvent publicOutboxEvent =
            PublicOutboxEvent.convertOutboxEvent(outboxEvent);

        given(groupUpdateService.sendPublicOutboxEventToAll(publicOutboxEvent)).willReturn(Mono.empty());
        given(groupUpdateService.sendOutboxEventToEventOwner(outboxEvent)).willReturn(Mono.empty());

        groupEventForwarder.processedEvents().accept(Flux.just(outboxEvent));

        verify(groupUpdateService).sendPublicOutboxEventToAll(publicOutboxEvent);
        verify(groupUpdateService).sendOutboxEventToEventOwner(outboxEvent);
    }

    @Test
    @DisplayName("Forwards failed events to only the event owner sink")
    void forwardsFailedEventsToTheUpdatesFailedSink() {
        final OutboxEvent outboxEvent =
            GroupTestUtility.generateOutboxEvent("ID", EventStatus.FAILED);

        given(groupUpdateService.sendOutboxEventToEventOwner(outboxEvent)).willReturn(Mono.empty());

        groupEventForwarder.processedEvents().accept(Flux.just(outboxEvent));

        verify(groupUpdateService).sendOutboxEventToEventOwner(outboxEvent);
    }

}
