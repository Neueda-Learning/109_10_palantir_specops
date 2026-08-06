-- Backup dataset for MySQL Workbench import
-- Purpose: immediately populate dashboard with all rule types and mixed alert statuses

USE transaction_monitoring;

SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE alert_status_history;
TRUNCATE TABLE alert_transactions;
TRUNCATE TABLE alerts;
TRUNCATE TABLE transactions;
TRUNCATE TABLE monitoring_rules;
SET FOREIGN_KEY_CHECKS = 1;

INSERT INTO monitoring_rules
    (id, name, description, type, severity, active, threshold_amount, transaction_count, time_window_minutes, daily_limit, created_at, updated_at)
VALUES
    (1, 'High Value Transaction', 'Alert when a single transaction exceeds $10,000', 'AMOUNT_THRESHOLD', 'HIGH', TRUE, 10000.00, NULL, NULL, NULL, NOW(), NOW()),
    (2, 'Rapid Transaction Velocity', 'Alert when more than 5 transactions occur within 10 minutes from the same account', 'VELOCITY', 'MEDIUM', TRUE, NULL, 5, 10, NULL, NOW(), NOW()),
    (3, 'New Payee Transaction', 'Alert when a transaction is made to a previously unseen payee', 'NEW_PAYEE', 'LOW', TRUE, NULL, NULL, NULL, NULL, NOW(), NOW()),
    (4, 'Daily Limit Exceeded', 'Alert when cumulative daily transaction amount exceeds $50,000', 'DAILY_LIMIT', 'HIGH', TRUE, NULL, NULL, NULL, 50000.00, NOW(), NOW());

INSERT INTO transactions
    (id, account_id, payee_id, amount, type, status, description, timestamp)
VALUES
    (1001, 'ACC-100', 'PAYEE-LEGACY-1', 2200.00, 'DEBIT', 'COMPLETED', 'Historical baseline transaction for new payee checks', DATE_SUB(NOW(), INTERVAL 2 DAY)),
    (1002, 'ACC-100', 'PAYEE-HIGH-1', 15250.00, 'DEBIT', 'COMPLETED', 'High value transaction to trigger AMOUNT_THRESHOLD', DATE_SUB(NOW(), INTERVAL 150 MINUTE)),

    (1003, 'ACC-200', 'PAYEE-VEL-1', 250.00, 'DEBIT', 'COMPLETED', 'Velocity transaction 1/6', DATE_SUB(NOW(), INTERVAL 9 MINUTE)),
    (1004, 'ACC-200', 'PAYEE-VEL-2', 275.00, 'DEBIT', 'COMPLETED', 'Velocity transaction 2/6', DATE_SUB(NOW(), INTERVAL 8 MINUTE)),
    (1005, 'ACC-200', 'PAYEE-VEL-3', 290.00, 'DEBIT', 'COMPLETED', 'Velocity transaction 3/6', DATE_SUB(NOW(), INTERVAL 7 MINUTE)),
    (1006, 'ACC-200', 'PAYEE-VEL-4', 260.00, 'DEBIT', 'COMPLETED', 'Velocity transaction 4/6', DATE_SUB(NOW(), INTERVAL 6 MINUTE)),
    (1007, 'ACC-200', 'PAYEE-VEL-5', 280.00, 'DEBIT', 'COMPLETED', 'Velocity transaction 5/6', DATE_SUB(NOW(), INTERVAL 5 MINUTE)),
    (1008, 'ACC-200', 'PAYEE-VEL-6', 310.00, 'DEBIT', 'COMPLETED', 'Velocity transaction 6/6', DATE_SUB(NOW(), INTERVAL 4 MINUTE)),

    (1009, 'ACC-300', 'PAYEE-NEW-ONE', 700.00, 'DEBIT', 'COMPLETED', 'First transfer to a new payee', DATE_SUB(NOW(), INTERVAL 55 MINUTE)),

    (1010, 'ACC-400', 'PAYEE-LIMIT-1', 18000.00, 'DEBIT', 'COMPLETED', 'Daily limit sequence transaction 1/3', DATE_SUB(NOW(), INTERVAL 220 MINUTE)),
    (1011, 'ACC-400', 'PAYEE-LIMIT-2', 17000.00, 'DEBIT', 'COMPLETED', 'Daily limit sequence transaction 2/3', DATE_SUB(NOW(), INTERVAL 180 MINUTE)),
    (1012, 'ACC-400', 'PAYEE-LIMIT-3', 19000.00, 'DEBIT', 'COMPLETED', 'Daily limit sequence transaction 3/3 total > 50,000', DATE_SUB(NOW(), INTERVAL 140 MINUTE)),

    (1013, 'ACC-500', 'PAYEE-HIGH-2', 12800.00, 'DEBIT', 'COMPLETED', 'Second high value transaction for dismissed lifecycle example', DATE_SUB(NOW(), INTERVAL 32 MINUTE)),
    (1014, 'ACC-600', 'PAYEE-VEL-X1', 330.00, 'DEBIT', 'COMPLETED', 'Recent velocity transaction set A', DATE_SUB(NOW(), INTERVAL 3 MINUTE)),
    (1015, 'ACC-600', 'PAYEE-VEL-X2', 340.00, 'DEBIT', 'COMPLETED', 'Recent velocity transaction set B', DATE_SUB(NOW(), INTERVAL 2 MINUTE));

INSERT INTO alerts
    (id, rule_id, severity, status, description, resolution_notes, created_at, updated_at, acknowledged_at, closed_at)
