package com.disputeintake.eval;

import com.disputeintake.suggestion.SuggestionResult;

/**
 * What the pipeline produced for one eval case. {@code result} is null if no valid suggestion was
 * produced; {@code apiFailure} is set when the model calls themselves failed.
 */
public record CaseOutcome(EvalCase evalCase, SuggestionResult result, int attempts, String apiFailure) {

    public CaseOutcome(EvalCase evalCase, SuggestionResult result, int attempts) {
        this(evalCase, result, attempts, null);
    }

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
