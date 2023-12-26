package org.grouphq.groupsync.group.sync;

import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.grouphq.groupsync.config.ClientProperties;
import org.grouphq.groupsync.group.domain.GroupServiceUnavailableException;
import org.grouphq.groupsync.groupservice.domain.groups.Group;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.util.retry.Retry;

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
            .uri("/api/groups")
            .retrieve()
            .bodyToFlux(Group.class)
            .timeout(Duration.ofMillis(
                clientProperties.getGroupsTimeoutMilliseconds()),
                Flux.error(new GroupServiceUnavailableException(
                    "Group service timed out on request to get groups")))
            .retryWhen(
                Retry.backoff(clientProperties.getGroupsRetryAttempts(),
                    Duration.ofMillis(clientProperties.getGroupsRetryBackoffMilliseconds())))
            .onErrorMap(throwable -> new GroupServiceUnavailableException(
                "Group service failed on request to get groups"));
    }
}
