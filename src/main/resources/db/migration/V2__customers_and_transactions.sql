-- Synthetic customers and card transactions. A dispute is always about one transaction
-- and the customer comes from the session, never from form input.

CREATE TABLE customers (
    id           UUID PRIMARY KEY,
    customer_ref TEXT        NOT NULL UNIQUE,
    display_name TEXT        NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE transactions (
    id            UUID PRIMARY KEY,
    customer_id   UUID        NOT NULL REFERENCES customers (id),
    merchant_name TEXT        NOT NULL,
    amount_cents  BIGINT      NOT NULL CHECK (amount_cents > 0),
    posted_on     DATE        NOT NULL,
    card_last4    TEXT        NOT NULL CHECK (card_last4 ~ '^[0-9]{4}$'),
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (id, customer_id)
);
CREATE INDEX transactions_customer_idx ON transactions (customer_id, posted_on DESC);

ALTER TABLE disputes
    DROP COLUMN customer_ref,
    ADD COLUMN customer_id    UUID NOT NULL REFERENCES customers (id),
    ADD COLUMN transaction_id UUID NOT NULL,
    -- A dispute can only point at a transaction owned by the same customer.
    ADD CONSTRAINT disputes_transaction_owner_fk
        FOREIGN KEY (transaction_id, customer_id) REFERENCES transactions (id, customer_id);

-- One open dispute per transaction. "Open" = awaiting analyst decision.
CREATE UNIQUE INDEX one_open_dispute_per_transaction
    ON disputes (transaction_id) WHERE status = 'PENDING_REVIEW';
CREATE INDEX disputes_customer_idx ON disputes (customer_id, created_at DESC);
