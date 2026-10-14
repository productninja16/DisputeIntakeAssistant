# Dispute Intake Assistant

A small demo of AI-assisted triage for card fraud and billing disputes at a bank.

A customer describes a problem in free text. An LLM **suggests** a dispute category, lists the
information that is still missing, explains its reasoning and proposes a routing queue. A human
analyst **always** approves or overrides before anything is final.

**Synthetic data only.** There are no real customers and no real card numbers anywhere in this project.

## Why

Analysts spend time reading messy descriptions and chasing missing details. The goal is faster,
more consistent triage, with humans accountable for every decision.

## Guardrails

- The LLM never makes a decision. Its output is validated against a JSON schema, stored in
  `ai_suggestions` (insert-only, enforced by a database trigger) and never applied.
- Only an analyst action creates a row in `decisions`, and every decision records who made it.
- If the model's output fails validation twice, no suggestion is stored and the case goes to the
  analyst unaided.
- Anything that looks like a card number (Luhn-checked) is masked before the text is stored or
  sent to the model.
- Customers see only "In review" or "Reviewed". They never see AI output, overrides or analyst names.

## Stack

Java 21, Spring Boot 3, Maven, PostgreSQL 16 (Docker Compose), Flyway, Thymeleaf with minimal CSS,
and the Anthropic Java SDK. Default model: `claude-haiku-5-5`.

## Quick start

Prerequisites: JDK 21, Maven, Docker.

```sh
docker compose up -d                                   # Postgres on localhost:5433
mvn spring-boot:run -Dspring-boot.run.profiles=seed    # migrates, seeds demo data, starts on :8080
```

The `seed` profile loads 5 synthetic customers with 6 transactions each. It only seeds an empty
database, so restarting is safe. By default suggestions come from a **stub** (simple keyword rules,
no network, no cost).

Then open http://localhost:8080:

1. Pick a demo customer, choose a transaction and describe the problem.
2. Open `/analyst`, sign in as `analyst.a` or `analyst.b`, and review the queue.
3. Approve the suggestion, or override it with your own category, queue and a note.

Both sign-ins are demo shortcuts, not real authentication.

### The three pages

- **Submit a dispute** (customer): choose a transaction, describe the problem. Card-like numbers are masked.
- **Dispute queue** (`/analyst/queue`): one table of all disputes with status (In review, Approved,
  Overridden), the AI's category, its confidence (LOW/MEDIUM/HIGH) and the proposed queue. Filter by
  status and by proposed queue. In-review cases sort oldest first.
- **Review** (`/analyst/disputes/{id}`): the customer's text and transaction on the left; the AI
  suggestion, rationale and missing information on the right. Approve the suggestion, or open
  Override to pick your own category and queue with a required note.

The confidence is the model's own rough self-assessment. It is shown as a hint only and is never used
to decide anything.

### Using the real model

Create a `.env` file in the project root (it is git-ignored; never commit it):

```
ANTHROPIC_API_KEY=sk-ant-...
SUGGESTION_CLIENT=anthropic
```

Restart the app. Each submitted dispute now makes one call (two if the first answer is invalid).
You need credit on your Anthropic account. If the key is blank the app refuses to start in
`anthropic` mode rather than falling back silently.

### Configuration

| Variable | Default | Purpose |
|---|---|---|
| `SUGGESTION_CLIENT` | `stub` | `stub` or `anthropic` |
| `ANTHROPIC_API_KEY` | empty | Required when `SUGGESTION_CLIENT=anthropic` |
| `ANTHROPIC_MODEL` | `claude-haiku-5-5` | Model used by the real client |
| `DB_URL`, `DB_USER`, `DB_PASSWORD` | local Compose database | Database connection |

Real environment variables take precedence over `.env`.

## How it works

```
customer submits  ->  dispute saved (PENDING_REVIEW, card numbers masked)
                          |  after commit, asynchronously
                          v
                  SuggestionEngine: model call -> schema validation -> one retry
                          |  valid                    |  invalid twice
                          v                           v
                  ai_suggestions (insert-only)   no suggestion
                          \                          /
                           v                        v
                        analyst queue: approve or override
                                   |
                                   v
                     decisions row + status APPROVED / OVERRIDDEN
```

