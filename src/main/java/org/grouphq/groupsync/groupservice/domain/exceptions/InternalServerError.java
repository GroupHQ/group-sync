package org.grouphq.groupsync.groupservice.domain.exceptions;

/**
 * Business exception informing that an action cannot be completed
 * because the server has encountered an unexpected error.
 */
public class InternalServerError extends RuntimeException {
    public InternalServerError() {
        super("""
            The server has encountered an unexpected error.
            """);
    }

    public InternalServerError(String action) {
        super(action + """
             because the server has encountered an unexpected error.
            """);
    }
}
