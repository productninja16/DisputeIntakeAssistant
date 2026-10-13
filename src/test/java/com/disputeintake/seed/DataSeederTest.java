package com.disputeintake.seed;

import static org.assertj.core.api.Assertions.assertThat;

import com.disputeintake.customer.CardTransactionRepository;
import com.disputeintake.customer.CustomerRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@ActiveProfiles("seed")
@Testcontainers
class DataSeederTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    DataSeeder seeder;
    @Autowired
    CustomerRepository customers;
    @Autowired
    CardTransactionRepository transactions;

    @Test
    void seedsOnStartupAndIsIdempotent() {
        assertThat(customers.count()).isEqualTo(5);
        long txCount = transactions.count();
        assertThat(txCount).isEqualTo(30);

        seeder.seed();

        assertThat(customers.count()).isEqualTo(5);
        assertThat(transactions.count()).isEqualTo(txCount);
    }
}
