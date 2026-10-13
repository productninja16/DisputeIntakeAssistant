package com.disputeintake.dispute;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CardNumberMaskerTest {

    // Well-known Luhn-valid test numbers, not real cards.
    private static final String VISA_TEST = "4111111111111111";
    private static final String MC_TEST = "5555555555554444";

    @Test
    void masksPlainCardNumber() {
        assertThat(CardNumberMasker.mask("my card " + VISA_TEST + " was used"))
                .isEqualTo("my card [CARD NUMBER REMOVED] was used");
    }

    @Test
    void masksNumbersWithSpacesAndDashes() {
        assertThat(CardNumberMasker.mask("4111 1111 1111 1111")).isEqualTo(CardNumberMasker.REPLACEMENT);
        assertThat(CardNumberMasker.mask("4111-1111-1111-1111")).isEqualTo(CardNumberMasker.REPLACEMENT);
    }

    @Test
    void masksEveryNumberInTheText() {
        assertThat(CardNumberMasker.mask(VISA_TEST + " and also " + MC_TEST))
                .isEqualTo("[CARD NUMBER REMOVED] and also [CARD NUMBER REMOVED]");
    }

    @Test
    void leavesDigitRunsThatFailLuhn() {
        assertThat(CardNumberMasker.mask("order 4111111111111112")).isEqualTo("order 4111111111111112");
    }

    @Test
    void leavesShortNumbersAndAmounts() {
        String text = "I was charged 129.99 on 2026-03-14, last four 1234, order 98765";
        assertThat(CardNumberMasker.mask(text)).isEqualTo(text);
    }

    @Test
    void doesNotMaskPartOfALongerDigitRun() {
        String twentyDigits = "41111111111111111111";
        assertThat(CardNumberMasker.mask(twentyDigits)).isEqualTo(twentyDigits);
    }

    @Test
    void handlesNullAndEmpty() {
        assertThat(CardNumberMasker.mask(null)).isNull();
        assertThat(CardNumberMasker.mask("")).isEmpty();
    }
}
