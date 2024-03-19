package org.grouphq.groupsync.group.sync.state;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Stream;
import org.grouphq.groupsync.GroupTestUtility;
import org.grouphq.groupsync.config.ClientProperties;
import org.grouphq.groupsync.group.domain.PublicOutboxEvent;
import org.grouphq.groupsync.group.sync.GroupFetchService;
import org.grouphq.groupsync.group.sync.GroupUpdateService;
import org.grouphq.groupsync.groupservice.domain.groups.Group;
import org.grouphq.groupsync.groupservice.domain.groups.GroupStatus;
import org.grouphq.groupsync.groupservice.domain.outbox.OutboxEvent;
import org.grouphq.groupsync.groupservice.domain.outbox.enums.EventType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.test.StepVerifier;

@Tag("UnitTest")
@ExtendWith(MockitoExtension.class)
class GroupInitialStateServiceTest {

    @Mock
    private static ClientProperties clientProperties;

    @Mock
    private GroupFetchService groupFetchService;

    @Mock
    private GroupUpdateService groupUpdateService;

    @Spy
    private GroupStateService groupStateService;

    @InjectMocks
    private GroupInitialStateService groupInitialStateService;


    @Test
    @DisplayName("When class loads, it should be in the dormant state")
    void whenClassLoadsThenItShouldBeInDormantState() {
        assertThat(groupInitialStateService.getState()).isExactlyInstanceOf(DormantState.class);
    }

    @Test
    @DisplayName("When a request is made for the current events, then transition to the loading state")
    void whenRequestIsMadeForCurrentStateThenTransitionToLoadingState() {
        given(groupFetchService.getGroupsAsEvents()).willReturn(Flux.never());

        StepVerifier.create(groupInitialStateService.requestCurrentEvents())
            .then(() -> assertThat(groupInitialStateService.getState()).isExactlyInstanceOf(LoadingState.class))
            .thenCancel()
            .verify();
    }

    @Test
    @DisplayName("When a request is made for the current state, then fetch the data and transition to the ready state")
    void whenRequestIsMadeForCurrentStateThenFetchTheCurrentStateAndReadyUp() {
        final List<PublicOutboxEvent> events =
            Stream.generate(() -> PublicOutboxEvent.convertOutboxEvent(GroupTestUtility.generateOutboxEvent()))
                .limit(5)
                .toList();
        final Flux<PublicOutboxEvent> eventFlux = Flux.fromIterable(events);

        given(groupFetchService.getGroupsAsEvents()).willReturn(eventFlux);
        given(groupUpdateService.publicUpdatesStream()).willReturn(Flux.never());

        StepVerifier.create(groupInitialStateService.requestCurrentEvents())
            .expectNextCount(5)
            .verifyComplete();

        assertThat(groupInitialStateService.getState()).isExactlyInstanceOf(ReadyState.class);
    }

    @Test
    @DisplayName("When a request is made in the ready state, return the current events")
    void whenRequestIsMadeInReadyStateThenReturnTheCurrentEvents() {
        final List<PublicOutboxEvent> events =
            Stream.generate(() -> PublicOutboxEvent.convertOutboxEvent(GroupTestUtility.generateOutboxEvent()))
                .limit(5)
                .toList();
        final Flux<PublicOutboxEvent> eventFlux = Flux.fromIterable(events);

        given(groupFetchService.getGroupsAsEvents()).willReturn(eventFlux);
        given(groupUpdateService.publicUpdatesStream()).willReturn(Flux.never());

        StepVerifier.create(groupInitialStateService.requestCurrentEvents())
            .expectNextCount(5)
            .verifyComplete();

        assertThat(groupInitialStateService.getState()).isExactlyInstanceOf(ReadyState.class);

        StepVerifier.create(groupInitialStateService.requestCurrentEvents())
            .expectNextCount(5)
            .verifyComplete();

        verify(groupFetchService, times(1)).getGroupsAsEvents();
    }

    @Test
    @DisplayName("When in the ready state, respond to event updates")
    void whenInReadyStateThenRespondToEventUpdates() {
        final Group group = GroupTestUtility.generateFullGroupDetailsWithMembers(GroupStatus.ACTIVE);

        final OutboxEvent outboxEvent =
            GroupTestUtility.generateOutboxEvent(group.id(), group, EventType.GROUP_CREATED);
        final PublicOutboxEvent publicOutboxEvent = PublicOutboxEvent.convertOutboxEvent(outboxEvent);

        final Flux<PublicOutboxEvent> eventFlux = Flux.just(publicOutboxEvent);

        given(groupFetchService.getGroupsAsEvents()).willReturn(eventFlux);


        final Group groupDisbanded = new Group(group.id(), group.title(), group.description(),
            group.maxGroupSize(), GroupStatus.DISBANDED, group.lastMemberActivity(), group.createdDate(),
            group.lastModifiedDate(), group.createdBy(), group.lastModifiedBy(), group.version(), group.members());

        final OutboxEvent outboxEventDisbanded =
            GroupTestUtility.generateOutboxEvent(group.id(), groupDisbanded, EventType.GROUP_UPDATED);

        final PublicOutboxEvent publicOutboxEventDisbanded =
            PublicOutboxEvent.convertOutboxEvent(outboxEventDisbanded);

        final Sinks.Many<PublicOutboxEvent> updateSink = Sinks.many().replay().all();
        final Flux<PublicOutboxEvent> eventFluxDisbanded = updateSink.asFlux();

        given(groupUpdateService.publicUpdatesStream()).willReturn(eventFluxDisbanded);

        /*
         TODO: Non-deterministic and subject to flakiness. Need to find a better way to test this.
         The issue here is that the sink emission may not cause the current state of events to update
         before requesting them again if the update takes too long.
         If you remove the delaySubscription call, this test will likely pass when run in isolation and with
         other tests in this file. But when running all unit tests, it usually fails.
         */
        StepVerifier.create(groupInitialStateService.requestCurrentEvents()
                .then(Mono.fromRunnable(() -> updateSink.tryEmitNext(publicOutboxEventDisbanded)))
                .thenMany(groupInitialStateService.requestCurrentEvents()
                    .delaySubscription(Duration.of(1000, ChronoUnit.MILLIS)))
            )
            .verifyComplete();
    }

    @Test
    @DisplayName("When retires exhausted in the loading state, transition to the dormant state")
    void whenRetriesExhaustedInLoadingStateThenTransitionToDormantState() {
        given(groupFetchService.getGroupsAsEvents())
            .willReturn(Flux.error(new RuntimeException("Failed to fetch groups")));
        given(clientProperties.getGroupsRetryAttempts()).willReturn(3L);
        given(clientProperties.getGroupsRetryBackoffMilliseconds()).willReturn(10L);

        StepVerifier.create(groupInitialStateService.requestCurrentEvents())
            .expectError()
            .verify();

        assertThat(groupInitialStateService.getState()).isExactlyInstanceOf(DormantState.class);
    }
}
