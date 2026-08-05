# Feature Proposal: Flagged Organisation Monitoring

> Detect customers who send money to anti-social organisations and identify the organisations themselves.

---

## 1. The Two Scenarios We Must Catch

| | Scenario A — "The Occasional Donor" | Scenario B — "The Front Organisation" |
|---|---|---|
| **Who** | A normal customer | An account set up around the organisation |
| **Behaviour** | Spends normally (groceries, rent, shopping) but sends a donation to an anti-social organisation once in a while | Almost all money flows towards the flagged organisation |
| **Example** | Buys groceries all month, sends $50 to Org-X every few months | 9 of 10 transactions go to Org-X |
| **What we want to do** | Flag the *contact* so the operator can check whether the customer is knowingly funding it (MEDIUM) | Flag the *account* as likely linked to the organisation for deep investigation (HIGH) |

---

## 2. The Idea — One Simple Concept

Keep a **Watchlist of flagged organisations** (beneficiaries/payees). From the moment an organisation is on the watchlist, every new transaction is checked against it — and two things can happen:

```mermaid
flowchart LR
    A[Admin adds an organisation to the Watchlist] --> B[Every transaction is checked against the Watchlist]
    B --> C{How much does this account deal with the flagged org?}
    C -->|Occasionally| D[MEDIUM alert<br/>Contact detected]
    C -->|Most or all of the time| E[HIGH alert<br/>Likely linked to flagged org]
    D --> F[Operator reviews the customer]
    E --> F
```

This reuses the system's **existing** alert workflow — every alert follows the same Open → Acknowledge → Investigate → Close lifecycle with a full audit trail.

---

## 3. What We Add (Small, Fits the Current System)

Two new monitoring rules, managed exactly like the existing ones (e.g. "amount > $10,000", "5 transactions in 10 minutes"):

| New Rule | Covers | How it fires | Suggested severity |
|---|---|---|---|
| **Flagged Payee Contact** | Scenario A | Any transaction to an organisation on the watchlist | MEDIUM |
| **Flagged Payee Concentration** | Scenario B | An account's transactions to flagged organisations exceed a set share (e.g. 50–80%) over a period (e.g. last 30 days) | HIGH |

| Area | Today | With this feature |
|---|---|---|
| Data | transactions, rules, alerts | + a `flagged_entities` watchlist table (payee, name, reason, date flagged) |
| Rules | 4 rule types | + 2 new rule types above |
| Rules page | toggle / edit / add rules | + "Flagged Organisations" tab to add and remove watchlist entries |
| Alerts & history | existing lifecycle | unchanged — new alerts flow through the same pipeline |

---

## 4. How It Looks in Action

### Scenario A — Occasional donor

```
Customer buys groceries, pays rent            (normal — no alert)
Customer sends $50 to Org-X (on watchlist)    (MEDIUM alert)
  → Operator: is the customer aware? Warn, watch, or escalate
```

### Scenario B — Front organisation

```
Account Org-Front-001 sends 9 of its last 10 payments to Org-X
  (HIGH concentration alert)
  → Operator: account is likely linked to Org-X → investigate / freeze
```

### Example flow with numbers

```mermaid
flowchart TB
    subgraph Watchlist
        W[Org-X: flagged]
    end
    TX1[$50 donation to Org-X] --> R1[Flagged Payee Contact rule]
    R1 -->|fires| A1[Alert: contact detected - MEDIUM]
    subgraph Existing pipeline
        A1 --> A2[Acknowledge] --> A3[Investigate] --> A4[Close / Dismiss]
    end
    TX2[9 of 10 payments to Org-X] --> R2[Concentration rule]
    R2 -->|fires| B1[Alert: likely linked - HIGH]
    B1 --> A2
```

---

## 5. Admin Workflow (How an Organisation Gets Flagged)

```
Rules page
   │
   ▼
"Flagged Organisations" tab
   │
   ▼
Add payee ID + name + reason  ──►  organisation is on the watchlist
   │
   ▼
Next transaction to it automatically fires an alert (rules above)
```

Unflagging an organisation works the same way — remove it from the watchlist and no new alerts fire. Historical alerts stay untouched (the system already snapshots alert details at creation time).

---

## 6. Why This Solves Both Scenarios Cleanly

- **One watchlist, two rules** — no special-casing; a single source of truth for who is flagged.
- **Catches the unknowing supporter** (Scenario A) without blocking normal banking.
- **Catches the organisation itself** (Scenario B) via concentration, which a simple blacklist alone would miss.
- **Zero disruption** — reuses the existing rule engine, alert lifecycle, audit trail, and dashboard.
- **Configurable, not hard-coded** — thresholds (share %, time window) can be tuned in the Rules page without code changes.

---

## 7. Out of Scope for v1 (Future Ideas)

- Matching by fuzzy organisation name (v1 matches on the exact payee ID recorded with each transaction).
- Automatic account freezing (v1 alerts the operator, who decides).
- Cross-account link analysis (e.g. detecting a network of supporters).
