package com.disputeintake.web;

import com.disputeintake.analyst.AnalystDirectory;
import com.disputeintake.analyst.AnalystQueueService;
import com.disputeintake.suggestion.ProposedQueue;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/** Analyst screens. Everything under /analyst except sign-in is guarded by AnalystAccessInterceptor. */
@Controller
class AnalystController {

    private final AnalystQueueService queue;

    AnalystController(AnalystQueueService queue) {
        this.queue = queue;
    }

    @GetMapping("/analyst")
    String signInPage(Model model) {
        model.addAttribute("analysts", AnalystDirectory.NAMES);
        return "analyst-sign-in";
    }

    @PostMapping("/analyst/sign-in")
    String signIn(@RequestParam String analyst, HttpSession session) {
        if (!AnalystDirectory.contains(analyst)) {
            return "redirect:/analyst";
        }
        AnalystSession.signIn(session, analyst);
        return "redirect:/analyst/queue";
    }

    @PostMapping("/analyst/sign-out")
    String signOut(HttpSession session) {
        AnalystSession.signOut(session);
        return "redirect:/analyst";
    }

    @GetMapping("/analyst/queue")
    String queuePage(@RequestParam(required = false) String queueFilter, Model model) {
        model.addAttribute("rows", queue.pending(queueFilter));
        model.addAttribute("filter", queueFilter == null ? "" : queueFilter);
        model.addAttribute("queues", ProposedQueue.values());
        model.addAttribute("noSuggestion", AnalystQueueService.NO_SUGGESTION);
        return "analyst-queue";
    }

    @GetMapping("/analyst/history")
    String history(Model model) {
        model.addAttribute("rows", queue.history());
        return "analyst-history";
    }
}
