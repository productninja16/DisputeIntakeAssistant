package com.disputeintake.eval;

import static org.assertj.core.api.Assertions.assertThat;

import com.disputeintake.suggestion.Category;
import com.disputeintake.suggestion.Confidence;
import com.disputeintake.suggestion.ProposedQueue;
import com.disputeintake.suggestion.SuggestionResult;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class EvalAbortGuardTest {

    private static EvalCase c(String id) {
        return new EvalCase(id, "desc", "Shop", 100, LocalDate.of(2026, 3, 1), Category.OTHER,
                ProposedQueue.GENERAL_REVIEW, false);
    }

    private static CaseOutcome apiFailed(String id) {
        return new CaseOutcome(c(id), null, 2, "400: Your credit balance is too low");
    }

    private static CaseOutcome invalidTwice(String id) {
        return new CaseOutcome(c(id), null, 2, null);
    }

    private static CaseOutcome ok(String id) {
        return new CaseOutcome(c(id), new SuggestionResult(Category.OTHER, List.of(), "r",
                ProposedQueue.GENERAL_REVIEW, Confidence.LOW), 1, null);
    }

    @Test
    void stopsWhenTheFirstThreeCasesAllFailWithApiErrors() {
        var msg = EvalAbortGuard.check(List.of(apiFailed("a"), apiFailed("b"), apiFailed("c")));
        assertThat(msg).isPresent();
        assertThat(msg.get()).contains("credit balance is too low", "no report was written", "ANTHROPIC_API_KEY");
    }

    @Test
    void doesNotStopBeforeThreeCases() {
        assertThat(EvalAbortGuard.check(List.of(apiFailed("a"), apiFailed("b")))).isEmpty();
    }

    @Test
    void doesNotStopWhenAnyEarlyCaseGotAnAnswer() {
        assertThat(EvalAbortGuard.check(List.of(apiFailed("a"), ok("b"), apiFailed("c")))).isEmpty();
    }

    @Test
    void invalidModelAnswersAreNotApiFailures() {
        assertThat(EvalAbortGuard.check(List.of(invalidTwice("a"), invalidTwice("b"), invalidTwice("c")))).isEmpty();
    }

    @Test
    void onlyChecksAtTheThresholdSoLaterFailuresDoNotAbort() {
        List<CaseOutcome> run = new ArrayList<>(List.of(ok("a"), ok("b"), ok("c")));
        run.add(apiFailed("d"));
        assertThat(EvalAbortGuard.check(run)).isEmpty();
    }
}
