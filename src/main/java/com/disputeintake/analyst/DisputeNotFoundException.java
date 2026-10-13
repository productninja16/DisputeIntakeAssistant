package com.disputeintake.analyst;

public class DisputeNotFoundException extends RuntimeException {

    public DisputeNotFoundException() {
        super("The dispute does not exist.");
    }
}
