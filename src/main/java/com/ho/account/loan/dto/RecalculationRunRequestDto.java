package com.ho.account.loan.dto;

import com.ho.account.loan.domain.RecalculationRun;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

/**
 * 재계산 실행 (RecalculationRun) 요청 DTO
 */
@Data
public class RecalculationRunRequestDto {
    @NotNull
    private Long loanId;

    @NotNull
    private LocalDate eventDate; // 재계산 트리거 이벤트 발생일

    @NotNull
    private RecalculationRun.RecalculationReason reason; // 재계산 사유

    @NotBlank
    @Size(max = 50)
    private String user;

    // 재계산용 선택 필드
    private BigDecimal newPrincipal; // 중도상환 등으로 인한 원금 변경
    private LocalDate newMaturityDate; // 만기일 변경
}
