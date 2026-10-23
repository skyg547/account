package com.ho.account.closing.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 기간 재오픈 승인 요청 (ReopenApproval) 요청 DTO
 */
@Data
public class ReopenApprovalRequestDto {
    @NotNull
    private Long fiscalPeriodId;

    @NotBlank
    @Size(max = 1000)
    private String reason;
}
