package com.disputeintake.dispute;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "disputes")
public class Dispute {

    @Id
    private UUID id;

    @Column(name = "customer_id", nullable = false, updatable = false)
    private UUID customerId;

    @Column(name = "transaction_id", nullable = false, updatable = false)
    private UUID transactionId;

    /** Customer text with card-number-like sequences already masked. */
    @Column(nullable = false)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DisputeStatus status;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    protected Dispute() {
    }

    public Dispute(UUID customerId, UUID transactionId, String description) {
        this.id = UUID.randomUUID();
        this.customerId = customerId;
        this.transactionId = transactionId;
        this.description = description;
        this.status = DisputeStatus.PENDING_REVIEW;
    }

    public UUID getId() {
        return id;
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public UUID getTransactionId() {
        return transactionId;
    }

    public String getDescription() {
        return description;
    }

    public DisputeStatus getStatus() {
        return status;
    }

    /** Called only by the analyst decision flow. */
    public void markDecided(DisputeStatus decided) {
        if (status != DisputeStatus.PENDING_REVIEW || decided == DisputeStatus.PENDING_REVIEW) {
            throw new IllegalStateException("Dispute is not awaiting a decision");
        }
        this.status = decided;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    /** Short, customer-friendly case reference, e.g. D-1A2B3C4D. */
    public String getCaseRef() {
        return caseRef(id);
    }

    public static String caseRef(UUID id) {
        return "D-" + id.toString().substring(0, 8).toUpperCase();
    }
}
