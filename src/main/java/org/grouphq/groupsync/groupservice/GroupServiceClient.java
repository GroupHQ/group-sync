package org.grouphq.groupsync.groupservice;

import org.grouphq.groupsync.groupservice.domain.groups.Group;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

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
}
