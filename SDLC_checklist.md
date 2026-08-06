# SDLC Checklist — Transaction Monitoring & Alerts Dashboard

> Live checklist aligned with the current codebase. Sources of truth: **[`README.md`](./README.md)** (overview), **[`FLOW.md`](./FLOW.md)** (deep dive), **[`feature-flagged-organisations.md`](./feature-flagged-organisations.md)** (in-scope next feature).
>
> **Status:** Core MVP (ingest → rules → alerts → lifecycle → dashboard) is **built**. The **Flagged Organisation Monitoring** feature is **in scope** and is the next planned body of work.

## Team Roster & Branch Strategy

| Member | Role | Branch |
|--------|------|--------|
| **Avinash** | Core Backend + Figma Designs | `feat/core-backend` |
| **Dhanush** | Full-Stack (Alert backend + Alert UI) | `feat/alert-case-service` |
| **Rakesh** | Full-Stack (Rules backend + Transactions/Rules UI) | `feat/rules-transactions-ui` |
| **Deepak** | Frontend (Dashboard, Charts, UI polish) | `feat/frontend-ui` |

**Git Workflow:** Branches are created from `develop` (release-only branch is `main`). Feature branches follow `feature/<your-name>-<short-description>` (e.g. `feature/rakesh-rules-page`), and all PRs target `develop`. Commits follow Conventional Commits (`feat:`, `fix:`, `chore:`, `docs:`, `test:`, `refactor:`, `style:`). Do a daily sync to catch conflicts early. (Full details: `GIT_WORKFLOW.md`.)

---

## Project Status at a Glance

| Area | Status |
|------|--------|
| Transaction ingest + rule engine (4 rule types) | ✅ Built |
| Alert lifecycle + de-duplication + audit trail | ✅ Built |
| Transactions / Alerts / AlertDetail / Rules / Dashboard UI | ✅ Built |
| Swagger UI + sample data generator | ✅ Built |
| CI/CD pipeline (GH Actions → Docker/GHCR → Jenkins CD) | ✅ Built |
| **Flagged Organisation Monitoring** | ⏳ **In scope — next iteration** |

---

## 1) Discovery & Requirements

- [x] **Project objective:** Build a real-time rule-based transaction monitoring system that records financial transactions, evaluates them instantly against configurable monitoring rules, raises alerts on suspicious patterns, and lets an operator investigate, acknowledge, close, or dismiss alerts with a full audit trail. `[All — Day 1]`
- [x] Confirm top user journeys:
  - [x] Analyst views and filters transaction history `[Rakesh]`
  - [x] Analyst triages and acknowledges a new alert `[Dhanush]`
  - [x] Analyst investigates, closes, or dismisses an alert with notes `[Dhanush]`
  - [x] Admin adds / edits / toggles / deletes a monitoring rule `[Rakesh]`
  - [x] Analyst views the dashboard (stats, charts, recent alerts) `[Deepak]`
  - [ ] Admin manages the **flagged organisations watchlist** (add / remove) → in scope `[Rakesh]`
  - [ ] Operator reviews a **flagged-org contact alert** (MEDIUM) and a **flagged-org concentration alert** (HIGH) → in scope `[Dhanush]`
- [x] Finalize **Must / Should / Could** feature list `[All — Day 1]`
- [x] Record assumptions and out-of-scope items `[Avinash]`
  - No authentication — single operator assumed (by design).
  - No automatic transaction blocking — the system alerts only, an analyst decides.

---

## 2) Functional Scope Definition

