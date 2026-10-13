package com.disputeintake.dispute;

public final class Money {

    private Money() {
    }

    /** Formats integer cents as dollars for display, e.g. 12999 -> $129.99. */
    public static String format(long cents) {
        return String.format("$%d.%02d", cents / 100, cents % 100);
    }
}
