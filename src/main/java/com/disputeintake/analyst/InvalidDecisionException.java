package com.disputeintake.analyst;

/** The analyst's input cannot be turned into a decision (for example an override with no note). */
public class InvalidDecisionException extends RuntimeException {

    public InvalidDecisionException(String message) {
        super(message);
    }
}
