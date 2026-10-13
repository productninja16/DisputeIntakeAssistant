package com.disputeintake.analyst;

import com.disputeintake.dispute.Dispute;
import com.disputeintake.dispute.DisputeStatus;
import com.disputeintake.dispute.Money;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** Everything the analyst case page shows. suggestion and decision are each null when absent. */
public record CaseView(UUID id, String description, DisputeStatus status, String merchantName, long amountCents,
        LocalDate postedOn, String cardLast4, Instant createdAt, SuggestionView suggestion, Decision decision) {

    public String caseRef() {
        return Dispute.caseRef(id);
    }

    public String amount() {
        return Money.format(amountCents);
    }

    public boolean pending() {
        return status == DisputeStatus.PENDING_REVIEW;
    }
}
