package com.disputeintake.eval;

import static org.assertj.core.api.Assertions.assertThat;

import com.disputeintake.suggestion.Category;
import com.disputeintake.suggestion.Confidence;
import com.disputeintake.suggestion.ProposedQueue;
import com.disputeintake.suggestion.SuggestionResult;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class EvalReportTest {

    private static EvalCase expect(String id, Category c, ProposedQueue q) {
        return new EvalCase(id, "desc " + id, "Shop", 100, LocalDate.of(2026, 3, 1), c, q, false);
    }

    private static SuggestionResult got(Category c, ProposedQueue q) {
        return new SuggestionResult(c, List.of("x"), "because", q, Confidence.MEDIUM);
    }

    private final List<CaseOutcome> outcomes = List.of(
            // right category and queue, first try
            new CaseOutcome(expect("a", Category.DUPLICATE_CHARGE, ProposedQueue.BILLING_ERRORS),
                    got(Category.DUPLICATE_CHARGE, ProposedQueue.BILLING_ERRORS), 1),
            // right category, wrong queue, after a retry
            new CaseOutcome(expect("b", Category.OTHER, ProposedQueue.GENERAL_REVIEW),
                    got(Category.OTHER, ProposedQueue.FRAUD), 2),
            // wrong category and queue
            new CaseOutcome(expect("c", Category.UNAUTHORIZED_TRANSACTION, ProposedQueue.FRAUD),
                    got(Category.OTHER, ProposedQueue.GENERAL_REVIEW), 1),
            // no valid suggestion at all
            new CaseOutcome(expect("d", Category.INCORRECT_AMOUNT, ProposedQueue.BILLING_ERRORS), null, 2));

    private final EvalReport report = new EvalReport(outcomes);

    @Test
    void countsAccuracyOverAllCasesIncludingFailures() {
        assertThat(report.total()).isEqualTo(4);
        assertThat(report.categoryCorrect()).isEqualTo(2);
        assertThat(report.queueCorrect()).isEqualTo(1);
        assertThat(report.bothCorrect()).isEqualTo(1);
    }

    @Test
    void countsValidationOutcomes() {
        assertThat(report.validFirstTry()).isEqualTo(2);
        assertThat(report.validAfterRetry()).isEqualTo(1);
        assertThat(report.noSuggestion()).isEqualTo(1);
    }

    @Test
    void confusionRecordsPredictionsAndMissingSuggestions() {
        assertThat(report.confusion().get(Category.UNAUTHORIZED_TRANSACTION)).containsEntry("OTHER", 1);
        assertThat(report.confusion().get(Category.INCORRECT_AMOUNT)).containsEntry("NONE", 1);
    }

    @Test
    void markdownListsMissesAndPercentages() {
        String md = report.toMarkdown("stub", "stub", Instant.parse("2026-01-01T00:00:00Z"));
        assertThat(md).contains("2/4 (50%)", "1/4 (25%)", "**b**", "**c**", "**d**", "no suggestion")
                .doesNotContain("**a**");
    }

    @Test
    void groupsAccuracyByConfidenceLevel() {
        var high = new SuggestionResult(Category.OTHER, List.of(), "r", ProposedQueue.GENERAL_REVIEW, Confidence.HIGH);
        var low = new SuggestionResult(Category.UNAUTHORIZED_TRANSACTION, List.of(), "r", ProposedQueue.FRAUD, Confidence.LOW);
        EvalReport r = new EvalReport(List.of(
                new CaseOutcome(expect("h1", Category.OTHER, ProposedQueue.GENERAL_REVIEW), high, 1),
                new CaseOutcome(expect("h2", Category.OTHER, ProposedQueue.GENERAL_REVIEW), high, 1),
                new CaseOutcome(expect("l1", Category.OTHER, ProposedQueue.GENERAL_REVIEW), low, 1),
                new CaseOutcome(expect("n", Category.OTHER, ProposedQueue.GENERAL_REVIEW), null, 2)));

        var rows = r.byConfidence();
        assertThat(rows).extracting(EvalReport.ConfidenceRow::confidence)
                .containsExactly(Confidence.LOW, Confidence.MEDIUM, Confidence.HIGH);
        assertThat(rows.get(0).cases()).isEqualTo(1);
        assertThat(rows.get(0).categoryCorrect()).isZero();
        assertThat(rows.get(1).cases()).isZero();
        assertThat(rows.get(2).cases()).isEqualTo(2);
        assertThat(rows.get(2).categoryCorrect()).isEqualTo(2);
        assertThat(r.toMarkdown("stub", "stub", Instant.EPOCH)).contains("Accuracy by confidence", "2/2 (100%)");
    }
}
