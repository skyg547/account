package com.ho.account.closing.dto;

import com.ho.account.closing.domain.ClosingCalendar;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 결산 캘린더 (ClosingCalendar) 요청 DTO
 */
@Data
public class ClosingCalendarRequestDto {
    @NotBlank
    @Size(min = 4, max = 4)
    @Pattern(regexp = "\\d{4}", message = "Fiscal year must be a 4-digit number")
    private String fiscalYear;

    @NotBlank
    @Size(max = 20)
    private String fiscalPeriod;

    private boolean isCurrentPeriod = false; // Default to false

    public ClosingCalendar toEntity() {
        ClosingCalendar calendar = new ClosingCalendar();
        calendar.setFiscalYear(this.fiscalYear);
        calendar.setFiscalPeriod(this.fiscalPeriod);
        calendar.setCurrentPeriod(this.isCurrentPeriod);
        return calendar;
    }
}
