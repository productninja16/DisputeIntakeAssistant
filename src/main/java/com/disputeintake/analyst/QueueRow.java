package com.disputeintake.analyst;

import com.disputeintake.suggestion.Category;
import com.disputeintake.suggestion.Confidence;
import com.disputeintake.suggestion.ProposedQueue;

/**
 * A dispute plus the latest AI suggestion's category, queue and confidence. The suggestion fields
 * are null if none exists; confidence is also null for suggestions stored before it was added.
 */
public record QueueRow(DisputeRow dispute, Category suggestedCategory, ProposedQueue suggestedQueue,
        Confidence confidence) {

    public boolean hasSuggestion() {
        return suggestedQueue != null;
    }
}
