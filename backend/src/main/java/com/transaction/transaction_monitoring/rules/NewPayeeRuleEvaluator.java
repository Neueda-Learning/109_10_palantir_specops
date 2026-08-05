package com.transaction.transaction_monitoring.rules;

import com.transaction.transaction_monitoring.enums.RuleType;
import com.transaction.transaction_monitoring.model.MonitoringRule;
import com.transaction.transaction_monitoring.model.Transaction;
import com.transaction.transaction_monitoring.repository.TransactionRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class NewPayeeRuleEvaluator implements RuleEvaluator {

    private final TransactionRepository transactionRepository;

    public NewPayeeRuleEvaluator(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @Override
    public Optional<AlertCandidate> evaluate(Transaction tx, MonitoringRule rule) {
        int prev = transactionRepository.countPreviousPayeeTransactions(
                tx.getAccountId(), tx.getPayeeId(), tx.getTimestamp());
        if (prev == 0) {
            String desc = String.format("Account %s made first-ever transaction to new payee %s",
                    tx.getAccountId(), tx.getPayeeId());
            return Optional.of(new AlertCandidate(rule.getId(), rule.getName(), rule.getSeverity(), desc, List.of(tx.getId())));
        }
        return Optional.empty();
    }

    public RuleType getSupportedType() { return RuleType.NEW_PAYEE; }
}
