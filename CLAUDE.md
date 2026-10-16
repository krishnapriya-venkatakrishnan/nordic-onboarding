# Nordic Onboarding

## What this is
A portfolio project: account opening for a Nordic savings and investment platform
that launches in a second country without forking the code. Sweden (home market)
and Denmark (new market) run through ONE onboarding flow. Everything that differs
between markets lives in a market module. It is built to show a hiring team three
things: an architecture that scales to more markets, safe and frequent delivery,
and observability of a market launch.

Not affiliated with any bank. No real company names or branding. Product rules
are simplified and illustrative. eID login (BankID, MitID) is simulated.

## The flow
Verify identity -> Your details -> Account type -> Confirmations -> Review -> Open.
Each market supplies: eID provider, national ID rule (Personnummer / CPR number),
currency and locale (SEK sv-SE / DKK da-DK), minimum age, account products
(ISK, Aktie- och fondkonto / Aktiesparekonto, Depot), deposit caps by year,
and confirmations, some scoped to one product.

## Stack
- backend/: Java 21, Spring Boot 3, Maven
- frontend/: Angular 21, standalone components, signals, reactive forms, Vitest
- CI/CD: GitHub Actions; deploy to Google Cloud Run (europe-north1)
- Observability: Cloud Logging, log-based metrics, Cloud Monitoring dashboards and alerts

## Architecture rules (do not break these)
1. backend/.../domain is plain Java: no Spring or other framework imports.
2. No country-specific code outside backend/.../markets/<code>/. Core code and the
   Angular app never branch on a market code. The UI renders what the market
   config describes.
3. Shared policy (identity verified, valid ID, minimum age, product offered,
   required confirmations) lives once in OnboardingService. Markets only supply data
   and parsing rules.
4. Rules are data: deposit caps are a dated map, not if-statements.
5. A market is live only if its code is in MARKETS_ENABLED. Built-but-disabled
   markets return 404 (dark launch).
6. Time comes from an injected Clock, so date and age logic is testable.
7. National ID numbers never appear in logs, metrics or error traces. Mask them.
8. Every new market must pass MarketContractTest.

## Reference implementation
A working reference copy is at: ../nordic-onboarding-reference
Port and adapt from it. Do not invent a different design. Its Spring layer has
never been run, so expect small fixes there.

## How we work
- The plan is a numbered list of steps. Each step = one commit. I will give you
  one step at a time. Do only that step. Do not run ahead.
- Every commit includes its tests and leaves the build green:
  backend: cd backend && mvn verify
  frontend: cd frontend && npx ng test --watch=false && npx ng build
- Before finishing a step: run the checks above, then show me a short summary of
  what changed and anything you were unsure about. I review the diff and commit.
- If a step is ambiguous or conflicts with these rules, ask before writing code.
- Keep changes small and readable. No new dependencies without saying why.
- Plain, short code comments that explain why, not what.

## Stages (for orientation)
0. Repo setup: this file, .gitignore, README stub.
1. Backend skeleton + CI running mvn verify on every push.
2. Domain: Market interface, Personnummer and CPR rules, Sweden and Denmark
   markets, contract test, registry, OnboardingService, REST API.
3. Frontend: Angular app, API client, market switcher and config panel,
   config-driven flow, component tests, styling (mobile, dark mode, a11y).
4. Ship: Dockerfile (Angular bundled into the Spring jar), Cloud Run deploy on
   green main, no-traffic revision + smoke test + traffic shift.
5. Observability: structured JSON logs (market, step, outcome; IDs masked),
   onboarding events, log-based metrics and dashboard as code, alerts, hourly
   synthetic sign-up, manual "toggle market" kill-switch workflow.
6. Story: final README, optional PWA.

## Out of scope
Real eID integration, persistence/database, authentication, payments, real legal
or tax text, any real customer data.
