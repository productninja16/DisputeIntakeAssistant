package com.disputeintake.suggestion;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers
class SuggestionServiceTest {

    static final String VALID = """
            {"category":"DUPLICATE_CHARGE","missing_info":["Receipt"],
             "reasoning":"Charged twice.","proposed_queue":"BILLING_ERRORS"}""";
    static final String INVALID = "{\"category\":\"NOPE\"}";

    /** Replays scripted answers; a null entry makes the call throw. */
    static class ScriptedClient implements SuggestionClient {
        final Queue<String> answers = new LinkedList<>();
        final List<SuggestionContext> contexts = new ArrayList<>();
        final List<List<String>> errorsSeen = new ArrayList<>();

        @Override
        public String suggest(SuggestionContext context, List<String> previousErrors) {
            contexts.add(context);
            errorsSeen.add(previousErrors);
            String next = answers.poll();
            if (next == null) {
                throw new IllegalStateException("scripted failure");
            }
            return next;
        }

        @Override
        public String modelName() {
            return "scripted";
        }
    }

    @TestConfiguration
    static class Config {
        @Bean
        @Primary
        ScriptedClient scriptedClient() {
            return new ScriptedClient();
        }
    }

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    SuggestionService service;
    @Autowired
    ScriptedClient client;
    @Autowired
    JdbcTemplate jdbc;

    @BeforeEach
    void reset() {
        client.answers.clear();
        client.contexts.clear();
        client.errorsSeen.clear();
    }

    private UUID newDispute(String description) {
        UUID customer = UUID.randomUUID();
        UUID tx = UUID.randomUUID();
        UUID dispute = UUID.randomUUID();
        jdbc.update("INSERT INTO customers (id, customer_ref, display_name) VALUES (?, ?, 'T')", customer,
                "T-" + customer);
        jdbc.update("""
                INSERT INTO transactions (id, customer_id, merchant_name, amount_cents, posted_on, card_last4)
                VALUES (?, ?, 'Shop', 1234, current_date, '1111')""", tx, customer);
        jdbc.update("INSERT INTO disputes (id, customer_id, transaction_id, description) VALUES (?, ?, ?, ?)",
                dispute, customer, tx, description);
        return dispute;
    }

    private int suggestionCount(UUID dispute) {
        return jdbc.queryForObject("SELECT count(*) FROM ai_suggestions WHERE dispute_id = ?", Integer.class, dispute);
    }

    @Test
    void validAnswerIsStoredWithoutTouchingTheDispute() {
        UUID d = newDispute("I was charged twice at the same store.");
        client.answers.add(VALID);

        assertThat(service.generateFor(d)).isPresent();

        assertThat(suggestionCount(d)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT model FROM ai_suggestions WHERE dispute_id = ?", String.class, d))
                .isEqualTo("scripted");
        assertThat(jdbc.queryForObject("SELECT status FROM disputes WHERE id = ?", String.class, d))
                .isEqualTo("PENDING_REVIEW");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM decisions WHERE dispute_id = ?", Integer.class, d))
                .isZero();
    }

    @Test
    void invalidThenValidRetriesOnceWithErrorsAndStoresOne() {
        UUID d = newDispute("I was charged twice at the same store.");
        client.answers.add(INVALID);
        client.answers.add(VALID);

        assertThat(service.generateFor(d)).isPresent();

        assertThat(suggestionCount(d)).isEqualTo(1);
        assertThat(client.contexts).hasSize(2);
        assertThat(client.errorsSeen.get(0)).isEmpty();
        assertThat(client.errorsSeen.get(1)).isNotEmpty();
    }

    @Test
    void invalidTwiceStoresNothing() {
        UUID d = newDispute("I was charged twice at the same store.");
        client.answers.add(INVALID);
        client.answers.add(INVALID);

        assertThat(service.generateFor(d)).isEmpty();

        assertThat(suggestionCount(d)).isZero();
        assertThat(client.contexts).hasSize(2);
    }

    @Test
    void clientFailuresTwiceStoreNothingAndDoNotThrow() {
        UUID d = newDispute("I was charged twice at the same store.");

        assertThat(service.generateFor(d)).isEmpty();

        assertThat(suggestionCount(d)).isZero();
        assertThat(client.contexts).hasSize(2);
    }

    @Test
    void cardNumbersAreMaskedBeforeReachingTheClient() {
        UUID d = newDispute("Card 4111 1111 1111 1111 was charged twice.");
        client.answers.add(VALID);

        service.generateFor(d);

        assertThat(client.contexts.get(0).description()).doesNotContain("4111 1111 1111 1111");
    }

    @Test
    void existingSuggestionIsNotDuplicated() {
        UUID d = newDispute("I was charged twice at the same store.");
        client.answers.add(VALID);
        client.answers.add(VALID);

        service.generateFor(d);
        assertThat(service.generateFor(d)).isEmpty();

        assertThat(suggestionCount(d)).isEqualTo(1);
    }
}
