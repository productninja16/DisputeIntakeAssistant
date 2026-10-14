package com.disputeintake.eval;

import com.disputeintake.suggestion.Category;
import com.disputeintake.suggestion.ProposedQueue;
import java.time.LocalDate;

/**
 * One labelled synthetic case. {@code ambiguous} marks cases where a reasonable analyst could
 * disagree with the label; they are scored the same but flagged in the report.
 */
public record EvalCase(String id, String description, String merchantName, long amountCents, LocalDate postedOn,
        Category expectedCategory, ProposedQueue expectedQueue, boolean ambiguous) {
}
