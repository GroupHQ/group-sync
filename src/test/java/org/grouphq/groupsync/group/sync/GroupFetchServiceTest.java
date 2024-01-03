package org.grouphq.groupsync.group.sync;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.util.UUID;
import org.grouphq.groupsync.GroupTestUtility;
import org.grouphq.groupsync.groupservice.domain.groups.Group;
import org.grouphq.groupsync.groupservice.domain.groups.GroupStatus;
import org.grouphq.groupsync.groupservice.domain.members.Member;
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

        StepVerifier.create(groupFetchService.getGroups())
            .expectNext(testGroups[0])
            .expectNext(testGroups[1])
            .verifyComplete();

        verify(groupServiceClient).getGroups();
    }

    @Test
    @DisplayName("When there are no active groups, then return an empty list")
    void whenNoActiveGroupsExistThenReturnEmptyList() {
        final Group[] testGroups = {};

        given(groupServiceClient.getGroups()).willReturn(Flux.just(testGroups));

        StepVerifier.create(groupFetchService.getGroups())
            .verifyComplete();

        verify(groupServiceClient).getGroups();
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