### Must Have (MVP — ✅ built)
- [x] Transaction ingest API — `POST /api/transactions` (synchronous rule evaluation) `[Avinash]`
- [x] Rule engine (Strategy pattern): `AMOUNT_THRESHOLD`, `VELOCITY`, `NEW_PAYEE`, `DAILY_LIMIT` `[Avinash]`
- [x] Rules stored as **data** in DB — configurable at runtime, not hard-coded `[Rakesh]`
- [x] Alert lifecycle: `OPEN → ACKNOWLEDGED → INVESTIGATING → CLOSED / DISMISSED` `[Dhanush]`
- [x] Alert de-duplication — one `OPEN` alert per (rule, account) `[Dhanush]`
- [x] Append-only audit trail per alert via `alert_status_history` `[Avinash]`
- [x] Alert ↔ transaction linking via `alert_transactions` join table `[Avinash]`
- [x] Bulk random transaction generator `POST /api/transactions/generate` `[Avinash]`
- [x] Transactions list UI with filters `[Rakesh]`
- [x] Alerts list UI with filters + context-sensitive actions `[Deepak]`
- [x] Alert detail page (related transactions + status-history timeline) `[Deepak]`
- [x] Rules management UI (add / edit / toggle / delete) `[Rakesh]`
- [x] Dashboard UI: stat cards, severity bar chart, status pie chart, recent alerts `[Deepak]`
- [x] Swagger / OpenAPI documentation `[Avinash]`

### Should Have (⚠️ in scope — next iteration)
- [ ] **Flagged Organisation Monitoring** (see dedicated section below):
  - [ ] `flagged_entities` watchlist table `[Avinash]`
  - [ ] `FLAGGED_PAYEE` rule — MEDIUM alert on any transaction to a flagged payee `[Avinash]`
  - [ ] `FLAGGED_PAYEE_CONCENTRATION` rule — HIGH alert when an account's share of transactions to flagged orgs exceeds a threshold `[Avinash]`
  - [ ] "Flagged Organisations" tab in the Rules page (add / remove watchlist entries) `[Rakesh]`
- [ ] Expanded test coverage (rule evaluators, lifecycle transitions, de-dup, flagged-org rules) `[All]`

### Could Have (Stretch — not planned)
- [ ] Rule simulation — dry-run a rule against historical transactions `[Avinash]`
- [ ] Real-time dashboard updates via WebSockets / SSE `[Dhanush + Deepak]`
- [ ] Async rule evaluation via a queue (currently inline/synchronous) `[Avinash]`
- [ ] CSV export of transaction or alert data `[Rakesh]`
- [ ] Fuzzy organisation-name matching (v1 matches exact `payee_id`) `[Avinash]`
- [ ] Automatic account freezing (v1 alerts only) `[Dhanush]`
- [ ] Cross-account link analysis (supporter-network detection) `[Avinash]`

**Alert severity levels:** `LOW / MEDIUM / HIGH`
**Alert states:** `OPEN → ACKNOWLEDGED → INVESTIGATING → CLOSED / DISMISSED`

---

## 3) Non-Functional Requirements

- [x] Alert evaluation target: < 500 ms per transaction `[Avinash]`
- [x] Every alert status change logged to the audit trail (`alert_status_history`) `[Avinash]`
- [x] Input validation on all write endpoints (Bean Validation → 400) `[Avinash + Dhanush + Rakesh]`
- [x] No authentication required (single operator assumed — by design) `[All]`
- [ ] No secrets hardcoded — dev credentials live in `application.properties` and **must be overridden locally** before sharing/CI `[All]`
- [ ] Standard error contract documented (`400 / 404 / 409 / 500`, body `{timestamp, status, error, message, path}`) — implemented, formalize in docs `[Avinash]`

---

## 4) Architecture & Design

- [x] High-level architecture diagram — see README "High-Level Architecture" `[Avinash — Day 1]`
  - Layered monolith: `Controller → Service → Repository (JdbcTemplate) → MySQL` + dedicated `rules` package (Strategy pattern)
- [x] Data flow documented: Transaction → Rule Evaluation → AlertCandidate → De-dup check → Alert + Audit rows → Dashboard stats — see `FLOW.md` §1–§5 `[Avinash]`
- [x] Sync vs async decision recorded: **synchronous, inline in the HTTP request**; async queue is a stretch `[Avinash]`
- [x] Error paths defined: invalid payload (400), missing resource (404), illegal state transition (409), unexpected (500) `[Avinash]`
- [x] Figma designs for all screens `[Avinash — Day 1–2]`
  - [x] Transactions list + filters
  - [x] Alert dashboard (summary cards + charts + table)
  - [x] Alert detail + actions + timeline
  - [x] Rules management
  - [ ] Flagged Organisations watchlist screen → in scope
