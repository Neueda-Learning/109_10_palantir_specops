package com.transaction.transaction_monitoring.service;

import com.transaction.transaction_monitoring.dto.RuleRequest;
import com.transaction.transaction_monitoring.dto.RuleResponse;
import com.transaction.transaction_monitoring.exception.ResourceNotFoundException;
import com.transaction.transaction_monitoring.model.MonitoringRule;
import com.transaction.transaction_monitoring.repository.MonitoringRuleRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RuleService {

    private final MonitoringRuleRepository ruleRepository;

    public RuleService(MonitoringRuleRepository ruleRepository) {
        this.ruleRepository = ruleRepository;
    }

    public List<RuleResponse> findAll() {
        return ruleRepository.findAll().stream().map(this::toResponse).toList();
    }

    public RuleResponse findById(Long id) {
        return toResponse(getOrThrow(id));
    }

    public RuleResponse create(RuleRequest req) {
        MonitoringRule rule = fromRequest(req);
        ruleRepository.save(rule);
        return toResponse(rule);
    }

    public RuleResponse update(Long id, RuleRequest req) {
        MonitoringRule existing = getOrThrow(id);
        MonitoringRule updated = fromRequest(req);
        updated.setId(existing.getId());
        ruleRepository.update(updated);
        return toResponse(getOrThrow(id));
    }

    public void delete(Long id) {
        getOrThrow(id);
        ruleRepository.deleteById(id);
    }

    public RuleResponse activate(Long id) {
        getOrThrow(id);
        ruleRepository.setActive(id, true);
        return toResponse(getOrThrow(id));
    }

    public RuleResponse deactivate(Long id) {
        getOrThrow(id);
        ruleRepository.setActive(id, false);
        return toResponse(getOrThrow(id));
    }

    private MonitoringRule getOrThrow(Long id) {
        return ruleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Rule not found: " + id));
    }

    private MonitoringRule fromRequest(RuleRequest req) {
        MonitoringRule r = new MonitoringRule();
        r.setName(req.getName());
        r.setDescription(req.getDescription());
        r.setType(req.getType());
        r.setSeverity(req.getSeverity());
        r.setActive(req.isActive());
        r.setThresholdAmount(req.getThresholdAmount());
        r.setTransactionCount(req.getTransactionCount());
        r.setTimeWindowMinutes(req.getTimeWindowMinutes());
        r.setDailyLimit(req.getDailyLimit());
        return r;
    }

    private RuleResponse toResponse(MonitoringRule r) {
        RuleResponse dto = new RuleResponse();
        dto.setId(r.getId());
        dto.setName(r.getName());
        dto.setDescription(r.getDescription());
        dto.setType(r.getType());
        dto.setSeverity(r.getSeverity());
        dto.setActive(r.isActive());
        dto.setThresholdAmount(r.getThresholdAmount());
        dto.setTransactionCount(r.getTransactionCount());
        dto.setTimeWindowMinutes(r.getTimeWindowMinutes());
        dto.setDailyLimit(r.getDailyLimit());
        dto.setCreatedAt(r.getCreatedAt());
        dto.setUpdatedAt(r.getUpdatedAt());
        return dto;
    }
}
