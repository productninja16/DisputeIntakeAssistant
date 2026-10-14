package com.disputeintake.suggestion;

import com.disputeintake.dispute.CardNumberMasker;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * The model call plus validation plus one retry, with no database. Shared by the app and the
 * eval runner so the eval measures exactly what production does.
 */
@Component
public class SuggestionEngine {

    private static final Logger log = LoggerFactory.getLogger(SuggestionEngine.class);
    static final int MAX_ATTEMPTS = 2;

    /** {@code result} is null when every attempt failed; {@code attempts} is how many were made. */
    public record Outcome(SuggestionResult result, String rawJson, int attempts) {

        public boolean succeeded() {
            return result != null;
        }
    }

    private final SuggestionClient client;
    private final SuggestionValidator validator;

    public SuggestionEngine(SuggestionClient client, SuggestionValidator validator) {
        this.client = client;
        this.validator = validator;
    }

    public Outcome run(SuggestionContext context) {
        SuggestionContext masked = new SuggestionContext(CardNumberMasker.mask(context.description()),
                context.merchantName(), context.amountCents(), context.postedOn());
        List<String> errors = List.of();
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            String raw;
            try {
                raw = client.suggest(masked, errors);
            } catch (RuntimeException e) {
                log.warn("Suggestion attempt {} failed: {}", attempt, e.toString());
                errors = List.of("The request failed; return the JSON object again");
                continue;
            }
            ValidationOutcome outcome = validator.validate(raw);
            if (outcome.isValid()) {
                return new Outcome(outcome.result(), raw, attempt);
            }
            log.warn("Suggestion attempt {} invalid: {}", attempt, outcome.errors());
            errors = outcome.errors();
        }
        return new Outcome(null, null, MAX_ATTEMPTS);
    }

    public String modelName() {
        return client.modelName();
    }
}
