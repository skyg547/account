package com.ho.account.loan.dto;

import com.ho.account.loan.application.port.in.LoanUseCase.LoanEventResult;

/** 상태 이벤트와 선택적 재계산 결과를 함께 반환하는 HTTP 응답 계약입니다. */
public record LoanEventResultDto(
        LoanEventDto event,
        RecalculationRunDto recalculationRun) {

    public static LoanEventResultDto from(LoanEventResult result) {
        return new LoanEventResultDto(
                LoanEventDto.fromEntity(result.event()),
                result.recalculationRun().map(RecalculationRunDto::fromEntity).orElse(null));
    }
}
