package org.grouphq.groupsync.group.domain;

public class GroupServiceTimeoutException extends RuntimeException {
    public GroupServiceTimeoutException(String message) {
        super(message);
    }
}
