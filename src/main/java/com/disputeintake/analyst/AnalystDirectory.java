package com.disputeintake.analyst;

import java.util.List;

/** Fixed demo analysts. A real deployment would take identity from the bank's authentication. */
public final class AnalystDirectory {

    public static final List<String> NAMES = List.of("analyst.a", "analyst.b");

    private AnalystDirectory() {
    }

    public static boolean contains(String name) {
        return NAMES.contains(name);
    }
}
