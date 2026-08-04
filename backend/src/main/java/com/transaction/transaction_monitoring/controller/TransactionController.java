package com.transaction.transaction_monitoring.controller;

import com.transaction.transaction_monitoring.dto.TransactionRequest;
import com.transaction.transaction_monitoring.dto.TransactionResponse;
import com.transaction.transaction_monitoring.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/transactions")
@Tag(name = "Transactions", description = "Transaction management endpoints")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a transaction and trigger rule evaluation")
    public TransactionResponse create(@Valid @RequestBody TransactionRequest req) {
        return transactionService.create(req);
    }

    @GetMapping
    @Operation(summary = "List transactions with optional filters")
    public List<TransactionResponse> findAll(
            @RequestParam(required = false) String accountId,
            @RequestParam(required = false) LocalDateTime from,
            @RequestParam(required = false) LocalDateTime to,
            @RequestParam(required = false) BigDecimal minAmount,
            @RequestParam(required = false) BigDecimal maxAmount,
            @RequestParam(required = false) String search) {
        return transactionService.findAll(accountId, from, to, minAmount, maxAmount, search);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a single transaction by ID")
    public TransactionResponse findById(@PathVariable Long id) {
        return transactionService.findById(id);
    }

    @PostMapping("/generate")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Generate N random transactions for testing")
    public List<TransactionResponse> generate(@RequestBody Map<String, Integer> body) {
        int count = body.getOrDefault("count", 10);
        return transactionService.generate(count);
    }
}
