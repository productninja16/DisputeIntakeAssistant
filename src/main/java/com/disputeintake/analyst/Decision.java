package com.disputeintake.analyst;

import com.disputeintake.suggestion.Category;
import com.disputeintake.suggestion.ProposedQueue;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** The human decision. Only an analyst action creates one. */
@Entity
@Table(name = "decisions")
public class Decision {

    @Id
    private UUID id;

    @Column(name = "dispute_id", nullable = false, updatable = false)
    private UUID disputeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "decision_type", nullable = false, updatable = false)
    private DecisionType decisionType;

    @Enumerated(EnumType.STRING)
    @Column(name = "final_category", nullable = false, updatable = false)
    private Category finalCategory;

    @Enumerated(EnumType.STRING)
    @Column(name = "final_queue", nullable = false, updatable = false)
    private ProposedQueue finalQueue;

    @Column(name = "decided_by", nullable = false, updatable = false)
    private String decidedBy;

    @Column(updatable = false)
    private String note;

    @Column(name = "decided_at", nullable = false, insertable = false, updatable = false)
    private Instant decidedAt;

    protected Decision() {
    }

    public Decision(UUID disputeId, DecisionType type, Category category, ProposedQueue queue, String decidedBy,
            String note) {
        this.id = UUID.randomUUID();
        this.disputeId = disputeId;
        this.decisionType = type;
        this.finalCategory = category;
        this.finalQueue = queue;
        this.decidedBy = decidedBy;
        this.note = note;
    }

    public UUID getId() {
        return id;
    }

    public UUID getDisputeId() {
        return disputeId;
    }

    public DecisionType getDecisionType() {
        return decisionType;
    }

    public Category getFinalCategory() {
        return finalCategory;
    }

    public ProposedQueue getFinalQueue() {
        return finalQueue;
    }

    public String getDecidedBy() {
        return decidedBy;
    }

    public String getNote() {
        return note;
    }

    public Instant getDecidedAt() {
        return decidedAt;
    }
}
