# Testing Report - Transaction Monitoring & Alerts Dashboard

Date: 2026-08-06
Scope: Executed tests and observed output for Section 9 testing work.

---

## 1) Summary

- Backend automated suite expanded and executed successfully.
- Performance test TS-14 executed on live API.
- UAT flow test TS-15 executed on live API.
- Remaining pending scope: TS-16 to TS-18 (flagged-organisation feature tests).

---

## 2) Test Matrix Status

| ID | Test Case | Type | Status | Notes |
|---|---|---|---|---|
| TS-01 | AMOUNT_THRESHOLD fires when amount > threshold | Unit | PASS | Covered in RuleEvaluatorsTest |
| TS-02 | VELOCITY fires when tx count exceeds configured count within window | Unit | PASS | Covered in RuleEvaluatorsTest |
| TS-03 | NEW_PAYEE fires only for first transaction to payee per account | Unit | PASS | Covered in RuleEvaluatorsTest |
| TS-04 | DAILY_LIMIT fires when same-day debit sum exceeds limit | Unit | PASS | Covered in RuleEvaluatorsTest |
| TS-05 | De-dup suppresses second OPEN alert for same (rule, account) | Unit | PASS | Covered in TransactionServiceTest |
| TS-06 | OPEN -> ACKNOWLEDGED transition succeeds | Unit | PASS | Covered in AlertServiceTest |
| TS-07 | ACKNOWLEDGED -> INVESTIGATING transition succeeds | Unit | PASS | Covered in AlertServiceTest |
| TS-08 | INVESTIGATING -> CLOSED with notes succeeds | Unit | PASS | Covered in AlertServiceTest |
| TS-09 | Illegal transition (OPEN -> CLOSED) returns 409 | Unit/Negative | PASS | Covered in AlertServiceTest + GlobalExceptionHandlerTest |
| TS-10 | Transaction -> rule eval -> alert -> history flow | Integration-style | PASS | Service-flow assertions with mocks |
| TS-11 | Alert detail includes linked transactions + history | Integration-style | PASS | Covered in AlertServiceTest |
| TS-12 | Validation failures return 400 contract | Negative | PASS | Covered in GlobalExceptionHandlerTest |
| TS-13 | Missing entity returns 404 contract | Negative | PASS | Covered in GlobalExceptionHandlerTest |
| TS-14 | Generate 1,000 transactions and measure time | Performance | PASS | Live API execution evidence below |
| TS-15 | Full analyst lifecycle flow end-to-end | UAT | PASS | Live API execution evidence below |
| TS-16 | FLAGGED_PAYEE rule test | Unit/Integration | PENDING | Feature not implemented yet |
| TS-17 | FLAGGED_PAYEE_CONCENTRATION rule test | Unit/Integration | PENDING | Feature not implemented yet |
| TS-18 | Unflagging behavior and history immutability | Integration | PENDING | Feature not implemented yet |

---

## 3) Commands Executed and Output

### Backend test suite

Command:
- .\\mvnw.cmd test

Observed output summary:
- Tests run: 16
- Failures: 0
- Errors: 0
- Skipped: 0
- Build status: SUCCESS

### Frontend lint

Command:
- npm run lint

Observed output summary:
- Status: PASS with warnings
- Warnings:
  - src/pages/Alerts.jsx: imported Link is unused
  - src/pages/Alerts.jsx: rowColor is declared but never used
  - src/pages/AlertDetail.jsx: useEffect missing dependency load

### Frontend build

Command:
- npm run build

Observed output summary:
- Status: PASS
- Vite build completed successfully
- Warning: some chunks larger than 500 kB after minification

---

## 4) TS-14 Performance Evidence

Scenario:
- Endpoint: POST /api/transactions/generate
- Payload: { "count": 1000 }

Measured result:
- generatedTransactions: 1000
- totalMilliseconds: 9985.75
- avgMillisecondsPerTransaction: 9.99

Raw result object:

{
  "test": "TS-14",
  "generatedTransactions": 1000,
  "totalMilliseconds": 9985.75,
  "avgMillisecondsPerTransaction": 9.99
}

Conclusion:
- PASS. The 1,000 transaction generation and evaluation completed successfully with stable average latency.

---

## 5) TS-15 UAT Evidence

Scenario executed via API:
1. Create transaction expected to trigger alert.
2. Fetch created alert detail.
3. Acknowledge alert.
4. Move alert to investigating.
5. Close alert with resolution notes.
6. Fetch final alert detail and dashboard stats.

Measured result object:

{
  "test": "TS-15",
  "runId": 1785996091385,
  "accountId": "ACC-UAT-1785996091385",
  "createdTransactionId": 1039,
  "alertIds": [18],
  "testedAlertId": 18,
  "initialStatus": "OPEN",
  "statusAfterAcknowledge": "ACKNOWLEDGED",
  "statusAfterInvestigate": "INVESTIGATING",
  "statusAfterClose": "CLOSED",
  "finalStatus": "CLOSED",
  "historyCount": 4,
  "transitionsValid": true,
  "openCountBefore": 13,
  "openCountAfter": 13,
  "closedCountBefore": 1,
  "closedCountAfter": 2,
  "dashboardUpdated": true
}

Conclusion:
- PASS. Lifecycle transitions were valid and dashboard metrics reflected closure.

---

## 6) Test Artifacts Added in Codebase

- backend/src/test/java/com/transaction/transaction_monitoring/rules/RuleEvaluatorsTest.java
- backend/src/test/java/com/transaction/transaction_monitoring/service/TransactionServiceTest.java
- backend/src/test/java/com/transaction/transaction_monitoring/service/AlertServiceTest.java
- backend/src/test/java/com/transaction/transaction_monitoring/exception/GlobalExceptionHandlerTest.java
- backend/src/test/java/com/transaction/transaction_monitoring/TransactionMonitoringApplicationTests.java

---

## 7) Remaining Work

- TS-16, TS-17, TS-18 remain pending until flagged-organisation feature implementation is complete.
