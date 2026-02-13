package com.ho.account.closing.dto;

import com.ho.account.closing.domain.ClosingCalendar;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 결산 캘린더 상태 업데이트 요청 DTO
 */
@Data
public class ClosingCalendarStatusUpdateDto {
    @NotNull
    private ClosingCalendar.ClosingCalendarStatus status;

    @NotBlank
    private String user; // 상태 변경 요청자
}
