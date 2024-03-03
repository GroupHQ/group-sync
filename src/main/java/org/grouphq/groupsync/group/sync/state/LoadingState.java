package org.grouphq.groupsync.group.sync.state;

import reactor.core.publisher.Mono;

/**
 * State representing when {@link GroupInitialStateService} is waiting for groups to be fetched.
 */
public class LoadingState extends State {

    private final Mono<Void> request;

    public LoadingState(GroupInitialStateService groupInitialStateService, Mono<Void> request) {
        super(groupInitialStateService);

        this.request = request;
    }

    /**
     * Returns the currently active request for the groups being fetched.
     *
     * @return the same request that was passed in the constructor
     */
    @Override
    public Mono<Void> onRequest() {
        return this.request;
    }
}
