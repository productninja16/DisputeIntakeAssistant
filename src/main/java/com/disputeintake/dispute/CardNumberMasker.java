package com.disputeintake.dispute;

import java.util.regex.Pattern;

/**
 * Replaces anything that looks like a payment card number (13-19 digits, optional single
 * space or dash separators, passing the Luhn check) so a real number is never stored or
 * sent to the LLM.
 */
public final class CardNumberMasker {

    public static final String REPLACEMENT = "[CARD NUMBER REMOVED]";

    private static final Pattern CANDIDATE = Pattern.compile("(?<!\\d)\\d(?:[ -]?\\d){12,18}(?!\\d)");

    private CardNumberMasker() {
    }

    public static String mask(String text) {
        if (text == null) {
            return null;
        }
        return CANDIDATE.matcher(text).replaceAll(match -> {
            String digits = match.group().replaceAll("[ -]", "");
            return isLuhnValid(digits) ? REPLACEMENT : match.group();
        });
    }

    static boolean isLuhnValid(String digits) {
        int sum = 0;
        boolean doubleIt = false;
        for (int i = digits.length() - 1; i >= 0; i--) {
            int d = digits.charAt(i) - '0';
            if (doubleIt) {
                d *= 2;
                if (d > 9) {
                    d -= 9;
                }
            }
            sum += d;
            doubleIt = !doubleIt;
        }
        return sum % 10 == 0;
    }
}