VALUES
    (5001, 1, 'HIGH', 'OPEN', 'Amount threshold breached: transaction 1002 exceeds configured threshold', NULL, DATE_SUB(NOW(), INTERVAL 145 MINUTE), DATE_SUB(NOW(), INTERVAL 145 MINUTE), NULL, NULL),
    (5002, 2, 'MEDIUM', 'ACKNOWLEDGED', 'Velocity rule triggered: more than 5 transactions within 10 minutes for account ACC-200', NULL, DATE_SUB(NOW(), INTERVAL 9 MINUTE), DATE_SUB(NOW(), INTERVAL 5 MINUTE), DATE_SUB(NOW(), INTERVAL 5 MINUTE), NULL),
    (5003, 3, 'LOW', 'INVESTIGATING', 'New payee detected for account ACC-300: PAYEE-NEW-ONE', NULL, DATE_SUB(NOW(), INTERVAL 55 MINUTE), DATE_SUB(NOW(), INTERVAL 48 MINUTE), DATE_SUB(NOW(), INTERVAL 50 MINUTE), NULL),
    (5004, 4, 'HIGH', 'CLOSED', 'Daily debit limit exceeded for account ACC-400', 'Customer confirmed expected scheduled vendor payments.', DATE_SUB(NOW(), INTERVAL 135 MINUTE), DATE_SUB(NOW(), INTERVAL 20 MINUTE), DATE_SUB(NOW(), INTERVAL 120 MINUTE), DATE_SUB(NOW(), INTERVAL 20 MINUTE)),
    (5005, 1, 'HIGH', 'DISMISSED', 'Amount threshold breached: transaction 1013 requires review', 'False positive: approved treasury transfer.', DATE_SUB(NOW(), INTERVAL 32 MINUTE), DATE_SUB(NOW(), INTERVAL 25 MINUTE), DATE_SUB(NOW(), INTERVAL 30 MINUTE), DATE_SUB(NOW(), INTERVAL 25 MINUTE)),
    (5006, 2, 'MEDIUM', 'OPEN', 'Velocity pattern detected for account ACC-600', NULL, DATE_SUB(NOW(), INTERVAL 2 MINUTE), DATE_SUB(NOW(), INTERVAL 2 MINUTE), NULL, NULL);

INSERT INTO alert_transactions (alert_id, transaction_id)
VALUES
    (5001, 1002),
    (5002, 1003), (5002, 1004), (5002, 1005), (5002, 1006), (5002, 1007), (5002, 1008),
    (5003, 1009),
    (5004, 1010), (5004, 1011), (5004, 1012),
    (5005, 1013),
    (5006, 1014), (5006, 1015);

INSERT INTO alert_status_history (alert_id, previous_status, new_status, notes, changed_at)
VALUES
    (5001, NULL, 'OPEN', 'Alert auto-generated by rule: High Value Transaction', DATE_SUB(NOW(), INTERVAL 145 MINUTE)),

    (5002, NULL, 'OPEN', 'Alert auto-generated by rule: Rapid Transaction Velocity', DATE_SUB(NOW(), INTERVAL 9 MINUTE)),
    (5002, 'OPEN', 'ACKNOWLEDGED', 'Reviewed by operations analyst', DATE_SUB(NOW(), INTERVAL 5 MINUTE)),

    (5003, NULL, 'OPEN', 'Alert auto-generated by rule: New Payee Transaction', DATE_SUB(NOW(), INTERVAL 55 MINUTE)),
    (5003, 'OPEN', 'ACKNOWLEDGED', 'Initial triage completed', DATE_SUB(NOW(), INTERVAL 50 MINUTE)),
    (5003, 'ACKNOWLEDGED', 'INVESTIGATING', 'Investigation in progress', DATE_SUB(NOW(), INTERVAL 48 MINUTE)),

    (5004, NULL, 'OPEN', 'Alert auto-generated by rule: Daily Limit Exceeded', DATE_SUB(NOW(), INTERVAL 135 MINUTE)),
    (5004, 'OPEN', 'ACKNOWLEDGED', 'Escalated to investigator', DATE_SUB(NOW(), INTERVAL 120 MINUTE)),
    (5004, 'ACKNOWLEDGED', 'INVESTIGATING', 'Confirmed transaction set and account owner contact', DATE_SUB(NOW(), INTERVAL 95 MINUTE)),
    (5004, 'INVESTIGATING', 'CLOSED', 'Case closed after customer confirmation', DATE_SUB(NOW(), INTERVAL 20 MINUTE)),

    (5005, NULL, 'OPEN', 'Alert auto-generated by rule: High Value Transaction', DATE_SUB(NOW(), INTERVAL 32 MINUTE)),
    (5005, 'OPEN', 'ACKNOWLEDGED', 'Analyst acknowledged and reviewed context', DATE_SUB(NOW(), INTERVAL 30 MINUTE)),
    (5005, 'ACKNOWLEDGED', 'DISMISSED', 'Transfer was expected and policy-compliant', DATE_SUB(NOW(), INTERVAL 25 MINUTE)),

    (5006, NULL, 'OPEN', 'Alert auto-generated by rule: Rapid Transaction Velocity', DATE_SUB(NOW(), INTERVAL 2 MINUTE));

ALTER TABLE monitoring_rules AUTO_INCREMENT = 5;
ALTER TABLE transactions AUTO_INCREMENT = 1016;
ALTER TABLE alerts AUTO_INCREMENT = 5007;