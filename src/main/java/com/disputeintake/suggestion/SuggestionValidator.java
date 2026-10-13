package com.disputeintake.suggestion;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/** Validates raw model output against suggestion-schema.json before anything is stored. */
@Component
public class SuggestionValidator {

    private final ObjectMapper mapper;
    private final JsonSchema schema;

    public SuggestionValidator(ObjectMapper mapper) {
        this.mapper = mapper;
        try (InputStream in = getClass().getResourceAsStream("/suggestion-schema.json")) {
            this.schema = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012).getSchema(in);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot load suggestion-schema.json", e);
        }
    }

    public ValidationOutcome validate(String rawJson) {
        if (rawJson == null || rawJson.isBlank()) {
            return ValidationOutcome.invalid(List.of("Empty response"));
        }
        JsonNode node;
        try {
            node = mapper.readTree(stripFences(rawJson));
        } catch (IOException e) {
            return ValidationOutcome.invalid(List.of("Not valid JSON"));
        }
        List<String> errors = new ArrayList<>();
        schema.validate(node).forEach(m -> errors.add(m.getMessage()));
        if (!errors.isEmpty()) {
            return ValidationOutcome.invalid(errors);
        }
        List<String> missing = new ArrayList<>();
        node.get("missing_info").forEach(n -> missing.add(n.asText()));
        return ValidationOutcome.valid(new SuggestionResult(
                Category.valueOf(node.get("category").asText()),
                List.copyOf(missing),
                node.get("reasoning").asText(),
                ProposedQueue.valueOf(node.get("proposed_queue").asText())));
    }

    /** Models sometimes wrap JSON in a markdown fence; tolerate that and nothing else. */
    static String stripFences(String raw) {
        String s = raw.strip();
        if (s.startsWith("```")) {
            int firstNewline = s.indexOf('\n');
            int lastFence = s.lastIndexOf("```");
            if (firstNewline > 0 && lastFence > firstNewline) {
                s = s.substring(firstNewline + 1, lastFence).strip();
            }
        }
        return s;
    }
}
