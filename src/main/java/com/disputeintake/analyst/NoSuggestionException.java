package com.disputeintake.analyst;

public class NoSuggestionException extends RuntimeException {

    public NoSuggestionException() {
        super("There is no AI suggestion to approve for this dispute.");
    }
}
