-- Synthetic data only. Categories and queues are simple internal values, not card network reason codes.

CREATE TABLE disputes (
    id           UUID PRIMARY KEY,
    customer_ref TEXT        NOT NULL,
    description  TEXT        NOT NULL CHECK (length(description) BETWEEN 10 AND 4000),
    status       TEXT        NOT NULL DEFAULT 'PENDING_REVIEW'
                 CHECK (status IN ('PENDING_REVIEW', 'APPROVED', 'OVERRIDDEN')),
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- What the LLM suggested. Insert-only: suggestions are evidence, never edited.
CREATE TABLE ai_suggestions (
    id             UUID PRIMARY KEY,
    dispute_id     UUID        NOT NULL REFERENCES disputes (id),
    category       TEXT        NOT NULL CHECK (category IN (
        'UNAUTHORIZED_TRANSACTION', 'DUPLICATE_CHARGE', 'MERCHANDISE_NOT_RECEIVED',
        'INCORRECT_AMOUNT', 'SUBSCRIPTION_CANCELLED', 'OTHER')),
    missing_info   JSONB       NOT NULL DEFAULT '[]',
    reasoning      TEXT        NOT NULL,
    proposed_queue TEXT        NOT NULL CHECK (proposed_queue IN (
        'FRAUD', 'BILLING_ERRORS', 'MERCHANT_DISPUTES', 'GENERAL_REVIEW')),
    model          TEXT        NOT NULL,
    raw_json       JSONB       NOT NULL,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX ai_suggestions_dispute_idx ON ai_suggestions (dispute_id);

CREATE FUNCTION forbid_change() RETURNS trigger AS $$
BEGIN
    RAISE EXCEPTION '% is insert-only', TG_TABLE_NAME;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER ai_suggestions_insert_only
    BEFORE UPDATE OR DELETE ON ai_suggestions
    FOR EACH ROW EXECUTE FUNCTION forbid_change();

-- The human decision. Only an analyst action creates a row here.
CREATE TABLE decisions (
    id             UUID PRIMARY KEY,
    dispute_id     UUID        NOT NULL UNIQUE REFERENCES disputes (id),
    decision_type  TEXT        NOT NULL CHECK (decision_type IN ('APPROVED', 'OVERRIDDEN')),
    final_category TEXT        NOT NULL CHECK (final_category IN (
        'UNAUTHORIZED_TRANSACTION', 'DUPLICATE_CHARGE', 'MERCHANDISE_NOT_RECEIVED',
        'INCORRECT_AMOUNT', 'SUBSCRIPTION_CANCELLED', 'OTHER')),
    final_queue    TEXT        NOT NULL CHECK (final_queue IN (
        'FRAUD', 'BILLING_ERRORS', 'MERCHANT_DISPUTES', 'GENERAL_REVIEW')),
    decided_by     TEXT        NOT NULL,
    note           TEXT,
    decided_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);
