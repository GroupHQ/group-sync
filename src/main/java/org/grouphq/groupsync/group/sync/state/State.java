package org.grouphq.groupsync.group.sync.state;

import reactor.core.publisher.Mono;

/**
 * State pattern for the group initial state service.
 */
public abstract class State {
    protected GroupInitialStateService groupInitialStateService;

    public State(GroupInitialStateService groupInitialStateService) {
        this.groupInitialStateService = groupInitialStateService;
    }

    public abstract Mono<Void> onRequest();
}
