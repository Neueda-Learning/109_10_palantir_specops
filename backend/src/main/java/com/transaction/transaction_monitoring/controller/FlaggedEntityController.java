package com.transaction.transaction_monitoring.controller;

import com.transaction.transaction_monitoring.dto.FlaggedEntityRequest;
import com.transaction.transaction_monitoring.dto.FlaggedEntityResponse;
import com.transaction.transaction_monitoring.service.FlaggedEntityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/flagged-entities")
@Tag(name = "Flagged Organisations", description = "Watchlist of flagged organisations / payees")
public class FlaggedEntityController {

    private final FlaggedEntityService flaggedEntityService;

    public FlaggedEntityController(FlaggedEntityService flaggedEntityService) {
        this.flaggedEntityService = flaggedEntityService;
    }

    @GetMapping
    @Operation(summary = "List all flagged organisations")
    public List<FlaggedEntityResponse> findAll() {
        return flaggedEntityService.findAll();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a flagged organisation by ID")
    public FlaggedEntityResponse findById(@PathVariable Long id) {
        return flaggedEntityService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Flag an organisation (payee) on the watchlist")
    public FlaggedEntityResponse create(@Valid @RequestBody FlaggedEntityRequest req) {
        return flaggedEntityService.create(req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Unflag / remove an organisation from the watchlist")
    public void delete(@PathVariable Long id) {
        flaggedEntityService.delete(id);
    }

    @PatchMapping("/{id}/activate")
    @Operation(summary = "Re-activate a flagged organisation")
    public FlaggedEntityResponse activate(@PathVariable Long id) {
        return flaggedEntityService.activate(id);
    }

    @PatchMapping("/{id}/deactivate")
    @Operation(summary = "Deactivate a flagged organisation (stops new alerts, keeps history)")
    public FlaggedEntityResponse deactivate(@PathVariable Long id) {
        return flaggedEntityService.deactivate(id);
    }
}
