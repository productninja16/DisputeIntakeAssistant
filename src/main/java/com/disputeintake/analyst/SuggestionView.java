package com.disputeintake.analyst;

import com.disputeintake.suggestion.Category;
import com.disputeintake.suggestion.ProposedQueue;
import java.util.List;
import java.util.UUID;

/** What the analyst sees of the AI output. It is a suggestion only. */
public record SuggestionView(UUID id, Category category, ProposedQueue proposedQueue, List<String> missingInfo,
        String reasoning, String model) {
}
