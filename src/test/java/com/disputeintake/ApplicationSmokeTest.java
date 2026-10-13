package com.disputeintake;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers
class ApplicationSmokeTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void migrationsCreateTheCoreTables() {
        List<String> tables = jdbc.queryForList(
                "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public'", String.class);
        assertThat(tables).contains("disputes", "ai_suggestions", "decisions");
    }

    @Test
    void aiSuggestionsAreInsertOnly() {
        jdbc.update("INSERT INTO customers (id, customer_ref, display_name) VALUES ('00000000-0000-0000-0000-000000000001', 'SMOKE-1', 'Smoke Test')");
        jdbc.update("INSERT INTO transactions (id, customer_id, merchant_name, amount_cents, posted_on, card_last4) VALUES ('00000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000001', 'Smoke Store', 1000, current_date, '1234')");
        jdbc.update("INSERT INTO disputes (id, customer_id, transaction_id, description) VALUES (gen_random_uuid(), '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000002', 'Synthetic: I was charged twice at the same store.')");
        String disputeId = jdbc.queryForObject("SELECT id::text FROM disputes LIMIT 1", String.class);
        jdbc.update("""
                INSERT INTO ai_suggestions (id, dispute_id, category, reasoning, proposed_queue, model, raw_json)
                VALUES (gen_random_uuid(), ?::uuid, 'DUPLICATE_CHARGE', 'Charged twice.', 'BILLING_ERRORS', 'test', '{}')
                """, disputeId);
        org.junit.jupiter.api.Assertions.assertThrows(Exception.class,
                () -> jdbc.update("UPDATE ai_suggestions SET reasoning = 'edited'"));
    }
}
