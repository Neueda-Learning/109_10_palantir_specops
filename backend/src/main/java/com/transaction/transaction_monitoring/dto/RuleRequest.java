package com.transaction.transaction_monitoring.dto;

import com.transaction.transaction_monitoring.enums.RuleType;
import com.transaction.transaction_monitoring.enums.Severity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class RuleRequest {

    @NotBlank(message = "name is required")
    private String name;

    private String description;

    @NotNull(message = "type is required")
    private RuleType type;

    @NotNull(message = "severity is required")
    private Severity severity;

    private boolean active = true;
    private BigDecimal thresholdAmount;
    private Integer transactionCount;
    private Integer timeWindowMinutes;
    private BigDecimal dailyLimit;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public RuleType getType() { return type; }
    public void setType(RuleType type) { this.type = type; }

    public Severity getSeverity() { return severity; }
    public void setSeverity(Severity severity) { this.severity = severity; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public BigDecimal getThresholdAmount() { return thresholdAmount; }
    public void setThresholdAmount(BigDecimal thresholdAmount) { this.thresholdAmount = thresholdAmount; }

    public Integer getTransactionCount() { return transactionCount; }
    public void setTransactionCount(Integer transactionCount) { this.transactionCount = transactionCount; }

    public Integer getTimeWindowMinutes() { return timeWindowMinutes; }
    public void setTimeWindowMinutes(Integer timeWindowMinutes) { this.timeWindowMinutes = timeWindowMinutes; }

    public BigDecimal getDailyLimit() { return dailyLimit; }
    public void setDailyLimit(BigDecimal dailyLimit) { this.dailyLimit = dailyLimit; }
}
