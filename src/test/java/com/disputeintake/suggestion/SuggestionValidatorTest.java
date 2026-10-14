package com.disputeintake.suggestion;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class SuggestionValidatorTest {

    private final SuggestionValidator validator = new SuggestionValidator(new ObjectMapper());

    private static final String VALID = """
            {"category":"DUPLICATE_CHARGE","missing_info":["Receipt"],
             "reasoning":"Charged twice.","proposed_queue":"BILLING_ERRORS","confidence":"HIGH"}""";

    @Test
    void acceptsValidJson() {
        ValidationOutcome o = validator.validate(VALID);
        assertThat(o.isValid()).isTrue();
        assertThat(o.result().category()).isEqualTo(Category.DUPLICATE_CHARGE);
        assertThat(o.result().missingInfo()).containsExactly("Receipt");
        assertThat(o.result().proposedQueue()).isEqualTo(ProposedQueue.BILLING_ERRORS);
        assertThat(o.result().confidence()).isEqualTo(Confidence.HIGH);
    }

    @Test
    void rejectsMissingOrUnknownConfidence() {
        assertThat(validator.validate(VALID.replace(",\"confidence\":\"HIGH\"", "")).isValid()).isFalse();
        assertThat(validator.validate(VALID.replace("HIGH", "CERTAIN")).isValid()).isFalse();
        assertThat(validator.validate(VALID.replace("\"HIGH\"", "0.9")).isValid()).isFalse();
    }

    @Test
    void toleratesMarkdownFence() {
        assertThat(validator.validate("```json\n" + VALID + "\n```").isValid()).isTrue();
    }

    @Test
    void rejectsUnknownCategory() {
        assertThat(validator.validate(VALID.replace("DUPLICATE_CHARGE", "CHARGEBACK_42")).isValid()).isFalse();
    }

    @Test
    void rejectsMissingField() {
        String noReasoning = """
                {"category":"OTHER","missing_info":[],"proposed_queue":"GENERAL_REVIEW","confidence":"LOW"}""";
        ValidationOutcome o = validator.validate(noReasoning);
        assertThat(o.isValid()).isFalse();
        assertThat(o.errors()).isNotEmpty();
    }

    @Test
    void rejectsExtraField() {
        assertThat(validator.validate(VALID.replace("{\"category\"", "{\"decision\":\"APPROVE\",\"category\"")).isValid())
                .isFalse();
    }

    @Test
    void rejectsNonJsonAndEmpty() {
        assertThat(validator.validate("Sure! Here is my answer").isValid()).isFalse();
        assertThat(validator.validate("").isValid()).isFalse();
        assertThat(validator.validate(null).isValid()).isFalse();
    }

    @Test
    void rejectsOverlongReasoning() {
        String longText = "x".repeat(1001);
        assertThat(validator.validate(VALID.replace("Charged twice.", longText)).isValid()).isFalse();
    }
}
