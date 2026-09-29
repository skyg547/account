package com.ho.account.closing.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 결산 상태 판정 요청 (ClosingStatusDetermine) DTO
 */
@Data
public class ClosingStatusDetermineRequestDto {
    @NotNull
    private Long calendarId;
}
