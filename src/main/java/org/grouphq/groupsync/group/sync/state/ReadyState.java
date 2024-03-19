package org.grouphq.groupsync.group.sync.state;

import reactor.core.publisher.Mono;

/**
 * State representing when {@link GroupInitialStateService} successfully initialized the current state
 * of groups and their members.
 */
public class ReadyState extends State {

    public ReadyState(GroupInitialStateService groupInitialStateService) {
        super(groupInitialStateService);
    }

    /**
     * Calls to this method will return an empty Mono, as the state is already ready.
     *
     * @return an empty Mono
     */
    @Override
    public Mono<Void> onRequest() {
        return Mono.empty();
    }
}
