package org.grouphq.groupsync.group.sync.state;

import jakarta.annotation.PreDestroy;
import java.time.Duration;
import java.util.HashSet;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.grouphq.groupsync.config.ClientProperties;
import org.grouphq.groupsync.group.domain.PublicOutboxEvent;
import org.grouphq.groupsync.group.sync.GroupFetchService;
import org.grouphq.groupsync.group.sync.GroupUpdateService;
import org.grouphq.groupsync.groupservice.domain.groups.Group;
import org.springframework.stereotype.Service;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.retry.Retry;

/**
 * Maintains the current relevant events to enable clients
 * to build the current state of groups and their members.
 * Relies on Java's Standard Library Concurrency Utilities
 */
@Service
@Slf4j
public class GroupInitialStateService {

    private final GroupFetchService groupFetchService;
    private final GroupUpdateService groupUpdateService;
    private final GroupStateService groupStateService;
    private final ClientProperties clientProperties;

    @Getter
    @Setter(AccessLevel.PACKAGE)
    private volatile State state;

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private transient Disposable updateSubscription;


    public GroupInitialStateService(GroupFetchService groupFetchService, GroupUpdateService groupUpdateService,
                                    GroupStateService groupStateService, ClientProperties clientProperties) {
        this.groupFetchService = groupFetchService;
        this.groupUpdateService = groupUpdateService;
        this.groupStateService = groupStateService;
        this.clientProperties = clientProperties;
        setState(new DormantState(this, clientProperties));
    }

    public Flux<PublicOutboxEvent> requestCurrentEvents() {
        return state.onRequest()
            .thenMany(buildState());
    }

    private Flux<PublicOutboxEvent> buildState() {
        return groupStateService.getCurrentGroupEvents()
            .filter(event -> event.eventData() instanceof Group)
            .flatMap(event -> {
                final Group group = (Group) event.eventData();
                return groupStateService.getMembersForGroup(group.id())
                    .collectList()
                    .map(publicMembers -> {
                        final Group groupWithMembers = group.withMembers(new HashSet<>(publicMembers));
                        return event.withNewEventData(groupWithMembers);
                    });
            });
    }

    protected Mono<Void> initializeGroupState() {
        return groupStateService.resetState().then(
            groupFetchService.getGroupsAsEvents()
                .flatMap(groupStateService::saveGroupEvent)
                .retryWhen(
                    Retry.backoff(clientProperties.getGroupsRetryAttempts(),
                            Duration.ofMillis(clientProperties.getGroupsRetryBackoffMilliseconds()))
                    .maxBackoff(Duration.ofSeconds(10))
                    .doBeforeRetry(retrySignal -> log.warn("Retrying due to error", retrySignal.failure())))
                .doOnComplete(this::createUpdateSubscription)
                .doOnError(error -> log.error("Error getting initial state of events", error))
                .then(Mono.empty())
        );
    }

    private void createUpdateSubscription() {
        disposeUpdateSubscription();

        updateSubscription = keepGroupsAndMembersUpToDate()
            .subscribeOn(Schedulers.boundedElastic())
            .subscribe();
    }

    private void disposeUpdateSubscription() {
        if (updateSubscription != null && !updateSubscription.isDisposed()) {
            updateSubscription.dispose();
        }
    }

    /**
     * Methods annotated with PreDestroy are called by the Spring framework before destroying the service bean.
     */
    @PreDestroy
    public void cleanUp() {
        disposeUpdateSubscription();
    }

    protected Flux<PublicOutboxEvent> keepGroupsAndMembersUpToDate() {
        return groupUpdateService.publicUpdatesStream()
            .flatMap(groupStateService::handleEventUpdate)
            .doOnError(throwable -> log.error("Error keeping groups and members up to date", throwable));
    }


}
