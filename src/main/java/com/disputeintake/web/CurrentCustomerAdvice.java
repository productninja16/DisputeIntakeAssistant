package com.disputeintake.web;

import com.disputeintake.customer.Customer;
import com.disputeintake.customer.CustomerRepository;
import jakarta.servlet.http.HttpSession;
import java.util.UUID;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
class CurrentCustomerAdvice {

    private final CustomerRepository customers;

    CurrentCustomerAdvice(CustomerRepository customers) {
        this.customers = customers;
    }

    @ModelAttribute("currentCustomer")
    Customer currentCustomer(HttpSession session) {
        UUID id = CustomerSession.customerId(session);
        return id == null ? null : customers.findById(id).orElse(null);
    }
}
