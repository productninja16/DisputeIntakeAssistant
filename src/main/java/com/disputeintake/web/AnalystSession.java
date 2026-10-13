package com.disputeintake.web;

import jakarta.servlet.http.HttpSession;

/** Demo analyst sign-in: the analyst name lives in the HTTP session. Production would use real authentication. */
final class AnalystSession {

    private static final String KEY = "analyst";

    private AnalystSession() {
    }

    static String analyst(HttpSession session) {
        return (String) session.getAttribute(KEY);
    }

    static void signIn(HttpSession session, String analyst) {
        session.setAttribute(KEY, analyst);
    }

    static void signOut(HttpSession session) {
        session.removeAttribute(KEY);
    }
}
