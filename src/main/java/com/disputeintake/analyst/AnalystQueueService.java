package com.disputeintake.analyst;

import com.disputeintake.customer.CardTransaction;
import com.disputeintake.customer.CardTransactionRepository;
import com.disputeintake.dispute.Dispute;
import com.disputeintake.dispute.DisputeRepository;
import com.disputeintake.dispute.DisputeStatus;
import com.disputeintake.suggestion.AiSuggestion;
import com.disputeintake.suggestion.AiSuggestionRepository;
import com.disputeintake.suggestion.ProposedQueue;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnalystQueueService {

    /** Filter value for "the AI produced no suggestion". */
    public static final String NO_SUGGESTION = "NONE";

    private final AnalystQueryRepository queries;
    private final AiSuggestionRepository suggestions;
    private final DisputeRepository disputes;
    private final CardTransactionRepository transactions;
    private final DecisionRepository decisions;
    private final ObjectMapper mapper;

    public AnalystQueueService(AnalystQueryRepository queries, AiSuggestionRepository suggestions,
            DisputeRepository disputes, CardTransactionRepository transactions, DecisionRepository decisions,
            ObjectMapper mapper) {
        this.queries = queries;
        this.suggestions = suggestions;
        this.disputes = disputes;
        this.transactions = transactions;
        this.decisions = decisions;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public CaseView getCase(UUID disputeId) {
        Dispute d = disputes.findById(disputeId).orElseThrow(DisputeNotFoundException::new);
        CardTransaction t = transactions.findById(d.getTransactionId()).orElseThrow(DisputeNotFoundException::new);
        SuggestionView suggestion = suggestions.findFirstByDisputeIdOrderByCreatedAtDesc(disputeId)
                .map(this::toView).orElse(null);
        Decision decision = decisions.findByDisputeId(disputeId).orElse(null);
        return new CaseView(d.getId(), d.getDescription(), d.getStatus(), t.getMerchantName(), t.getAmountCents(),
                t.getPostedOn(), t.getCardLast4(), d.getCreatedAt(), suggestion, decision);
    }

    private SuggestionView toView(AiSuggestion s) {
        List<String> missing;
        try {
            missing = mapper.readValue(s.getMissingInfoJson(), new TypeReference<List<String>>() {
            });
        } catch (JsonProcessingException e) {
            missing = List.of();
        }
        return new SuggestionView(s.getId(), s.getCategory(), s.getProposedQueue(), s.getConfidence(), missing,
                s.getReasoning(), s.getModel());
    }

    /** Status filter value meaning "every status". */
    public static final String ALL_STATUSES = "ALL";

    /**
     * Disputes for the queue table. In-review disputes sort oldest first (work the longest-waiting
     * case first); any other view sorts newest first.
     *
     * @param statusFilter a {@link DisputeStatus} name or {@link #ALL_STATUSES}; null, blank or
     *                     unrecognised means in review
     * @param queueFilter  null/blank for all, {@link #NO_SUGGESTION}, or a {@link ProposedQueue} name
     */
    @Transactional(readOnly = true)
    public List<QueueRow> list(String statusFilter, String queueFilter) {
        String status = normalizeStatus(statusFilter);
        List<DisputeRow> rows = queries.findAllRows().stream()
                .filter(r -> ALL_STATUSES.equals(status) || r.status().name().equals(status))
                .collect(java.util.stream.Collectors.toCollection(java.util.ArrayList::new));
        if (!DisputeStatus.PENDING_REVIEW.name().equals(status)) {
            java.util.Collections.reverse(rows); // repository returns oldest first
        }
        Map<UUID, AiSuggestion> latest = new HashMap<>();
        if (!rows.isEmpty()) {
            suggestions.findByDisputeIdIn(rows.stream().map(DisputeRow::id).toList()).stream()
                    .sorted(Comparator.comparing(AiSuggestion::getCreatedAt))
                    .forEach(s -> latest.put(s.getDisputeId(), s)); // later ones overwrite earlier
        }
        return rows.stream()
                .map(r -> {
                    AiSuggestion s = latest.get(r.id());
                    return new QueueRow(r, s == null ? null : s.getCategory(),
                            s == null ? null : s.getProposedQueue(), s == null ? null : s.getConfidence());
                })
                .filter(r -> matches(r, queueFilter))
                .toList();
    }

    /** The status filter actually applied, so the page can show it selected. */
    public static String normalizeStatus(String statusFilter) {
        if (ALL_STATUSES.equals(statusFilter)) {
            return ALL_STATUSES;
        }
        for (DisputeStatus s : DisputeStatus.values()) {
            if (s.name().equals(statusFilter)) {
                return s.name();
            }
        }
        return DisputeStatus.PENDING_REVIEW.name();
    }

    private static boolean matches(QueueRow row, String filter) {
        if (filter == null || filter.isBlank()) {
            return true;
        }
        if (NO_SUGGESTION.equals(filter)) {
            return !row.hasSuggestion();
        }
        return row.suggestedQueue() != null && row.suggestedQueue().name().equals(filter);
    }
}
