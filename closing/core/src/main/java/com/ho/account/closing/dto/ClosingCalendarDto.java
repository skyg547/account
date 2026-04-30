package com.ho.account.closing.dto;

import com.ho.account.closing.domain.ClosingCalendar;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 결산 캘린더 (ClosingCalendar) 응답 DTO
 */
@Data
@Builder
public class ClosingCalendarDto {
    private Long id;
    private String fiscalYear;
    private String fiscalPeriod;
    private ClosingCalendar.ClosingCalendarStatus status;
    private String closeInitiatedBy;
    private LocalDateTime closeInitiatedAt;
    private String closedBy;
    private LocalDateTime closedAt;
    private String reopenedBy;
    private LocalDateTime reopenedAt;
    private boolean isCurrentPeriod;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String auditUser;

    public static ClosingCalendarDto fromEntity(ClosingCalendar entity) {
        return ClosingCalendarDto.builder()
                .id(entity.getId())
                .fiscalYear(entity.getFiscalYear())
                .fiscalPeriod(entity.getFiscalPeriod())
                .status(entity.getStatus())
                .closeInitiatedBy(entity.getCloseInitiatedBy())
                .closeInitiatedAt(entity.getCloseInitiatedAt())
                .closedBy(entity.getClosedBy())
                .closedAt(entity.getClosedAt())
                .reopenedBy(entity.getReopenedBy())
                .reopenedAt(entity.getReopenedAt())
                .isCurrentPeriod(entity.isCurrentPeriod())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .auditUser(entity.getAuditUser())
                .build();
    }
}
