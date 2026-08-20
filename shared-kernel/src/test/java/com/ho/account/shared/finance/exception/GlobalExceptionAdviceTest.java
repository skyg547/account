package com.ho.account.shared.finance.exception;

import static org.assertj.core.api.Assertions.assertThat;

import com.ho.account.shared.finance.dto.ApiResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class GlobalExceptionAdviceTest {

    private GlobalExceptionAdvice advice;

    @BeforeEach
    void setUp() {
        advice = new GlobalExceptionAdvice();
    }

    @Test
    @DisplayName("ResourceNotFoundException 처리 시 404 NOT_FOUND 상태와 에러 메시지를 반환한다")
    void testHandleResourceNotFound() {
        ResourceNotFoundException ex = new ResourceNotFoundException("Account", 99);
        ResponseEntity<ApiResponse<Void>> response = advice.handleResourceNotFound(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getMessage()).isEqualTo("Account not found: 99");
    }

    @Test
    @DisplayName("IllegalArgumentException 처리 시 400 BAD_REQUEST 상태와 에러 메시지를 반환한다")
    void testHandleIllegalArgument() {
        IllegalArgumentException ex = new IllegalArgumentException("Invalid amount: -100");
        ResponseEntity<ApiResponse<Void>> response = advice.handleIllegalArgument(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getMessage()).isEqualTo("Invalid amount: -100");
    }

    @Test
    @DisplayName("일반 Exception 처리 시 500 INTERNAL_SERVER_ERROR 상태와 일반 메시지를 반환한다")
    void testHandleUnexpected() {
        Exception ex = new RuntimeException("Unexpected DB deadlock");
        ResponseEntity<ApiResponse<Void>> response = advice.handleUnexpected(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getMessage()).isEqualTo("Internal server error");
    }
}
