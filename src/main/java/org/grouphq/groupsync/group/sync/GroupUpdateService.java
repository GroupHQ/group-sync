package org.grouphq.groupsync.group.sync;

import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.grouphq.groupsync.group.domain.PublicOutboxEvent;
import org.grouphq.groupsync.groupservice.domain.outbox.OutboxEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.BufferOverflowStrategy;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
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

    public Mono<Void> sendPublicOutboxEventToAll(PublicOutboxEvent outboxEvent) {
        return Mono.defer(() ->
            Mono.just(publicUpdatesSink.tryEmitNext(outboxEvent))
                .doOnNext(result -> emitResultLogger("PUBLIC", outboxEvent, result))
                .then()
        );
    }

    public Mono<Void> sendOutboxEventToEventOwner(OutboxEvent outboxEvent) {
        return Mono.defer(() ->
            Mono.just(userUpdatesSink.tryEmitNext(outboxEvent))
                .doOnNext(result -> emitResultLogger("USER", outboxEvent, result))
                .then()
        );
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
