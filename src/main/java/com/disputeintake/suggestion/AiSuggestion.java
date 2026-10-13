package com.disputeintake.suggestion;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Insert-only evidence of what the model suggested. Never edited, never applied. */
@Entity
@Table(name = "ai_suggestions")
public class AiSuggestion {

    @Id
    private UUID id;

    @Column(name = "dispute_id", nullable = false, updatable = false)
    private UUID disputeId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private Category category;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "missing_info", nullable = false, updatable = false)
    private String missingInfoJson;

    @Column(nullable = false, updatable = false)
    private String reasoning;

    @Enumerated(EnumType.STRING)
    @Column(name = "proposed_queue", nullable = false, updatable = false)
    private ProposedQueue proposedQueue;

    @Column(nullable = false, updatable = false)
    private String model;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_json", nullable = false, updatable = false)
    private String rawJson;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    protected AiSuggestion() {
    }

    public AiSuggestion(UUID disputeId, SuggestionResult result, String missingInfoJson, String model,
            String rawJson) {
        this.id = UUID.randomUUID();
        this.disputeId = disputeId;
        this.category = result.category();
        this.missingInfoJson = missingInfoJson;
        this.reasoning = result.reasoning();
        this.proposedQueue = result.proposedQueue();
        this.model = model;
        this.rawJson = rawJson;
    }

    public UUID getId() {
        return id;
    }

    public UUID getDisputeId() {
        return disputeId;
    }

    public Category getCategory() {
        return category;
    }

    public String getMissingInfoJson() {
        return missingInfoJson;
    }

    public String getReasoning() {
        return reasoning;
    }

    public ProposedQueue getProposedQueue() {
        return proposedQueue;
    }

    public String getModel() {
        return model;
    }

    public String getRawJson() {
        return rawJson;
    }
}
