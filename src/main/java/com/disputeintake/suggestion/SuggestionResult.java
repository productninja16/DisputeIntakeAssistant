package com.disputeintake.suggestion;

import java.util.List;

/** A validated LLM suggestion. Never applied automatically; an analyst decides. */
public record SuggestionResult(Category category, List<String> missingInfo, String reasoning,
        ProposedQueue proposedQueue, Confidence confidence) {
}
