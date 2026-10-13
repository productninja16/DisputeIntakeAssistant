package com.disputeintake.dispute;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** Row for the customer's "my disputes" list. Deliberately carries no status or AI output. */
public record DisputeSummary(UUID id, String merchantName, long amountCents, LocalDate postedOn, Instant createdAt) {

    public String caseRef() {
        return Dispute.caseRef(id);
    }

    public String amount() {
        return Money.format(amountCents);
    }
}
