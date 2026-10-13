package com.disputeintake.customer;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "customers")
public class Customer {

    @Id
    private UUID id;

    @Column(name = "customer_ref", nullable = false, unique = true)
    private String customerRef;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    protected Customer() {
    }

    public Customer(String customerRef, String displayName) {
        this.id = UUID.randomUUID();
        this.customerRef = customerRef;
        this.displayName = displayName;
    }

    public UUID getId() {
        return id;
    }

    public String getCustomerRef() {
        return customerRef;
    }

    public String getDisplayName() {
        return displayName;
    }
}
