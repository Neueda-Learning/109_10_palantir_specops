package com.transaction.transaction_monitoring.dto;

import com.transaction.transaction_monitoring.enums.Severity;
import jakarta.validation.constraints.NotBlank;

public class FlaggedEntityRequest {

    @NotBlank(message = "entityName is required")
    private String entityName;

    @NotBlank(message = "payeeId is required")
    private String payeeId;

    private String reason;

    private Severity riskLevel = Severity.MEDIUM;

    private boolean active = true;

    public String getEntityName() { return entityName; }
    public void setEntityName(String entityName) { this.entityName = entityName; }

    public String getPayeeId() { return payeeId; }
    public void setPayeeId(String payeeId) { this.payeeId = payeeId; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public Severity getRiskLevel() { return riskLevel; }
    public void setRiskLevel(Severity riskLevel) { this.riskLevel = riskLevel; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
