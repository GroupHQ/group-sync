package org.grouphq.groupsync.group.event;

import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.Mockito.verify;

import org.grouphq.groupsync.group.sync.GroupUpdateService;
import org.grouphq.groupsync.groupservice.domain.outbox.OutboxEvent;
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
    @DisplayName("Forwards events to the sink")
    void forwardsEventsToTheSink() {
        final OutboxEvent outboxEvent = GroupTestUtility.generateOutboxEvent();

        willDoNothing().given(groupUpdateService).sendOutboxEventUpdate(outboxEvent);

        groupEventForwarder.processedEvents().accept(Flux.just(outboxEvent));

        verify(groupUpdateService).sendOutboxEventUpdate(outboxEvent);
    }
}
