package com.transaction.transaction_monitoring.rules;

import com.transaction.transaction_monitoring.enums.RuleType;
import com.transaction.transaction_monitoring.enums.TransactionType;
import com.transaction.transaction_monitoring.model.MonitoringRule;
import com.transaction.transaction_monitoring.model.Transaction;
import com.transaction.transaction_monitoring.repository.TransactionRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Component
public class FlaggedPayeeConcentrationRuleEvaluator implements RuleEvaluator {

    private final TransactionRepository transactionRepository;

    public FlaggedPayeeConcentrationRuleEvaluator(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @Override
    public Optional<AlertCandidate> evaluate(Transaction tx, MonitoringRule rule) {
        if (rule.getThresholdAmount() == null || rule.getTimeWindowMinutes() == null) return Optional.empty();
        if (tx.getType() != TransactionType.DEBIT) return Optional.empty();

        LocalDateTime from = tx.getTimestamp().minusMinutes(rule.getTimeWindowMinutes());
        BigDecimal flagged = transactionRepository.sumFlaggedPayeeDebits(tx.getAccountId(), from);
        BigDecimal total = transactionRepository.sumDebitsInWindow(tx.getAccountId(), from);

        if (total.signum() <= 0) return Optional.empty();

        double share = flagged.doubleValue() / total.doubleValue() * 100.0;
        if (share > rule.getThresholdAmount().doubleValue()) {
            String desc = String.format(
                    "Account %s sent $%.2f of $%.2f (%.1f%%) to flagged organisations in the last %d minutes (threshold: %s%%)",
                    tx.getAccountId(), flagged, total, share, rule.getTimeWindowMinutes(),
                    rule.getThresholdAmount().toPlainString());
            return Optional.of(new AlertCandidate(rule.getId(), rule.getName(), rule.getSeverity(), desc, List.of(tx.getId())));
        }
        return Optional.empty();
    }

    public RuleType getSupportedType() { return RuleType.FLAGGED_PAYEE_CONCENTRATION; }
}
