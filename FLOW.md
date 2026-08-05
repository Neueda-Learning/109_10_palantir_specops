# Transaction Monitoring System — Complete Flow

## 1. Architecture at a glance

```
┌──────────────────────────┐
│  React SPA (Vite :5173)  │  5 pages + navbar, calls axios
│  BrowserRouter routes    │
└───────────┬──────────────┘
            │  /api/* proxied by Vite dev server
┌───────────▼──────────────┐
│  Spring Boot REST (:8080)│
│  Controller → Service →  │
│  Repository(JdbcTemplate)│
│  + RuleEngine (Strategy) │
└───────────┬──────────────┘
┌───────────▼──────────────┐
│  MySQL (transaction_     │
│  monitoring db)          │
│  5 tables (below)        │
└──────────────────────────┘
```

Flow is **synchronous and REST-driven** (no queues). Rule evaluation happens *inline* inside the same HTTP request that creates a transaction.

---

## 2. Database schema (5 tables)

### `transactions`
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

Indexes: `(account_id, timestamp)`, `(payee_id)`, `(timestamp)` — these speed up the velocity/new-payee/daily-limit SQL.

### `monitoring_rules`
| Column | Type | Notes |
|---|---|---|
| id | BIGINT PK | |
| name | VARCHAR(100) | e.g. "High Value Transaction" |
| description | VARCHAR(255) | |
| type | ENUM(AMOUNT_THRESHOLD, VELOCITY, NEW_PAYEE, DAILY_LIMIT) | drives which evaluator runs |
| severity | ENUM(HIGH, MEDIUM, LOW) | default MEDIUM |
| active | BOOLEAN | only `active=true` rules are evaluated |
| threshold_amount | DECIMAL(15,2) | used by AMOUNT_THRESHOLD |
| transaction_count | INT | used by VELOCITY |
| time_window_minutes | INT | used by VELOCITY |
| daily_limit | DECIMAL(15,2) | used by DAILY_LIMIT |
| created_at / updated_at | DATETIME | |

One `type` column is the "strategy selector" — the engine routes to a matching evaluator class.

### `alerts`
| Column | Type | Notes |
|---|---|---|
| id | BIGINT PK | |
| rule_id | BIGINT **FK → monitoring_rules.id** | the rule that fired |
| severity | ENUM(HIGH/MEDIUM/LOW) | **copied from rule at creation** (snapshot) |
| status | ENUM(OPEN, ACKNOWLEDGED, INVESTIGATING, CLOSED, DISMISSED) | default OPEN |
| description | VARCHAR(500) | generated message |
| resolution_notes | TEXT | operator notes on close/dismiss |
| created_at / updated_at | DATETIME | |
| acknowledged_at / closed_at | DATETIME | nullable, used for resolution-time stats |

### `alert_transactions` (join table → the heart of the linking)
| Column | Type |
|---|---|
| alert_id | BIGINT **FK → alerts.id** |
| transaction_id | BIGINT **FK → transactions.id** |
| PK | (alert_id, transaction_id) |

This is a **many-to-many** link: one alert can reference several transactions, one transaction can appear in several alerts. It's populated with `INSERT IGNORE` so it's idempotent.

### `alert_status_history` (audit trail)
| Column | Type |
|---|---|
| id | BIGINT PK |
| alert_id | BIGINT **FK → alerts.id** |
| previous_status | VARCHAR(20) |
| new_status | VARCHAR(20) |
| notes | TEXT |
| changed_at | DATETIME |

Every alert transition writes a row here → the UI renders it as a timeline.

---

## 3. How RULES, ALERTS and TRANSACTIONS are linked

```
monitoring_rules ──(rule_id)──┐
      ▲                       │ 1:N
      │  severity copied at   │
      │  alert creation       ▼
      │                   alerts
      │                       │ 1:N  (audit trail)
      │                       ▼
      │             alert_status_history
      │
      └──(type)──▶ RuleEvaluator (strategy per type)
                           │ produces
                           ▼
                   AlertCandidate
                           │
   transactions ◀──(transaction_id)─── alert_transactions ──(alert_id)──▶ alerts
```

Key links in plain words:
- A **rule** *fires* → produces an **alert** (alert.rule_id → rule.id). The alert keeps its own copy of severity/description so later rule edits never rewrite history.
- An **alert** *points to* the transaction(s) that triggered it through the `alert_transactions` join table.
- Every alert status change is logged in **alert_status_history**.
- One rule can spawn many alerts; one alert can point to many transactions (useful when a rule like VELOCITY is conceptually about a batch, though this implementation currently links just the triggering transaction).

