package org.grouphq.groupsync.group.sync;

import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.grouphq.groupsync.config.ClientProperties;
import org.grouphq.groupsync.group.domain.GroupServiceTimeoutException;
import org.grouphq.groupsync.groupservice.domain.groups.Group;
import org.grouphq.groupsync.groupservice.domain.members.Member;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

/**
 * Service class for interacting with group service.
 */
@Service
@RequiredArgsConstructor
public class GroupServiceClient {
    private final WebClient webClient;

    private final ClientProperties clientProperties;

    public Flux<Group> getGroups() {
        return webClient
            .get()
            .uri("/groups")
            .retrieve()
            .bodyToFlux(Group.class)
            .timeout(Duration.ofMillis(clientProperties.getGroupsTimeout()),
                Flux.error(new GroupServiceTimeoutException(
                    "Group service timed out on request to get groups")));
    }

    public Flux<Member> getGroupMembers(Long groupId) {
        return webClient
            .get()
            .uri("/groups/" + groupId + "/members")
            .retrieve()
            .bodyToFlux(Member.class)
            .timeout(Duration.ofMillis(clientProperties.getGroupMembersTimeout()),
                Flux.error(new GroupServiceTimeoutException(
                    "Group service timed out on request to get group members")));
    }
}
