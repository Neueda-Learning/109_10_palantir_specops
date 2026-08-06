package com.transaction.transaction_monitoring.service;

import com.transaction.transaction_monitoring.dto.AlertActionRequest;
import com.transaction.transaction_monitoring.enums.AlertStatus;
import com.transaction.transaction_monitoring.enums.Severity;
import com.transaction.transaction_monitoring.enums.TransactionStatus;
import com.transaction.transaction_monitoring.enums.TransactionType;
import com.transaction.transaction_monitoring.exception.InvalidStateTransitionException;
import com.transaction.transaction_monitoring.model.Alert;
import com.transaction.transaction_monitoring.model.AlertStatusHistory;
import com.transaction.transaction_monitoring.model.MonitoringRule;
import com.transaction.transaction_monitoring.model.Transaction;
import com.transaction.transaction_monitoring.repository.AlertRepository;
import com.transaction.transaction_monitoring.repository.MonitoringRuleRepository;
import com.transaction.transaction_monitoring.repository.TransactionRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AlertServiceTest {

    @Test
    void acknowledge_shouldUpdateStatusAndWriteHistory() {
        AlertRepository alertRepo = mock(AlertRepository.class);
        MonitoringRuleRepository ruleRepo = mock(MonitoringRuleRepository.class);
        TransactionRepository txRepo = mock(TransactionRepository.class);
        AlertService service = new AlertService(alertRepo, ruleRepo, txRepo);

        Alert open = alert(1L, 10L, AlertStatus.OPEN);
        Alert acknowledged = alert(1L, 10L, AlertStatus.ACKNOWLEDGED);
        acknowledged.setAcknowledgedAt(LocalDateTime.now());

        MonitoringRule rule = new MonitoringRule();
        rule.setId(10L);
        rule.setName("High Value Transaction");

        when(alertRepo.findById(1L)).thenReturn(Optional.of(open), Optional.of(acknowledged));
        when(ruleRepo.findById(10L)).thenReturn(Optional.of(rule));

        var response = service.acknowledge(1L);

        assertThat(response.getStatus()).isEqualTo(AlertStatus.ACKNOWLEDGED);
        verify(alertRepo).updateStatus(eq(1L), eq(AlertStatus.ACKNOWLEDGED), isNull(), any(LocalDateTime.class), isNull());
        verify(alertRepo).saveStatusHistory(any(AlertStatusHistory.class));
    }

    @Test
    void close_shouldRejectIllegalTransition_withConflictException() {
        AlertRepository alertRepo = mock(AlertRepository.class);
        MonitoringRuleRepository ruleRepo = mock(MonitoringRuleRepository.class);
        TransactionRepository txRepo = mock(TransactionRepository.class);
        AlertService service = new AlertService(alertRepo, ruleRepo, txRepo);

        Alert open = alert(1L, 10L, AlertStatus.OPEN);
        when(alertRepo.findById(1L)).thenReturn(Optional.of(open));

        AlertActionRequest req = new AlertActionRequest();
        req.setResolutionNotes("notes");

        assertThatThrownBy(() -> service.close(1L, req))
                .isInstanceOf(InvalidStateTransitionException.class)
                .hasMessageContaining("Cannot close alert in status: OPEN");

        verify(alertRepo, never()).updateStatus(anyLong(), any(), anyString(), any(), any());
        verify(alertRepo, never()).saveStatusHistory(any());
    }

    @Test
    void investigate_shouldUpdateStatusAndWriteHistory() {
        AlertRepository alertRepo = mock(AlertRepository.class);
        MonitoringRuleRepository ruleRepo = mock(MonitoringRuleRepository.class);
        TransactionRepository txRepo = mock(TransactionRepository.class);
        AlertService service = new AlertService(alertRepo, ruleRepo, txRepo);

        Alert acknowledged = alert(2L, 11L, AlertStatus.ACKNOWLEDGED);
        acknowledged.setAcknowledgedAt(LocalDateTime.now().minusMinutes(2));
        Alert investigating = alert(2L, 11L, AlertStatus.INVESTIGATING);

        MonitoringRule rule = new MonitoringRule();
        rule.setId(11L);
        rule.setName("Velocity");

        when(alertRepo.findById(2L)).thenReturn(Optional.of(acknowledged), Optional.of(investigating));
        when(ruleRepo.findById(11L)).thenReturn(Optional.of(rule));

        var response = service.investigate(2L);

        assertThat(response.getStatus()).isEqualTo(AlertStatus.INVESTIGATING);
        verify(alertRepo).updateStatus(eq(2L), eq(AlertStatus.INVESTIGATING), isNull(), eq(acknowledged.getAcknowledgedAt()), isNull());
        verify(alertRepo).saveStatusHistory(any(AlertStatusHistory.class));
    }

    @Test
    void close_shouldUpdateStatusSetClosedAtAndWriteHistory() {
        AlertRepository alertRepo = mock(AlertRepository.class);
        MonitoringRuleRepository ruleRepo = mock(MonitoringRuleRepository.class);
        TransactionRepository txRepo = mock(TransactionRepository.class);
        AlertService service = new AlertService(alertRepo, ruleRepo, txRepo);

        Alert investigating = alert(3L, 12L, AlertStatus.INVESTIGATING);
        investigating.setAcknowledgedAt(LocalDateTime.now().minusMinutes(8));
        Alert closed = alert(3L, 12L, AlertStatus.CLOSED);
        closed.setClosedAt(LocalDateTime.now());

        MonitoringRule rule = new MonitoringRule();
        rule.setId(12L);
        rule.setName("Daily Limit");

        AlertActionRequest req = new AlertActionRequest();
        req.setResolutionNotes("resolved");

        when(alertRepo.findById(3L)).thenReturn(Optional.of(investigating), Optional.of(closed));
        when(ruleRepo.findById(12L)).thenReturn(Optional.of(rule));

        var response = service.close(3L, req);

        assertThat(response.getStatus()).isEqualTo(AlertStatus.CLOSED);
        verify(alertRepo).updateStatus(eq(3L), eq(AlertStatus.CLOSED), eq("resolved"), eq(investigating.getAcknowledgedAt()), any(LocalDateTime.class));
        verify(alertRepo).saveStatusHistory(any(AlertStatusHistory.class));
    }

    @Test
    void findById_shouldIncludeLinkedTransactionsAndHistory() {
        AlertRepository alertRepo = mock(AlertRepository.class);
        MonitoringRuleRepository ruleRepo = mock(MonitoringRuleRepository.class);
        TransactionRepository txRepo = mock(TransactionRepository.class);
        AlertService service = new AlertService(alertRepo, ruleRepo, txRepo);

        Alert alert = alert(9L, 5L, AlertStatus.INVESTIGATING);
        MonitoringRule rule = new MonitoringRule();
        rule.setId(5L);
        rule.setName("New Payee Transaction");

        Transaction tx = new Transaction();
        tx.setId(101L);
        tx.setAccountId("ACC-TEST");
        tx.setPayeeId("PAYEE-TEST");
        tx.setAmount(new BigDecimal("50.00"));
        tx.setType(TransactionType.DEBIT);
        tx.setStatus(TransactionStatus.COMPLETED);
        tx.setTimestamp(LocalDateTime.now());

        AlertStatusHistory history = new AlertStatusHistory();
        history.setAlertId(9L);
        history.setPreviousStatus("OPEN");
        history.setNewStatus("ACKNOWLEDGED");

        when(alertRepo.findById(9L)).thenReturn(Optional.of(alert));
        when(ruleRepo.findById(5L)).thenReturn(Optional.of(rule));
        when(alertRepo.findTransactionIdsByAlertId(9L)).thenReturn(List.of(101L));
        when(txRepo.findByIds(List.of(101L))).thenReturn(List.of(tx));
        when(alertRepo.findStatusHistoryByAlertId(9L)).thenReturn(List.of(history));

        var detail = service.findById(9L);

        assertThat(detail.getTransactions()).hasSize(1);
        assertThat(detail.getTransactions().get(0).getId()).isEqualTo(101L);
        assertThat(detail.getHistory()).hasSize(1);
        assertThat(detail.getHistory().get(0).getNewStatus()).isEqualTo("ACKNOWLEDGED");
    }

    private Alert alert(Long id, Long ruleId, AlertStatus status) {
        Alert alert = new Alert();
        alert.setId(id);
        alert.setRuleId(ruleId);
        alert.setStatus(status);
        alert.setSeverity(Severity.HIGH);
        alert.setDescription("desc");
        alert.setCreatedAt(LocalDateTime.now());
        alert.setUpdatedAt(LocalDateTime.now());
        return alert;
    }
}