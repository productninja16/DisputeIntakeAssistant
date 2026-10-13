package com.disputeintake.suggestion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** Runs the suggestion off the request thread, only after the dispute is committed. */
@Component
public class SuggestionListener {

    private static final Logger log = LoggerFactory.getLogger(SuggestionListener.class);

    private final SuggestionService service;

    public SuggestionListener(SuggestionService service) {
        this.service = service;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onDisputeSubmitted(DisputeSubmitted event) {
        try {
            service.generateFor(event.disputeId());
        } catch (RuntimeException e) {
            // Intake must never be affected by suggestion problems.
            log.error("Suggestion failed for dispute {}", event.disputeId(), e);
        }
    }
}
