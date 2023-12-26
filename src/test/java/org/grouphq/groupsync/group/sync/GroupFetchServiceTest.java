package org.grouphq.groupsync.group.sync;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import org.grouphq.groupsync.GroupTestUtility;
import org.grouphq.groupsync.groupservice.domain.groups.Group;
import org.grouphq.groupsync.groupservice.domain.groups.GroupStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
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
}
