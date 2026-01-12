package com.bullhorn.mockservice.exception;

/**
 * Generic exception for Bullhorn API related errors
 */
public class BullhornApiException extends RuntimeException {

    public BullhornApiException(String message) {
        super(message);
    }

    public BullhornApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
