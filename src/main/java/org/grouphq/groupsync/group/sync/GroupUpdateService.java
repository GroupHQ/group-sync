package org.grouphq.groupsync.group.sync;

import lombok.extern.slf4j.Slf4j;
import org.grouphq.groupsync.group.domain.PublicOutboxEvent;
import org.grouphq.groupsync.groupservice.domain.outbox.OutboxEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

/**
 * Manages a sink of outbox events as a hot flux.
 */
@Slf4j
@Service
public class GroupUpdateService {

    private final Sinks.Many<PublicOutboxEvent> publicUpdatesSink;
    private final Flux<PublicOutboxEvent> publicUpdatesFlux;

    private final Sinks.Many<OutboxEvent> userUpdatesSink;
    private final Flux<OutboxEvent> userUpdatesFlux;

    public GroupUpdateService() {
        publicUpdatesSink = Sinks.many().multicast().onBackpressureBuffer();
        publicUpdatesFlux = publicUpdatesSink.asFlux();

        userUpdatesSink = Sinks.many().multicast().onBackpressureBuffer();
        userUpdatesFlux = userUpdatesSink.asFlux();
    }

    public Flux<PublicOutboxEvent> publicUpdatesStream() {
        return publicUpdatesFlux.cache();
    }

    public Flux<OutboxEvent> eventOwnerUpdateStream() {
        return userUpdatesFlux.cache();
    }

    public void sendPublicOutboxEventToAll(PublicOutboxEvent outboxEvent) {
        final Sinks.EmitResult result = publicUpdatesSink.tryEmitNext(outboxEvent);
        emitResultLogger("PUBLIC", outboxEvent, result);
    }

    public void sendOutboxEventToEventOwner(OutboxEvent outboxEvent) {
        final Sinks.EmitResult result = userUpdatesSink.tryEmitNext(outboxEvent);
        emitResultLogger(outboxEvent.getEventStatus().toString(), outboxEvent, result);
    }

    private void emitResultLogger(String eventName,
                                  Object outboxEvent,
                                  Sinks.EmitResult result) {
        final String resultString = switch (result) {
            case OK -> "OK";
            case FAIL_OVERFLOW -> "FAIL_OVERFLOW";
            case FAIL_NON_SERIALIZED -> "FAIL_NON_SERIALIZED";
            case FAIL_CANCELLED -> "FAIL_CANCELLED";
            case FAIL_TERMINATED -> "FAIL_TERMINATED";
            case FAIL_ZERO_SUBSCRIBER -> "FAIL_ZERO_SUBSCRIBER";
        };

        if (result.isSuccess()) {
            log.info("Successfully emitted {} event. Event: {}. EmitResult: {}",
                eventName, outboxEvent, resultString);
        } else {
            log.error("Failed to emit {} event. Event: {}. EmitResult: {}",
                eventName, outboxEvent, resultString);
        }
    }
}
