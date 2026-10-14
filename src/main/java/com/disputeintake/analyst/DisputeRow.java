package com.disputeintake.analyst;

import com.disputeintake.dispute.Dispute;
import com.disputeintake.dispute.DisputeStatus;
import com.disputeintake.dispute.Money;
import java.time.Instant;
import java.util.UUID;

/** One dispute as listed in the analyst queue, whatever its status. */
public record DisputeRow(UUID id, String merchantName, long amountCents, Instant createdAt, DisputeStatus status) {

    public String caseRef() {
        return Dispute.caseRef(id);
    }

    public String amount() {
        return Money.format(amountCents);
    }

    public String statusLabel() {
        return switch (status) {
            case PENDING_REVIEW -> "In review";
            case APPROVED -> "Approved";
            case OVERRIDDEN -> "Overridden";
        };
    }
}
