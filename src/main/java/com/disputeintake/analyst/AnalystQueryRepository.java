package com.disputeintake.analyst;

import com.disputeintake.dispute.Dispute;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

/** Read-only list queries for the analyst screens. */
public interface AnalystQueryRepository extends Repository<Dispute, UUID> {

    @Query("""
            select new com.disputeintake.analyst.PendingRow(d.id, t.merchantName, t.amountCents, d.createdAt)
            from Dispute d, CardTransaction t
            where t.id = d.transactionId and d.status = 'PENDING_REVIEW'
            order by d.createdAt asc
            """)
    List<PendingRow> findPending();

    @Query("""
            select new com.disputeintake.analyst.HistoryRow(d.id, t.merchantName, t.amountCents,
                   x.decisionType, x.finalCategory, x.finalQueue, x.decidedBy, x.decidedAt)
            from Dispute d, CardTransaction t, Decision x
            where t.id = d.transactionId and x.disputeId = d.id
            order by x.decidedAt desc
            """)
    List<HistoryRow> findHistory();
}
