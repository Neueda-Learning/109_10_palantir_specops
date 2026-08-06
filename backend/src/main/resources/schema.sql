CREATE TABLE IF NOT EXISTS transactions (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_id    VARCHAR(50)    NOT NULL,
    payee_id      VARCHAR(50)    NOT NULL,
    amount        DECIMAL(15,2)  NOT NULL,
    type          ENUM('DEBIT','CREDIT') NOT NULL DEFAULT 'DEBIT',
    status        ENUM('PENDING','COMPLETED','FAILED') NOT NULL DEFAULT 'COMPLETED',
    description   VARCHAR(255),
    timestamp     DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_account_timestamp (account_id, timestamp),
    INDEX idx_payee (payee_id),
    INDEX idx_timestamp (timestamp)
);

CREATE TABLE IF NOT EXISTS monitoring_rules (
    id                   BIGINT AUTO_INCREMENT PRIMARY KEY,
    name                 VARCHAR(100)   NOT NULL,
    description          VARCHAR(255),
    type                 ENUM('AMOUNT_THRESHOLD','VELOCITY','NEW_PAYEE','DAILY_LIMIT','FLAGGED_PAYEE','FLAGGED_PAYEE_CONCENTRATION') NOT NULL,
    severity             ENUM('HIGH','MEDIUM','LOW') NOT NULL DEFAULT 'MEDIUM',
    active               BOOLEAN        NOT NULL DEFAULT TRUE,
    threshold_amount     DECIMAL(15,2),
    transaction_count    INT,
    time_window_minutes  INT,
    daily_limit          DECIMAL(15,2),
    created_at           DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Upgrades an existing DB so the type enum accepts the two new flagged-organisation rule types.
-- Safe to re-run: MODIFY with the same definition is idempotent.
ALTER TABLE monitoring_rules
    MODIFY COLUMN type ENUM('AMOUNT_THRESHOLD','VELOCITY','NEW_PAYEE','DAILY_LIMIT','FLAGGED_PAYEE','FLAGGED_PAYEE_CONCENTRATION') NOT NULL;

CREATE TABLE IF NOT EXISTS alerts (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    rule_id           BIGINT         NOT NULL,
    severity          ENUM('HIGH','MEDIUM','LOW') NOT NULL,
    status            ENUM('OPEN','ACKNOWLEDGED','INVESTIGATING','CLOSED','DISMISSED') NOT NULL DEFAULT 'OPEN',
    description       VARCHAR(500),
    resolution_notes  TEXT,
    created_at        DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    acknowledged_at   DATETIME,
    closed_at         DATETIME,
    FOREIGN KEY (rule_id) REFERENCES monitoring_rules(id)
);

CREATE TABLE IF NOT EXISTS alert_transactions (
    alert_id       BIGINT NOT NULL,
    transaction_id BIGINT NOT NULL,
    PRIMARY KEY (alert_id, transaction_id),
    FOREIGN KEY (alert_id)       REFERENCES alerts(id),
    FOREIGN KEY (transaction_id) REFERENCES transactions(id)
);

CREATE TABLE IF NOT EXISTS alert_status_history (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    alert_id         BIGINT       NOT NULL,
    previous_status  VARCHAR(20),
    new_status       VARCHAR(20)  NOT NULL,
    notes            TEXT,
    changed_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (alert_id) REFERENCES alerts(id)
);

CREATE TABLE IF NOT EXISTS flagged_entities (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    entity_name  VARCHAR(120)   NOT NULL,
    payee_id     VARCHAR(50)    NOT NULL,
    reason       VARCHAR(255),
    risk_level   ENUM('HIGH','MEDIUM','LOW') NOT NULL DEFAULT 'MEDIUM',
    active       BOOLEAN        NOT NULL DEFAULT TRUE,
    created_at   DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_flagged_payee (payee_id)
);