- Intake never waits for, or fails because of, the model. Suggestion problems are logged only.
- Approve is tied to the exact suggestion the analyst was shown, so a newer one arriving mid-review
  cannot change what they approved.
- Deciding locks the dispute row, so two analysts cannot both decide the same case.

Internal categories (not card-network reason codes): `UNAUTHORIZED_TRANSACTION`, `DUPLICATE_CHARGE`,
`MERCHANDISE_NOT_RECEIVED`, `INCORRECT_AMOUNT`, `SUBSCRIPTION_CANCELLED`, `OTHER`.
Queues: `FRAUD`, `BILLING_ERRORS`, `MERCHANT_DISPUTES`, `GENERAL_REVIEW`.

## Evaluation

`eval/cases.json` holds 30 synthetic, hand-labelled cases (5 per category, including hard ones: vague
text, a card number, a prompt-injection attempt and a few deliberately ambiguous refund cases).
The runner uses the same engine as the app and prints a report: accuracy, validation outcomes, a
confusion matrix and every miss.

```sh
zsh -ic './scripts/eval.sh'                                        # stub: free and offline
zsh -ic './scripts/eval.sh --client anthropic --confirm-spend'     # real model: costs money
```

`--confirm-spend` is required for the real model. If the first 3 cases all fail with API errors
(bad key, no credit) the run stops with a message instead of reporting a misleading 0%.

| | Stub (keyword rules) | `claude-haiku-5-5` |
|---|---|---|
| Category correct | 16/30 (53%) | 30/30 (100%) |
| Queue correct | 17/30 (57%) | 28/30 (93%) |
| Valid on first try | 30/30 | 30/30 |

Reports: [stub baseline](eval/results/baseline-stub.md), [Haiku run](eval/results/haiku-5-5-run1.md).

The reports also include an "accuracy by confidence" table, to check whether a higher self-reported
confidence really means a more accurate category. The Haiku run above was made **before confidence
was added**, so it has no such table; re-run the real-model eval to get one.

**Read these numbers with care.** The cases and labels were written by one person, are probably
easier than real customer messages, and the real-model figure is a single run. The two queue
misses were a restaurant over-charge (label `BILLING_ERRORS`, model `MERCHANT_DISPUTES`) and a
refund case already flagged as ambiguous. A perfect category score means this set cannot separate
a good setup from a great one; harder cases would be the next improvement.

## Tests

```sh
mvn test
```

Tests use Testcontainers (Docker required) and never call the real API. Coverage includes the
migrations and insert-only trigger, card masking, the schema validator, the retry and failure
paths, the asynchronous suggestion, decisions (approve, override, double decision, locking), access
control for analyst pages, what customers can see, and the eval scoring.

## Project layout

```
src/main/java/com/disputeintake/
  customer/    customers and synthetic transactions
  dispute/     intake, card-number masking, dispute status
  suggestion/  schema validator, stub and Anthropic clients, SuggestionEngine, async listener
  analyst/     queue, case view, DecisionService (the only place decisions are created)
  eval/        case loader, runner, scoring report
  seed/        profile-gated synthetic data seeder
  web/         controllers, sessions, access interceptor
src/main/resources/
  db/migration/          Flyway schema (V1 to V3)
  suggestion-schema.json JSON Schema every model answer must satisfy
eval/        labelled cases and saved reports
scripts/     eval runner script
```

## Known limitations

- Demo sign-in only, for both customers and analysts. A real deployment needs real authentication,
  authorisation and CSRF protection.
- Suggestions run on Spring's default async executor with no persistent queue. If the app stops
  mid-suggestion the case simply has no suggestion.
- Decisions are not linked to a suggestion by an ID column; the eval and any analysis compare a
  decision to the latest suggestion for the same dispute.
- Small by design: no pagination, notifications, case assignment or audit export.
