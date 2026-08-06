package com.transaction.transaction_monitoring.dto;

import com.transaction.transaction_monitoring.enums.Severity;

import java.time.LocalDateTime;

public class FlaggedEntityResponse {
    private Long id;
    private String entityName;
    private String payeeId;
    private String reason;
    private Severity riskLevel;
    private boolean active;
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

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

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
