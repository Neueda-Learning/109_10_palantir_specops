# Transaction Monitoring & Alerts Dashboard

> Real-time rule-based transaction monitoring with an interactive dashboard, alert lifecycle management, and full audit trail.

A full-stack application that records financial transactions, evaluates them **instantly** against configurable monitoring rules, raises alerts when suspicious patterns are detected, and lets an operator investigate, acknowledge, close, or dismiss those alerts from a web dashboard.

Deep-dive flow documentation lives in **[`FLOW.md`](./FLOW.md)** — this README is the friendly entry point.

---

## The Problem It Solves

Banks and fintechs must detect suspicious activity (fraud, money laundering, unusual spend) in real time. Manually reviewing every transaction is impossible. This project automates detection:

1. Every transaction that enters the system is checked against **rules** (e.g. "amount > $10,000", "more than 5 payments in 10 minutes", "first payment to a new payee", "daily spend > $50,000").
2. When a rule fires, an **alert** is created and linked to the transaction(s) that triggered it.
3. An **operator** works the alert through a strict lifecycle — acknowledge → investigate → close (or dismiss as false positive) — with every step recorded in an audit trail.

---

## Main Features

- **Rule engine** — 4 pluggable rule types (Strategy pattern), rules stored as *data* in the database, fully configurable at runtime.
- **Instant alert generation** — creating a transaction synchronously evaluates all active rules.
- **Alert de-duplication** — only one `OPEN` alert per (rule, account) pair, preventing alert flooding.
- **Alert lifecycle** — `OPEN → ACKNOWLEDGED → INVESTIGATING → CLOSED/DISMISSED` with strict state validation.
- **Audit trail** — every status change logged in `alert_status_history`.
- **Transaction generation** — one-click bulk random transaction generator for demos and testing.
- **Rich dashboard** — stat cards, severity bar chart, status pie chart, recent alerts.
- **Filtering & search** — transactions and alerts filterable by account, amount, date, status, severity, free-text.
- **Swagger UI** — auto-generated interactive API documentation.

---

## Technology Stack

| Layer | Technology |
|---|---|
| **Backend** | Java 25 · Spring Boot 4 · JDBC (`JdbcTemplate`) · Bean Validation |
| **Database** | MySQL 8+ (raw SQL, no ORM) |
| **Frontend** | React 18/19 · Vite 8 · Tailwind CSS v4 · React Router · Axios · Recharts |
| **API docs** | Swagger UI / OpenAPI (springdoc-openapi) |
| **Testing** | JUnit (Spring Boot test starter) |
| **Frontend lint** | oxlint |
| **Build tools** | Maven (via `mvnw` wrapper) · npm |

---

