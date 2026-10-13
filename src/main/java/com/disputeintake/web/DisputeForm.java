package com.disputeintake.web;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/** Intake form. The customer is deliberately not a field; it comes from the session. */
public class DisputeForm {

    @NotNull(message = "Please choose the transaction you are disputing.")
    private UUID transactionId;

    @Size(min = 10, max = 4000, message = "Please describe the problem in 10 to 4000 characters.")
    private String description;

    public UUID getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(UUID transactionId) {
        this.transactionId = transactionId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
