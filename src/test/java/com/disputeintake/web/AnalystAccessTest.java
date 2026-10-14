package com.disputeintake.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
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
class AnalystAccessTest {

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
    org.springframework.jdbc.core.JdbcTemplate jdbc;

    @Test
    void analystPagesRedirectToSignInWhenNotSignedIn() throws Exception {
        mvc.perform(get("/analyst/queue")).andExpect(redirectedUrl("/analyst"));
        mvc.perform(get("/analyst/disputes/00000000-0000-0000-0000-000000000000"))
                .andExpect(redirectedUrl("/analyst"));
    }

    @Test
    void signInPageIsPublic() throws Exception {
        mvc.perform(get("/analyst")).andExpect(status().isOk()).andExpect(content().string(
                org.hamcrest.Matchers.containsString("analyst.a")));
    }

    @Test
    void unknownAnalystCannotSignIn() throws Exception {
        MockHttpSession session = new MockHttpSession();
        mvc.perform(post("/analyst/sign-in").param("analyst", "mallory").session(session))
                .andExpect(redirectedUrl("/analyst"));
        mvc.perform(get("/analyst/queue").session(session)).andExpect(redirectedUrl("/analyst"));
    }

    @Test
    void signedInAnalystSeesTheQueue() throws Exception {
        MockHttpSession session = new MockHttpSession();
        mvc.perform(post("/analyst/sign-in").param("analyst", "analyst.a").session(session))
                .andExpect(redirectedUrl("/analyst/queue"));
        mvc.perform(get("/analyst/queue").session(session)).andExpect(status().isOk());
    }

