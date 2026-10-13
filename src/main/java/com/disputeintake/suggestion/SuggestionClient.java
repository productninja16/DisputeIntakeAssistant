package com.disputeintake.suggestion;

import java.util.List;

/** Source of raw suggestion JSON. Output is untrusted until the validator accepts it. */
public interface SuggestionClient {

    /**
     * @param previousErrors validation errors from the failed first attempt; empty on the first call
     * @return raw JSON text as produced by the model
     */
    String suggest(SuggestionContext context, List<String> previousErrors);

    /** Recorded with each stored suggestion. */
    String modelName();
}