## High-Level Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                        BROWSER                               │
│   React SPA (http://localhost:5173)                          │
│   Dashboard · Transactions · Alerts · AlertDetail · Rules    │
└──────────────────────────┬──────────────────────────────────┘
                           │  /api/*  (Vite dev server proxies)
                           ▼
┌─────────────────────────────────────────────────────────────┐
│              SPRING BOOT REST API (:8080)                    │
│                                                              │
│   Controller ──▶ Service ──▶ Repository ──▶ MySQL            │
│   (HTTP layer)   (business    (JDBC/SQL)     (5 tables)      │
│                    logic)                                    │
│                       │                                      │
│                       ▼                                      │
│               RuleEngine (Strategy pattern)                  │
│               AmountThreshold / Velocity /                   │
│               NewPayee / DailyLimit                          │
└─────────────────────────────────────────────────────────────┘
```

- **Layered, client-server, monolithic** application.
- Layering: `controller` → `service` → `repository` → database, plus a dedicated `rules` package for the engine.
- Rule evaluation happens **inline inside the HTTP request** that creates a transaction (synchronous, no message queue).

---

## Repository Structure

```text
monitoring-master/
├── backend/                          # Spring Boot REST API
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/transaction/transaction_monitoring/
│   │   │   │   ├── controller/       # HTTP endpoints (Transactions, Alerts, Rules, Dashboard)
│   │   │   │   ├── service/          # Business logic (TransactionService, AlertService, RuleService)
│   │   │   │   ├── rules/            # Rule engine: RuleEvaluator interface + 4 strategies + engine
│   │   │   │   ├── repository/       # JDBC data access (Transaction, MonitoringRule, Alert)
│   │   │   │   ├── model/            # Plain data classes (Transaction, MonitoringRule, Alert, ...)
│   │   │   │   ├── dto/              # Request/response objects
│   │   │   │   ├── enums/            # AlertStatus, RuleType, Severity, TransactionType/Status
│   │   │   │   ├── exception/        # Custom exceptions + global error handler
│   │   │   │   └── DataSeeder.java   # Standalone JDBC seeder — inserts 1000 demo transactions + alerts
│   │   │   └── resources/
│   │   │       ├── schema.sql        # Creates the 5 DB tables
│   │   │       ├── data.sql          # Seeds 4 default monitoring rules
│   │   │       └── application.properties  # DB connection + server + Swagger config
│   │   └── test/                     # Spring context-load test
│   ├── pom.xml                       # Maven build & dependencies (includes exec-maven-plugin)
│   └── mvnw.cmd / mvnw               # Maven wrapper
├── frontend/                         # React + Vite SPA
│   ├── src/
│   │   ├── main.jsx                  # React bootstrap (entry point)
│   │   ├── App.jsx                   # Router + layout
│   │   ├── pages/                    # Dashboard, Transactions, Alerts, AlertDetail, Rules
│   │   ├── components/               # Navbar, SeverityBadge, StatusBadge, LoadingSpinner, ErrorMessage
│   │   ├── services/                 # Axios wrappers (api.js + one service per resource)
│   │   └── index.css                 # Tailwind entry
│   ├── vite.config.js                # Dev server + /api proxy → :8080
│   └── package.json                  # npm scripts & dependencies
├── README.md                         # This file
├── FLOW.md                           # Deep-dive: schema, engine, endpoints, end-to-end scenario
├── transaction_monitoring.md         # Original training brief / requirements
└── GIT_WORKFLOW.md                   # Team branch & PR workflow
```

> `backend/target/` and `frontend/node_modules/` are build output / dependencies and are not part of the source.

---

## Application Flow Summary

```
Operator opens Dashboard
        │
        ▼
GET /api/dashboard/stats  ──▶ alert counts + alerts today + avg resolution
GET /api/alerts           ──▶ 10 most recent alerts
        │
        ▼
Transaction arrives (manual form, API, or "Generate 10 Random")
        │
        ▼
POST /api/transactions
        │
        ├─ 1. Save transaction to `transactions` table
        ├─ 2. RuleEngine.evaluate(tx)  ──▶ loop ALL active rules
        │      each evaluator returns AlertCandidate or nothing
        ├─ 3. For each candidate: de-dup check (OPEN alert for same rule+account?)
        │      if none → create `alerts` row (status OPEN)
        │               → link tx via `alert_transactions`
        │               → write `alert_status_history` (→OPEN)
        └─ 4. Response includes alertIds that fired
        │
        ▼
Operator works the alert (Alerts page / Alert detail)
   OPEN ──acknowledge──▶ ACKNOWLEDGED ──investigate──▶ INVESTIGATING ──close──▶ CLOSED
                            │                              │
                            └───────── dismiss ────────────┘ ──▶ DISMISSED
        │
        ▼
Every transition writes alert_status_history; dashboard stats update
```

---

## How Rules, Alerts and Transactions Are Linked

```
monitoring_rules ──(rule_id)────▶ alerts  ◀──(alert_id)──── alert_transactions
       ▲                                │        (join table)       │
       │  severity copied at            │                           │
       │  alert creation                └──(alert_id)──▶ alert_status_history
       │                                                          (audit trail)
       │
       └──(type)──▶ RuleEvaluator (strategy per type)
                          │  produces
                          ▼
                  AlertCandidate ──(transaction_id)────▶ transactions
```

| Relationship | Meaning | Where |
|---|---|---|
| rule **1:N** alert | one rule fires many alerts | `alerts.rule_id` FK |
| alert **N:M** transaction | an alert points to the tx(s) that triggered it | `alert_transactions` join table |
| alert **1:N** history | every status change is an audit row | `alert_status_history.alert_id` FK |

**Key detail:** when an alert is created it *snapshots* the rule's severity + a generated description, so later rule edits never rewrite historical alerts.

---

## Alert Lifecycle

```
OPEN ──acknowledge──▶ ACKNOWLEDGED ──investigate──▶ INVESTIGATING ──close──▶ CLOSED
                         │                              │
                         └──────── dismiss ─────────────┘──▶ DISMISSED
```

| Status | Meaning |
|---|---|
| **OPEN** | Generated, not yet reviewed |
| **ACKNOWLEDGED** | Seen by an operator |
| **INVESTIGATING** | Actively being investigated |
| **CLOSED** | Resolved / confirmed legitimate |
| **DISMISSED** | False positive, no action needed |

Illegal transitions are rejected with HTTP **409** (e.g. closing an `OPEN` alert).

---

## Monitoring Rule Types

| Type | Trigger | Config columns |
|---|---|---|
| `AMOUNT_THRESHOLD` | single transaction amount > threshold | `threshold_amount` |
| `VELOCITY` | more than N transactions within a time window (same account) | `transaction_count`, `time_window_minutes` |
| `NEW_PAYEE` | first-ever transaction to a payee | — |
| `DAILY_LIMIT` | cumulative DEBIT total today > limit (same account) | `daily_limit` |

Rules are stored in the database and are **data, not code** — create/edit/activate/deactivate them at runtime via the Rules page or API. The rule engine is a **Strategy pattern**: each type has its own evaluator class implementing `RuleEvaluator`.

---

## Prerequisites

- **Java 21+** (JDK)
- **Maven** (or use the bundled `mvnw` wrapper — no separate Maven install needed)
- **Node.js 18+** and **npm**
- **MySQL 8+** running locally on port **3306**

---

## Quick Start

### 1. Clone

```bash
git clone <repository-url>
cd monitoring-master
```

### 2. Configure the database

The database `transaction_monitoring` is **created automatically** on first run. Credentials live in:

```text
backend/src/main/resources/application.properties
```

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/transaction_monitoring?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
spring.datasource.username=root
spring.datasource.password=<your-database-password>
```

> ⚠️ The repo ships with a local dev password — **change it** to match your MySQL install before starting. `schema.sql` and `data.sql` run automatically on startup.

### 3. Start the backend

```bash
cd backend

# Windows
.\mvnw.cmd spring-boot:run

# macOS / Linux
./mvnw spring-boot:run
```

Backend ready at **http://localhost:8080** · Swagger UI at **http://localhost:8080/swagger-ui.html**.

### 4. Start the frontend

```bash
cd frontend
npm install       # first time only
npm run dev
```

Frontend ready at **http://localhost:5173** (Vite proxies `/api/*` to :8080).

### 5. Verify it works

- Open the dashboard: you should see the stat cards and "No alerts yet."
- Open **Transactions** → **⚡ Generate 10 Random** → back on **Alerts** you'll see auto-generated alerts.
- Open **Swagger** (`/swagger-ui.html`) and try `POST /api/transactions`.

---

## Seed Demo Data (1000 Transactions)

A standalone seeder (`DataSeeder.java`) inserts **1000 realistic transactions** spread over the last 90 days and automatically evaluates all 4 monitoring rules, creating alerts with full status history.

### What gets seeded

| Segment | Count | Purpose |
|---|---|---|
| Random activity ($50–$9,500) | 870 | Normal traffic across 10 accounts, 60 payees |
| Large transactions ($11k–$90k) | 30 | Guaranteed `AMOUNT_THRESHOLD` alerts |
| New payee transactions | 20 | Guaranteed `NEW_PAYEE` alerts (PAYEE061-PAYEE080) |
| Velocity bursts (5 accts × 10 txns in 9 min) | 50 | Guaranteed `VELOCITY` alerts |
| High-value days (3 accts × 10 txns/day ~$70k) | 30 | Guaranteed `DAILY_LIMIT` alerts |

All 5 tables are populated: `transactions`, `alerts`, `alert_transactions`, `alert_status_history`, plus references to the existing `monitoring_rules`.

Alert statuses reflect realistic age-based progression:
- **< 7 days old** → `OPEN`
- **7–30 days old** → `ACKNOWLEDGED`
- **30–60 days old** → `CLOSED` or `DISMISSED`
- **> 60 days old** → fully `CLOSED` with complete OPEN → ACKNOWLEDGED → INVESTIGATING → CLOSED history

### How to run

**Step 1** — Start the Spring Boot app at least once so `data.sql` seeds the monitoring rules:
```bash
cd backend

# Windows
.\mvnw.cmd spring-boot:run

# macOS / Linux
./mvnw spring-boot:run
```

**Step 2** — Stop the app, then run the seeder:
```bash
# Windows
.\mvnw.cmd compile exec:java

# macOS / Linux
./mvnw compile exec:java
```

Expected output:
```
======================================
   Transaction Monitoring Data Seeder
======================================
Rules loaded: {AMOUNT_THRESHOLD=1, VELOCITY=2, NEW_PAYEE=3, DAILY_LIMIT=4}
Inserted 1000 transactions

============ Seeding Complete ============
Transactions     : 1000
Total Alerts     : ~75
  AMOUNT_THRESHOLD : ~30
  VELOCITY         : 5
  NEW_PAYEE        : 20
  DAILY_LIMIT      : ~3
==========================================
```

> The seeder uses a fixed random seed (`42`) so results are reproducible. Run it only once per clean database — running it again will add a second batch of 1000 transactions and duplicate alerts.

---

## Sample Usage (API)

```bash
# Create a transaction (rule engine runs automatically)
curl -X POST http://localhost:8080/api/transactions \
  -H "Content-Type: application/json" \
  -d '{"accountId":"ACC-001","payeeId":"PAYEE-NEW-1","amount":15000,"type":"DEBIT","status":"COMPLETED"}'
# → 201 Created, body includes "alertIds": [1, 2] if rules fired

# List alerts (filter by status)
curl "http://localhost:8080/api/alerts?status=OPEN"

# Get full alert detail (linked transactions + history)
curl http://localhost:8080/api/alerts/1

# Acknowledge an open alert
curl -X PATCH http://localhost:8080/api/alerts/1/acknowledge

# Close an investigating alert with notes
curl -X PATCH http://localhost:8080/api/alerts/1/close \
  -H "Content-Type: application/json" \
  -d '{"resolutionNotes":"Customer confirmed legitimate purchase"}'

# Create a custom rule
curl -X POST http://localhost:8080/api/rules \
  -H "Content-Type: application/json" \
  -d '{"name":"Rapid 3 in 2 min","type":"VELOCITY","severity":"LOW","transactionCount":3,"timeWindowMinutes":2}'

# Dashboard stats
curl http://localhost:8080/api/dashboard/stats
```

---

## API Summary

| Method | Endpoint | Purpose |
|---|---|---|
| `POST` | `/api/transactions` | Create transaction + evaluate rules |
| `GET` | `/api/transactions` | List (filter: accountId, from, to, minAmount, maxAmount, search) |
| `GET` | `/api/transactions/{id}` | Get one transaction |
| `POST` | `/api/transactions/generate` | Generate N random transactions |
| `GET` | `/api/alerts` | List alerts (filter: status, severity, from, to) |
| `GET` | `/api/alerts/{id}` | Alert detail + transactions + history |
| `GET` | `/api/alerts/stats` | Alert statistics |
| `PATCH` | `/api/alerts/{id}/acknowledge` | OPEN → ACKNOWLEDGED |
| `PATCH` | `/api/alerts/{id}/investigate` | ACKNOWLEDGED → INVESTIGATING |
| `PATCH` | `/api/alerts/{id}/close` | INVESTIGATING → CLOSED |
| `PATCH` | `/api/alerts/{id}/dismiss` | ACKNOWLEDGED/INVESTIGATING → DISMISSED |
| `GET` | `/api/rules` | List rules |
| `POST` | `/api/rules` | Create rule |
| `PUT` | `/api/rules/{id}` | Update rule |
| `DELETE` | `/api/rules/{id}` | Delete rule |
| `PATCH` | `/api/rules/{id}/activate` | Activate rule |
| `PATCH` | `/api/rules/{id}/deactivate` | Deactivate rule |
| `GET` | `/api/dashboard/stats` | Dashboard statistics |

Full endpoint/request/response details: see **[`FLOW.md`](./FLOW.md#6-api-surface-all-endpoints)** and Swagger UI.

---

## Database Summary

Five tables in MySQL database `transaction_monitoring` (created by `schema.sql`):

```text
transactions            monitoring_rules            alerts
+----------------+      +-------------------+      +-------------------+
| id (PK)        |      | id (PK)           |      | id (PK)           |
| account_id     |      | name              |      | rule_id (FK) ───────► monitoring_rules
| payee_id       |      | description       |      | severity          |
| amount         |      | type              |      | status            |
| type           |      | severity          |      | description       |
| status         |      | active            |      | resolution_notes  |
| description    |      | threshold_amount  |      | created_at        |
| timestamp      |      | transaction_count |      | updated_at        |
+----------------+      | time_window_minutes|     | acknowledged_at   |
                        | daily_limit       |      | closed_at         |
                        | created_at        |      +-------------------+
                        | updated_at        |            │ 1:N
                        +-------------------+            ▼
                                          alert_status_history
                                          +-------------------+
                                          | id (PK)           |
                                          | alert_id (FK) ───────► alerts
                                          | previous_status   |
                                          | new_status        |
                                          | notes             |
                                          | changed_at        |
                                          +-------------------+

alert_transactions (join table)
+---------------------+
| alert_id (FK) ◀───────► alerts
| transaction_id (FK) ◀─► transactions
| PK (alert_id, transaction_id)
+---------------------+
```

- **No ORM** — repositories use `JdbcTemplate` with parameterized SQL (safe from SQL injection).
- Indexes on `transactions(account_id, timestamp)`, `(payee_id)`, `(timestamp)` speed up rule queries.
- `data.sql` seeds the 4 default rules (only if no rules exist).

---

## Configuration

| File | Controls |
|---|---|
| `backend/src/main/resources/application.properties` | DB URL/username/password, auto schema+data init, server port (8080), Swagger paths |
| `frontend/vite.config.js` | Dev server port (5173) + `/api` proxy target (:8080) |
| `backend/pom.xml` | Java version, Spring Boot, MySQL driver, springdoc, validation, exec-maven-plugin (for DataSeeder) |
| `frontend/package.json` | React, Vite, Tailwind, Router, Axios, Recharts; scripts: `dev`, `build`, `lint`, `preview` |

---

## Build & Test

```bash
# Backend: compile + test
cd backend
.\mvnw.cmd test

# Backend: package (skip tests)
.\mvnw.cmd clean package -DskipTests

# Seed database with 1000 demo transactions (run after first app start)
.\mvnw.cmd compile exec:java

# Frontend: lint
cd frontend
npm run lint

# Frontend: production build (output → frontend/dist/)
npm run build
```

Current tests: a single Spring context-load test at `backend/src/test/java/.../TransactionMonitoringApplicationTests.java`. There are **no** rule-engine, repository, or controller unit tests yet — see the Code Quality review in the docs.

---

## Documentation

| Document | What it covers |
|---|---|
| **[`FLOW.md`](./FLOW.md)** | Deep dive: DB schema, rule engine internals, how rules/alerts/transactions link, all endpoints, an end-to-end user scenario |
| **[`README.md`](./README.md)** | This file — friendly overview + quick start |
| [`transaction_monitoring.md`](./transaction_monitoring.md) | Original training requirements & enhancement ideas |
| [`GIT_WORKFLOW.md`](./GIT_WORKFLOW.md) | Team branch strategy, PR workflow, phase plan |

---

## Common Troubleshooting

| Problem | Likely cause | Fix |
|---|---|---|
| Backend fails to start | MySQL not running / wrong credentials | Start MySQL, check `application.properties` username/password |
| `Communications link failure` | Wrong DB host/port | Confirm MySQL is on `localhost:3306` |
| `Port 8080 already in use` | Another process on 8080 | Stop it, or change `server.port` |
| Frontend loads but API calls fail | Backend not running | Start backend first; Vite proxies `/api` → 8080 |
| `npm install` errors | Old Node/npm | Use Node 18+ |
| Alerts not created | All rules deactivated | Check Rules page toggles / `active` column |
| `409` on alert action | Invalid lifecycle transition | Follow OPEN→ACKNOWLEDGED→INVESTIGATING→CLOSED |

---

## Known Limitations

- **No authentication** — single operator assumed (by design).
- **Synchronous evaluation** — bulk "generate N" runs the engine N times inline; no queue/async.
- **De-dup is narrow** — only suppresses a second `OPEN` alert for the same (rule, account).
- **`avgResolutionMinutes`** counts only alerts with a `closed_at` (closed ones), not dismissed.
- **Minimal test coverage** — only a context-load test exists.
- **Dev credentials** are committed in `application.properties` (should be overridden locally).
- Velocity/new-payee/daily-limit alerts link only the *triggering* transaction, not the whole batch.

---

## Suggested Learning Order

1. Read this `README.md` end to end.
2. Read **[`FLOW.md`](./FLOW.md)** §1–§5 (architecture, schema, engine, linking).
3. Open `backend/src/main/resources/schema.sql` and `data.sql` — the database.
4. Follow one flow in code: `controller/TransactionController.java` → `service/TransactionService.java` → `rules/RuleEngineService.java` → `rules/*Evaluator.java` → `repository/*`.
5. Read `AlertController.java` + `service/AlertService.java` to see the lifecycle enforcement.
6. Read the frontend: `App.jsx` → `pages/` → `services/`.
7. Run the app and exercise the sample API calls above.
8. Read `GIT_WORKFLOW.md` before contributing.

---

## Contribution

Team workflow, branch naming, PR review assignments, and commit-message format are documented in **[`GIT_WORKFLOW.md`](./GIT_WORKFLOW.md)**.

- Branch naming: `feature/<your-name>-<short-description>` (e.g. `feature/rakesh-rules-page`)
- All PRs target `develop`; `main` is release-only.
- Commit messages follow Conventional Commits (`feat:`, `fix:`, `chore:`, `docs:`, `test:`, `refactor:`, `style:`).

---

## URLs Summary

| Service | URL |
|---|---|
| Frontend (dev) | http://localhost:5173 |
| Backend API | http://localhost:8080 |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| OpenAPI JSON | http://localhost:8080/api-docs |
