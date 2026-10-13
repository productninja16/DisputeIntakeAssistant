package com.disputeintake.suggestion;

import com.disputeintake.customer.CardTransaction;
import com.disputeintake.customer.CardTransactionRepository;
import com.disputeintake.dispute.CardNumberMasker;
import com.disputeintake.dispute.Dispute;
import com.disputeintake.dispute.DisputeRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Asks the model for a suggestion, validates it, retries once, and stores it. Never changes the
 * dispute and never creates a decision. If both attempts fail, nothing is stored and the case
 * simply reaches the analyst with no suggestion.
 */
@Service
public class SuggestionService {

    private static final Logger log = LoggerFactory.getLogger(SuggestionService.class);
    private static final int MAX_ATTEMPTS = 2;

    private final DisputeRepository disputes;
    private final CardTransactionRepository transactions;
    private final AiSuggestionRepository suggestions;
    private final SuggestionClient client;
    private final SuggestionValidator validator;
    private final ObjectMapper mapper;

    public SuggestionService(DisputeRepository disputes, CardTransactionRepository transactions,
            AiSuggestionRepository suggestions, SuggestionClient client, SuggestionValidator validator,
            ObjectMapper mapper) {
        this.disputes = disputes;
        this.transactions = transactions;
        this.suggestions = suggestions;
        this.client = client;
        this.validator = validator;
        this.mapper = mapper;
    }

    /** Not transactional on purpose: the model call is slow and must not hold a DB connection. */
    public Optional<AiSuggestion> generateFor(UUID disputeId) {
        if (suggestions.existsByDisputeId(disputeId)) {
            return Optional.empty();
        }
        Dispute dispute = disputes.findById(disputeId).orElse(null);
        CardTransaction tx = dispute == null ? null : transactions.findById(dispute.getTransactionId()).orElse(null);
        if (tx == null) {
            log.warn("Suggestion skipped: dispute {} or its transaction not found", disputeId);
            return Optional.empty();
        }
        SuggestionContext context = new SuggestionContext(
                CardNumberMasker.mask(dispute.getDescription()), // defence in depth
                tx.getMerchantName(), tx.getAmountCents(), tx.getPostedOn());

        List<String> errors = List.of();
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            String raw;
            try {
                raw = client.suggest(context, errors);
            } catch (RuntimeException e) {
                log.warn("Suggestion attempt {} for dispute {} failed: {}", attempt, disputeId, e.toString());
                errors = List.of("The request failed; return the JSON object again");
                continue;
            }
            ValidationOutcome outcome = validator.validate(raw);
            if (outcome.isValid()) {
                return Optional.of(store(disputeId, outcome.result(), raw));
            }
            log.warn("Suggestion attempt {} for dispute {} invalid: {}", attempt, disputeId, outcome.errors());
            errors = outcome.errors();
        }
        log.warn("No suggestion for dispute {} after {} attempts; analyst reviews unaided", disputeId, MAX_ATTEMPTS);
        return Optional.empty();
    }

    private AiSuggestion store(UUID disputeId, SuggestionResult result, String raw) {
        try {
            String missing = mapper.writeValueAsString(result.missingInfo());
            String canonicalRaw = mapper.writeValueAsString(mapper.readTree(SuggestionValidator.stripFences(raw)));
            return suggestions.save(new AiSuggestion(disputeId, result, missing, client.modelName(), canonicalRaw));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }
}
