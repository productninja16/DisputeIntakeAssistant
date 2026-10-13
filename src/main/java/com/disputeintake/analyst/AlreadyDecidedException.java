package com.disputeintake.analyst;

public class AlreadyDecidedException extends RuntimeException {

    public AlreadyDecidedException() {
        super("A decision has already been recorded for this dispute.");
    }
}
