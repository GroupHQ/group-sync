package org.grouphq.groupsync.group.web;

import static org.mockito.BDDMockito.given;

import java.time.Duration;
import org.grouphq.groupsync.GroupTestUtility;
import org.grouphq.groupsync.group.domain.PublicOutboxEvent;
import org.grouphq.groupsync.group.security.UserService;
import org.grouphq.groupsync.group.sync.GroupUpdateService;
import org.grouphq.groupsync.group.sync.state.GroupInitialStateService;
import org.grouphq.groupsync.groupservice.domain.outbox.OutboxEvent;
import org.grouphq.groupsync.groupservice.domain.outbox.enums.EventStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.test.context.annotation.SecurityTestExecutionListeners;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import reactor.test.StepVerifier;

@ExtendWith({MockitoExtension.class, SpringExtension.class})
@SecurityTestExecutionListeners
@Tag("UnitTest")
class GroupSyncControllerTest {

    @Spy
    private UserService userService;

    @Mock
    private GroupInitialStateService groupInitialStateService;

    @Mock
    private GroupUpdateService groupUpdateService;

    @InjectMocks
    private GroupSyncController groupSyncController;

    @Test
    @DisplayName("Test ping")
    void testPing() {
        StepVerifier.create(groupSyncController.ping())
            .expectNext(true)
            .verifyComplete();
    }

    @Test
    @DisplayName("Test streaming outbox events to all clients")
    void testGetOutboxEventUpdates() {
        final OutboxEvent event = GroupTestUtility.generateOutboxEvent();
        final PublicOutboxEvent publicOutboxEvent = PublicOutboxEvent.convertOutboxEvent(event);

        // Mimic a stream of events, followed by an error that should be ignored
        // The stream should continue after the error
        final Sinks.Many<PublicOutboxEvent> sink = Sinks.many().replay().limit(100);
        sink.tryEmitNext(publicOutboxEvent);
        sink.tryEmitNext(publicOutboxEvent);
        sink.tryEmitNext(publicOutboxEvent);

        given(groupInitialStateService.requestCurrentEvents()).willReturn(Flux.empty());
        given(groupUpdateService.publicUpdatesStream()).willReturn(sink.asFlux());

        StepVerifier.create(groupSyncController.getPublicUpdates())
            .expectNext(publicOutboxEvent)
            .expectNext(publicOutboxEvent)
            .expectNext(publicOutboxEvent)
            .thenCancel()
            .verify(Duration.ofSeconds(1));
    }

    @Test
    @WithMockUser(username = "Banana")
    @DisplayName("Test streaming outbox events to the client who made the request")
    void testGetOutboxEventUpdatesFailed() {
        final OutboxEvent[] events = {
            GroupTestUtility.generateOutboxEvent("Apricot", EventStatus.FAILED),
            GroupTestUtility.generateOutboxEvent("Banana", EventStatus.FAILED),
            GroupTestUtility.generateOutboxEvent("Cherry", EventStatus.FAILED)
        };

        // Mimic a stream of events
        final Sinks.Many<OutboxEvent> sink = Sinks.many().replay().limit(100);
        sink.tryEmitNext(events[0]);
        sink.tryEmitNext(events[1]);
        sink.tryEmitNext(events[2]);

        given(groupUpdateService.eventOwnerUpdateStream()).willReturn(sink.asFlux());

        StepVerifier.create(groupSyncController.getEventOwnerUpdates())
            .expectNext(events[1])
            .thenCancel()
            .verify(Duration.ofSeconds(1));
    }
}
