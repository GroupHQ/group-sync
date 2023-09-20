package org.grouphq.groupsync.group.domain.outbox;

/**
 * Data class for the error field in the outbox.
 *
 * @param error The error message.
 */
public record ErrorData(
    String error
) {
}
