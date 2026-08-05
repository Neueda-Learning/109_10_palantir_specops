package com.transaction.transaction_monitoring.rules;

import com.transaction.transaction_monitoring.enums.RuleType;
import com.transaction.transaction_monitoring.enums.TransactionType;
import com.transaction.transaction_monitoring.model.MonitoringRule;
import com.transaction.transaction_monitoring.model.Transaction;
import com.transaction.transaction_monitoring.repository.TransactionRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Component
public class DailyLimitRuleEvaluator implements RuleEvaluator {

    private final TransactionRepository transactionRepository;

    public DailyLimitRuleEvaluator(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @Override
    public Optional<AlertCandidate> evaluate(Transaction tx, MonitoringRule rule) {
        if (rule.getDailyLimit() == null) return Optional.empty();
        if (tx.getType() != TransactionType.DEBIT) return Optional.empty();
        BigDecimal dailyTotal = transactionRepository.sumDebitsForAccountToday(tx.getAccountId());
        if (dailyTotal.compareTo(rule.getDailyLimit()) > 0) {
            String desc = String.format("Account %s daily debit total of $%.2f exceeds daily limit of $%.2f",
                    tx.getAccountId(), dailyTotal, rule.getDailyLimit());
            return Optional.of(new AlertCandidate(rule.getId(), rule.getName(), rule.getSeverity(), desc, List.of(tx.getId())));
        }
        return Optional.empty();
    }

    public RuleType getSupportedType() { return RuleType.DAILY_LIMIT; }
}
