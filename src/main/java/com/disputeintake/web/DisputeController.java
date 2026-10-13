package com.disputeintake.web;

import com.disputeintake.dispute.Dispute;
import com.disputeintake.dispute.DisputeService;
import com.disputeintake.dispute.DuplicateOpenDisputeException;
import com.disputeintake.dispute.TransactionNotFoundException;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;

@Controller
class DisputeController {

    private static final String SIGN_IN = "redirect:/";

    private final DisputeService service;

    DisputeController(DisputeService service) {
        this.service = service;
    }

    @GetMapping("/disputes")
    String list(HttpSession session, Model model) {
        UUID customerId = CustomerSession.customerId(session);
        if (customerId == null) {
            return SIGN_IN;
        }
        model.addAttribute("disputes", service.listForCustomer(customerId));
        return "disputes";
    }

    @GetMapping("/disputes/new")
    String newForm(HttpSession session, Model model) {
        UUID customerId = CustomerSession.customerId(session);
        if (customerId == null) {
            return SIGN_IN;
        }
        model.addAttribute("disputeForm", new DisputeForm());
        model.addAttribute("options", service.transactionOptions(customerId));
        return "dispute-form";
    }

    @PostMapping("/disputes")
    String submit(@Valid @ModelAttribute DisputeForm form, BindingResult errors, HttpSession session,
            Model model) {
        UUID customerId = CustomerSession.customerId(session);
        if (customerId == null) {
            return SIGN_IN;
        }
        if (!errors.hasErrors()) {
            try {
                Dispute dispute = service.submit(customerId, form.getTransactionId(), form.getDescription());
                return "redirect:/disputes/" + dispute.getId() + "/received";
            } catch (DuplicateOpenDisputeException e) {
                errors.rejectValue("transactionId", "duplicate",
                        "A dispute is already open for this transaction.");
            }
        }
        model.addAttribute("options", service.transactionOptions(customerId));
        return "dispute-form";
    }

    @GetMapping("/disputes/{id}/received")
    String received(@PathVariable UUID id, HttpSession session, Model model) {
        UUID customerId = CustomerSession.customerId(session);
        if (customerId == null) {
            return SIGN_IN;
        }
        model.addAttribute("caseRef", service.getForCustomer(id, customerId).getCaseRef());
        return "dispute-received";
    }

    @ExceptionHandler(TransactionNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    String notFound() {
        return "not-found";
    }
}
