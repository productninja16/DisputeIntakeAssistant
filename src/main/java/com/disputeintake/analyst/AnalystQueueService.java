package com.disputeintake.analyst;

import com.disputeintake.suggestion.AiSuggestion;
import com.disputeintake.suggestion.AiSuggestionRepository;
import com.disputeintake.suggestion.ProposedQueue;
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

    public AnalystQueueService(AnalystQueryRepository queries, AiSuggestionRepository suggestions) {
        this.queries = queries;
        this.suggestions = suggestions;
    }

    /**
     * Pending disputes, oldest first.
     *
     * @param filter null/blank for all, {@link #NO_SUGGESTION}, or a {@link ProposedQueue} name
     */
    @Transactional(readOnly = true)
    public List<QueueRow> pending(String filter) {
        List<PendingRow> pending = queries.findPending();
        Map<UUID, AiSuggestion> latest = new HashMap<>();
        if (!pending.isEmpty()) {
            suggestions.findByDisputeIdIn(pending.stream().map(PendingRow::id).toList()).stream()
                    .sorted(Comparator.comparing(AiSuggestion::getCreatedAt))
                    .forEach(s -> latest.put(s.getDisputeId(), s)); // later ones overwrite earlier
        }
        return pending.stream()
                .map(p -> {
                    AiSuggestion s = latest.get(p.id());
                    return new QueueRow(p, s == null ? null : s.getCategory(), s == null ? null : s.getProposedQueue());
                })
                .filter(r -> matches(r, filter))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<HistoryRow> history() {
        return queries.findHistory();
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
