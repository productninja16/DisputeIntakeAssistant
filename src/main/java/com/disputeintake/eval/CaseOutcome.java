package com.disputeintake.eval;

import com.disputeintake.suggestion.SuggestionResult;

/** What the pipeline produced for one eval case. {@code result} is null if validation failed twice. */
public record CaseOutcome(EvalCase evalCase, SuggestionResult result, int attempts) {

    public boolean categoryCorrect() {
        return result != null && result.category() == evalCase.expectedCategory();
    }

    public boolean queueCorrect() {
        return result != null && result.proposedQueue() == evalCase.expectedQueue();
    }

    public boolean bothCorrect() {
        return categoryCorrect() && queueCorrect();
    }
}
