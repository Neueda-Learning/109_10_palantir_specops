package com.transaction.transaction_monitoring.controller;

import com.transaction.transaction_monitoring.dto.RuleRequest;
import com.transaction.transaction_monitoring.dto.RuleResponse;
import com.transaction.transaction_monitoring.service.RuleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rules")
@Tag(name = "Monitoring Rules", description = "CRUD and activation of monitoring rules")
public class RuleController {

    private final RuleService ruleService;

    public RuleController(RuleService ruleService) {
        this.ruleService = ruleService;
    }

    @GetMapping
    @Operation(summary = "List all monitoring rules")
    public List<RuleResponse> findAll() {
        return ruleService.findAll();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a monitoring rule by ID")
    public RuleResponse findById(@PathVariable Long id) {
        return ruleService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new monitoring rule")
    public RuleResponse create(@Valid @RequestBody RuleRequest req) {
        return ruleService.create(req);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing monitoring rule")
    public RuleResponse update(@PathVariable Long id, @Valid @RequestBody RuleRequest req) {
        return ruleService.update(id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a monitoring rule")
    public void delete(@PathVariable Long id) {
        ruleService.delete(id);
    }

    @PatchMapping("/{id}/activate")
    @Operation(summary = "Activate a monitoring rule")
    public RuleResponse activate(@PathVariable Long id) {
        return ruleService.activate(id);
    }

    @PatchMapping("/{id}/deactivate")
    @Operation(summary = "Deactivate a monitoring rule")
    public RuleResponse deactivate(@PathVariable Long id) {
        return ruleService.deactivate(id);
    }
}
