package com.disputeintake.web;

import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
class CurrentAnalystAdvice {

    @ModelAttribute("currentAnalyst")
    String currentAnalyst(HttpSession session) {
        return AnalystSession.analyst(session);
    }
}
