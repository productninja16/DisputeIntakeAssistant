package com.disputeintake.eval;

import com.disputeintake.suggestion.Category;
import com.disputeintake.suggestion.Confidence;
import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Scores a run. A case with no valid suggestion counts as wrong; it is never dropped from the
 * denominator.
 */
public final class EvalReport {

    private static final String NONE = "NONE";

    private final List<CaseOutcome> outcomes;

    public EvalReport(List<CaseOutcome> outcomes) {
        this.outcomes = List.copyOf(outcomes);
    }

    public int total() {
        return outcomes.size();
    }

    public long categoryCorrect() {
        return outcomes.stream().filter(CaseOutcome::categoryCorrect).count();
    }

    public long queueCorrect() {
        return outcomes.stream().filter(CaseOutcome::queueCorrect).count();
    }

    public long bothCorrect() {
        return outcomes.stream().filter(CaseOutcome::bothCorrect).count();
    }

    public long validFirstTry() {
        return outcomes.stream().filter(o -> o.result() != null && o.attempts() == 1).count();
    }

    public long validAfterRetry() {
        return outcomes.stream().filter(o -> o.result() != null && o.attempts() > 1).count();
    }

    public long noSuggestion() {
        return outcomes.stream().filter(o -> o.result() == null).count();
    }

    /** expected category -> (predicted category name or NONE -> count). */
    public Map<Category, Map<String, Integer>> confusion() {
        Map<Category, Map<String, Integer>> m = new EnumMap<>(Category.class);
        for (Category c : Category.values()) {
            m.put(c, new java.util.LinkedHashMap<>());
        }
        for (CaseOutcome o : outcomes) {
            String predicted = o.result() == null ? NONE : o.result().category().name();
            m.get(o.evalCase().expectedCategory()).merge(predicted, 1, Integer::sum);
        }
        return m;
    }

    /** Cases the model gave at each confidence level, and how many of those had the right category. */
    public record ConfidenceRow(Confidence confidence, long cases, long categoryCorrect) {
    }

    public List<ConfidenceRow> byConfidence() {
        return java.util.Arrays.stream(Confidence.values())
                .map(level -> {
                    List<CaseOutcome> atLevel = outcomes.stream()
                            .filter(o -> o.result() != null && o.result().confidence() == level).toList();
                    return new ConfidenceRow(level, atLevel.size(),
                            atLevel.stream().filter(CaseOutcome::categoryCorrect).count());
                })
                .toList();
    }

    public List<CaseOutcome> misses() {
        return outcomes.stream().filter(o -> !o.bothCorrect()).toList();
    }

    private String pct(long n) {
        return total() == 0 ? "n/a" : String.format("%d/%d (%.0f%%)", n, total(), 100.0 * n / total());
    }

    public String toMarkdown(String client, String model, Instant when) {
        StringBuilder sb = new StringBuilder();
        sb.append("# Eval report\n\n");
        sb.append("- Client: ").append(client).append('\n');
        sb.append("- Model: ").append(model).append('\n');
        sb.append("- Run at: ").append(when).append('\n');
        sb.append("- Cases: ").append(total()).append("\n\n");
        sb.append("## Accuracy\n\n");
        sb.append("| Measure | Result |\n|---|---|\n");
        sb.append("| Category correct | ").append(pct(categoryCorrect())).append(" |\n");
        sb.append("| Queue correct | ").append(pct(queueCorrect())).append(" |\n");
        sb.append("| Both correct | ").append(pct(bothCorrect())).append(" |\n\n");
        sb.append("A case with no valid suggestion counts as wrong.\n\n");
        sb.append("## Validation\n\n");
        sb.append("| Measure | Result |\n|---|---|\n");
        sb.append("| Valid on first try | ").append(pct(validFirstTry())).append(" |\n");
        sb.append("| Valid after one retry | ").append(pct(validAfterRetry())).append(" |\n");
        sb.append("| No suggestion (failed twice) | ").append(pct(noSuggestion())).append(" |\n\n");
        sb.append("## Accuracy by confidence\n\n");
        sb.append("Does a higher self-reported confidence mean a more accurate category? ")
                .append("Informational only; confidence is never used to decide anything.\n\n");
        sb.append("| Confidence | Cases | Category correct |\n|---|---|---|\n");
        for (ConfidenceRow r : byConfidence()) {
            sb.append("| ").append(r.confidence()).append(" | ").append(r.cases()).append(" | ")
                    .append(r.cases() == 0 ? "n/a"
                            : String.format("%d/%d (%.0f%%)", r.categoryCorrect(), r.cases(),
                                    100.0 * r.categoryCorrect() / r.cases()))
                    .append(" |\n");
        }
        sb.append('\n');
        sb.append("## Confusion (rows: expected category, columns: predicted)\n\n");
        sb.append("| Expected | Predicted |\n|---|---|\n");
        confusion().forEach((expected, row) -> {
            if (!row.isEmpty()) {
                sb.append("| ").append(expected).append(" | ");
                row.forEach((p, n) -> sb.append(p).append(" x").append(n).append("; "));
                sb.append("|\n");
            }
        });
        sb.append("\n## Misses\n\n");
        if (misses().isEmpty()) {
            sb.append("None.\n");
        }
        for (CaseOutcome o : misses()) {
            EvalCase c = o.evalCase();
            sb.append("- **").append(c.id()).append("**").append(c.ambiguous() ? " (ambiguous label)" : "")
                    .append(": expected ").append(c.expectedCategory()).append(" / ").append(c.expectedQueue())
                    .append(", got ");
            if (o.result() == null) {
                sb.append("no suggestion");
            } else {
                sb.append(o.result().category()).append(" / ").append(o.result().proposedQueue());
            }
            sb.append("\n  - \"").append(c.description().replace("\n", " ")).append("\"\n");
            if (o.result() != null) {
                sb.append("  - reasoning: ").append(o.result().reasoning().replace("\n", " ")).append('\n');
            }
        }
        sb.append("\n## Missing-info (not scored; for reading)\n\n");
        for (CaseOutcome o : outcomes) {
            if (o.result() != null && !o.result().missingInfo().isEmpty()) {
                sb.append("- ").append(o.evalCase().id()).append(": ")
                        .append(String.join("; ", o.result().missingInfo())).append('\n');
            }
        }
        return sb.toString();
    }
}
