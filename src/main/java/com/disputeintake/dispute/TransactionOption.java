package com.disputeintake.dispute;

import java.time.LocalDate;
import java.util.UUID;

/** A transaction the customer can choose to dispute. */
public record TransactionOption(UUID id, String merchantName, long amountCents, LocalDate postedOn,
        String cardLast4, boolean disputeOpen) {

    public String amount() {
        return Money.format(amountCents);
    }
}