- [ ] Flagged-org flow documented in `FLOW.md` (contact + concentration rules, watchlist check) `[Avinash]`

---

## 5) Data Model & Persistence

**Owner: Avinash — schema shared on Day 1 so all branches align. Current schema: 5 tables** (`backend/src/main/resources/schema.sql`).

- [x] `transactions` — `id, account_id, payee_id, amount, type (DEBIT/CREDIT), status (PENDING/COMPLETED/FAILED), description, timestamp` `[Avinash]`
- [x] `monitoring_rules` — `id, name, description, type, severity, active, threshold_amount, transaction_count, time_window_minutes, daily_limit, created_at, updated_at` (typed columns per rule type) `[Rakesh]`
- [x] `alerts` — `id, rule_id (FK), severity (snapshotted), status, description, resolution_notes, created_at, updated_at, acknowledged_at, closed_at` `[Avinash]`
- [x] `alert_transactions` (join) — `(alert_id, transaction_id)` composite PK `[Avinash]`
- [x] `alert_status_history` — `id, alert_id (FK), previous_status, new_status, notes, changed_at` (audit trail, insert-only by design) `[Avinash]`
- [x] Indexes: `transactions(account_id, timestamp)`, `(payee_id)`, `(timestamp)` — support velocity / new-payee / daily-limit queries `[Avinash]`
- [x] Seed data: 4 default active rules in `data.sql` (inserted only if none exist) `[Avinash]`

### In scope — add (flagged organisation feature)
- [ ] `flagged_entities` — `id, entity_name, payee_id, reason, risk_level, active, created_at` (watchlist of anti-social organisations) `[Avinash]`
- [ ] Extend `monitoring_rules.type` with `FLAGGED_PAYEE` and `FLAGGED_PAYEE_CONCENTRATION` (+ any config columns e.g. `concentration_threshold`, `lookback_days`) `[Avinash]`
- [ ] Severity snapshot behaviour unchanged — historical alerts never rewritten on watchlist changes `[Avinash]`

---

## 6) API & Contract Design

**No version prefix — endpoints live under `/api/*`** (Vite dev server proxies `/api` → :8080).

### Transactions (`/api/transactions`)
- [x] `POST /transactions` — ingest + run rule engine inline `[Avinash]`
- [x] `GET /transactions?accountId=&minAmount=&maxAmount=&from=&to=&search=` — list with filters `[Rakesh]`
- [x] `GET /transactions/{id}` `[Avinash]`
- [x] `POST /transactions/generate` — body `{count}` (default 10) `[Avinash]`

### Alerts (`/api/alerts`)
- [x] `GET /alerts?status=&severity=&from=&to=` — list with filters `[Dhanush]`
- [x] `GET /alerts/{id}` — detail with linked transactions + history timeline `[Dhanush]`
- [x] `GET /alerts/stats` — counts by status, alerts today, avg resolution minutes `[Dhanush]`
- [x] `PATCH /alerts/{id}/acknowledge` — OPEN → ACKNOWLEDGED `[Dhanush]`
- [x] `PATCH /alerts/{id}/investigate` — ACKNOWLEDGED → INVESTIGATING `[Dhanush]`
- [x] `PATCH /alerts/{id}/close` — INVESTIGATING → CLOSED, body `{resolutionNotes}` `[Dhanush]`
- [x] `PATCH /alerts/{id}/dismiss` — ACKNOWLEDGED/INVESTIGATING → DISMISSED, body `{resolutionNotes}` `[Dhanush]`

### Rules (`/api/rules`)
- [x] `GET /rules`, `GET /rules/{id}` `[Rakesh]`
- [x] `POST /rules` — create (validation: name, type, severity) `[Rakesh]`
- [x] `PUT /rules/{id}` — full update `[Rakesh]`
- [x] `DELETE /rules/{id}` `[Rakesh]`
- [x] `PATCH /rules/{id}/activate` / `PATCH /rules/{id}/deactivate` `[Rakesh]`

