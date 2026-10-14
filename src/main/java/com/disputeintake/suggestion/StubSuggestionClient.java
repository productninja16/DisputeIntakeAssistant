package com.disputeintake.suggestion;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Keyword-rule stand-in for the LLM. Default client; makes no network calls. */
@Component
@ConditionalOnProperty(name = "suggestion.client", havingValue = "stub", matchIfMissing = true)
public class StubSuggestionClient implements SuggestionClient {

    private final ObjectMapper mapper;

    public StubSuggestionClient(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public String suggest(SuggestionContext context, List<String> previousErrors) {
        String text = context.description().toLowerCase(Locale.ROOT);
        Category category;
        ProposedQueue queue;
        Confidence confidence = Confidence.MEDIUM; // keyword match
        List<String> missing = List.of();
        String reasoning;
        if (containsAny(text, "twice", "duplicate", "double", "two times")) {
            category = Category.DUPLICATE_CHARGE;
            queue = ProposedQueue.BILLING_ERRORS;
            reasoning = "The customer reports being charged more than once.";
        } else if (containsAny(text, "cancel", "subscription")) {
            category = Category.SUBSCRIPTION_CANCELLED;
            queue = ProposedQueue.MERCHANT_DISPUTES;
            missing = List.of("Date the subscription was cancelled", "Cancellation confirmation");
            reasoning = "The customer says a subscription was charged after cancelling.";
        } else if (containsAny(text, "never arrived", "not received", "didn't arrive", "never received", "refund")) {
            category = Category.MERCHANDISE_NOT_RECEIVED;
            queue = ProposedQueue.MERCHANT_DISPUTES;
            missing = List.of("Order number", "Contact with the merchant");
            reasoning = "The customer says goods or a refund were not received.";
        } else if (containsAny(text, "wrong amount", "overcharged", "more than", "quoted")) {
            category = Category.INCORRECT_AMOUNT;
            queue = ProposedQueue.BILLING_ERRORS;
            missing = List.of("Amount the customer expected");
            reasoning = "The customer says the charged amount differs from what was agreed.";
        } else if (containsAny(text, "don't recognise", "don't recognize", "not me", "stolen", "fraud", "unknown")) {
            category = Category.UNAUTHORIZED_TRANSACTION;
            queue = ProposedQueue.FRAUD;
            missing = List.of("Whether the card is still in the customer's possession");
            reasoning = "The customer does not recognise the transaction.";
        } else {
            category = Category.OTHER;
            queue = ProposedQueue.GENERAL_REVIEW;
            missing = List.of("More detail about what went wrong");
            reasoning = "The description does not clearly match a known category.";
            confidence = Confidence.LOW;
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("category", category.name());
        out.put("missing_info", missing);
        out.put("reasoning", reasoning + " (stub rules, not a model)");
        out.put("proposed_queue", queue.name());
        out.put("confidence", confidence.name());
        try {
            return mapper.writeValueAsString(out);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public String modelName() {
        return "stub";
    }

    private static boolean containsAny(String text, String... needles) {
        for (String n : needles) {
            if (text.contains(n)) {
                return true;
            }
        }
        return false;
    }
}
