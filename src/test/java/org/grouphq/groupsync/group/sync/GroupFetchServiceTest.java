package org.grouphq.groupsync.group.sync;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.grouphq.groupsync.GroupTestUtility;
import org.grouphq.groupsync.group.domain.PublicOutboxEvent;
import org.grouphq.groupsync.groupservice.domain.groups.Group;
import org.grouphq.groupsync.groupservice.domain.groups.GroupStatus;
import org.grouphq.groupsync.groupservice.domain.members.Member;
import org.grouphq.groupsync.groupservice.domain.outbox.OutboxEvent;
import org.grouphq.groupsync.groupservice.domain.outbox.enums.EventType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
@Tag("UnitTest")
class GroupFetchServiceTest {

    @Mock
    private GroupServiceClient groupServiceClient;

    @InjectMocks
    private GroupFetchService groupFetchService;

    @Test
    @DisplayName("When there are active groups, then return a list of active groups")
    void whenActiveGroupsExistThenReturnActiveGroups() {
        final Group[] testGroups = {
            GroupTestUtility.generateFullGroupDetails(GroupStatus.ACTIVE),
            GroupTestUtility.generateFullGroupDetails(GroupStatus.ACTIVE)
        };

        given(groupServiceClient.getGroups()).willReturn(Flux.just(testGroups));

        StepVerifier.create(groupFetchService.getGroups().collectList())
            .expectNext(List.of(testGroups[0], testGroups[1]))
            .verifyComplete();

        verify(groupServiceClient).getGroups();
    }

    @Test
    @DisplayName("When there are no active groups, then return an empty list")
    void whenNoActiveGroupsExistThenReturnEmptyList() {
        given(groupServiceClient.getGroups()).willReturn(Flux.empty());

        StepVerifier.create(groupFetchService.getGroups())
            .verifyComplete();

        verify(groupServiceClient).getGroups();
    }

    @Test
    @DisplayName("When there are active groups, then return a list of active groups as events")
    void whenActiveGroupsExistThenReturnActiveGroupsAsEvents() {
        final List<PublicOutboxEvent> testEvents = Stream.generate(
            () -> {
                final Group group = GroupTestUtility.generateFullGroupDetailsWithMembers(GroupStatus.ACTIVE);
                final OutboxEvent outboxEvent =
                    GroupTestUtility.generateOutboxEvent(group.id(), group, EventType.GROUP_CREATED);
                return PublicOutboxEvent.convertOutboxEvent(outboxEvent);
            }
        ).limit(5).toList();

        given(groupServiceClient.getGroupsAsEvents()).willReturn(Flux.fromIterable(testEvents));

        StepVerifier.create(groupFetchService.getGroupsAsEvents().collectList())
            .assertNext(events -> assertThat(events).containsExactlyInAnyOrderElementsOf(testEvents))
            .verifyComplete();

        verify(groupServiceClient).getGroupsAsEvents();
    }

    @Test
    @DisplayName("When there are no active groups, then return an empty list of events")
    void whenNoActiveGroupsExistThenReturnEmptyListOfEvents() {
        given(groupServiceClient.getGroupsAsEvents()).willReturn(Flux.empty());

        StepVerifier.create(groupFetchService.getGroupsAsEvents())
            .verifyComplete();

        verify(groupServiceClient).getGroupsAsEvents();
    }

    @Test
    @DisplayName("When a user requests their member, then return their member")
    void whenUserRequestsTheirMemberThenReturnTheirMember() {
        final String websocketId = "websocketId";
        final Member testMember = Member.of(UUID.randomUUID(), "test-username", 1L);

        given(groupServiceClient.getMyMember(websocketId)).willReturn(Mono.just(testMember));

        StepVerifier.create(groupFetchService.getMyMember(websocketId))
            .expectNext(testMember)
            .verifyComplete();

        verify(groupServiceClient).getMyMember(websocketId);
    }

    @Test
    @DisplayName("When a user requests their member, but they have no member, then return an empty Mono")
    void whenUserRequestsTheirMemberButTheyHaveNoMemberThenReturnEmptyMono() {
        final String websocketId = "websocketId";

        given(groupServiceClient.getMyMember(websocketId)).willReturn(Mono.empty());

        StepVerifier.create(groupFetchService.getMyMember(websocketId))
            .verifyComplete();

        verify(groupServiceClient).getMyMember(websocketId);
    }
}
