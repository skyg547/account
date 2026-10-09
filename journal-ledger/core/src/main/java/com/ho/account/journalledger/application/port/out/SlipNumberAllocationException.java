package com.ho.account.journalledger.application.port.out;

/** Database slip-number allocation failed; callers may retry after service recovery. */
public class SlipNumberAllocationException extends RuntimeException {
    public SlipNumberAllocationException(String message) {
        super(message);
    }

    public SlipNumberAllocationException(String message, Throwable cause) {
        super(message, cause);
    }
}
