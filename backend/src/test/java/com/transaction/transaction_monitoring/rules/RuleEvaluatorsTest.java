package com.transaction.transaction_monitoring.rules;

import com.transaction.transaction_monitoring.enums.RuleType;
import com.transaction.transaction_monitoring.enums.Severity;
import com.transaction.transaction_monitoring.enums.TransactionType;
import com.transaction.transaction_monitoring.model.MonitoringRule;
import com.transaction.transaction_monitoring.model.Transaction;
import com.transaction.transaction_monitoring.repository.TransactionRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RuleEvaluatorsTest {

    @Test
    void amountThreshold_shouldCreateCandidate_whenAmountExceedsThreshold() {
        AmountThresholdRuleEvaluator evaluator = new AmountThresholdRuleEvaluator();
        MonitoringRule rule = rule(1L, RuleType.AMOUNT_THRESHOLD, Severity.HIGH);
        rule.setThresholdAmount(new BigDecimal("10000.00"));
        Transaction tx = tx(10L, "ACC-1", "PAYEE-1", new BigDecimal("12000.00"), TransactionType.DEBIT);

        var result = evaluator.evaluate(tx, rule);

        assertThat(result).isPresent();
        assertThat(result.get().getRuleId()).isEqualTo(1L);
        assertThat(result.get().getTriggeringTransactionIds()).containsExactly(10L);
    }

    @Test
    void velocity_shouldCreateCandidate_whenCountExceedsLimit() {
        TransactionRepository repo = mock(TransactionRepository.class);
        VelocityRuleEvaluator evaluator = new VelocityRuleEvaluator(repo);
        MonitoringRule rule = rule(2L, RuleType.VELOCITY, Severity.MEDIUM);
        rule.setTransactionCount(5);
        rule.setTimeWindowMinutes(10);
        Transaction tx = tx(20L, "ACC-2", "PAYEE-2", new BigDecimal("200.00"), TransactionType.DEBIT);

        when(repo.countByAccountInWindow("ACC-2", 10)).thenReturn(6);

        var result = evaluator.evaluate(tx, rule);

        assertThat(result).isPresent();
        assertThat(result.get().getDescription()).contains("ACC-2");
        assertThat(result.get().getSeverity()).isEqualTo(Severity.MEDIUM);
    }

    @Test
    void newPayee_shouldCreateCandidate_onlyForFirstPayeeTransaction() {
        TransactionRepository repo = mock(TransactionRepository.class);
        NewPayeeRuleEvaluator evaluator = new NewPayeeRuleEvaluator(repo);
        MonitoringRule rule = rule(3L, RuleType.NEW_PAYEE, Severity.LOW);
        Transaction tx = tx(30L, "ACC-3", "PAYEE-NEW", new BigDecimal("75.00"), TransactionType.DEBIT);

        when(repo.countPreviousPayeeTransactions("ACC-3", "PAYEE-NEW", tx.getTimestamp())).thenReturn(0);
        assertThat(evaluator.evaluate(tx, rule)).isPresent();

        when(repo.countPreviousPayeeTransactions("ACC-3", "PAYEE-NEW", tx.getTimestamp())).thenReturn(2);
        assertThat(evaluator.evaluate(tx, rule)).isEmpty();
    }

    @Test
    void dailyLimit_shouldCreateCandidate_forDebitWhenSumExceedsLimit() {
        TransactionRepository repo = mock(TransactionRepository.class);
        DailyLimitRuleEvaluator evaluator = new DailyLimitRuleEvaluator(repo);
        MonitoringRule rule = rule(4L, RuleType.DAILY_LIMIT, Severity.HIGH);
        rule.setDailyLimit(new BigDecimal("50000.00"));
        Transaction tx = tx(40L, "ACC-4", "PAYEE-4", new BigDecimal("4000.00"), TransactionType.DEBIT);

        when(repo.sumDebitsForAccountToday("ACC-4")).thenReturn(new BigDecimal("54000.00"));

        var result = evaluator.evaluate(tx, rule);

        assertThat(result).isPresent();
        assertThat(result.get().getDescription()).contains("ACC-4");
    }

    @Test
    void dailyLimit_shouldNotCreateCandidate_forCreditTransactions() {
        TransactionRepository repo = mock(TransactionRepository.class);
        DailyLimitRuleEvaluator evaluator = new DailyLimitRuleEvaluator(repo);
        MonitoringRule rule = rule(4L, RuleType.DAILY_LIMIT, Severity.HIGH);
        rule.setDailyLimit(new BigDecimal("50000.00"));
        Transaction tx = tx(41L, "ACC-4", "PAYEE-4", new BigDecimal("4000.00"), TransactionType.CREDIT);

        var result = evaluator.evaluate(tx, rule);

        assertThat(result).isEmpty();
    }

    private MonitoringRule rule(Long id, RuleType type, Severity severity) {
        MonitoringRule rule = new MonitoringRule();
        rule.setId(id);
        rule.setName(type.name() + " Rule");
        rule.setType(type);
        rule.setSeverity(severity);
        rule.setActive(true);
        return rule;
    }

    private Transaction tx(Long id, String accountId, String payeeId, BigDecimal amount, TransactionType type) {
        Transaction tx = new Transaction();
        tx.setId(id);
        tx.setAccountId(accountId);
        tx.setPayeeId(payeeId);
        tx.setAmount(amount);
        tx.setType(type);
        tx.setTimestamp(LocalDateTime.now());
        return tx;
    }
}