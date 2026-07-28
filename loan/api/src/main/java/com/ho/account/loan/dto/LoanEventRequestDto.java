package com.ho.account.loan.dto;

// HTTP 요청 계약은 loan:api 인바운드 어댑터가 소유합니다.

import com.ho.account.loan.domain.LoanEvent;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 대출 이벤트 (LoanEvent) 요청 DTO
 */
@Data
public class LoanEventRequestDto {
    @NotNull
    @Positive
    private Long loanId;

    @NotNull
    private LoanEvent.EventType eventType;

    @NotNull
    private LocalDate eventDate;

    @Size(max = 1000)
    private String description;

    @NotBlank
    @Size(max = 50)
    private String user;

    // 재계산용 선택 필드
    @DecimalMin(value = "0.0", inclusive = false)
    private BigDecimal newPrincipal;
    private LocalDate newMaturityDate;
}