---

## 4. The Rule Engine (deep dive)

`RuleEngineService` (`rules/RuleEngineService.java`) uses the **Strategy pattern**:
- It loads **all active rules** from `monitoring_rules WHERE active=TRUE`.
- For each rule, looks up its evaluator in an `EnumMap<RuleType, RuleEvaluator>`.
- Calls `evaluator.evaluate(tx, rule)` → returns `Optional<AlertCandidate>`.

`AlertCandidate` = `{ ruleId, ruleName, severity, description, triggeringTransactionIds }`.

The four evaluators (each is a `@Component` implementing `RuleEvaluator`):

| Evaluator | Condition (SQL behind it) | Note |
|---|---|---|
| **AmountThresholdRuleEvaluator** | `amount > threshold_amount` | Pure comparison, no DB lookups |
| **VelocityRuleEvaluator** | `COUNT(*) WHERE account_id=? AND timestamp > NOW() - INTERVAL ? MINUTE` | Window is *now*, so it includes the just-inserted tx; counts all types |
| **NewPayeeRuleEvaluator** | `COUNT(*) WHERE account_id=? AND payee_id=? AND timestamp < tx.timestamp` | Looks *before* the current tx, so the fresh row isn't counted → count 0 = new payee |
| **DailyLimitRuleEvaluator** | `SUM(amount) WHERE account_id=? AND type='DEBIT' AND DATE(timestamp)=CURDATE()` | Only DEBITs, today; includes the just-inserted row; `COALESCE(...,0)` |

**Critical ordering:** in `TransactionService.create()`, the transaction is **saved first, then evaluated** (`TransactionService.java:49-51`). So every aggregate (velocity count, daily sum) naturally includes the new transaction.

---

## 5. Alert generation & de-duplication (the glue)

In `TransactionService.create()` after evaluation:
1. For each candidate: check `alertRepository.existsOpenAlertForRule(ruleId, accountId)` — SQL joins alerts → alert_transactions → transactions to find any **OPEN** alert for the *same rule + same account*.
2. If one exists → **skip** (this is the built-in alert-throttling / de-dup: prevents 100 velocity alerts for the same account while one is still open).
3. Otherwise: insert `alerts` row (status=OPEN, severity/description from candidate) → insert join rows in `alert_transactions` for every `triggeringTransactionIds` → insert `alert_status_history` row (`previous=null, new=OPEN`, note "Alert auto-generated by rule: <name>").
4. The transaction response returns `alertIds` so the caller knows what fired.

---

## 6. API surface (all endpoints)

**Transactions** (`/api/transactions`)
| Method | Path | Purpose |
|---|---|---|
| POST | `/api/transactions` | create + run rule engine inline |
| GET | `/api/transactions` | list; filters: `accountId, from, to, minAmount, maxAmount, search` (LIKE on desc/account/payee), ordered `timestamp DESC` |
| GET | `/api/transactions/{id}` | single tx, 404 via `ResourceNotFoundException` |
| POST | `/api/transactions/generate` | body `{count}` (default 10); random accounts/payees, amount 100–20100, each goes through full `create()` |

**Alerts** (`/api/alerts`)
| Method | Path | Purpose |
|---|---|---|
| GET | `/api/alerts` | list; filters: `status, severity, from, to` |
| GET | `/api/alerts/stats` | counts by status + alertsToday + avg resolution minutes |
| GET | `/api/alerts/{id}` | detail: alert + rule name + linked transactions + history timeline |
| PATCH | `/api/alerts/{id}/acknowledge` | OPEN → ACKNOWLEDGED (no body) |
| PATCH | `/api/alerts/{id}/investigate` | ACKNOWLEDGED → INVESTIGATING (no body) |
| PATCH | `/api/alerts/{id}/close` | INVESTIGATING → CLOSED, body `{resolutionNotes}` |
| PATCH | `/api/alerts/{id}/dismiss` | ACKNOWLEDGED/INVESTIGATING → DISMISSED, body `{resolutionNotes}` |

**Rules** (`/api/rules`)
| Method | Path | Purpose |
|---|---|---|
| GET | `/api/rules` | list all, ordered `created_at DESC` |
| GET | `/api/rules/{id}` | single rule |
| POST | `/api/rules` | create (validation: name, type, severity required) |
| PUT | `/api/rules/{id}` | full update |
| DELETE | `/api/rules/{id}` | delete |
| PATCH | `/api/rules/{id}/activate` | `active=true` |
| PATCH | `/api/rules/{id}/deactivate` | `active=false` |

