package com.disputeintake.suggestion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.disputeintake.customer.CardTransaction;
import com.disputeintake.customer.CardTransactionRepository;
import com.disputeintake.customer.Customer;
import com.disputeintake.customer.CustomerRepository;
import com.disputeintake.dispute.Dispute;
import com.disputeintake.dispute.DisputeService;
import java.time.Duration;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** End to end with the default stub client: submit returns, the suggestion appears afterwards. */
// Pinned to the stub so tests never hit the real API, even if a local .env enables it.
@SpringBootTest(properties = "suggestion.client=stub")
@Testcontainers
class SuggestionAsyncTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    DisputeService disputeService;
    @Autowired
    CustomerRepository customers;
    @Autowired
    CardTransactionRepository transactions;
    @Autowired
    AiSuggestionRepository suggestions;

    @Test
    void submitTriggersSuggestionAfterCommit() {
        Customer c = customers.save(new Customer("ASYNC-1", "Async Test"));
        CardTransaction tx = transactions.save(
                new CardTransaction(c.getId(), "Shop", 2500, LocalDate.now(), "9999"));

        Dispute d = disputeService.submit(c.getId(), tx.getId(), "I was charged twice at this shop.");

        await().atMost(Duration.ofSeconds(10)).until(() -> suggestions.existsByDisputeId(d.getId()));
        AiSuggestion s = suggestions.findByDisputeId(d.getId()).get(0);
        assertThat(s.getCategory()).isEqualTo(Category.DUPLICATE_CHARGE);
        assertThat(s.getModel()).isEqualTo("stub");
        assertThat(s.getConfidence()).isEqualTo(Confidence.MEDIUM);
    }
}
