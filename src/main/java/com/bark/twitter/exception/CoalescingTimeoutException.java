package com.bark.twitter.exception;

/**
 * Thrown when a coalesced request times out waiting for the initiator.
 * This is expected during upstream service outages and should not log a full stack trace.
 */
public class CoalescingTimeoutException extends RuntimeException {
    public CoalescingTimeoutException(String message) {
        super(message);
    }

    public CoalescingTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }
}
