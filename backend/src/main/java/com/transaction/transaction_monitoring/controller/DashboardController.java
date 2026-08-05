package com.transaction.transaction_monitoring.controller;

import com.transaction.transaction_monitoring.dto.DashboardStatsResponse;
import com.transaction.transaction_monitoring.service.AlertService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@Tag(name = "Dashboard", description = "Dashboard summary statistics")
public class DashboardController {

    private final AlertService alertService;

    public DashboardController(AlertService alertService) {
        this.alertService = alertService;
    }

    @GetMapping("/stats")
    @Operation(summary = "Get dashboard summary statistics")
    public DashboardStatsResponse getStats() {
        return alertService.getStats();
    }
}