### Dashboard
- [x] `GET /api/dashboard/stats` — open/acknowledged counts, alerts today, avg resolution `[Deepak]`

### In scope — flagged organisations
- [ ] `GET /flagged-entities` — list watchlist entries `[Rakesh]`
- [ ] `POST /flagged-entities` — flag an organisation `{ payeeId, name, reason, riskLevel }` `[Rakesh]`
- [ ] `DELETE /flagged-entities/{id}` — remove from watchlist `[Rakesh]`

### Errors & docs
- [x] `400` invalid payload · `404` resource not found · `409` invalid state transition · `500` other; body `{timestamp, status, error, message, path}` `[Avinash]`
- [x] Swagger / OpenAPI published at **`/swagger-ui.html`** (`springdoc-openapi`) `[Avinash]`
- [ ] Flagged-org endpoints added to Swagger + `FLOW.md` API tables `[Avinash]`

---

## 7) Security & Compliance Basics

- [x] Input validation on all write endpoints `[Avinash + Dhanush + Rakesh]`
- [x] Every alert state change written to `alert_status_history` `[Avinash + Dhanush]`
- [x] Parameterized SQL throughout (JdbcTemplate) — injection-safe `[Avinash]`
- [x] "Single operator, not for production" disclaimer in README `[Avinash]`
- [ ] Move dev DB credentials out of committed `application.properties` into env-configurable values `[Avinash]`

---

## 8) Implementation Notes

### Unique Differentiators — Build These
- [x] **Rules as data** — create / edit / activate / deactivate rules at runtime via Rules page or API; the engine routes by `type` `[Avinash + Rakesh]`
- [x] **Strategy pattern rule engine** — one `@Component` evaluator class per rule type implementing `RuleEvaluator`; `EnumMap<RuleType, RuleEvaluator>` dispatch `[Avinash]`
- [x] **Inline synchronous evaluation** — transaction saved first, then evaluated, so aggregates include the new row (`TransactionService`) `[Avinash]`
- [x] **Alert de-duplication** — only one `OPEN` alert per (rule, account); throttles alert creation, not evaluation `[Dhanush]`
- [x] **Explainability** — every alert carries a generated description, snapshotted with severity at creation `[Avinash]`
- [x] **Alert ↔ transaction linking** — `alert_transactions` join table, populated idempotently (`INSERT IGNORE`) `[Avinash]`
- [x] **Audit trail as a timeline** — `alert_status_history` rendered on AlertDetail `[Dhanush + Deepak]`
- [ ] **Watchlist-based monitoring** (in scope) — flagged-organisation watchlist checked on every transaction, feeding the two new rules `[Avinash + Rakesh]`

### Technical Decisions (Mini ADR)
- [x] Rule engine: Strategy pattern — one class per rule type, easy to extend with new types `[Avinash]`
- [x] Alert evaluation: synchronous on `POST /transactions` (async queue as stretch) `[Avinash]`
- [x] Alert state machine enforced in `AlertService`, not DB constraints `[Dhanush]`
- [x] Audit trail: append-only by design (repository exposes no update/delete) `[Avinash]`
- [ ] Flagged-org rules reuse the same `RuleEvaluator` interface and alert pipeline — no new lifecycle machinery `[Avinash]`

---

## 9) Testing Strategy

> Current state: backend unit tests now cover rule evaluators, TransactionService de-dup/linking, AlertService lifecycle transitions, and GlobalExceptionHandler contracts (see testing_report.md for execution evidence).

