# Dispute Intake Assistant

Read this before every task.

## Product
"Dispute Intake Assistant" for card fraud and billing disputes at a bank.
A customer describes a problem in free text. An LLM suggests a dispute category,
lists missing information, explains its reasoning, and proposes a routing queue.
A human analyst ALWAYS approves or overrides before anything is final.

## Why
Analysts spend time reading messy descriptions and chasing missing details.
Goal: faster, more consistent triage, with humans accountable for decisions.

## Stack
Java 21 (Temurin), Spring Boot 3, Maven, PostgreSQL in Docker Compose,
Anthropic Java SDK for the LLM, simple server-rendered UI (Thymeleaf + minimal CSS).

## Constraints
- Synthetic data only. No real customer data, no real card numbers.
- Use simple internal dispute categories, not card network reason codes.
- Every LLM output must be structured JSON validated against a schema.
- The LLM never makes a final decision; it only suggests.
- Keep it small enough to finish in 2–3 weeks. Prefer simple over clever.

## Working agreements
- Work in small steps. Before writing code, tell the owner the plan in a few bullets and wait for an OK.
- Small commits, one logical change each.
- Secrets live in `.env` (git-ignored). Never print or commit the API key. Don't call the Anthropic API unless asked.

## Steps
0. CLAUDE.md
1. Skeleton: Maven, Spring Boot, Compose (Postgres 16), Flyway schema, smoke test
2. Customer intake form + synthetic-data seeder
3. LLM suggestion service: JSON schema, validation, one retry, stub implementation first
4. Analyst queue: approve or override
5. Evaluation set (~30 synthetic descriptions) + accuracy script
6. README

## Guardrails
- LLM output is validated, stored in `ai_suggestions` (insert-only) and never applied. Only an analyst action creates a `decisions` row.
- If validation fails twice, the case goes to the analyst with no suggestion.
- Mask anything that looks like a card number before text is sent to the LLM.

## Local environment
- JDK 21 is set in `~/.zshrc`; non-interactive shells don't read it, so run Maven via `zsh -ic '...'`.
- Default model: `claude-haiku-5-5`, configurable via `anthropic.model`.

## Decision log
- Project lives in `DisputeIntakeAssistant`, separate from the earlier DisputeTracker project.
- Default model `claude-haiku-5-5`.
