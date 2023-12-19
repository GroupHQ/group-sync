package org.grouphq.groupsync.group.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.util.Base64;
import java.util.UUID;
import org.grouphq.groupsync.GroupTestUtility;
import org.grouphq.groupsync.config.SecurityConfig;
import org.grouphq.groupsync.group.sync.GroupFetchService;
import org.grouphq.groupsync.groupservice.domain.groups.Group;
import org.grouphq.groupsync.groupservice.domain.groups.GroupStatus;
import org.grouphq.groupsync.groupservice.domain.members.Member;
import org.grouphq.groupsync.groupservice.web.objects.egress.PublicMember;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.rsocket.annotation.support.RSocketMessageHandler;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;

@WebFluxTest(GroupController.class)
@Import({SecurityConfig.class, RSocketMessageHandler.class})
@Tag("IntegrationTest")
class GroupControllerTest {

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

        final String credentials = UUID.randomUUID() + ":password";
        final String authorization = Base64.getEncoder().encodeToString(credentials.getBytes());

        webTestClient
            .get()
            .uri("/api/groups")
            .header("Authorization", "Basic " + authorization)
            .exchange()
            .expectStatus().is2xxSuccessful()
            .expectBodyList(Group.class).value(groups -> {
                assertThat(groups).isNotEmpty();
                assertThat(groups).containsExactlyInAnyOrder(testGroups);
            });

        verify(groupFetchService).getGroups();
    }

    @Test
    @DisplayName("When there are group members, then return a list of group members as public")
    void returnGroupMembers() {
        final PublicMember[] testMembers = {
            Member.toPublicMember(GroupTestUtility.generateFullMemberDetails("Member A", 1L)),
            Member.toPublicMember(GroupTestUtility.generateFullMemberDetails("Member B", 1L))
        };

        given(groupFetchService.getGroupMembers(1L)).willReturn(Flux.just(testMembers));

        final String credentials = UUID.randomUUID() + ":password";
        final String authorization = Base64.getEncoder().encodeToString(credentials.getBytes());

        webTestClient
            .get()
            .uri("/api/groups" + "/1" + "/members")
            .header("Authorization", "Basic " + authorization)
            .exchange()
            .expectStatus().is2xxSuccessful()
            .expectBodyList(PublicMember.class).value(members -> {
                assertThat(members).isNotEmpty();
                assertThat(members).containsExactlyInAnyOrder(testMembers);
            });

        verify(groupFetchService).getGroupMembers(1L);
    }
}
