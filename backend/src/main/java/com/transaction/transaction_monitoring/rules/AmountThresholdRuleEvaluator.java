package com.transaction.transaction_monitoring.rules;

import com.transaction.transaction_monitoring.enums.RuleType;
import com.transaction.transaction_monitoring.model.MonitoringRule;
import com.transaction.transaction_monitoring.model.Transaction;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class AmountThresholdRuleEvaluator implements RuleEvaluator {

    @Override
    public Optional<AlertCandidate> evaluate(Transaction tx, MonitoringRule rule) {
        if (rule.getThresholdAmount() == null) return Optional.empty();
        if (tx.getAmount().compareTo(rule.getThresholdAmount()) > 0) {
            String desc = String.format("Transaction of $%.2f from account %s exceeds threshold of $%.2f",
                    tx.getAmount(), tx.getAccountId(), rule.getThresholdAmount());
            return Optional.of(new AlertCandidate(rule.getId(), rule.getName(), rule.getSeverity(), desc, List.of(tx.getId())));
        }
        return Optional.empty();
    }

    public RuleType getSupportedType() { return RuleType.AMOUNT_THRESHOLD; }
}
