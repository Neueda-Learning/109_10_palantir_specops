package com.transaction.transaction_monitoring.rules;

import com.transaction.transaction_monitoring.enums.RuleType;
import com.transaction.transaction_monitoring.model.MonitoringRule;
import com.transaction.transaction_monitoring.model.Transaction;
import com.transaction.transaction_monitoring.repository.MonitoringRuleRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class RuleEngineService {

    private final MonitoringRuleRepository ruleRepository;
    private final Map<RuleType, RuleEvaluator> evaluators;

    public RuleEngineService(MonitoringRuleRepository ruleRepository,
                             AmountThresholdRuleEvaluator amountEvaluator,
                             VelocityRuleEvaluator velocityEvaluator,
                             NewPayeeRuleEvaluator newPayeeEvaluator,
                             DailyLimitRuleEvaluator dailyLimitEvaluator,
                             FlaggedPayeeRuleEvaluator flaggedPayeeEvaluator,
                             FlaggedPayeeConcentrationRuleEvaluator flaggedConcentrationEvaluator) {
        this.ruleRepository = ruleRepository;
        this.evaluators = new EnumMap<>(RuleType.class);
        evaluators.put(RuleType.AMOUNT_THRESHOLD, amountEvaluator);
        evaluators.put(RuleType.VELOCITY, velocityEvaluator);
        evaluators.put(RuleType.NEW_PAYEE, newPayeeEvaluator);
        evaluators.put(RuleType.DAILY_LIMIT, dailyLimitEvaluator);
        evaluators.put(RuleType.FLAGGED_PAYEE, flaggedPayeeEvaluator);
        evaluators.put(RuleType.FLAGGED_PAYEE_CONCENTRATION, flaggedConcentrationEvaluator);
    }

    public List<AlertCandidate> evaluate(Transaction transaction) {
        List<MonitoringRule> activeRules = ruleRepository.findAllActive();
        List<AlertCandidate> candidates = new ArrayList<>();

        for (MonitoringRule rule : activeRules) {
            RuleEvaluator evaluator = evaluators.get(rule.getType());
            if (evaluator == null) continue;
            Optional<AlertCandidate> candidate = evaluator.evaluate(transaction, rule);
            candidate.ifPresent(candidates::add);
        }
        return candidates;
    }
}
