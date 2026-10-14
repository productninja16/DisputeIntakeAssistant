package com.disputeintake.suggestion;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import org.junit.jupiter.api.Test;

class SuggestionEngineTest {

    private static final String VALID = """
            {"category":"OTHER","missing_info":[],"reasoning":"r","proposed_queue":"GENERAL_REVIEW","confidence":"LOW"}""";

    private static final SuggestionContext CTX = new SuggestionContext("Something odd happened", "Shop", 100,
            LocalDate.of(2026, 3, 1));

    /** A null answer makes the call throw, like an API error. */
    private static SuggestionEngine engine(String... answers) {
        Queue<String> q = new LinkedList<>(List.of(answers));
        SuggestionClient client = new SuggestionClient() {
            @Override
            public String suggest(SuggestionContext context, List<String> previousErrors) {
                String next = q.poll();
                if (next == null || next.equals("THROW")) {
                    throw new IllegalStateException("400: credit balance too low");
                }
                return next;
            }

            @Override
            public String modelName() {
                return "scripted";
            }
        };
        return new SuggestionEngine(client, new SuggestionValidator(new ObjectMapper()));
    }

    @Test
    void reportsApiFailureWhenEveryCallThrows() {
        var outcome = engine("THROW", "THROW").run(CTX);
        assertThat(outcome.succeeded()).isFalse();
        assertThat(outcome.apiFailure()).contains("credit balance too low");
    }

    @Test
    void invalidAnswersAreNotApiFailures() {
        var outcome = engine("{\"category\":\"NOPE\"}", "not json").run(CTX);
        assertThat(outcome.succeeded()).isFalse();
        assertThat(outcome.apiFailure()).isNull();
    }

    @Test
    void oneThrowFollowedByAValidAnswerStillSucceeds() {
        var outcome = engine("THROW", VALID).run(CTX);
        assertThat(outcome.succeeded()).isTrue();
        assertThat(outcome.attempts()).isEqualTo(2);
        assertThat(outcome.apiFailure()).isNull();
    }
}
