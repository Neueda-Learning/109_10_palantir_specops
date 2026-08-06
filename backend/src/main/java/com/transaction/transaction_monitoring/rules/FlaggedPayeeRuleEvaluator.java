package com.transaction.transaction_monitoring.rules;

import com.transaction.transaction_monitoring.enums.RuleType;
import com.transaction.transaction_monitoring.model.FlaggedEntity;
import com.transaction.transaction_monitoring.model.MonitoringRule;
import com.transaction.transaction_monitoring.model.Transaction;
import com.transaction.transaction_monitoring.repository.FlaggedEntityRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class FlaggedPayeeRuleEvaluator implements RuleEvaluator {

    private final FlaggedEntityRepository flaggedEntityRepository;

    public FlaggedPayeeRuleEvaluator(FlaggedEntityRepository flaggedEntityRepository) {
        this.flaggedEntityRepository = flaggedEntityRepository;
    }

    @Override
    public Optional<AlertCandidate> evaluate(Transaction tx, MonitoringRule rule) {
        Optional<FlaggedEntity> match = flaggedEntityRepository.findActiveByPayeeId(tx.getPayeeId());
        if (match.isEmpty()) return Optional.empty();

        FlaggedEntity fe = match.get();
        String desc = String.format("Account %s sent a transaction to flagged organisation '%s' (%s): %s",
                tx.getAccountId(), fe.getEntityName(), fe.getPayeeId(),
                fe.getReason() != null ? fe.getReason() : "No reason recorded");
        return Optional.of(new AlertCandidate(rule.getId(), rule.getName(), rule.getSeverity(), desc, List.of(tx.getId())));
    }

    public RuleType getSupportedType() { return RuleType.FLAGGED_PAYEE; }
}
