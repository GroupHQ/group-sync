package org.grouphq.groupsync.group.domain;

/**
 * Exception thrown when group service is unavailable.
 */
public class GroupServiceUnavailableException extends RuntimeException {
    public GroupServiceUnavailableException(String message) {
        super(message);
    }
}
