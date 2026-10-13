package com.disputeintake.web;

import jakarta.servlet.http.HttpSession;
import java.util.UUID;

/** Demo sign-in: the customer id lives in the HTTP session. Production would use real authentication. */
final class CustomerSession {

    private static final String KEY = "customerId";

    private CustomerSession() {
    }

    static UUID customerId(HttpSession session) {
        return (UUID) session.getAttribute(KEY);
    }

    static void signIn(HttpSession session, UUID customerId) {
        session.setAttribute(KEY, customerId);
    }
}
