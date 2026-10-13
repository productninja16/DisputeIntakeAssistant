package com.disputeintake.dispute;

/** The transaction doesn't exist or isn't owned by the signed-in customer. */
public class TransactionNotFoundException extends RuntimeException {

    public TransactionNotFoundException() {
        super("Transaction not found");
    }
}
