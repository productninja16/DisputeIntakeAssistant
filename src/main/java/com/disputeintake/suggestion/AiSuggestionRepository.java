package com.disputeintake.suggestion;

import java.util.List;
import java.util.UUID;
import org.springframework.data.repository.Repository;

/** Insert and read only: the table rejects UPDATE and DELETE. */
public interface AiSuggestionRepository extends Repository<AiSuggestion, UUID> {

    AiSuggestion save(AiSuggestion suggestion);

    boolean existsByDisputeId(UUID disputeId);

    List<AiSuggestion> findByDisputeId(UUID disputeId);
}
