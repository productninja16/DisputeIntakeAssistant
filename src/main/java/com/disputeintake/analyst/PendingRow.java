package com.disputeintake.analyst;

import com.disputeintake.dispute.Dispute;
import com.disputeintake.dispute.Money;
import java.time.Instant;
import java.util.UUID;

/** A dispute awaiting a decision, as listed in the analyst queue. */
public record PendingRow(UUID id, String merchantName, long amountCents, Instant createdAt) {

    public String caseRef() {
        return Dispute.caseRef(id);
    }

    public String amount() {
        return Money.format(amountCents);
    }
}
