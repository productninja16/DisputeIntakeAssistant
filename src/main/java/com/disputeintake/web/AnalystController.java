package com.disputeintake.web;

import com.disputeintake.analyst.AnalystDirectory;
import com.disputeintake.analyst.AlreadyDecidedException;
import com.disputeintake.analyst.AnalystQueueService;
import com.disputeintake.analyst.CaseView;
import com.disputeintake.analyst.DecisionService;
import com.disputeintake.analyst.DisputeNotFoundException;
import com.disputeintake.analyst.InvalidDecisionException;
import com.disputeintake.analyst.NoSuggestionException;
import com.disputeintake.dispute.Dispute;
import com.disputeintake.dispute.DisputeStatus;
import com.disputeintake.suggestion.Category;
import com.disputeintake.suggestion.ProposedQueue;
import jakarta.servlet.http.HttpSession;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Analyst screens. Everything under /analyst except sign-in is guarded by AnalystAccessInterceptor. */
@Controller
class AnalystController {

    private final AnalystQueueService queue;
    private final DecisionService decisions;

    AnalystController(AnalystQueueService queue, DecisionService decisions) {
        this.queue = queue;
        this.decisions = decisions;
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
    String queuePage(@RequestParam(required = false) String status,
            @RequestParam(required = false) String queueFilter, Model model) {
        String appliedStatus = AnalystQueueService.normalizeStatus(status);
        model.addAttribute("rows", queue.list(appliedStatus, queueFilter));
        model.addAttribute("statusFilter", appliedStatus);
        model.addAttribute("filter", queueFilter == null ? "" : queueFilter);
        model.addAttribute("statuses", DisputeStatus.values());
        model.addAttribute("allStatuses", AnalystQueueService.ALL_STATUSES);
        model.addAttribute("queues", ProposedQueue.values());
        model.addAttribute("noSuggestion", AnalystQueueService.NO_SUGGESTION);
        return "analyst-queue";
    }

    @GetMapping("/analyst/disputes/{id}")
    String casePage(@PathVariable UUID id, Model model) {
        CaseView view = queue.getCase(id);
        fillCase(model, view, null, null, null);
        return "analyst-case";
    }

    @PostMapping("/analyst/disputes/{id}/approve")
    String approve(@PathVariable UUID id, @RequestParam UUID suggestionId, HttpSession session,
            RedirectAttributes redirect) {
        try {
            decisions.approve(id, suggestionId, AnalystSession.analyst(session));
            redirect.addFlashAttribute("message", "Approved " + Dispute.caseRef(id) + ".");
            return "redirect:/analyst/queue";
        } catch (AlreadyDecidedException | NoSuggestionException e) {
            redirect.addFlashAttribute("error", e.getMessage());
            return "redirect:/analyst/disputes/" + id;
        }
    }

    @PostMapping("/analyst/disputes/{id}/override")
    String override(@PathVariable UUID id, @RequestParam Category category, @RequestParam ProposedQueue queueChoice,
            @RequestParam(defaultValue = "") String note, HttpSession session, RedirectAttributes redirect,
            Model model) {
        try {
            decisions.override(id, category, queueChoice, note, AnalystSession.analyst(session));
            redirect.addFlashAttribute("message", "Overrode " + Dispute.caseRef(id) + ".");
            return "redirect:/analyst/queue";
        } catch (AlreadyDecidedException e) {
            redirect.addFlashAttribute("error", e.getMessage());
            return "redirect:/analyst/disputes/" + id;
        } catch (InvalidDecisionException e) {
            fillCase(model, queue.getCase(id), category, queueChoice, note);
            model.addAttribute("error", e.getMessage());
            return "analyst-case";
        }
    }

    /** Form defaults: the AI's values if present, otherwise OTHER / GENERAL_REVIEW, unless re-showing input. */
    private void fillCase(Model model, CaseView view, Category category, ProposedQueue queueChoice, String note) {
        model.addAttribute("case", view);
        model.addAttribute("categories", Category.values());
        model.addAttribute("queues", ProposedQueue.values());
        Category defaultCategory = view.suggestion() != null ? view.suggestion().category() : Category.OTHER;
        ProposedQueue defaultQueue = view.suggestion() != null ? view.suggestion().proposedQueue()
                : ProposedQueue.GENERAL_REVIEW;
        model.addAttribute("formCategory", category != null ? category : defaultCategory);
        model.addAttribute("formQueue", queueChoice != null ? queueChoice : defaultQueue);
        model.addAttribute("formNote", note == null ? "" : note);
    }

    @ExceptionHandler(DisputeNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    String notFound() {
        return "not-found";
    }
}
