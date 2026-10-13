package com.disputeintake.customer;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CardTransactionRepository extends JpaRepository<CardTransaction, UUID> {

    List<CardTransaction> findByCustomerIdOrderByPostedOnDesc(UUID customerId);

    Optional<CardTransaction> findByIdAndCustomerId(UUID id, UUID customerId);
}
