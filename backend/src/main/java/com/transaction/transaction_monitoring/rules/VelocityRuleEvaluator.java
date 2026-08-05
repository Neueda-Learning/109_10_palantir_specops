package com.transaction.transaction_monitoring.rules;

import com.transaction.transaction_monitoring.enums.RuleType;
import com.transaction.transaction_monitoring.model.MonitoringRule;
import com.transaction.transaction_monitoring.model.Transaction;
import com.transaction.transaction_monitoring.repository.TransactionRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class VelocityRuleEvaluator implements RuleEvaluator {

    private final TransactionRepository transactionRepository;

    public VelocityRuleEvaluator(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @Override
    public Optional<AlertCandidate> evaluate(Transaction tx, MonitoringRule rule) {
        if (rule.getTransactionCount() == null || rule.getTimeWindowMinutes() == null) return Optional.empty();
        int count = transactionRepository.countByAccountInWindow(tx.getAccountId(), rule.getTimeWindowMinutes());
        if (count > rule.getTransactionCount()) {
            String desc = String.format("Account %s has %d transactions in the last %d minutes (limit: %d)",
                    tx.getAccountId(), count, rule.getTimeWindowMinutes(), rule.getTransactionCount());
            return Optional.of(new AlertCandidate(rule.getId(), rule.getName(), rule.getSeverity(), desc, List.of(tx.getId())));
        }
        return Optional.empty();
    }

    public RuleType getSupportedType() { return RuleType.VELOCITY; }
}
