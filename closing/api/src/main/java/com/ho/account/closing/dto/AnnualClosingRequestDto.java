package com.ho.account.closing.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 연차 손익 대체를 요청할 때 외부에서 입력받는 회계연도입니다.
 * 이익잉여금 계정과 처리 대상 법인은 애플리케이션 정책이 결정하므로 요청 필드로 노출하지 않습니다.
 */
@Data
public class AnnualClosingRequestDto {

    @NotNull
    @Min(1900)
    @Max(9999)
    private Integer year;

    /**
     * Spring의 전역 Jackson 설정과 무관하게 회계연도 외 요청 필드를 실패 처리합니다.
     * 계정이나 법인 선택값이 무시된 채 요청이 성공하는 모호한 API 계약을 방지합니다.
     */
    @JsonAnySetter
    public void rejectUnknownField(String ignoredFieldName, Object ignoredValue) {
        throw new IllegalArgumentException("Annual closing request supports only the year field");
    }
}
