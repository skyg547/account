package com.ho.account.internalaudit.api.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

class InternalAuditApiExceptionHandlerTest {

    private final InternalAuditApiExceptionHandler handler =
            new InternalAuditApiExceptionHandler();

    @Test
    void parentMismatchIsBadRequest() {
        ResponseEntity<Map<String, String>> response =
                handler.invalidRequest(new IllegalArgumentException("parent mismatch"));

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody()).containsEntry("message", "parent mismatch");
    }

    @Test
    void missingParentIsNotFound() {
        ResponseEntity<Map<String, String>> response =
                handler.notFound(new NoSuchElementException("missing parent"));

        assertThat(response.getStatusCode().value()).isEqualTo(404);
        assertThat(response.getBody()).containsEntry("message", "missing parent");
    }
}
