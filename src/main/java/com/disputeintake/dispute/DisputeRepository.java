package com.disputeintake.dispute;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface DisputeRepository extends JpaRepository<Dispute, UUID> {

    Optional<Dispute> findByIdAndCustomerId(UUID id, UUID customerId);

    boolean existsByTransactionIdAndStatus(UUID transactionId, DisputeStatus status);

    @Query("select d.transactionId from Dispute d where d.customerId = :customerId and d.status = 'PENDING_REVIEW'")
    List<UUID> findOpenTransactionIds(UUID customerId);

    @Query("""
            select new com.disputeintake.dispute.DisputeSummary(d.id, t.merchantName, t.amountCents, t.postedOn, d.createdAt)
            from Dispute d, CardTransaction t
            where t.id = d.transactionId and d.customerId = :customerId
            order by d.createdAt desc
            """)
    List<DisputeSummary> findSummaries(UUID customerId);
}
