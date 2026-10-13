package com.disputeintake.customer;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.UUID;

/** A synthetic card transaction. Money is integer cents. */
@Entity
@Table(name = "transactions")
public class CardTransaction {

    @Id
    private UUID id;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "merchant_name", nullable = false)
    private String merchantName;

    @Column(name = "amount_cents", nullable = false)
    private long amountCents;

    @Column(name = "posted_on", nullable = false)
    private LocalDate postedOn;

    @Column(name = "card_last4", nullable = false)
    private String cardLast4;

    protected CardTransaction() {
    }

    public CardTransaction(UUID customerId, String merchantName, long amountCents, LocalDate postedOn,
            String cardLast4) {
        this.id = UUID.randomUUID();
        this.customerId = customerId;
        this.merchantName = merchantName;
        this.amountCents = amountCents;
        this.postedOn = postedOn;
        this.cardLast4 = cardLast4;
    }

    public UUID getId() {
        return id;
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public String getMerchantName() {
        return merchantName;
    }

    public long getAmountCents() {
        return amountCents;
    }

    public LocalDate getPostedOn() {
        return postedOn;
    }

    public String getCardLast4() {
        return cardLast4;
    }
}
