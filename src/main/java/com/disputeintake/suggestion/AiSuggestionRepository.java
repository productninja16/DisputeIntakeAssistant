package com.disputeintake.suggestion;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.repository.Repository;

/** Insert and read only: the table rejects UPDATE and DELETE. */
public interface AiSuggestionRepository extends Repository<AiSuggestion, UUID> {

    AiSuggestion save(AiSuggestion suggestion);

    boolean existsByDisputeId(UUID disputeId);

    List<AiSuggestion> findByDisputeId(UUID disputeId);

    Optional<AiSuggestion> findById(UUID id);

    Optional<AiSuggestion> findFirstByDisputeIdOrderByCreatedAtDesc(UUID disputeId);

    List<AiSuggestion> findByDisputeIdIn(Collection<UUID> disputeIds);
}