| Test | Owner | Status |
|------|-------|--------|
| Unit: each rule type fires correctly (amount/velocity/new-payee/daily-limit) | Avinash | planned |
| Unit: de-dup suppresses second OPEN alert for same (rule, account) | Dhanush | planned |
| Unit: alert state transitions + illegal-transition rejection (409) | Dhanush | planned |
| Integration: transaction -> rule eval -> alert -> history rows | Dhanush | planned |
| Integration: alert detail bundles linked transactions + history | Avinash | planned |
| Negative: bad payloads (400), missing resource (404), invalid state change (409) | Rakesh | planned |
| Performance: 1 000 transactions, measure rule-eval time | Rakesh | planned |
| UAT: full analyst scenario end-to-end | All | planned |
| Flagged-org (in scope): FLAGGED_PAYEE fires on watchlist match; concentration rule fires above threshold; unflagging stops new alerts while history is untouched | Avinash + Dhanush | planned |

Detailed execution logs and evidence are captured in testing_report.md.

---
## 10) CI/CD & Quality Gates

- [x] Backend build via Maven wrapper (`mvnw clean verify` in `.github/workflows/backend-ci.yml`) `[Avinash — Day 1]`
- [ ] Frontend lint via oxlint (`npm run lint`) — script exists locally, **not wired into CI yet** `[Deepak — Day 1]`
- [x] Frontend production build (`npm run build`) wired into the check pipeline — `.github/workflows/frontend-ci.yml` `[Deepak]`
- [x] Backend unit/integration tests run in CI pipeline (frontend has no test suite yet) `[Avinash]`
- [x] Docker images built & pushed to GHCR on push to `main` (`specops-api`, `specops-ui`) `[Avinash + Deepak]`
- [x] Jenkins CD triggered after push (`specops-api-deploy-job`, `specops-ui-deploy-job`) `[Avinash + Deepak]`
- [ ] Branch protection: PR requires passing checks before merge `[Avinash]`
- [ ] Separate dev / demo config (env files, not hardcoded) `[All]`

---

## 11) Monitoring & Operations

- [x] `GET /api/dashboard/stats` — open / acknowledged / alerts today / avg resolution `[Dhanush]`
- [x] Resolution-time metric: `AVG(TIMESTAMPDIFF(MINUTE, created_at, closed_at))` over closed alerts `[Avinash]`
- [ ] `GET /health` endpoint returns service status *(stretch)* `[Avinash]`
- [ ] Correlation / request ID propagated through the pipeline *(stretch)* `[Avinash]`

---

## 12) Documentation

- [x] README: overview, architecture, quick start, API summary, troubleshooting `[Avinash]`
- [x] FLOW.md: DB schema, rule-engine internals, all endpoints, end-to-end scenario `[Avinash]`
- [x] GenerateDataset.md: dummy-data generation guide (backend API + frontend dashboard) `[Avinash]`
- [x] Feature proposal: `feature-flagged-organisations.md` (scenarios A & B, watchlist design, rule semantics) `[All]`
- [x] Swagger / OpenAPI at `/swagger-ui.html` `[Avinash]`
- [ ] Testing summary (what was tested, results) `[Rakesh]`
- [ ] Known limitations + future enhancements kept in sync with README `[All]`
- [ ] Flagged-org feature docs: table, rules, endpoints, and admin workflow in FLOW.md + feature doc `[Avinash]`

---

## 13) Demo & Presentation Readiness

- [x] Demo script with seeded rules + bulk transaction generator `[Avinash + Dhanush]`
- [x] Demonstrate the core lifecycle:
  - [x] Transaction arrives → rule engine fires → alert with explanation
  - [x] Analyst acknowledges → investigates → closes/dismisses with notes
  - [x] Audit trail shows the full status timeline
  - [x] Alert de-dup visible (no alert flooding for the same account)
  - [x] Rules configurable at runtime (toggle / edit)
  - [x] Dashboard stats + charts update
- [ ] Demonstrate flagged-organisation flow (in scope):
  - [ ] Admin flags an organisation on the Rules page
  - [ ] Occasional donor → MEDIUM contact alert (Scenario A)
  - [ ] Concentration account → HIGH alert (Scenario B)
  - [ ] Unflagging stops new alerts; history untouched
- [ ] Build 15–20 min narrative `[All — Day 6]`
- [ ] "What we'd do next" slide: ML anomaly detection, real-time WebSocket updates, rule simulation, async queue `[All]`
- [ ] Backup screenshots in case of demo failure `[Deepak]`
- [ ] Everyone speaks during the presentation `[All]`

