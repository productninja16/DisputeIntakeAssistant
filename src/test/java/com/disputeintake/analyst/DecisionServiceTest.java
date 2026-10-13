package com.disputeintake.analyst;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.disputeintake.suggestion.Category;
import com.disputeintake.suggestion.ProposedQueue;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(properties = "suggestion.client=stub")
@Testcontainers
class DecisionServiceTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    DecisionService service;
    @Autowired
    JdbcTemplate jdbc;

    private UUID newDispute() {
        UUID customer = UUID.randomUUID();
        UUID tx = UUID.randomUUID();
        UUID dispute = UUID.randomUUID();
        jdbc.update("INSERT INTO customers (id, customer_ref, display_name) VALUES (?, ?, 'T')", customer,
                "T-" + customer);
        jdbc.update("""
                INSERT INTO transactions (id, customer_id, merchant_name, amount_cents, posted_on, card_last4)
                VALUES (?, ?, 'Shop', 1234, current_date, '1111')""", tx, customer);
        jdbc.update("INSERT INTO disputes (id, customer_id, transaction_id, description) VALUES (?, ?, ?, ?)",
                dispute, customer, tx, "I was charged twice at the same store.");
        return dispute;
    }

    private UUID suggest(UUID dispute, String category, String queue) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO ai_suggestions (id, dispute_id, category, reasoning, proposed_queue, model, raw_json)
                VALUES (?, ?, ?, 'r', ?, 'test', '{}')""", id, dispute, category, queue);
        return id;
    }

    private String status(UUID dispute) {
        return jdbc.queryForObject("SELECT status FROM disputes WHERE id = ?", String.class, dispute);
    }

    private int decisionCount(UUID dispute) {
        return jdbc.queryForObject("SELECT count(*) FROM decisions WHERE dispute_id = ?", Integer.class, dispute);
    }

    @Test
    void approveCopiesTheSuggestionAndMarksApproved() {
        UUID d = newDispute();
        UUID s = suggest(d, "DUPLICATE_CHARGE", "BILLING_ERRORS");

        Decision decision = service.approve(d, s, "analyst.a");

        assertThat(decision.getDecisionType()).isEqualTo(DecisionType.APPROVED);
        assertThat(decision.getFinalCategory()).isEqualTo(Category.DUPLICATE_CHARGE);
        assertThat(decision.getFinalQueue()).isEqualTo(ProposedQueue.BILLING_ERRORS);
        assertThat(decision.getDecidedBy()).isEqualTo("analyst.a");
        assertThat(status(d)).isEqualTo("APPROVED");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM ai_suggestions WHERE dispute_id = ?", Integer.class, d))
                .isEqualTo(1);
    }

    @Test
    void overrideStoresTheAnalystsValuesAndNote() {
        UUID d = newDispute();
        suggest(d, "DUPLICATE_CHARGE", "BILLING_ERRORS");

        Decision decision = service.override(d, Category.UNAUTHORIZED_TRANSACTION, ProposedQueue.FRAUD,
                "  Customer does not recognise either charge.  ", "analyst.b");

        assertThat(decision.getDecisionType()).isEqualTo(DecisionType.OVERRIDDEN);
        assertThat(decision.getFinalCategory()).isEqualTo(Category.UNAUTHORIZED_TRANSACTION);
        assertThat(decision.getFinalQueue()).isEqualTo(ProposedQueue.FRAUD);
        assertThat(decision.getNote()).isEqualTo("Customer does not recognise either charge.");
        assertThat(status(d)).isEqualTo("OVERRIDDEN");
    }

    @Test
    void overrideWorksWhenThereIsNoSuggestion() {
        UUID d = newDispute();

        service.override(d, Category.OTHER, ProposedQueue.GENERAL_REVIEW, "Unclear description.", "analyst.a");

        assertThat(status(d)).isEqualTo("OVERRIDDEN");
    }

    @Test
    void overrideRequiresANote() {
        UUID d = newDispute();

        assertThatThrownBy(() -> service.override(d, Category.OTHER, ProposedQueue.GENERAL_REVIEW, "   ", "analyst.a"))
                .isInstanceOf(InvalidDecisionException.class);
        assertThatThrownBy(() -> service.override(d, Category.OTHER, ProposedQueue.GENERAL_REVIEW, null, "analyst.a"))
                .isInstanceOf(InvalidDecisionException.class);

        assertThat(decisionCount(d)).isZero();
        assertThat(status(d)).isEqualTo("PENDING_REVIEW");
    }

    @Test
    void secondDecisionIsRejected() {
        UUID d = newDispute();
        UUID s = suggest(d, "OTHER", "GENERAL_REVIEW");
        service.approve(d, s, "analyst.a");

        assertThatThrownBy(() -> service.approve(d, s, "analyst.b")).isInstanceOf(AlreadyDecidedException.class);
        assertThatThrownBy(() -> service.override(d, Category.OTHER, ProposedQueue.FRAUD, "x", "analyst.b"))
                .isInstanceOf(AlreadyDecidedException.class);

        assertThat(decisionCount(d)).isEqualTo(1);
    }

    @Test
    void approveFailsWithoutASuggestionForThatDispute() {
        UUID d = newDispute();
        UUID other = newDispute();
        UUID othersSuggestion = suggest(other, "OTHER", "GENERAL_REVIEW");

        assertThatThrownBy(() -> service.approve(d, UUID.randomUUID(), "analyst.a"))
                .isInstanceOf(NoSuggestionException.class);
        assertThatThrownBy(() -> service.approve(d, othersSuggestion, "analyst.a"))
                .isInstanceOf(NoSuggestionException.class);

        assertThat(decisionCount(d)).isZero();
        assertThat(status(d)).isEqualTo("PENDING_REVIEW");
    }

    @Test
    void unknownAnalystAndUnknownDisputeAreRejected() {
        UUID d = newDispute();
        UUID s = suggest(d, "OTHER", "GENERAL_REVIEW");

        assertThatThrownBy(() -> service.approve(d, s, "mallory")).isInstanceOf(InvalidDecisionException.class);
        assertThatThrownBy(() -> service.approve(UUID.randomUUID(), s, "analyst.a"))
                .isInstanceOf(DisputeNotFoundException.class);
    }
}
