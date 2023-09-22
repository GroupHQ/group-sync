package org.grouphq.groupsync.group.sync;

import lombok.extern.slf4j.Slf4j;
import org.grouphq.groupsync.groupservice.domain.outbox.OutboxEvent;
import org.grouphq.groupsync.groupservice.domain.outbox.enums.EventStatus;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

/**
 * Manages a sink of outbox events as a hot flux.
 */
@Slf4j
@Service
public class GroupUpdateService {

    private final Sinks.Many<OutboxEvent> updatesSink;
    private final Flux<OutboxEvent> updatesFlux;

    private final Sinks.Many<OutboxEvent> failedUpdatesSink;
    private final Flux<OutboxEvent> failedUpdatesFlux;

    public GroupUpdateService() {
        updatesSink = Sinks.many().multicast().onBackpressureBuffer();
        updatesFlux = updatesSink.asFlux();

        failedUpdatesSink = Sinks.many().multicast().onBackpressureBuffer();
        failedUpdatesFlux = failedUpdatesSink.asFlux();
    }


    public Flux<OutboxEvent> outboxEventUpdateStream() {
        return updatesFlux;
    }

    public Flux<OutboxEvent> outboxEventFailedUpdateStream() {
        return failedUpdatesFlux;
    }

    public void sendOutboxEventUpdate(OutboxEvent outboxEvent) {
        try {
            Sinks.EmitResult result = updatesSink.tryEmitNext(outboxEvent);
            emitResultLogger(EventStatus.SUCCESSFUL.toString(), outboxEvent, result);
        } catch (Exception e) {
            log.error("Error while trying to emit outbox event to updates sink. Event: {}",
                outboxEvent, e);
        }
    }

    public void sendOutboxEventUpdateFailed(OutboxEvent outboxEvent) {
        try {
            Sinks.EmitResult result = failedUpdatesSink.tryEmitNext(outboxEvent);
            emitResultLogger(EventStatus.FAILED.toString(), outboxEvent, result);
        } catch (Exception e) {
            log.error("Error while trying to emit outbox event to failed updates sink. Event: {}",
                outboxEvent, e);
        }

    }

    private void emitResultLogger(String eventName,
                                  OutboxEvent outboxEvent,
                                  Sinks.EmitResult result) {
        String resultString = switch (result) {
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