---

## 14) Post-Project Retrospective

- [ ] What worked well (team / process / architecture)?
- [ ] What caused rework?
- [ ] Which decisions were most impactful?
- [ ] What would we do differently?
- [ ] Lessons learned for portfolio / interviews

---

## 15) Database Schema & Dummy Data

> **Owner: Avinash — matches `backend/src/main/resources/schema.sql` + `data.sql`. 5 tables today; `flagged_entities` is in scope.**

### Table: `transactions`

| Column | Type | Notes |
|---|---|---|
| id | BIGINT PK | auto-increment |
| account_id | VARCHAR(50) | the customer account |
| payee_id | VARCHAR(50) | the counterparty |
| amount | DECIMAL(15,2) | |
| type | ENUM(DEBIT, CREDIT) | default DEBIT |
| status | ENUM(PENDING, COMPLETED, FAILED) | default COMPLETED |
| description | VARCHAR(255) | free text |
| timestamp | DATETIME | default NOW |

Indexes: `(account_id, timestamp)`, `(payee_id)`, `(timestamp)`.

### Table: `monitoring_rules`

| Column | Type | Notes |
|---|---|---|
| id | BIGINT PK | |
| name | VARCHAR(100) | e.g. "High Value Transaction" |
| description | VARCHAR(255) | |
| type | ENUM(AMOUNT_THRESHOLD, VELOCITY, NEW_PAYEE, DAILY_LIMIT) | strategy selector |
| severity | ENUM(HIGH, MEDIUM, LOW) | default MEDIUM |
| active | BOOLEAN | only `active=true` rules evaluated |
| threshold_amount | DECIMAL(15,2) | AMOUNT_THRESHOLD |
| transaction_count | INT | VELOCITY |
| time_window_minutes | INT | VELOCITY |
| daily_limit | DECIMAL(15,2) | DAILY_LIMIT |
| created_at / updated_at | DATETIME | |

Seed data (4 rules, inserted only if none exist): High Value Transaction (>$10,000, HIGH) · Rapid Transaction Velocity (>5 in 10 min, MEDIUM) · New Payee Transaction (LOW) · Daily Limit Exceeded (>$50,000/day, HIGH).

### Table: `alerts`

| Column | Type | Notes |
|---|---|---|
| id | BIGINT PK | |
| rule_id | BIGINT FK → monitoring_rules.id | the rule that fired |
| severity | ENUM(HIGH/MEDIUM/LOW) | snapshotted from rule at creation |
| status | ENUM(OPEN, ACKNOWLEDGED, INVESTIGATING, CLOSED, DISMISSED) | default OPEN |
| description | VARCHAR(500) | generated message |
| resolution_notes | TEXT | operator notes on close/dismiss |
| created_at / updated_at | DATETIME | |
| acknowledged_at / closed_at | DATETIME | nullable, for resolution-time stats |

### Table: `alert_transactions` *(join table)*

| Column | Type |
|---|---|
| alert_id | BIGINT FK → alerts.id |
| transaction_id | BIGINT FK → transactions.id |
| PK | (alert_id, transaction_id) |

Many-to-many link between alerts and the transactions that triggered them (populated idempotently).

### Table: `alert_status_history` *(audit trail)*

| Column | Type |
|---|---|
| id | BIGINT PK |
| alert_id | BIGINT FK → alerts.id |
| previous_status | VARCHAR(20) |
| new_status | VARCHAR(20) |
| notes | TEXT |
| changed_at | DATETIME |

Every transition writes a row here → rendered as the AlertDetail timeline.

### In scope — Table: `flagged_entities` *(watchlist)*

| Column | Type | Notes |
|---|---|---|
| id | BIGINT PK | |
| entity_name | VARCHAR(120) | e.g. "Global Charity X" |
| payee_id | VARCHAR(50) | matched against `transactions.payee_id` |
| reason | VARCHAR(255) | why it was flagged |
| risk_level | ENUM(LOW, MEDIUM, HIGH) | feeds suggested alert severity |
| active | BOOLEAN | only active entries are checked |
| created_at | DATETIME | |

