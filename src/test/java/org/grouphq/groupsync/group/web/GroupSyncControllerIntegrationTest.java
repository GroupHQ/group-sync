package org.grouphq.groupsync.group.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import org.grouphq.groupsync.config.SecurityConfig;
import org.grouphq.groupsync.group.sync.GroupFetchService;
import org.grouphq.groupsync.groupservice.domain.groups.Group;
import org.grouphq.groupsync.groupservice.domain.groups.GroupStatus;
import org.grouphq.groupsync.groupservice.domain.members.Member;
import org.grouphq.groupsync.testutility.GroupTestUtility;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;

@WebFluxTest(GroupSyncController.class)
@Import(SecurityConfig.class)
@Tag("IntegrationTest")
public class GroupSyncControllerIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private GroupFetchService groupFetchService;

    @Test
    @DisplayName("When there are active groups, then return a list of active groups")
    void returnActiveGroups() {
        final Group[] testGroups = {
            GroupTestUtility.generateFullGroupDetails(GroupStatus.ACTIVE),
            GroupTestUtility.generateFullGroupDetails(GroupStatus.ACTIVE)
        };

        given(groupFetchService.getGroups()).willReturn(Flux.just(testGroups));

        webTestClient
            .get()
            .uri("/groups")
            .exchange()
            .expectStatus().is2xxSuccessful()
            .expectBodyList(Group.class).value(groups -> {
                assertThat(groups).isNotEmpty();
                assertThat(groups).containsExactlyInAnyOrder(testGroups);
            });

        verify(groupFetchService).getGroups();
    }

    @Test
    @DisplayName("When there are group members, then return a list of group members")
    void returnGroupMembers() {
        final Member[] testMembers = {
            GroupTestUtility.generateFullMemberDetails("Member A", 1L),
            GroupTestUtility.generateFullMemberDetails("Member B", 1L)
        };

        given(groupFetchService.getGroupMembers(1L)).willReturn(Flux.just(testMembers));

        webTestClient
            .get()
            .uri("/groups" + "/1" + "/members")
            .exchange()
            .expectStatus().is2xxSuccessful()
            .expectBodyList(Member.class).value(members -> {
                assertThat(members).isNotEmpty();
                assertThat(members).containsExactlyInAnyOrder(testMembers);
            });

        verify(groupFetchService).getGroupMembers(1L);
    }
}
