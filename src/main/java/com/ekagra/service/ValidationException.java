package com.ekagra.service;

/** Thrown when user input breaks a business rule; the message is shown directly in the UI. */
public class ValidationException extends RuntimeException {
    public ValidationException(String message) {
        super(message);
    }
}
