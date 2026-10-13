package com.disputeintake.seed;

import com.disputeintake.customer.CardTransaction;
import com.disputeintake.customer.CardTransactionRepository;
import com.disputeintake.customer.Customer;
import com.disputeintake.customer.CustomerRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Loads synthetic customers and card transactions. Runs only with the "seed" profile
 * and does nothing if customers already exist. Synthetic data only: last-4 digits, no full card numbers.
 */
@Component
@Profile("seed")
public class DataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private record Tx(String merchant, long cents, int daysAgo) {
    }

    private record Seed(String ref, String name, String last4, List<Tx> transactions) {
    }

    private static final List<Seed> SEEDS = List.of(
            new Seed("CUST-0001", "Alex Rivera", "4821", List.of(
                    new Tx("Corner Cafe", 1250, 3),
                    new Tx("Metro Grocery", 8734, 5),
                    new Tx("Metro Grocery", 8734, 5), // duplicate charge
                    new Tx("Skyline Airlines", 42000, 12),
                    new Tx("Fuel Stop 22", 5510, 15),
                    new Tx("Bookshop Online", 2399, 20))),
            new Seed("CUST-0002", "Sam Okafor", "7305", List.of(
                    new Tx("StreamBox Monthly", 1599, 2),
                    new Tx("StreamBox Monthly", 1599, 32), // subscription after cancelling
                    new Tx("Unknown Merchant LLC", 19900, 7), // unrecognised charge
                    new Tx("Pharmacy Plus", 3420, 9),
                    new Tx("Hardware Depot", 15675, 18),
                    new Tx("Taxi Direct", 2240, 21))),
            new Seed("CUST-0003", "Priya Nair", "1938", List.of(
                    new Tx("Gadget World", 129900, 4), // item never arrived
                    new Tx("Cinema Central", 3600, 6),
                    new Tx("Green Market", 4521, 8),
                    new Tx("Home Furnish Co", 54999, 14), // refund promised, not received
                    new Tx("Pizza Palace", 2875, 16),
                    new Tx("Gym Club Fees", 4500, 25))),
            new Seed("CUST-0004", "Jordan Lee", "6640", List.of(
                    new Tx("Hotel Lakeside", 38800, 10), // charged more than quoted
                    new Tx("Car Rental Express", 21050, 11),
                    new Tx("Noodle House", 3150, 13),
                    new Tx("Electronics Mart", 89900, 19),
                    new Tx("Coffee Roasters", 1475, 22),
                    new Tx("Parking Garage 5", 1800, 23))),
            new Seed("CUST-0005", "Maria Santos", "2057", List.of(
                    new Tx("Online Marketplace", 6400, 1),
                    new Tx("Online Marketplace", 6400, 1), // possible duplicate
                    new Tx("Utility Telecom", 7999, 17), // billed wrong amount
                    new Tx("Daily Bakery", 890, 26),
                    new Tx("Pet Supplies Co", 5320, 28),
                    new Tx("Foreign ATM Fee", 500, 30))));

    private final CustomerRepository customers;
    private final CardTransactionRepository transactions;

    public DataSeeder(CustomerRepository customers, CardTransactionRepository transactions) {
        this.customers = customers;
        this.transactions = transactions;
    }

    @Override
    public void run(ApplicationArguments args) {
        seed();
    }

    @Transactional
    public void seed() {
        if (customers.count() > 0) {
            log.info("Seed skipped: customers already exist");
            return;
        }
        LocalDate today = LocalDate.now();
        for (Seed s : SEEDS) {
            Customer customer = customers.save(new Customer(s.ref(), s.name()));
            List<CardTransaction> txs = new ArrayList<>();
            for (Tx t : s.transactions()) {
                txs.add(new CardTransaction(customer.getId(), t.merchant(), t.cents(),
                        today.minusDays(t.daysAgo()), s.last4()));
            }
            transactions.saveAll(txs);
        }
        log.info("Seeded {} customers", SEEDS.size());
    }
}
