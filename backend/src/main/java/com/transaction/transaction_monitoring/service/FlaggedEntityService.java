package com.transaction.transaction_monitoring.service;

import com.transaction.transaction_monitoring.dto.FlaggedEntityRequest;
import com.transaction.transaction_monitoring.dto.FlaggedEntityResponse;
import com.transaction.transaction_monitoring.enums.Severity;
import com.transaction.transaction_monitoring.exception.ResourceNotFoundException;
import com.transaction.transaction_monitoring.model.FlaggedEntity;
import com.transaction.transaction_monitoring.repository.FlaggedEntityRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class FlaggedEntityService {

    private final FlaggedEntityRepository flaggedEntityRepository;

    public FlaggedEntityService(FlaggedEntityRepository flaggedEntityRepository) {
        this.flaggedEntityRepository = flaggedEntityRepository;
    }

    public List<FlaggedEntityResponse> findAll() {
        return flaggedEntityRepository.findAll().stream().map(this::toResponse).toList();
    }

    public FlaggedEntityResponse findById(Long id) {
        return toResponse(getOrThrow(id));
    }

    public FlaggedEntityResponse create(FlaggedEntityRequest req) {
        Optional<FlaggedEntity> existing = flaggedEntityRepository.findByPayeeId(req.getPayeeId());
        if (existing.isPresent()) {
            FlaggedEntity e = existing.get();
            e.setEntityName(req.getEntityName());
            e.setReason(req.getReason());
            e.setRiskLevel(req.getRiskLevel() != null ? req.getRiskLevel() : Severity.MEDIUM);
            e.setActive(true);
            flaggedEntityRepository.update(e);
            return toResponse(getOrThrow(e.getId()));
        }

        FlaggedEntity e = new FlaggedEntity();
        e.setEntityName(req.getEntityName());
        e.setPayeeId(req.getPayeeId());
        e.setReason(req.getReason());
        e.setRiskLevel(req.getRiskLevel() != null ? req.getRiskLevel() : Severity.MEDIUM);
        e.setActive(req.isActive());
        flaggedEntityRepository.save(e);
        return toResponse(e);
    }

    public void delete(Long id) {
        getOrThrow(id);
        flaggedEntityRepository.deleteById(id);
    }

    public FlaggedEntityResponse activate(Long id) {
        getOrThrow(id);
        flaggedEntityRepository.setActive(id, true);
        return toResponse(getOrThrow(id));
    }

    public FlaggedEntityResponse deactivate(Long id) {
        getOrThrow(id);
        flaggedEntityRepository.setActive(id, false);
        return toResponse(getOrThrow(id));
    }

    private FlaggedEntity getOrThrow(Long id) {
        return flaggedEntityRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Flagged organisation not found: " + id));
    }

    private FlaggedEntityResponse toResponse(FlaggedEntity e) {
        FlaggedEntityResponse dto = new FlaggedEntityResponse();
        dto.setId(e.getId());
        dto.setEntityName(e.getEntityName());
        dto.setPayeeId(e.getPayeeId());
        dto.setReason(e.getReason());
        dto.setRiskLevel(e.getRiskLevel());
        dto.setActive(e.isActive());
        dto.setCreatedAt(e.getCreatedAt());
        return dto;
    }
}
