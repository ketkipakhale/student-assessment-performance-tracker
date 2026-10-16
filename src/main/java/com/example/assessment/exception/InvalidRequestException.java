package com.example.assessment.exception;

/** Business-rule validation failure. Maps to HTTP 400. */
public class InvalidRequestException extends RuntimeException {
    public InvalidRequestException(String message) {
        super(message);
    }
}
