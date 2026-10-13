package com.disputeintake.analyst;

import com.disputeintake.dispute.Dispute;
import com.disputeintake.dispute.DisputeRepository;
import com.disputeintake.dispute.DisputeStatus;
import com.disputeintake.suggestion.AiSuggestion;
import com.disputeintake.suggestion.AiSuggestionRepository;
import com.disputeintake.suggestion.Category;
import com.disputeintake.suggestion.ProposedQueue;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The only place a {@link Decision} is created. Each call records one analyst action and moves
 * the dispute out of PENDING_REVIEW in a single transaction.
 */
@Service
public class DecisionService {

    static final int MAX_NOTE_LENGTH = 1000;

    private final DisputeRepository disputes;
    private final AiSuggestionRepository suggestions;
    private final DecisionRepository decisions;

    public DecisionService(DisputeRepository disputes, AiSuggestionRepository suggestions,
            DecisionRepository decisions) {
        this.disputes = disputes;
        this.suggestions = suggestions;
        this.decisions = decisions;
    }

    /** Accepts exactly what the given suggestion proposed. */
    @Transactional
    public Decision approve(UUID disputeId, UUID suggestionId, String analyst) {
        requireAnalyst(analyst);
        Dispute dispute = lockPending(disputeId);
        AiSuggestion suggestion = suggestions.findById(suggestionId)
                .filter(s -> s.getDisputeId().equals(disputeId))
                .orElseThrow(NoSuggestionException::new);
        return record(dispute, DecisionType.APPROVED, suggestion.getCategory(), suggestion.getProposedQueue(),
                analyst, null);
    }

    /** Records the analyst's own category and queue. A note is required. */
    @Transactional
    public Decision override(UUID disputeId, Category category, ProposedQueue queue, String note, String analyst) {
        requireAnalyst(analyst);
        if (category == null || queue == null) {
            throw new InvalidDecisionException("Choose a category and a queue.");
        }
        String trimmed = note == null ? "" : note.strip();
        if (trimmed.isEmpty()) {
            throw new InvalidDecisionException("A note is required when overriding.");
        }
        if (trimmed.length() > MAX_NOTE_LENGTH) {
            throw new InvalidDecisionException("The note must be at most " + MAX_NOTE_LENGTH + " characters.");
        }
        Dispute dispute = lockPending(disputeId);
        return record(dispute, DecisionType.OVERRIDDEN, category, queue, analyst, trimmed);
    }

    private Decision record(Dispute dispute, DecisionType type, Category category, ProposedQueue queue,
            String analyst, String note) {
        Decision decision = decisions.saveAndFlush(new Decision(dispute.getId(), type, category, queue, analyst, note));
        dispute.markDecided(type == DecisionType.APPROVED ? DisputeStatus.APPROVED : DisputeStatus.OVERRIDDEN);
        return decision;
    }

    /** Row lock so two analysts acting at once cannot both decide the same dispute. */
    private Dispute lockPending(UUID disputeId) {
        Dispute dispute = disputes.findForUpdate(disputeId).orElseThrow(DisputeNotFoundException::new);
        if (dispute.getStatus() != DisputeStatus.PENDING_REVIEW) {
            throw new AlreadyDecidedException();
        }
        return dispute;
    }

    private static void requireAnalyst(String analyst) {
        if (analyst == null || !AnalystDirectory.contains(analyst)) {
            throw new InvalidDecisionException("Unknown analyst.");
        }
    }
}
