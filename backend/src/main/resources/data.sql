INSERT INTO monitoring_rules (name, description, type, severity, active, threshold_amount, transaction_count, time_window_minutes, daily_limit)
SELECT 'High Value Transaction', 'Alert when a single transaction exceeds $10,000', 'AMOUNT_THRESHOLD', 'HIGH', TRUE, 10000.00, NULL, NULL, NULL
WHERE NOT EXISTS (SELECT 1 FROM monitoring_rules WHERE type = 'AMOUNT_THRESHOLD' LIMIT 1);

INSERT INTO monitoring_rules (name, description, type, severity, active, threshold_amount, transaction_count, time_window_minutes, daily_limit)
SELECT 'Rapid Transaction Velocity', 'Alert when more than 5 transactions occur within 10 minutes from the same account', 'VELOCITY', 'MEDIUM', TRUE, NULL, 5, 10, NULL
WHERE NOT EXISTS (SELECT 1 FROM monitoring_rules WHERE type = 'VELOCITY' LIMIT 1);

INSERT INTO monitoring_rules (name, description, type, severity, active, threshold_amount, transaction_count, time_window_minutes, daily_limit)
SELECT 'New Payee Transaction', 'Alert when a transaction is made to a previously unseen payee', 'NEW_PAYEE', 'LOW', TRUE, NULL, NULL, NULL, NULL
WHERE NOT EXISTS (SELECT 1 FROM monitoring_rules WHERE type = 'NEW_PAYEE' LIMIT 1);

INSERT INTO monitoring_rules (name, description, type, severity, active, threshold_amount, transaction_count, time_window_minutes, daily_limit)
SELECT 'Daily Limit Exceeded', 'Alert when cumulative daily transaction amount exceeds $50,000', 'DAILY_LIMIT', 'HIGH', TRUE, NULL, NULL, NULL, 50000.00
WHERE NOT EXISTS (SELECT 1 FROM monitoring_rules WHERE type = 'DAILY_LIMIT' LIMIT 1);

INSERT INTO monitoring_rules (name, description, type, severity, active, threshold_amount, transaction_count, time_window_minutes, daily_limit)
SELECT 'Flagged Organisation Contact', 'Alert when any transaction is sent to an organisation on the flagged watchlist', 'FLAGGED_PAYEE', 'MEDIUM', TRUE, NULL, NULL, NULL, NULL
WHERE NOT EXISTS (SELECT 1 FROM monitoring_rules WHERE type = 'FLAGGED_PAYEE' LIMIT 1);

INSERT INTO monitoring_rules (name, description, type, severity, active, threshold_amount, transaction_count, time_window_minutes, daily_limit)
SELECT 'Flagged Organisation Concentration', 'Alert when DEBIT spending to flagged organisations exceeds 80% of an account total within 30 days', 'FLAGGED_PAYEE_CONCENTRATION', 'HIGH', TRUE, 80.00, NULL, 43200, NULL
WHERE NOT EXISTS (SELECT 1 FROM monitoring_rules WHERE type = 'FLAGGED_PAYEE_CONCENTRATION' LIMIT 1);

INSERT INTO flagged_entities (entity_name, payee_id, reason, risk_level, active)
SELECT 'Global Charity X', 'ORG-12345', 'Known anti-social organisation', 'HIGH', TRUE
WHERE NOT EXISTS (SELECT 1 FROM flagged_entities WHERE payee_id = 'ORG-12345' LIMIT 1);
