package com.transaction.transaction_monitoring.rules;

import com.transaction.transaction_monitoring.enums.Severity;

import java.util.List;

public class AlertCandidate {
    private final Long ruleId;
    private final String ruleName;
    private final Severity severity;
    private final String description;
    private final List<Long> triggeringTransactionIds;

    public AlertCandidate(Long ruleId, String ruleName, Severity severity,
                          String description, List<Long> triggeringTransactionIds) {
        this.ruleId = ruleId;
        this.ruleName = ruleName;
        this.severity = severity;
        this.description = description;
        this.triggeringTransactionIds = triggeringTransactionIds;
    }

    public Long getRuleId() { return ruleId; }
    public String getRuleName() { return ruleName; }
    public Severity getSeverity() { return severity; }
    public String getDescription() { return description; }
    public List<Long> getTriggeringTransactionIds() { return triggeringTransactionIds; }
}
