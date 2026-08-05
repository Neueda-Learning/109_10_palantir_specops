package com.transaction.transaction_monitoring.controller;

import com.transaction.transaction_monitoring.dto.AlertActionRequest;
import com.transaction.transaction_monitoring.dto.AlertDetailResponse;
import com.transaction.transaction_monitoring.dto.AlertResponse;
import com.transaction.transaction_monitoring.dto.DashboardStatsResponse;
import com.transaction.transaction_monitoring.service.AlertService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/alerts")
@Tag(name = "Alerts", description = "Alert lifecycle management endpoints")
public class AlertController {

    private final AlertService alertService;

    public AlertController(AlertService alertService) {
        this.alertService = alertService;
    }

    @GetMapping
    @Operation(summary = "List alerts with optional filters")
    public List<AlertResponse> findAll(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) LocalDateTime from,
            @RequestParam(required = false) LocalDateTime to) {
        return alertService.findAll(status, severity, from, to);
    }

    @GetMapping("/stats")
    @Operation(summary = "Get alert statistics (counts by status, avg resolution time)")
    public DashboardStatsResponse getStats() {
        return alertService.getStats();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get full alert detail including transactions and history")
    public AlertDetailResponse findById(@PathVariable Long id) {
        return alertService.findById(id);
    }

    @PatchMapping("/{id}/acknowledge")
    @Operation(summary = "Acknowledge an alert (OPEN → ACKNOWLEDGED)")
    public AlertResponse acknowledge(@PathVariable Long id) {
        return alertService.acknowledge(id);
    }

    @PatchMapping("/{id}/investigate")
    @Operation(summary = "Start investigating an alert (ACKNOWLEDGED → INVESTIGATING)")
    public AlertResponse investigate(@PathVariable Long id) {
        return alertService.investigate(id);
    }

    @PatchMapping("/{id}/close")
    @Operation(summary = "Close an alert (INVESTIGATING → CLOSED)")
    public AlertResponse close(@PathVariable Long id,
                               @RequestBody(required = false) AlertActionRequest req) {
        return alertService.close(id, req);
    }

    @PatchMapping("/{id}/dismiss")
    @Operation(summary = "Dismiss an alert as false positive (ACKNOWLEDGED or INVESTIGATING → DISMISSED)")
    public AlertResponse dismiss(@PathVariable Long id,
                                 @RequestBody(required = false) AlertActionRequest req) {
        return alertService.dismiss(id, req);
    }
}
