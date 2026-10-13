package com.disputeintake.analyst;

import com.disputeintake.suggestion.Category;
import com.disputeintake.suggestion.ProposedQueue;

/** A pending dispute plus the latest AI suggestion's category and queue, both null if none exists. */
public record QueueRow(PendingRow dispute, Category suggestedCategory, ProposedQueue suggestedQueue) {

    public boolean hasSuggestion() {
        return suggestedQueue != null;
    }
}
