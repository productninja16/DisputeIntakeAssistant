package com.disputeintake.eval;

import static org.assertj.core.api.Assertions.assertThat;

import com.disputeintake.dispute.CardNumberMasker;
import com.disputeintake.suggestion.Category;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class EvalCasesFileTest {

    private final List<EvalCase> cases = load();

    private static List<EvalCase> load() {
        try {
            return EvalCaseLoader.load(Path.of("eval/cases.json"));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    void hasThirtyCasesWithUniqueIds() {
        assertThat(cases).hasSize(30);
        assertThat(new HashSet<>(cases.stream().map(EvalCase::id).toList())).hasSize(30);
    }

    @Test
    void everyCategoryHasAtLeastThreeCases() {
        Map<Category, Integer> counts = new EnumMap<>(Category.class);
        cases.forEach(c -> counts.merge(c.expectedCategory(), 1, Integer::sum));
        for (Category c : Category.values()) {
            assertThat(counts.getOrDefault(c, 0)).as(c.name()).isGreaterThanOrEqualTo(3);
        }
    }

    @Test
    void descriptionsFitTheProductionLimits() {
        for (EvalCase c : cases) {
            assertThat(c.description().length()).as(c.id()).isBetween(10, 4000);
            assertThat(c.amountCents()).as(c.id()).isPositive();
            assertThat(c.merchantName()).as(c.id()).isNotBlank();
        }
    }

    @Test
    void includesACaseWithACardNumberThatGetsMasked() {
        assertThat(cases).anyMatch(c -> !CardNumberMasker.mask(c.description()).equals(c.description()));
    }
}
