package com.ho.account.loan.dto;

import com.ho.account.loan.domain.LoanEvent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal; // Added import
import java.time.LocalDate;
import java.util.Optional; // 선택 필드용

/**
 * 대출 이벤트 (LoanEvent) 요청 DTO
 */
@Data
public class LoanEventRequestDto {
    @NotNull
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
    private Optional<BigDecimal> newPrincipal = Optional.empty();
    private Optional<LocalDate> newMaturityDate = Optional.empty();
}
