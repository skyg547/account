package com.ho.account.internalaudit.api.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.ho.account.internalaudit.core.domain.IdempotencyConflictException;
import java.util.Map;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

class InternalAuditApiExceptionHandlerTest {

    private final InternalAuditApiExceptionHandler handler =
            new InternalAuditApiExceptionHandler();

    @Test
    void idempotencyConflictDoesNotExposeStoredCommandDetails() {
        IdempotencyConflictException exception = mock(IdempotencyConflictException.class);
        when(exception.getMessage()).thenReturn(
                "stored-key=synthetic-key actor=synthetic-actor snapshot=synthetic-result");

        ResponseEntity<Map<String, String>> response = handler.idempotencyConflict(exception);

        assertThat(response.getStatusCode().value()).isEqualTo(409);
        assertThat(response.getBody()).isEqualTo(Map.of(
                "code", "IDEMPOTENCY_CONFLICT",
                "message", "Idempotency key cannot be reused for this command."));
    }

    @Test
    void internalFailureDoesNotExposeReceiptOrCauseDetails() {
        IllegalStateException exception = new IllegalStateException(
                "stored-key=synthetic-key actor=synthetic-actor",
                new IllegalArgumentException("snapshot=synthetic-result"));

        ResponseEntity<Map<String, String>> response = handler.internalFailure(exception);

        assertThat(response.getStatusCode().value()).isEqualTo(500);
        assertThat(response.getBody()).isEqualTo(Map.of(
                "code", "INTERNAL_AUDIT_ERROR",
                "message", "Unable to process the command."));
    }

    @Test
    void parentMismatchIsBadRequest() {
        ResponseEntity<Map<String, String>> response =
                handler.invalidRequest(new IllegalArgumentException("parent mismatch"));

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody()).isEqualTo(Map.of("message", "parent mismatch"));
    }

    @Test
    void missingParentIsNotFound() {
        ResponseEntity<Map<String, String>> response =
                handler.notFound(new NoSuchElementException("missing parent"));

        assertThat(response.getStatusCode().value()).isEqualTo(404);
        assertThat(response.getBody()).isEqualTo(Map.of("message", "missing parent"));
    }
}
