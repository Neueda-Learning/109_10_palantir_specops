package com.transaction.transaction_monitoring.service;

import com.transaction.transaction_monitoring.dto.TransactionRequest;
import com.transaction.transaction_monitoring.enums.Severity;
import com.transaction.transaction_monitoring.enums.TransactionStatus;
import com.transaction.transaction_monitoring.enums.TransactionType;
import com.transaction.transaction_monitoring.model.Alert;
import com.transaction.transaction_monitoring.model.Transaction;
import com.transaction.transaction_monitoring.repository.AlertRepository;
import com.transaction.transaction_monitoring.repository.TransactionRepository;
import com.transaction.transaction_monitoring.rules.AlertCandidate;
import com.transaction.transaction_monitoring.rules.RuleEngineService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class TransactionServiceTest {

    @Test
    void create_shouldSuppressDuplicateOpenAlert_forSameRuleAndAccount() {
        TransactionRepository txRepo = mock(TransactionRepository.class);
        AlertRepository alertRepo = mock(AlertRepository.class);
        RuleEngineService ruleEngineService = mock(RuleEngineService.class);
        TransactionService service = new TransactionService(txRepo, alertRepo, ruleEngineService);

        TransactionRequest req = request("ACC-1", "PAYEE-1", "12000.00");

        doAnswer(invocation -> {
            Transaction tx = invocation.getArgument(0);
            tx.setId(101L);
            return tx;
        }).when(txRepo).save(any(Transaction.class));

        when(ruleEngineService.evaluate(any(Transaction.class))).thenReturn(List.of(
                new AlertCandidate(1L, "High Value", Severity.HIGH, "desc", List.of(101L))
        ));
        when(alertRepo.existsOpenAlertForRule(1L, "ACC-1")).thenReturn(true);

        var response = service.create(req);

        assertThat(response.getAlertIds()).isEmpty();
        verify(alertRepo, never()).save(any(Alert.class));
        verify(alertRepo, never()).saveAlertTransaction(anyLong(), anyLong());
        verify(alertRepo, never()).saveStatusHistory(any());
    }

    @Test
    void create_shouldPersistAlertLinksAndHistory_whenNoDuplicateOpenAlertExists() {
        TransactionRepository txRepo = mock(TransactionRepository.class);
        AlertRepository alertRepo = mock(AlertRepository.class);
        RuleEngineService ruleEngineService = mock(RuleEngineService.class);
        TransactionService service = new TransactionService(txRepo, alertRepo, ruleEngineService);

        TransactionRequest req = request("ACC-2", "PAYEE-2", "18000.00");

        doAnswer(invocation -> {
            Transaction tx = invocation.getArgument(0);
            tx.setId(202L);
            return tx;
        }).when(txRepo).save(any(Transaction.class));

        when(ruleEngineService.evaluate(any(Transaction.class))).thenReturn(List.of(
                new AlertCandidate(2L, "Rapid Transaction Velocity", Severity.MEDIUM, "velocity hit", List.of(202L, 203L))
        ));
        when(alertRepo.existsOpenAlertForRule(2L, "ACC-2")).thenReturn(false);

        doAnswer(invocation -> {
            Alert alert = invocation.getArgument(0);
            alert.setId(9001L);
            return alert;
        }).when(alertRepo).save(any(Alert.class));

        var response = service.create(req);

        assertThat(response.getAlertIds()).containsExactly(9001L);
        verify(alertRepo, times(1)).save(any(Alert.class));
        verify(alertRepo, times(2)).saveAlertTransaction(eq(9001L), anyLong());
        verify(alertRepo, times(1)).saveStatusHistory(any());
    }

    private TransactionRequest request(String accountId, String payeeId, String amount) {
        TransactionRequest req = new TransactionRequest();
        req.setAccountId(accountId);
        req.setPayeeId(payeeId);
        req.setAmount(new BigDecimal(amount));
        req.setType(TransactionType.DEBIT);
        req.setStatus(TransactionStatus.COMPLETED);
        req.setDescription("test tx");
        return req;
    }
}