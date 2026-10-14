package com.stayease.exception;

/** The request is invalid (HTTP 400). The message is shown to the client, so keep it user-facing. */
public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) {
        super(message);
    }
}
