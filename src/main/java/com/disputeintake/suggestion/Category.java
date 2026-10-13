package com.disputeintake.suggestion;

/** Simple internal dispute categories. Must match the CHECK constraints in the schema. */
public enum Category {
    UNAUTHORIZED_TRANSACTION,
    DUPLICATE_CHARGE,
    MERCHANDISE_NOT_RECEIVED,
    INCORRECT_AMOUNT,
    SUBSCRIPTION_CANCELLED,
    OTHER
}
