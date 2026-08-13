package com.ho.account.shared.finance.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

/**
 * 공통 API 응답 포맷의 팩토리 계약을 검증합니다.
 */
class ApiResponseTest {

    @Test
    void successMarksResponseAsSuccessfulAndCarriesPayload() {
        LocalDateTime before = LocalDateTime.now();

        ApiResponse<String> response = ApiResponse.success("payload");

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getMessage()).isEqualTo("SUCCESS");
        assertThat(response.getData()).isEqualTo("payload");
        assertThat(response.getTimestamp()).isNotNull().isAfterOrEqualTo(before);
    }

    @Test
    void successAcceptsNullPayload() {
        ApiResponse<String> response = ApiResponse.success(null);

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData()).isNull();
    }

    @Test
    void failureMarksResponseAsFailedAndOmitsPayload() {
        LocalDateTime before = LocalDateTime.now();

        ApiResponse<String> response = ApiResponse.failure("계정과목을 찾을 수 없습니다.");

        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getMessage()).isEqualTo("계정과목을 찾을 수 없습니다.");
        assertThat(response.getData()).isNull();
        assertThat(response.getTimestamp()).isNotNull().isAfterOrEqualTo(before);
    }
}
