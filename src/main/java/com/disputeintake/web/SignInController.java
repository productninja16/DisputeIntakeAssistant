package com.disputeintake.web;

import com.disputeintake.customer.CustomerRepository;
import jakarta.servlet.http.HttpSession;
import java.util.UUID;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/** Demo-only "sign in as" picker over synthetic customers. */
@Controller
class SignInController {

    private final CustomerRepository customers;

    SignInController(CustomerRepository customers) {
        this.customers = customers;
    }

    @GetMapping("/")
    String picker(Model model) {
        model.addAttribute("customers", customers.findAllByOrderByCustomerRefAsc());
        return "index";
    }

    @PostMapping("/sign-in")
    String signIn(@RequestParam UUID customerId, HttpSession session) {
        if (!customers.existsById(customerId)) {
            return "redirect:/";
        }
        CustomerSession.signIn(session, customerId);
        return "redirect:/disputes";
    }

    @PostMapping("/sign-out")
    String signOut(HttpSession session) {
        session.invalidate();
        return "redirect:/";
    }
}
