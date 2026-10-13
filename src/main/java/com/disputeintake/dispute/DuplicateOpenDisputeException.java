package com.disputeintake.dispute;

public class DuplicateOpenDisputeException extends RuntimeException {

    public DuplicateOpenDisputeException() {
        super("This transaction already has an open dispute");
    }
}
