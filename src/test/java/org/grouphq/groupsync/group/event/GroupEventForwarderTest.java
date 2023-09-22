package org.grouphq.groupsync.group.event;

import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.Mockito.verify;

import org.grouphq.groupsync.group.sync.GroupUpdateService;
import org.grouphq.groupsync.groupservice.domain.outbox.OutboxEvent;
import org.grouphq.groupsync.groupservice.domain.outbox.enums.EventStatus;
import org.grouphq.groupsync.testutility.GroupTestUtility;
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
    @DisplayName("Forwards successful events to the 'updates' sink")
    void forwardsSuccessfulEventsToTheUpdatesSink() {
        final OutboxEvent outboxEvent =
            GroupTestUtility.generateOutboxEvent("ID", EventStatus.SUCCESSFUL);

        willDoNothing().given(groupUpdateService).sendOutboxEventUpdate(outboxEvent);

        groupEventForwarder.processedEvents().accept(Flux.just(outboxEvent));

        verify(groupUpdateService).sendOutboxEventUpdate(outboxEvent);
    }

    @Test
    @DisplayName("Forwards failed events to the 'failed updates' sink")
    void forwardsFailedEventsToTheUpdatesFailedSink() {
        final OutboxEvent outboxEvent =
            GroupTestUtility.generateOutboxEvent("ID", EventStatus.FAILED);

        willDoNothing().given(groupUpdateService).sendOutboxEventUpdateFailed(outboxEvent);

        groupEventForwarder.processedEvents().accept(Flux.just(outboxEvent));

        verify(groupUpdateService).sendOutboxEventUpdateFailed(outboxEvent);
    }

}
