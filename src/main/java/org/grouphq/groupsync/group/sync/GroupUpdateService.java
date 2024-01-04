package org.grouphq.groupsync.group.sync;

import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.grouphq.groupsync.group.domain.PublicOutboxEvent;
import org.grouphq.groupsync.groupservice.domain.outbox.OutboxEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.BufferOverflowStrategy;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

/**
 * Manages a sink of outbox events as a hot flux.
 */
@Slf4j
@Service
public class GroupUpdateService {

    private final Sinks.Many<PublicOutboxEvent> publicUpdatesSink;

    private final Sinks.Many<OutboxEvent> userUpdatesSink;

    public GroupUpdateService() {
        publicUpdatesSink = Sinks.many().replay().limit(Duration.ofSeconds(5));

        userUpdatesSink = Sinks.many().replay().limit(Duration.ofSeconds(5));
    }

    public Flux<PublicOutboxEvent> publicUpdatesStream() {
        return publicUpdatesSink.asFlux()
            .onBackpressureBuffer(100, BufferOverflowStrategy.DROP_OLDEST);
    }

    public Flux<OutboxEvent> eventOwnerUpdateStream() {
        return userUpdatesSink.asFlux()
            .onBackpressureBuffer(100, BufferOverflowStrategy.DROP_OLDEST);
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
        final String resultString = result.name();
        if (result.isFailure()) {
            log.error("Failed to emit {} event. Event: {}. EmitResult: {}",
                eventName, outboxEvent, resultString);
        }
    }
}
