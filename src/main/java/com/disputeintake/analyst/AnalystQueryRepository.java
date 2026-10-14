package com.disputeintake.analyst;

import com.disputeintake.dispute.Dispute;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

/** Read-only list query for the analyst queue. */
public interface AnalystQueryRepository extends Repository<Dispute, UUID> {

    @Query("""
            select new com.disputeintake.analyst.DisputeRow(d.id, t.merchantName, t.amountCents, d.createdAt, d.status)
            from Dispute d, CardTransaction t
            where t.id = d.transactionId
            order by d.createdAt asc
            """)
    List<DisputeRow> findAllRows();
}