    @Test
    void customerSignInDoesNotGrantAnalystAccess() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("customerId", java.util.UUID.randomUUID());
        mvc.perform(get("/analyst/queue").session(session)).andExpect(redirectedUrl("/analyst"));
    }

    @Test
    void queueListsPendingDisputesWithSuggestionAndFilters() throws Exception {
        java.util.UUID customer = java.util.UUID.randomUUID();
        java.util.UUID tx = java.util.UUID.randomUUID();
        java.util.UUID withSuggestion = java.util.UUID.randomUUID();
        java.util.UUID without = java.util.UUID.randomUUID();
        jdbc.update("INSERT INTO customers (id, customer_ref, display_name) VALUES (?, 'Q-1', 'Q')", customer);
        jdbc.update("""
                INSERT INTO transactions (id, customer_id, merchant_name, amount_cents, posted_on, card_last4)
                VALUES (?, ?, 'Queue Shop', 4200, current_date, '1111')""", tx, customer);
        java.util.UUID tx2 = java.util.UUID.randomUUID();
        jdbc.update("""
                INSERT INTO transactions (id, customer_id, merchant_name, amount_cents, posted_on, card_last4)
                VALUES (?, ?, 'Other Shop', 100, current_date, '1111')""", tx2, customer);
        jdbc.update("INSERT INTO disputes (id, customer_id, transaction_id, description) VALUES (?, ?, ?, 'Charged twice at the shop')",
                withSuggestion, customer, tx);
        jdbc.update("INSERT INTO disputes (id, customer_id, transaction_id, description) VALUES (?, ?, ?, 'Something odd happened')",
                without, customer, tx2);
        jdbc.update("""
                INSERT INTO ai_suggestions (id, dispute_id, category, reasoning, proposed_queue, model, raw_json)
                VALUES (gen_random_uuid(), ?, 'DUPLICATE_CHARGE', 'r', 'BILLING_ERRORS', 'test', '{}')""", withSuggestion);

        MockHttpSession session = new MockHttpSession();
        mvc.perform(post("/analyst/sign-in").param("analyst", "analyst.a").session(session));

        String all = mvc.perform(get("/analyst/queue").session(session)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        org.assertj.core.api.Assertions.assertThat(all).contains("Queue Shop", "Other Shop", "DUPLICATE_CHARGE");

        String billing = mvc.perform(get("/analyst/queue").param("queueFilter", "BILLING_ERRORS").session(session))
                .andReturn().getResponse().getContentAsString();
        org.assertj.core.api.Assertions.assertThat(billing).contains("Queue Shop").doesNotContain("Other Shop");

        String none = mvc.perform(get("/analyst/queue").param("queueFilter", "NONE").session(session))
                .andReturn().getResponse().getContentAsString();
        org.assertj.core.api.Assertions.assertThat(none).contains("Other Shop").doesNotContain("Queue Shop");
    }

    private java.util.UUID customerWithTx(String ref, String merchant) {
        java.util.UUID customer = java.util.UUID.randomUUID();
        jdbc.update("INSERT INTO customers (id, customer_ref, display_name) VALUES (?, ?, 'Q')", customer, ref);
        return customer;
    }

    private java.util.UUID dispute(java.util.UUID customer, String merchant, String status, String createdAt) {
        java.util.UUID tx = java.util.UUID.randomUUID();
        java.util.UUID d = java.util.UUID.randomUUID();
        jdbc.update("""
                INSERT INTO transactions (id, customer_id, merchant_name, amount_cents, posted_on, card_last4)
                VALUES (?, ?, ?, 100, current_date, '1111')""", tx, customer, merchant);
        jdbc.update("""
                INSERT INTO disputes (id, customer_id, transaction_id, description, status, created_at)
                VALUES (?, ?, ?, 'Some customer text here', ?, ?::timestamptz)""", d, customer, tx, status, createdAt);
        return d;
    }

    @Test
    void queueShowsStatusAndConfidenceAndFiltersByStatus() throws Exception {
        java.util.UUID c = customerWithTx("ST-1", "x");
        java.util.UUID waiting = dispute(c, "Waiting Shop", "PENDING_REVIEW", "2026-01-02T00:00:00Z");
        java.util.UUID done = dispute(c, "Done Shop", "APPROVED", "2026-01-01T00:00:00Z");
        java.util.UUID changed = dispute(c, "Changed Shop", "OVERRIDDEN", "2026-01-03T00:00:00Z");
        jdbc.update("""
                INSERT INTO ai_suggestions (id, dispute_id, category, reasoning, proposed_queue, confidence, model, raw_json)
                VALUES (gen_random_uuid(), ?, 'OTHER', 'r', 'GENERAL_REVIEW', 'HIGH', 'test', '{}')""", waiting);
        // an old suggestion written before confidence existed
        jdbc.update("""
                INSERT INTO ai_suggestions (id, dispute_id, category, reasoning, proposed_queue, model, raw_json)
                VALUES (gen_random_uuid(), ?, 'OTHER', 'r', 'GENERAL_REVIEW', 'test', '{}')""", done);

        MockHttpSession session = new MockHttpSession();
        mvc.perform(post("/analyst/sign-in").param("analyst", "analyst.a").session(session));

        String dflt = page(session, null);
        org.assertj.core.api.Assertions.assertThat(dflt).contains("Waiting Shop", "In review", "HIGH")
                .doesNotContain("Done Shop", "Changed Shop");

        String approved = page(session, "APPROVED");
        org.assertj.core.api.Assertions.assertThat(approved).contains("Done Shop", "Approved")
                .doesNotContain("Waiting Shop", "Changed Shop");

        String overridden = page(session, "OVERRIDDEN");
        org.assertj.core.api.Assertions.assertThat(overridden).contains("Changed Shop", "Overridden");

        String all = page(session, "ALL");
        org.assertj.core.api.Assertions.assertThat(all).contains("Waiting Shop", "Done Shop", "Changed Shop");

        String bogus = page(session, "NOT_A_STATUS");
        org.assertj.core.api.Assertions.assertThat(bogus).contains("Waiting Shop").doesNotContain("Done Shop");
    }

    @Test
    void inReviewSortsOldestFirstAndOtherViewsNewestFirst() throws Exception {
        java.util.UUID c = customerWithTx("SORT-1", "x");
        dispute(c, "Sort Old", "PENDING_REVIEW", "2026-02-01T00:00:00Z");
        dispute(c, "Sort New", "PENDING_REVIEW", "2026-02-03T00:00:00Z");
        dispute(c, "Sort Mid", "PENDING_REVIEW", "2026-02-02T00:00:00Z");

        MockHttpSession session = new MockHttpSession();
        mvc.perform(post("/analyst/sign-in").param("analyst", "analyst.a").session(session));

        String pending = page(session, "PENDING_REVIEW");
        org.assertj.core.api.Assertions.assertThat(pending.indexOf("Sort Old"))
                .isLessThan(pending.indexOf("Sort Mid"));
        org.assertj.core.api.Assertions.assertThat(pending.indexOf("Sort Mid"))
                .isLessThan(pending.indexOf("Sort New"));

        String all = page(session, "ALL");
        org.assertj.core.api.Assertions.assertThat(all.indexOf("Sort New"))
                .isLessThan(all.indexOf("Sort Mid"));
        org.assertj.core.api.Assertions.assertThat(all.indexOf("Sort Mid"))
                .isLessThan(all.indexOf("Sort Old"));
    }

    private String page(MockHttpSession session, String status) throws Exception {
        var req = get("/analyst/queue").session(session);
        if (status != null) {
            req = req.param("status", status);
        }
        return mvc.perform(req).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    }

    @Test
    void historyPageIsGone() throws Exception {
        MockHttpSession session = new MockHttpSession();
        mvc.perform(post("/analyst/sign-in").param("analyst", "analyst.a").session(session));
        mvc.perform(get("/analyst/history").session(session)).andExpect(status().is4xxClientError());
    }
}
