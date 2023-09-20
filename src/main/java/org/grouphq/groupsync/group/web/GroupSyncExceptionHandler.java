package org.grouphq.groupsync.group.web;

import org.grouphq.groupsync.groupservice.domain.exceptions.InternalServerError;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * A controller advice to handle exceptions that may be
 * encountered by the application's controllers.
 */
@ControllerAdvice
public class GroupSyncExceptionHandler {
    @ExceptionHandler(InternalServerError.class)
    public ResponseEntity<String> handleInternalServerError(
        InternalServerError exception) {
        return new ResponseEntity<>(exception.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
