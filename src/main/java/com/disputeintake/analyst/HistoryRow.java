package com.disputeintake.analyst;

import com.disputeintake.dispute.Dispute;
import com.disputeintake.dispute.Money;
import com.disputeintake.suggestion.Category;
import com.disputeintake.suggestion.ProposedQueue;
import java.time.Instant;
import java.util.UUID;

/** A decided dispute. Overridden decisions are flagged so they stand out. */
public record HistoryRow(UUID id, String merchantName, long amountCents, DecisionType decisionType,
        Category finalCategory, ProposedQueue finalQueue, String decidedBy, Instant decidedAt) {

    public String caseRef() {
        return Dispute.caseRef(id);
    }

    public String amount() {
        return Money.format(amountCents);
    }

    public boolean overridden() {
        return decisionType == DecisionType.OVERRIDDEN;
    }
}