**Dashboard** (`/api/dashboard`)
| Method | Path | Purpose |
|---|---|---|
| GET | `/api/dashboard/stats` | same `DashboardStatsResponse` as alerts/stats (delegates to `AlertService.getStats()`) |

**Validation & errors** (`GlobalExceptionHandler`):
- 400 — `MethodArgumentNotValidException` (e.g. missing accountId, amount ≤ 0)
- 404 — `ResourceNotFoundException` ("Transaction not found: id")
- 409 — `InvalidStateTransitionException` (illegal status change)
- 500 — anything else; all return `{timestamp, status, error, message, path}`.

**Alert state machine** (enforced in `AlertService`):
```
OPEN ──acknowledge──▶ ACKNOWLEDGED ──investigate──▶ INVESTIGATING ──close──▶ CLOSED
                         │                              │
                         └──────── dismiss ─────────────┘──▶ DISMISSED
```
Wrong transition → 409 (e.g. trying to close an OPEN alert). Closed/dismissed are terminal.

---

## 7. Frontend flow (React Router)

`App.jsx` routes: `/` Dashboard, `/transactions`, `/alerts`, `/alerts/:id`, `/rules`. Navbar links all 4 sections. Services in `frontend/src/services/*.js` are thin axios wrappers over `api.js` (`baseURL: '/api'`, proxied to :8080).

- **Dashboard.jsx** — loads `GET /dashboard/stats` + `GET /alerts` in parallel (`Promise.all`), shows 4 stat cards (open, acknowledged, alertsToday, avg resolution), severity bar chart + status pie chart (recharts), and 10 recent alerts (row click → `/alerts/{id}`).
- **Transactions.jsx** — table with client-side sorting, search/filter form, "Generate 10 Random" (`POST /generate`), and Create-Transaction modal (`POST /transactions`) that notes "rules will be evaluated".
- **Alerts.jsx** — filter form (status/severity/date), desktop table + mobile cards, and context-sensitive action buttons (`ActionButtons`): OPEN→Acknowledge, ACKNOWLEDGED→Investigate/Dismiss, INVESTIGATING→Close/Dismiss. Close/Dismiss open a `NotesModal` for resolution notes.
- **AlertDetail.jsx** — fetches `GET /alerts/{id}`; renders alert meta, action buttons with a notes textarea, **Related Transactions** table (from `alert.transactions`), and the **Status History** timeline (from `alert.history`).
- **Rules.jsx** — table of rules with dynamic parameter display (`> $X`, `> N txns / M min`, `> $Y / day`), toggle switch (activate/deactivate), edit/delete, and an Add/Edit modal with **type-dependent fields** (threshold amount / count+window / daily limit).

---

## 8. End-to-end user scenario (walk-through)

**Setup — seed data** (`data.sql`) inserts 4 active rules at startup: High Value (>$10k, HIGH), Rapid Velocity (>5 in 10 min, MEDIUM), New Payee (LOW), Daily Limit (>$50k/day debits, HIGH). Empty `transactions`/`alerts` tables.

**Step 1 — Landing page.** Operator opens `http://localhost:5173/`. Dashboard fires `GET /api/dashboard/stats` and `GET /api/alerts`. Stats: open=0, acked=0, today=0, avg=—. Empty "Recent Alerts".

**Step 2 — Creating a rule.** Operator goes to **Rules**, clicks "+ Add Rule", picks type `VELOCITY`, name "Rapid 3 in 2 min", count=3, window=2, severity=LOW, active. → `POST /api/rules` → `MonitoringRuleRepository.save()` inserts it. Now `findAllActive()` returns 5 rules. (This shows rules are *data*, configurable at runtime.)

**Step 3 — Submitting a transaction that fires multiple rules.** On **Transactions**, "Create Transaction": `accountId=ACC-001, payeeId=PAYEE-NEW-1, amount=15000, type=DEBIT, status=COMPLETED`. → `POST /api/transactions`.

Inside `TransactionService.create()`:
1. Insert transaction (gets `id`, timestamp=now).
2. `ruleEngineService.evaluate(tx)` loads the 5 active rules:
   - **AmountThreshold**: 15000 > 10000 ✅ candidate (HIGH).
   - **Velocity**: counts ACC-001 txs in last 10 min = 1 → not > 5 ❌.
   - **NewPayee**: count of ACC-001→PAYEE-NEW-1 before now = 0 ✅ candidate (LOW).
   - **DailyLimit**: ACC-001 today's DEBIT sum = 15000 → not > 50000 ❌.
   - **Custom velocity rule**: 1 not > 3 ❌.
