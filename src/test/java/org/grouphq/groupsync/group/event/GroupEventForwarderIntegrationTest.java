package org.grouphq.groupsync.group.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.grouphq.groupsync.GroupTestUtility;
import org.grouphq.groupsync.group.domain.PublicOutboxEvent;
import org.grouphq.groupsync.group.sync.state.GroupInitialStateService;
import org.grouphq.groupsync.group.web.GroupSyncController;
import org.grouphq.groupsync.groupservice.domain.members.Member;
import org.grouphq.groupsync.groupservice.domain.outbox.OutboxEvent;
import org.grouphq.groupsync.groupservice.domain.outbox.enums.EventStatus;
import org.grouphq.groupsync.groupservice.domain.outbox.enums.EventType;
import org.grouphq.groupsync.groupservice.web.objects.egress.PublicMember;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.cloud.stream.binder.test.InputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.security.test.context.support.WithMockUser;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

@SpringBootTest
@Import(TestChannelBinderConfiguration.class)
@Tag("IntegrationTest")
class GroupEventForwarderIntegrationTest {

    @Autowired
    private InputDestination inputDestination;

    @SpyBean
    private GroupInitialStateService groupInitialStateService;

    @Autowired
    private GroupSyncController groupSyncController;

    @Value("${spring.cloud.stream.bindings.processedEvents-in-0.destination}")
    private String eventDestination;

    private static final String USER = "USER";

    @Test
    @DisplayName("Forwards events to the outbox event update successful sink")
    void forwardsEventsToTheOutboxEventUpdateSink() {
        final List<PublicOutboxEvent> publicEvents = List.of(
            PublicOutboxEvent.convertOutboxEvent(GroupTestUtility.generateOutboxEvent()),
            PublicOutboxEvent.convertOutboxEvent(GroupTestUtility.generateOutboxEvent()),
            PublicOutboxEvent.convertOutboxEvent(GroupTestUtility.generateOutboxEvent())
        );

        given(groupInitialStateService.requestCurrentEvents()).willReturn(Flux.empty());

        final Flux<PublicOutboxEvent> groupUpdatesStream =
            groupSyncController.getPublicUpdates()
            .doOnSubscribe(subscription ->
                publicEvents.forEach(event ->
                    inputDestination.send(new GenericMessage<>(event), eventDestination)));

        final var steps = StepVerifier.create(groupUpdatesStream.filter(event ->
                publicEvents.stream().anyMatch(eventTracked -> eventTracked.eventId().equals(event.eventId())))
            )
            .recordWith(ArrayList::new)
            .expectNextCount(publicEvents.size())
            .consumeRecordedWith(received -> assertThat(received).containsAll(publicEvents))
            .thenCancel();

        // Subscribe to updates sink
        steps.verify(Duration.ofSeconds(1));

        // Subscribe again--should also receive a replay of events
        steps.verify(Duration.ofSeconds(1));
    }

    @Test
    @WithMockUser(username = USER)
    @DisplayName("Forwards private events to user with public event data models")
    void forwardsPrivateMemberEventsToUserWithPublicEventDataModels() {
        final Member member = GroupTestUtility.generateFullMemberDetails(USER, 1L);
        final List<OutboxEvent> outboxEvents = List.of(
            GroupTestUtility.generateOutboxEvent(1L, member, EventType.MEMBER_JOINED),
            GroupTestUtility.generateOutboxEvent(1L, member, EventType.MEMBER_LEFT)
        );

        given(groupInitialStateService.requestCurrentEvents()).willReturn(Flux.empty());

        final Flux<PublicOutboxEvent> groupUpdatesStream =
            groupSyncController.getPublicUpdates()
            .doOnSubscribe(subscription ->
                outboxEvents.forEach(event ->
                    inputDestination.send(new GenericMessage<>(event), eventDestination)));

        // Subscribe to updates sink
        StepVerifier.create(groupUpdatesStream.filter(event ->
                outboxEvents.stream().anyMatch(eventTracked -> eventTracked.getEventId().equals(event.eventId())))
            )
            .recordWith(ArrayList::new)
            .expectNextCount(outboxEvents.size())
            .consumeRecordedWith(received -> assertThat(received).allSatisfy(publicEvent ->
                assertThat(publicEvent.eventData()).isExactlyInstanceOf(PublicMember.class)))
            .thenCancel()
            .verify(Duration.ofSeconds(1));
    }

    @Test
    @WithMockUser(username = USER)
    @DisplayName("Forwards events to the outbox event update failed sink")
    void forwardsEventsToTheOutboxEventUpdateFailedSink() {
        final List<OutboxEvent> outboxEvents = List.of(
            GroupTestUtility.generateOutboxEvent(USER, EventStatus.FAILED),
            GroupTestUtility.generateOutboxEvent(USER, EventStatus.FAILED),
            GroupTestUtility.generateOutboxEvent(USER, EventStatus.FAILED)
        );

        final Flux<OutboxEvent> groupUpdatesStream =
            groupSyncController.getEventOwnerUpdates()
            .doOnSubscribe(subscription ->
                outboxEvents.forEach(event ->
                    inputDestination.send(new GenericMessage<>(event), eventDestination)));

        final var steps = StepVerifier.create(groupUpdatesStream.filter(event ->
                outboxEvents.stream().anyMatch(eventTracked -> eventTracked.getEventId().equals(event.getEventId())))
            )
            .recordWith(ArrayList::new)
            .expectNextCount(outboxEvents.size())
            .consumeRecordedWith(received -> assertThat(received).containsAll(outboxEvents))
            .thenCancel();

        // Subscribe to updates sink
        steps.verify(Duration.ofSeconds(1));

        // Subscribe again--should also receive a replay of events
        steps.verify(Duration.ofSeconds(1));
    }
}
