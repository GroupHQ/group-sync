package org.grouphq.groupsync.group.sync;

import java.time.Duration;
import java.util.Base64;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.grouphq.groupsync.config.ClientProperties;
import org.grouphq.groupsync.group.domain.GroupServiceUnavailableException;
import org.grouphq.groupsync.group.domain.PublicOutboxEvent;
import org.grouphq.groupsync.groupservice.domain.groups.Group;
import org.grouphq.groupsync.groupservice.domain.members.Member;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

/**
 * Service class for interacting with group service.
 */
@Service
@Slf4j
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
            .doOnError(throwable -> log.error("Group service failed on request to get groups. Error: {}",
                throwable.getMessage()))
            .onErrorMap(throwable -> new GroupServiceUnavailableException(
                "Group service failed on request to get groups"));
    }

    public Flux<PublicOutboxEvent> getGroupsAsEvents() {
        return webClient
            .get()
            .uri("/api/groups/events")
            .retrieve()
            .bodyToFlux(PublicOutboxEvent.class)
            .timeout(Duration.ofMillis(
                    clientProperties.getGroupsTimeoutMilliseconds()),
                Flux.error(new GroupServiceUnavailableException(
                    "Group service timed out on request to get groups")))
            .retryWhen(
                Retry.backoff(clientProperties.getGroupsRetryAttempts(),
                    Duration.ofMillis(clientProperties.getGroupsRetryBackoffMilliseconds())))
            .doOnError(throwable -> log.error("Group service failed on request to get groups as events: {}",
                throwable.getMessage()))
            .onErrorMap(throwable -> new GroupServiceUnavailableException(
                "Group service failed on request to get groups as events"));
    }

    public Mono<Member> getMyMember(String websocketId) {
        return webClient
            .get()
            .uri("/api/groups/my-member")
            .header("Authorization", basicAuthHeaderValue(websocketId))
            .retrieve()
            .bodyToMono(Member.class)
            .timeout(Duration.ofMillis(
                clientProperties.getGroupsTimeoutMilliseconds()),
                Mono.error(new GroupServiceUnavailableException(
                    "Group service timed out on request to get my member")))
            .retryWhen(
                Retry.backoff(clientProperties.getGroupsRetryAttempts(),
                    Duration.ofMillis(clientProperties.getGroupsRetryBackoffMilliseconds())))
            .doOnError(throwable -> log.error("Error while getting my member from group service: {}",
                throwable.getMessage()))
            .onErrorMap(throwable -> new GroupServiceUnavailableException(
                "Group service failed on request to get my member"));
    }

    private String basicAuthHeaderValue(String websocketId) {
        final String credentials = websocketId + ":";
        return "Basic " + new String(Base64.getEncoder().encode(credentials.getBytes()));
    }

}
