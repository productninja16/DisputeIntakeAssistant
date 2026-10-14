package com.disputeintake.eval;

import java.util.List;
import java.util.Optional;

/**
 * Stops a run early when the model calls themselves are failing (bad key, no credit, no network).
 * Otherwise such a run would be scored as a model that gets everything wrong.
 */
public final class EvalAbortGuard {

    /** How many leading cases must all fail with API errors before the run is abandoned. */
    public static final int THRESHOLD = 3;

    private EvalAbortGuard() {
    }

    /** Call after each case; returns a message when the run should stop. */
    public static Optional<String> check(List<CaseOutcome> soFar) {
        if (soFar.size() != THRESHOLD || !soFar.stream().allMatch(o -> o.apiFailure() != null)) {
            return Optional.empty();
        }
        String last = soFar.get(THRESHOLD - 1).apiFailure();
        if (last.length() > 400) {
            last = last.substring(0, 400) + "...";
        }
        return Optional.of("Stopped after " + THRESHOLD + " cases: every model call failed with an API error, "
                + "so no accuracy can be reported and no report was written.\n"
                + "Last error: " + last + "\n"
                + "Check ANTHROPIC_API_KEY, your billing credit, and your network, then run again.");
    }
}
