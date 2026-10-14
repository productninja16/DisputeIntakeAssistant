package com.disputeintake.suggestion;

import com.disputeintake.customer.CardTransaction;
import com.disputeintake.customer.CardTransactionRepository;
import com.disputeintake.dispute.Dispute;
import com.disputeintake.dispute.DisputeRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Gets a suggestion from the {@link SuggestionEngine} and stores it. Never changes the dispute and
 * never creates a decision. If the engine fails, nothing is stored and the case simply reaches the
 * analyst with no suggestion.
 */
@Service
public class SuggestionService {

    private static final Logger log = LoggerFactory.getLogger(SuggestionService.class);

    private final DisputeRepository disputes;
    private final CardTransactionRepository transactions;
    private final AiSuggestionRepository suggestions;
    private final SuggestionEngine engine;
    private final ObjectMapper mapper;

    public SuggestionService(DisputeRepository disputes, CardTransactionRepository transactions,
            AiSuggestionRepository suggestions, SuggestionEngine engine, ObjectMapper mapper) {
        this.disputes = disputes;
        this.transactions = transactions;
        this.suggestions = suggestions;
        this.engine = engine;
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
        SuggestionEngine.Outcome outcome = engine.run(new SuggestionContext(
                dispute.getDescription(), tx.getMerchantName(), tx.getAmountCents(), tx.getPostedOn()));
        if (!outcome.succeeded()) {
            log.warn("No suggestion for dispute {} after {} attempts; analyst reviews unaided", disputeId,
                    outcome.attempts());
            return Optional.empty();
        }
        return Optional.of(store(disputeId, outcome));
    }

    private AiSuggestion store(UUID disputeId, SuggestionEngine.Outcome outcome) {
        try {
            String missing = mapper.writeValueAsString(outcome.result().missingInfo());
            String canonicalRaw = mapper
                    .writeValueAsString(mapper.readTree(SuggestionValidator.stripFences(outcome.rawJson())));
            return suggestions.save(
                    new AiSuggestion(disputeId, outcome.result(), missing, engine.modelName(), canonicalRaw));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }
}
