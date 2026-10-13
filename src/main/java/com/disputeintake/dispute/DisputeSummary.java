package com.disputeintake.dispute;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** Row for the customer's "my disputes" list. Carries only a coarse status, never AI output or decision details. */
public record DisputeSummary(UUID id, String merchantName, long amountCents, LocalDate postedOn, Instant createdAt,
        DisputeStatus status) {

    public String caseRef() {
        return Dispute.caseRef(id);
    }

    /** Customers see "In review" or "Reviewed"; whether an analyst approved or overrode is internal. */
    public String statusLabel() {
        return status == DisputeStatus.PENDING_REVIEW ? "In review" : "Reviewed";
    }

    public String amount() {
        return Money.format(amountCents);
    }
}