3. Two candidates → de-dup check → no OPEN alerts exist → create **two alerts**, each linked to the tx via `alert_transactions`, each with an OPEN history row.
4. Response: `{...tx fields, alertIds:[1,2]}`.

So one transaction produced two alerts: alert #1 (HIGH, "exceeds threshold"), alert #2 (LOW, "new payee").

**Step 4 — The velocity alert via rapid submission.** Operator submits 3 quick transactions of $100 from ACC-002 to PAYEE-B. First two: velocity count (2, then 3) not > 5 (or >3 for custom rule). Third tx: the new custom rule's count (3) > 3 ✅ → candidate. And after 4–6 submissions the built-in velocity rule (5 in 10 min) also fires. Only **one** OPEN velocity alert per account is created due to de-dup — subsequent rapid txs while it's OPEN are skipped. Note velocity/daily-limit rules only fire on new txs that push the running total over the line.

**Step 5 — Bulk data.** "⚡ Generate 10 Random" → `POST /api/transactions/generate {count:10}` → loops `create()` for each. Because amounts are random 100–20100, ~half exceed $10k and fire the amount rule; "PAYEE-NEW-1/2" fire new-payee; ACC-001 could hit $50k/day debits → daily-limit alert. Alerts page now fills up.

**Step 6 — Investigating an alert.** Operator goes to **Alerts**, filters `severity=HIGH`. Sees alert #1. Clicks "✓ Acknowledge" → `PATCH /api/alerts/1/acknowledge` → status→ACKNOWLEDGED, `acknowledged_at` set, history row written (OPEN→ACKNOWLEDGED). Clicks "🔍 Investigate" → `PATCH /api/alerts/1/investigate` → INVESTIGATING. Clicks "✓ Close", types "Customer confirmed legitimate purchase" → `PATCH /api/alerts/1/close {resolutionNotes}` → CLOSED, `closed_at` set, notes saved, history row (INVESTIGATING→CLOSED).

**Step 7 — Alert detail.** Operator clicks alert #1 → `GET /api/alerts/1`. Response bundles: alert fields + `ruleName` ("High Value Transaction", joined from rule repo) + `transactions` (fetched via `alert_transactions` then `findByIds`, showing the $15,000 tx) + `history` (the 4 timeline entries: auto-generated OPEN → ACKNOWLEDGED → INVESTIGATING → CLOSED). The status timeline renders "Created as OPEN" then the arrows.

**Step 8 — Dismiss a false positive.** Alert #2 (new payee) → Acknowledge → Dismiss with notes "Known payee, false positive" → `PATCH /api/alerts/2/dismiss` → DISMISSED (terminal).

**Step 9 — Illegal transition guard.** Operator tries to Acknowledge the now-CLOSED alert #1 → backend throws `InvalidStateTransitionException` → **409** with message "Cannot acknowledge alert in status: CLOSED. Must be OPEN." Shown in the UI via the error banner.

**Step 10 — Dashboard reflects everything.** Back on `/`, stats now show open/acknowledged counts from the day's activity, `alertsToday` > 0, and `avgResolutionMinutes` = `AVG(TIMESTAMPDIFF(MINUTE, created_at, closed_at))` over closed alerts. Recent Alerts lists the latest 10.

**Step 11 — Rule governance.** Operator deactivates the velocity rule (toggle → `PATCH /api/rules/{id}/deactivate`) or deletes it (`DELETE /api/rules/{id}`). From then on `findAllActive()` excludes it, so **no new alerts** are generated by that rule — but existing alerts and their history are untouched (rules and alerts are decoupled after creation).

---

## 9. Design notes / gotchas

- **No auth, single operator** — by design (see `transaction_monitoring.md`).
- **Synchronous evaluation** — each POST /generate of N runs the full engine N times; the spec's appendix suggests async via a queue, but this build is inline.
- **De-dup is per (rule, account) while OPEN** — it throttles *alert creation*, not rule evaluation.
- **Severity is snapshotted** onto the alert, and `ruleName` is joined at read time (so renaming a rule updates the label on all its historical alerts).
- **`avgResolutionMinutes`** only counts alerts with `closed_at` (closed ones), not dismissed.
- **Indexes** exist specifically for the three window/aggregate queries used by the evaluators.
