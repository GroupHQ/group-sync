package org.grouphq.groupsync.groupservice.domain.outbox;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Data class for the error field in the outbox.
 * The @type annotation is temporarily being ignored until GROUP-89 is resolved.
 *
 * @param error The error message.
 */
@JsonIgnoreProperties(value = {"@type"})
public record ErrorData(
    String error
) {
}
