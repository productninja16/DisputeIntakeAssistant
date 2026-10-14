package com.disputeintake.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
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
class AnalystDecisionFlowTest {

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

    MockHttpSession session;

    @BeforeEach
    void signIn() throws Exception {
        session = new MockHttpSession();
        mvc.perform(post("/analyst/sign-in").param("analyst", "analyst.b").session(session));
    }

    private UUID newDispute(boolean withSuggestion, UUID[] suggestionOut) {
        UUID customer = UUID.randomUUID();
        UUID tx = UUID.randomUUID();
        UUID dispute = UUID.randomUUID();
        jdbc.update("INSERT INTO customers (id, customer_ref, display_name) VALUES (?, ?, 'T')", customer,
                "F-" + customer);
        jdbc.update("""
                INSERT INTO transactions (id, customer_id, merchant_name, amount_cents, posted_on, card_last4)
                VALUES (?, ?, 'Flow Shop', 999, current_date, '1111')""", tx, customer);
        jdbc.update("INSERT INTO disputes (id, customer_id, transaction_id, description) VALUES (?, ?, ?, 'Charged twice at the shop')",
                dispute, customer, tx);
        if (withSuggestion) {
            UUID s = UUID.randomUUID();
            jdbc.update("""
                    INSERT INTO ai_suggestions (id, dispute_id, category, missing_info, reasoning, proposed_queue, model, raw_json)
                    VALUES (?, ?, 'DUPLICATE_CHARGE', '["Receipt"]', 'Looks like a double charge.', 'BILLING_ERRORS', 'test', '{}')""",
                    s, dispute);
            suggestionOut[0] = s;
        }
        return dispute;
    }

    private String statusOf(UUID d) {
        return jdbc.queryForObject("SELECT status FROM disputes WHERE id = ?", String.class, d);
    }

    @Test
    void casePageShowsDescriptionAndSuggestion() throws Exception {
        UUID d = newDispute(true, new UUID[1]);
        String html = mvc.perform(get("/analyst/disputes/" + d).session(session)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(html).contains("Charged twice at the shop", "Looks like a double charge.", "Receipt",
                "suggestion only, not a decision", "Approve the AI suggestion");
    }

    @Test
    void casePageWithoutSuggestionOffersNoApprove() throws Exception {
        UUID d = newDispute(false, new UUID[1]);
        String html = mvc.perform(get("/analyst/disputes/" + d).session(session)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(html).contains("No suggestion is available").doesNotContain("Approve the AI suggestion");
    }

    @Test
    void approveRecordsDecisionAttributedToTheSignedInAnalyst() throws Exception {
        UUID[] s = new UUID[1];
        UUID d = newDispute(true, s);

        mvc.perform(post("/analyst/disputes/" + d + "/approve").param("suggestionId", s[0].toString()).session(session))
                .andExpect(redirectedUrl("/analyst/queue"));

        assertThat(statusOf(d)).isEqualTo("APPROVED");
        assertThat(jdbc.queryForObject("SELECT decided_by FROM decisions WHERE dispute_id = ?", String.class, d))
                .isEqualTo("analyst.b");
        String html = mvc.perform(get("/analyst/disputes/" + d).session(session)).andReturn().getResponse()
                .getContentAsString();
        assertThat(html).contains("Approved").doesNotContain("Record my decision");
    }

    @Test
    void overrideWithoutNoteReShowsFormAndRecordsNothing() throws Exception {
        UUID d = newDispute(true, new UUID[1]);

        String html = mvc.perform(post("/analyst/disputes/" + d + "/override").param("category", "OTHER")
                .param("queueChoice", "FRAUD").param("note", "  ").session(session))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        assertThat(html).contains("A note is required when overriding.");
        assertThat(statusOf(d)).isEqualTo("PENDING_REVIEW");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM decisions WHERE dispute_id = ?", Integer.class, d))
                .isZero();
    }

    @Test
    void overrideRecordsAndTheQueueShowsItAsOverridden() throws Exception {
        UUID d = newDispute(false, new UUID[1]);

        mvc.perform(post("/analyst/disputes/" + d + "/override").param("category", "UNAUTHORIZED_TRANSACTION")
                .param("queueChoice", "FRAUD").param("note", "Customer does not recognise it.").session(session))
                .andExpect(redirectedUrl("/analyst/queue"));

        assertThat(statusOf(d)).isEqualTo("OVERRIDDEN");
        String queue = mvc.perform(get("/analyst/queue").param("status", "OVERRIDDEN").session(session))
                .andReturn().getResponse().getContentAsString();
        assertThat(queue).contains("Flow Shop", "Overridden");
        String casePage = mvc.perform(get("/analyst/disputes/" + d).session(session)).andReturn().getResponse()
                .getContentAsString();
        assertThat(casePage).contains("analyst.b", "Customer does not recognise it.");
    }

    @Test
    void secondApproveIsBlockedWithAMessage() throws Exception {
        UUID[] s = new UUID[1];
        UUID d = newDispute(true, s);
        mvc.perform(post("/analyst/disputes/" + d + "/approve").param("suggestionId", s[0].toString()).session(session));

        mvc.perform(post("/analyst/disputes/" + d + "/approve").param("suggestionId", s[0].toString()).session(session))
                .andExpect(redirectedUrl("/analyst/disputes/" + d));

        assertThat(jdbc.queryForObject("SELECT count(*) FROM decisions WHERE dispute_id = ?", Integer.class, d))
                .isEqualTo(1);
    }

    @Test
    void unknownDisputeIs404() throws Exception {
        mvc.perform(get("/analyst/disputes/" + UUID.randomUUID()).session(session)).andExpect(status().isNotFound());
    }

    @Test
    void decisionEndpointsRequireAnalystSession() throws Exception {
        UUID[] s = new UUID[1];
        UUID d = newDispute(true, s);

        mvc.perform(post("/analyst/disputes/" + d + "/approve").param("suggestionId", s[0].toString()))
                .andExpect(redirectedUrl("/analyst"));

        assertThat(statusOf(d)).isEqualTo("PENDING_REVIEW");
    }
}
