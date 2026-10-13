package com.disputeintake.suggestion;

import java.util.List;

public record ValidationOutcome(SuggestionResult result, List<String> errors) {

    public boolean isValid() {
        return result != null;
    }

    static ValidationOutcome valid(SuggestionResult result) {
        return new ValidationOutcome(result, List.of());
    }

    static ValidationOutcome invalid(List<String> errors) {
        return new ValidationOutcome(null, errors);
    }
}
