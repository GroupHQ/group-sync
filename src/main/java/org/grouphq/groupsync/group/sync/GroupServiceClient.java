package org.grouphq.groupsync.group.sync;

import org.grouphq.groupsync.groupservice.domain.groups.Group;
import org.grouphq.groupsync.groupservice.domain.members.Member;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

/**
 * Service class for interacting with group service.
 */
@Component
public class GroupServiceClient {
    private final WebClient webClient;

    public GroupServiceClient(WebClient webClient) {
        this.webClient = webClient;
    }

    public Flux<Group> getGroups() {
        return webClient
                .get()
                .uri("/groups")
                .retrieve()
                .bodyToFlux(Group.class);
    }

    public Flux<Member> getGroupMembers(Long groupId) {
        return webClient
                .get()
                .uri("/groups/" + groupId + "/members")
                .retrieve()
                .bodyToFlux(Member.class);
    }
}
