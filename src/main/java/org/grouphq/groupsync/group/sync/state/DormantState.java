package org.grouphq.groupsync.group.sync.state;

import java.util.concurrent.atomic.AtomicReference;
import lombok.extern.slf4j.Slf4j;
import org.grouphq.groupsync.config.ClientProperties;
import reactor.core.publisher.Mono;
import reactor.core.publisher.SignalType;

/**
 * State representing when {@link GroupInitialStateService} is waiting for a client to trigger the initialization.
 */
@Slf4j
public class DormantState extends State {

    private final ClientProperties clientProperties;

    private final AtomicReference<Mono<Void>> initialRequest = new AtomicReference<>();

    public DormantState(GroupInitialStateService groupInitialStateService, ClientProperties clientProperties) {
        super(groupInitialStateService);
        this.clientProperties = clientProperties;
    }

    /**
     * Triggers the initialization of the current state of groups and their members.
     * Note that this method will only trigger the initialization once, subsequent calls will return the same request.
     * This is done by caching the mono, making it a hot source.
     *
     * @return a Mono that will initialize the current state of groups and their members
     */
    @Override
    public Mono<Void> onRequest() {
        initialRequest.compareAndSet(null,
            groupInitialStateService.initializeGroupState()
                .doFinally(signalType -> {
                    if (signalType == SignalType.ON_COMPLETE) {
                        groupInitialStateService.setState(new ReadyState(groupInitialStateService));
                    } else if (signalType == SignalType.ON_ERROR) {
                        groupInitialStateService.setState(
                            new DormantState(groupInitialStateService, clientProperties));
                    }
                }).cache()
        );

        final Mono<Void> cachedRequest = initialRequest.get();

        groupInitialStateService.setState(new LoadingState(groupInitialStateService, cachedRequest));
        return cachedRequest;
    }
}
