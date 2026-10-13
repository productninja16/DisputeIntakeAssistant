package com.disputeintake.suggestion;

import java.time.LocalDate;

/** What the model is told about a dispute. The description is already card-number masked. */
public record SuggestionContext(String description, String merchantName, long amountCents, LocalDate postedOn) {
}
