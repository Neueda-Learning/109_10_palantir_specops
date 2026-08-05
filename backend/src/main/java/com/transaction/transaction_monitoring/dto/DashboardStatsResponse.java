package com.transaction.transaction_monitoring.dto;

public class DashboardStatsResponse {
    private long openCount;
    private long acknowledgedCount;
    private long investigatingCount;
    private long closedCount;
    private long dismissedCount;
    private long alertsToday;
    private long avgResolutionMinutes;

    public long getOpenCount() { return openCount; }
    public void setOpenCount(long openCount) { this.openCount = openCount; }

    public long getAcknowledgedCount() { return acknowledgedCount; }
    public void setAcknowledgedCount(long acknowledgedCount) { this.acknowledgedCount = acknowledgedCount; }

    public long getInvestigatingCount() { return investigatingCount; }
    public void setInvestigatingCount(long investigatingCount) { this.investigatingCount = investigatingCount; }

    public long getClosedCount() { return closedCount; }
    public void setClosedCount(long closedCount) { this.closedCount = closedCount; }

    public long getDismissedCount() { return dismissedCount; }
    public void setDismissedCount(long dismissedCount) { this.dismissedCount = dismissedCount; }

    public long getAlertsToday() { return alertsToday; }
    public void setAlertsToday(long alertsToday) { this.alertsToday = alertsToday; }

    public long getAvgResolutionMinutes() { return avgResolutionMinutes; }
    public void setAvgResolutionMinutes(long avgResolutionMinutes) { this.avgResolutionMinutes = avgResolutionMinutes; }
}
