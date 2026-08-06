package com.transaction.transaction_monitoring.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void shouldReturn404Payload_forNotFoundException() {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getRequestURI()).thenReturn("/api/rules/999");

        ResponseEntity<Map<String, Object>> response = handler.handleNotFound(
                new ResourceNotFoundException("Rule not found: 999"), req);

        assertThat(response.getStatusCode().value()).isEqualTo(404);
        assertThat(response.getBody()).containsEntry("status", 404);
        assertThat(response.getBody()).containsEntry("error", "Not Found");
        assertThat(response.getBody()).containsEntry("message", "Rule not found: 999");
        assertThat(response.getBody()).containsEntry("path", "/api/rules/999");
        assertThat(response.getBody()).containsKey("timestamp");
    }

    @Test
    void shouldReturn409Payload_forInvalidStateTransition() {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getRequestURI()).thenReturn("/api/alerts/1/close");

        ResponseEntity<Map<String, Object>> response = handler.handleConflict(
                new InvalidStateTransitionException("Cannot close alert in status: OPEN. Must be INVESTIGATING."), req);

        assertThat(response.getStatusCode().value()).isEqualTo(409);
        assertThat(response.getBody()).containsEntry("status", 409);
        assertThat(response.getBody()).containsEntry("error", "Conflict");
        assertThat(response.getBody()).containsEntry("path", "/api/alerts/1/close");
    }

    @Test
    void shouldReturn400Payload_forValidationErrors() {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getRequestURI()).thenReturn("/api/transactions");

        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(
                new FieldError("transactionRequest", "accountId", "accountId is required"),
                new FieldError("transactionRequest", "amount", "amount is required")
        ));

        ResponseEntity<Map<String, Object>> response = handler.handleValidation(ex, req);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody()).containsEntry("status", 400);
        assertThat(response.getBody()).containsEntry("error", "Bad Request");
        assertThat(response.getBody()).containsEntry("path", "/api/transactions");
        assertThat(String.valueOf(response.getBody().get("message")))
                .contains("accountId: accountId is required")
                .contains("amount: amount is required");
    }
}