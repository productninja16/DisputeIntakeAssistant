package com.disputeintake.analyst;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DecisionRepository extends JpaRepository<Decision, UUID> {

    Optional<Decision> findByDisputeId(UUID disputeId);
}