### Entity Relationship Summary

```
monitoring_rules ──< alerts >── alert_status_history   (audit trail)
      ▲                │
      │  severity       └──< alert_transactions >── transactions
      │  snapshot           (many-to-many)
      │
      └──(type)──► RuleEvaluator (strategy per type) ──► AlertCandidate

flagged_entities (in scope) ──(payee_id match)──► FLAGGED_PAYEE / FLAGGED_PAYEE_CONCENTRATION evaluators
```

---

## In Scope — Flagged Organisation Monitoring

> Feature proposal: **[`feature-flagged-organisations.md`](./feature-flagged-organisations.md)**
>
> Detect customers who send money to anti-social organisations, and identify the organisations themselves. This is a **watchlist-based** extension that reuses the existing rule engine, alert lifecycle, audit trail, and dashboard — **no new alert machinery**.

### The two scenarios it catches

| | Scenario A — "The Occasional Donor" | Scenario B — "The Front Organisation" |
|---|---|---|
| Who | A normal customer | An account set up around the organisation |
| Behaviour | Spends normally but sends a donation to an anti-social org occasionally | Almost all money flows toward the flagged org (e.g. 9 of 10 transactions) |
| Result | MEDIUM alert — flag the *contact* for review | HIGH alert — flag the *account* as likely linked to the org |

### What we add

| New rule type | Covers | How it fires | Severity |
|---|---|---|---|
| **FLAGGED_PAYEE** | Scenario A | Any transaction to an organisation on the watchlist | MEDIUM |
| **FLAGGED_PAYEE_CONCENTRATION** | Scenario B | An account's transactions to flagged orgs exceed a set share (e.g. 50–80%) over a period (e.g. last 30 days) | HIGH |

- **New table:** `flagged_entities` watchlist (payee, name, reason, risk level, active).
- **New UI:** "Flagged Organisations" tab in the Rules page to add/remove watchlist entries.
- **Unchanged:** alert lifecycle, audit trail, de-dup, dashboard — new alerts flow through the existing pipeline.
- **Unflagging** stops new alerts; historical alerts stay untouched (severity/description already snapshotted at creation).
- **Explicitly out of scope for v1:** fuzzy name matching, automatic account freezing, cross-account link analysis.

### Checklist

- [ ] `flagged_entities` table added to `schema.sql` + seed a demo flagged org `[Avinash]`
- [ ] `FLAGGED_PAYEE` / `FLAGGED_PAYEE_CONCENTRATION` added to `RuleType` enum + `monitoring_rules.type` `[Avinash]`
- [ ] `FlaggedPayeeRuleEvaluator` — watchlist match on `payee_id` `[Avinash]`
- [ ] `FlaggedPayeeConcentrationRuleEvaluator` — share-of-flagged-orgs calculation over the lookback window `[Avinash]`
- [ ] Watchlist CRUD: `GET/POST/DELETE /api/flagged-entities` `[Rakesh]`
- [ ] "Flagged Organisations" tab in Rules page (list, add, remove) `[Rakesh]`
- [ ] Alert description includes the matched organisation name/reason `[Avinash]`
- [ ] Tests for both new rules + watchlist CRUD `[Avinash + Dhanush]`
- [ ] Docs updated (FLOW.md schema/API/flow, README feature list) `[Avinash]`
- [ ] Demo scenario A + B run end-to-end `[All]`

---

## Exit Criteria (Project Complete When…)

- [x] All Must-Have features work end-to-end
- [x] Alert lifecycle enforced and tested in the UI + API
- [x] Audit trail complete and queryable via AlertDetail timeline
- [x] Rules configurable at runtime without code changes
- [x] Figma designs match the built UI
- [x] Demo is stable, data is seeded, presentation is ready
- [ ] **Flagged organisation feature (in scope) delivered, tested, and demonstrated**
- [ ] Test coverage expanded beyond the context-load test


