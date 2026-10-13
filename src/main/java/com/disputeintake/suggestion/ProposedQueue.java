package com.disputeintake.suggestion;

/** Routing queues. Must match the CHECK constraints in the schema. */
public enum ProposedQueue {
    FRAUD,
    BILLING_ERRORS,
    MERCHANT_DISPUTES,
    GENERAL_REVIEW
}
