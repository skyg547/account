package com.ho.account.shared.finance.exception;

import static org.assertj.core.api.Assertions.assertThat;

import com.ho.account.shared.finance.dto.ApiResponse;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;

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

    @Test
    void missingParameterUsesFixedBadRequestInsteadOfParameterDetails() {
        MissingServletRequestParameterException exception =
                new MissingServletRequestParameterException("synthetic-private-name", "String");
        exception.getBody().setDetail("synthetic-private-problem-detail");

        assertFailure(advice.handleBadRequest(exception), HttpStatus.BAD_REQUEST, "Bad request");
    }

    @Test
    void unreadableMessageUsesFixedBadRequestInsteadOfMessageOrCause() {
        HttpMessageNotReadableException exception = new HttpMessageNotReadableException(
                "synthetic-private-payload", new IllegalStateException("synthetic-private-cause"),
                new MockHttpInputMessage(new byte[0]));

        assertFailure(advice.handleBadRequest(exception), HttpStatus.BAD_REQUEST, "Bad request");
    }

    @Test
    void unsupportedMethodPreservesAllAllowValuesButNotProblemDetail() {
        HttpRequestMethodNotSupportedException exception =
                new HttpRequestMethodNotSupportedException("synthetic-private-method", List.of("GET", "POST"));
        exception.getBody().setDetail("synthetic-private-problem-detail");

        ResponseEntity<ApiResponse<Void>> response = advice.handleMethodNotSupported(exception);

        assertFailure(response, HttpStatus.METHOD_NOT_ALLOWED, "Method not allowed");
        assertThat(response.getHeaders()).isEqualTo(exception.getHeaders());
        assertThat(response.getHeaders().getAllow()).containsExactlyInAnyOrder(HttpMethod.GET, HttpMethod.POST);
    }

    @ParameterizedTest
    @ValueSource(strings = {"POST", "PATCH"})
    void unsupportedMediaTypePreservesNegotiationHeadersButNotProblemDetail(String method) {
        HttpMediaTypeNotSupportedException exception = new HttpMediaTypeNotSupportedException(
                MediaType.TEXT_PLAIN, List.of(MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML),
                HttpMethod.valueOf(method), "synthetic-private-media-detail");
        exception.getBody().setDetail("synthetic-private-problem-detail");

        ResponseEntity<ApiResponse<Void>> response = advice.handleMediaTypeNotSupported(exception);

        assertFailure(response, HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Unsupported media type");
        assertThat(response.getHeaders()).isEqualTo(exception.getHeaders());
        assertThat(response.getHeaders().getAccept())
                .containsExactly(MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML);
        if ("PATCH".equals(method)) {
            assertThat(response.getHeaders().getAcceptPatch())
                    .containsExactly(MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML);
        } else {
            assertThat(response.getHeaders().getAcceptPatch()).isEmpty();
        }
    }

    private static void assertFailure(
            ResponseEntity<ApiResponse<Void>> response, HttpStatus status, String message) {
        assertThat(response.getStatusCode()).isEqualTo(status);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getData()).isNull();
        assertThat(response.getBody().getMessage()).isEqualTo(message);
        assertThat(response.getBody().getTimestamp()).isNotNull();
    }
}
