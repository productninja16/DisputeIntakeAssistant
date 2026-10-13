package com.disputeintake.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(properties = "suggestion.client=stub")
@AutoConfigureMockMvc
@Testcontainers
class CustomerStatusTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    MockMvc mvc;
    @Autowired
    JdbcTemplate jdbc;

    @Test
    void customerSeesCoarseStatusAndNeverAiOrOverrideDetails() throws Exception {
        UUID customer = UUID.randomUUID();
        UUID txPending = UUID.randomUUID();
        UUID txDone = UUID.randomUUID();
        jdbc.update("INSERT INTO customers (id, customer_ref, display_name) VALUES (?, 'S-1', 'S')", customer);
        for (Object[] t : new Object[][] {{txPending, "Pending Shop"}, {txDone, "Done Shop"}}) {
            jdbc.update("""
                    INSERT INTO transactions (id, customer_id, merchant_name, amount_cents, posted_on, card_last4)
                    VALUES (?, ?, ?, 500, current_date, '1111')""", t[0], customer, t[1]);
        }
        jdbc.update("INSERT INTO disputes (id, customer_id, transaction_id, description) VALUES (gen_random_uuid(), ?, ?, 'Charged twice at the shop')",
                customer, txPending);
        UUID done = UUID.randomUUID();
        jdbc.update("INSERT INTO disputes (id, customer_id, transaction_id, description) VALUES (?, ?, ?, 'Never arrived at all')",
                done, customer, txDone);
        jdbc.update("""
                INSERT INTO ai_suggestions (id, dispute_id, category, reasoning, proposed_queue, model, raw_json)
                VALUES (gen_random_uuid(), ?, 'OTHER', 'secret ai reasoning', 'GENERAL_REVIEW', 'test', '{}')""", done);
        jdbc.update("""
                INSERT INTO decisions (id, dispute_id, decision_type, final_category, final_queue, decided_by, note)
                VALUES (gen_random_uuid(), ?, 'OVERRIDDEN', 'MERCHANDISE_NOT_RECEIVED', 'MERCHANT_DISPUTES', 'analyst.a', 'internal note')""", done);
        jdbc.update("UPDATE disputes SET status = 'OVERRIDDEN' WHERE id = ?", done);

        MockHttpSession session = new MockHttpSession();
        session.setAttribute("customerId", customer);
        String html = mvc.perform(get("/disputes").session(session)).andReturn().getResponse().getContentAsString();

        assertThat(html).contains("In review", "Reviewed");
        assertThat(html).doesNotContain("Overridden", "OVERRIDDEN", "secret ai reasoning", "internal note",
                "analyst.a", "MERCHANT_DISPUTES");
    }
}
