package com.transaction.transaction_monitoring.service;

import com.transaction.transaction_monitoring.dto.AlertActionRequest;
import com.transaction.transaction_monitoring.dto.AlertDetailResponse;
import com.transaction.transaction_monitoring.dto.AlertResponse;
import com.transaction.transaction_monitoring.dto.DashboardStatsResponse;
import com.transaction.transaction_monitoring.dto.TransactionResponse;
import com.transaction.transaction_monitoring.enums.AlertStatus;
import com.transaction.transaction_monitoring.exception.InvalidStateTransitionException;
import com.transaction.transaction_monitoring.exception.ResourceNotFoundException;
import com.transaction.transaction_monitoring.model.Alert;
import com.transaction.transaction_monitoring.model.AlertStatusHistory;
import com.transaction.transaction_monitoring.model.MonitoringRule;
import com.transaction.transaction_monitoring.model.Transaction;
import com.transaction.transaction_monitoring.repository.AlertRepository;
import com.transaction.transaction_monitoring.repository.MonitoringRuleRepository;
import com.transaction.transaction_monitoring.repository.TransactionRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class AlertService {

    private final AlertRepository alertRepository;
    private final MonitoringRuleRepository ruleRepository;
    private final TransactionRepository transactionRepository;

    public AlertService(AlertRepository alertRepository,
                        MonitoringRuleRepository ruleRepository,
                        TransactionRepository transactionRepository) {
        this.alertRepository = alertRepository;
        this.ruleRepository = ruleRepository;
        this.transactionRepository = transactionRepository;
    }

    public List<AlertResponse> findAll(String status, String severity, LocalDateTime from, LocalDateTime to) {
        return alertRepository.findAll(status, severity, from, to)
                .stream().map(this::toResponse).toList();
    }

    public AlertDetailResponse findById(Long id) {
        Alert alert = getOrThrow(id);
        return toDetailResponse(alert);
    }

    public AlertResponse acknowledge(Long id) {
        Alert alert = getOrThrow(id);
        requireStatus(alert, AlertStatus.OPEN, "acknowledge");
        LocalDateTime now = LocalDateTime.now();
        alertRepository.updateStatus(id, AlertStatus.ACKNOWLEDGED, null, now, null);
        recordHistory(id, AlertStatus.OPEN, AlertStatus.ACKNOWLEDGED, null);
        return toResponse(getOrThrow(id));
    }

    public AlertResponse investigate(Long id) {
        Alert alert = getOrThrow(id);
        requireStatus(alert, AlertStatus.ACKNOWLEDGED, "investigate");
        alertRepository.updateStatus(id, AlertStatus.INVESTIGATING, null, alert.getAcknowledgedAt(), null);
        recordHistory(id, AlertStatus.ACKNOWLEDGED, AlertStatus.INVESTIGATING, null);
        return toResponse(getOrThrow(id));
    }

    public AlertResponse close(Long id, AlertActionRequest req) {
        Alert alert = getOrThrow(id);
        requireStatus(alert, AlertStatus.INVESTIGATING, "close");
        LocalDateTime now = LocalDateTime.now();
        alertRepository.updateStatus(id, AlertStatus.CLOSED, req != null ? req.getResolutionNotes() : null,
                alert.getAcknowledgedAt(), now);
        recordHistory(id, AlertStatus.INVESTIGATING, AlertStatus.CLOSED, req != null ? req.getResolutionNotes() : null);
        return toResponse(getOrThrow(id));
    }

    public AlertResponse dismiss(Long id, AlertActionRequest req) {
        Alert alert = getOrThrow(id);
        if (alert.getStatus() != AlertStatus.ACKNOWLEDGED && alert.getStatus() != AlertStatus.INVESTIGATING) {
            throw new InvalidStateTransitionException(
                    "Cannot dismiss alert in status: " + alert.getStatus() + ". Must be ACKNOWLEDGED or INVESTIGATING.");
        }
        LocalDateTime now = LocalDateTime.now();
        alertRepository.updateStatus(id, AlertStatus.DISMISSED, req != null ? req.getResolutionNotes() : null,
                alert.getAcknowledgedAt(), now);
        recordHistory(id, alert.getStatus(), AlertStatus.DISMISSED, req != null ? req.getResolutionNotes() : null);
        return toResponse(getOrThrow(id));
    }

    public DashboardStatsResponse getStats() {
        Map<String, Object> raw = alertRepository.getStats();
        DashboardStatsResponse dto = new DashboardStatsResponse();
        dto.setOpenCount(toLong(raw.get("openCount")));
        dto.setAcknowledgedCount(toLong(raw.get("acknowledgedCount")));
        dto.setInvestigatingCount(toLong(raw.get("investigatingCount")));
        dto.setClosedCount(toLong(raw.get("closedCount")));
        dto.setDismissedCount(toLong(raw.get("dismissedCount")));
        dto.setAlertsToday(toLong(raw.get("alertsToday")));
        dto.setAvgResolutionMinutes(toLong(raw.get("avgResolutionMinutes")));
        return dto;
    }

    // --- helpers ---

    private Alert getOrThrow(Long id) {
        return alertRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Alert not found: " + id));
    }

    private void requireStatus(Alert alert, AlertStatus required, String action) {
        if (alert.getStatus() != required) {
            throw new InvalidStateTransitionException(
                    "Cannot " + action + " alert in status: " + alert.getStatus() + ". Must be " + required + ".");
        }
    }

    private void recordHistory(Long alertId, AlertStatus from, AlertStatus to, String notes) {
        AlertStatusHistory h = new AlertStatusHistory();
        h.setAlertId(alertId);
        h.setPreviousStatus(from != null ? from.name() : null);
        h.setNewStatus(to.name());
        h.setNotes(notes);
        alertRepository.saveStatusHistory(h);
    }

    private AlertResponse toResponse(Alert a) {
        AlertResponse r = new AlertResponse();
        r.setId(a.getId());
        r.setRuleId(a.getRuleId());
        r.setSeverity(a.getSeverity());
        r.setStatus(a.getStatus());
        r.setDescription(a.getDescription());
        r.setResolutionNotes(a.getResolutionNotes());
        r.setCreatedAt(a.getCreatedAt());
        r.setUpdatedAt(a.getUpdatedAt());
        r.setAcknowledgedAt(a.getAcknowledgedAt());
        r.setClosedAt(a.getClosedAt());
        ruleRepository.findById(a.getRuleId()).ifPresent(rule -> r.setRuleName(rule.getName()));
        return r;
    }

    private AlertDetailResponse toDetailResponse(Alert a) {
        AlertDetailResponse r = new AlertDetailResponse();
        r.setId(a.getId());
        r.setRuleId(a.getRuleId());
        r.setSeverity(a.getSeverity());
        r.setStatus(a.getStatus());
        r.setDescription(a.getDescription());
        r.setResolutionNotes(a.getResolutionNotes());
        r.setCreatedAt(a.getCreatedAt());
        r.setUpdatedAt(a.getUpdatedAt());
        r.setAcknowledgedAt(a.getAcknowledgedAt());
        r.setClosedAt(a.getClosedAt());
        ruleRepository.findById(a.getRuleId()).ifPresent(rule -> r.setRuleName(rule.getName()));

        List<Long> txIds = alertRepository.findTransactionIdsByAlertId(a.getId());
        List<TransactionResponse> txResponses = transactionRepository.findByIds(txIds).stream().map(tx -> {
            TransactionResponse tr = new TransactionResponse();
            tr.setId(tx.getId());
            tr.setAccountId(tx.getAccountId());
            tr.setPayeeId(tx.getPayeeId());
            tr.setAmount(tx.getAmount());
            tr.setType(tx.getType());
            tr.setStatus(tx.getStatus());
            tr.setDescription(tx.getDescription());
            tr.setTimestamp(tx.getTimestamp());
            tr.setAlertIds(List.of());
            return tr;
        }).toList();
        r.setTransactions(txResponses);
        r.setHistory(alertRepository.findStatusHistoryByAlertId(a.getId()));
        return r;
    }

    private long toLong(Object val) {
        if (val == null) return 0L;
        if (val instanceof Number n) return n.longValue();
        return 0L;
    }
}
