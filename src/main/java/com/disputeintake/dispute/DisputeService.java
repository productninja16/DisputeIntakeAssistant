package com.disputeintake.dispute;

import com.disputeintake.customer.CardTransaction;
import com.disputeintake.customer.CardTransactionRepository;
import com.disputeintake.suggestion.DisputeSubmitted;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DisputeService {

    private final DisputeRepository disputes;
    private final CardTransactionRepository transactions;

    private final ApplicationEventPublisher events;

    public DisputeService(DisputeRepository disputes, CardTransactionRepository transactions,
            ApplicationEventPublisher events) {
        this.disputes = disputes;
        this.transactions = transactions;
        this.events = events;
    }

    /**
     * Records a new dispute as PENDING_REVIEW. No decision is made; the AI suggestion is requested asynchronously after commit.
     * Card-number-like sequences are masked before the text is stored.
     */
    @Transactional
    public Dispute submit(UUID customerId, UUID transactionId, String description) {
        CardTransaction transaction = transactions.findByIdAndCustomerId(transactionId, customerId)
                .orElseThrow(TransactionNotFoundException::new);
        if (disputes.existsByTransactionIdAndStatus(transaction.getId(), DisputeStatus.PENDING_REVIEW)) {
            throw new DuplicateOpenDisputeException();
        }
        Dispute dispute = new Dispute(customerId, transaction.getId(), CardNumberMasker.mask(description.strip()));
        try {
            Dispute saved = disputes.saveAndFlush(dispute);
            // Handled asynchronously after commit; the suggestion never blocks or alters intake.
            events.publishEvent(new DisputeSubmitted(saved.getId()));
            return saved;
        } catch (DataIntegrityViolationException e) {
            // Lost a race with a concurrent submit; the partial unique index is the final guard.
            throw new DuplicateOpenDisputeException();
        }
    }

    @Transactional(readOnly = true)
    public List<TransactionOption> transactionOptions(UUID customerId) {
        Set<UUID> open = new HashSet<>(disputes.findOpenTransactionIds(customerId));
        return transactions.findByCustomerIdOrderByPostedOnDesc(customerId).stream()
                .map(t -> new TransactionOption(t.getId(), t.getMerchantName(), t.getAmountCents(),
                        t.getPostedOn(), t.getCardLast4(), open.contains(t.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DisputeSummary> listForCustomer(UUID customerId) {
        return disputes.findSummaries(customerId);
    }

    @Transactional(readOnly = true)
    public Dispute getForCustomer(UUID disputeId, UUID customerId) {
        return disputes.findByIdAndCustomerId(disputeId, customerId)
                .orElseThrow(TransactionNotFoundException::new);
    }
}
