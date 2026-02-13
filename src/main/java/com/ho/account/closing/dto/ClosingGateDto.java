package com.ho.account.closing.dto;

import com.ho.account.closing.domain.ClosingGate;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 결산 게이트 (ClosingGate) 응답 DTO
 */
@Data
@Builder
public class ClosingGateDto {
    private Long id;
    private Long calendarId;
    private String name;
    private String description;
    private ClosingGate.ClosingGateStatus status;
    private String checkConditionJson;
    private String passedBy;
    private LocalDateTime passedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String auditUser;

    public static ClosingGateDto fromEntity(ClosingGate entity) {
        return ClosingGateDto.builder()
                .id(entity.getId())
                .calendarId(entity.getClosingCalendar() != null ? entity.getClosingCalendar().getId() : null)
                .name(entity.getName())
                .description(entity.getDescription())
                .status(entity.getStatus())
                .checkConditionJson(entity.getCheckConditionJson())
                .passedBy(entity.getPassedBy())
                .passedAt(entity.getPassedAt())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .auditUser(entity.getAuditUser())
                .build();
    }
}
