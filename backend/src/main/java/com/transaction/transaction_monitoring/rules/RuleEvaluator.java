package com.transaction.transaction_monitoring.rules;

import com.transaction.transaction_monitoring.model.MonitoringRule;
import com.transaction.transaction_monitoring.model.Transaction;

import java.util.Optional;

public interface RuleEvaluator {
    Optional<AlertCandidate> evaluate(Transaction transaction, MonitoringRule rule);
}
