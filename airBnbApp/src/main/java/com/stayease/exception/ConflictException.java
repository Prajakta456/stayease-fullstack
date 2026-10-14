package com.stayease.exception;

/**
 * The request conflicts with the current state, e.g. a room that is no longer available (HTTP 409).
 * The message is shown to the client, so keep it user-facing.
 */
public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}
