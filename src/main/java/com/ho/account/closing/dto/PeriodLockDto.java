package com.ho.account.closing.dto;

import com.ho.account.closing.domain.PeriodLock;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 기간 잠금 (PeriodLock) 응답 DTO
 */
@Data
@Builder
public class PeriodLockDto {
    private Long id;
    private Long fiscalPeriodId;
    private String fiscalYear;
    private String fiscalPeriod;
    private PeriodLock.PeriodLockType lockType;
    private String lockedBy;
    private LocalDateTime lockedAt;
    private String reason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String auditUser;

    public static PeriodLockDto fromEntity(PeriodLock entity) {
        return PeriodLockDto.builder()
                .id(entity.getId())
                .fiscalPeriodId(entity.getFiscalPeriod() != null ? entity.getFiscalPeriod().getId() : null)
                .fiscalYear(entity.getFiscalPeriod() != null ? entity.getFiscalPeriod().getFiscalYear() : null)
                .fiscalPeriod(entity.getFiscalPeriod() != null ? entity.getFiscalPeriod().getFiscalPeriod() : null)
                .lockType(entity.getLockType())
                .lockedBy(entity.getLockedBy())
                .lockedAt(entity.getLockedAt())
                .reason(entity.getReason())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .auditUser(entity.getAuditUser())
                .build();
    }
}
