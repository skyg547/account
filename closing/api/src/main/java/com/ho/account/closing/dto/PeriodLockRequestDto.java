package com.ho.account.closing.dto;

import com.ho.account.closing.domain.PeriodLock;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 기간 잠금 (PeriodLock) 요청 DTO
 */
@Data
public class PeriodLockRequestDto {
    @NotNull
    private Long fiscalPeriodId;

    @NotNull
    private PeriodLock.PeriodLockType lockType;

    @Size(max = 1000)
    private String reason;
}
