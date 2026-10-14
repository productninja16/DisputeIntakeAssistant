package com.disputeintake.eval;

import com.disputeintake.suggestion.AnthropicSuggestionClient;
import com.disputeintake.suggestion.StubSuggestionClient;
import com.disputeintake.suggestion.SuggestionClient;
import com.disputeintake.suggestion.SuggestionContext;
import com.disputeintake.suggestion.SuggestionEngine;
import com.disputeintake.suggestion.SuggestionValidator;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Runs the labelled cases through the same engine the app uses and writes a report.
 *
 * <pre>
 * --client stub|anthropic   default stub (free, offline)
 * --confirm-spend           required with --client anthropic; the real model costs money
 * --cases PATH              default eval/cases.json
 * --out DIR                 default eval/results
 * --name NAME               report file name (default: timestamp-client)
 * </pre>
 */
public final class EvalRunner {

    private EvalRunner() {
    }

    public static void main(String[] args) throws Exception {
        String client = "stub";
        boolean confirmed = false;
        Path casesFile = Path.of("eval/cases.json");
        Path outDir = Path.of("eval/results");
        String name = null;
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--client" -> client = args[++i];
                case "--confirm-spend" -> confirmed = true;
                case "--cases" -> casesFile = Path.of(args[++i]);
                case "--out" -> outDir = Path.of(args[++i]);
                case "--name" -> name = args[++i];
                default -> throw new IllegalArgumentException("Unknown argument: " + args[i]);
            }
        }
        List<EvalCase> cases = EvalCaseLoader.load(casesFile);
        ObjectMapper mapper = new ObjectMapper();
        SuggestionEngine engine = new SuggestionEngine(newClient(client, confirmed, cases.size(), mapper),
                new SuggestionValidator(mapper));

        List<CaseOutcome> outcomes = new ArrayList<>();
        for (EvalCase c : cases) {
            var outcome = engine.run(new SuggestionContext(
                    c.description(), c.merchantName(), c.amountCents(), c.postedOn()));
            outcomes.add(new CaseOutcome(c, outcome.result(), outcome.attempts()));
            System.out.printf("%s %s%n", c.id(), outcome.succeeded() ? outcome.result().category() : "NO_SUGGESTION");
        }

        Instant now = Instant.now();
        String markdown = new EvalReport(outcomes).toMarkdown(client, engine.modelName(), now);
        Files.createDirectories(outDir);
        String fileName = name != null ? name
                : DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").withZone(ZoneOffset.UTC).format(now) + "-" + client;
        Path out = outDir.resolve(fileName + ".md");
        Files.writeString(out, markdown);
        System.out.println();
        System.out.println(markdown);
        System.out.println("Saved " + out);
    }

    private static SuggestionClient newClient(String client, boolean confirmed, int calls, ObjectMapper mapper) {
        switch (client) {
            case "stub":
                return new StubSuggestionClient(mapper);
            case "anthropic":
                if (!confirmed) {
                    throw new IllegalArgumentException("--client anthropic makes " + calls
                            + " real API calls (a few cents). Re-run with --confirm-spend to proceed.");
                }
                String key = System.getenv("ANTHROPIC_API_KEY");
                String model = System.getenv().getOrDefault("ANTHROPIC_MODEL", "claude-haiku-5-5");
                return new AnthropicSuggestionClient(key, model); // fails fast if the key is blank
            default:
                throw new IllegalArgumentException("Unknown client: " + client + " (use stub or anthropic)");
        }
    }
}
