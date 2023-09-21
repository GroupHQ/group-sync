package org.grouphq.groupsync.group.sync;

import org.grouphq.groupsync.groupservice.domain.outbox.OutboxEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

/**
 * Manages a sink of outbox events as a hot flux.
 */
@Service
public class GroupSyncService {

    private final Sinks.Many<OutboxEvent> outboxEventFluxSink;

    private final Flux<OutboxEvent> outboxEventFlux;

    public GroupSyncService() {
        outboxEventFluxSink = Sinks.many().multicast().onBackpressureBuffer();
        outboxEventFlux = outboxEventFluxSink.asFlux();
    }


    public Flux<OutboxEvent> outboxEventUpdateStream() {
        return outboxEventFlux;
    }

    public void sendOutboxEventUpdate(OutboxEvent outboxEvent) {
        outboxEventFluxSink.emitNext(outboxEvent, Sinks.EmitFailureHandler.FAIL_FAST);
    }
}
