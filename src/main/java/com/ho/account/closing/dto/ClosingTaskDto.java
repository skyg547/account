package com.ho.account.closing.dto;

import com.ho.account.closing.domain.ClosingTask;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 결산 태스크 (ClosingTask) 응답 DTO
 */
@Data
@Builder
public class ClosingTaskDto {
    private Long id;
    private Long calendarId;
    private String name;
    private String description;
    private ClosingTask.ClosingTaskCategory category;
    private LocalDateTime dueDate;
    private String assignedTo;
    private ClosingTask.ClosingTaskStatus status;
    private String completionConditionJson;
    private boolean isMandatory;
    private Integer taskOrder;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String auditUser;

    public static ClosingTaskDto fromEntity(ClosingTask entity) {
        return ClosingTaskDto.builder()
                .id(entity.getId())
                .calendarId(entity.getClosingCalendar() != null ? entity.getClosingCalendar().getId() : null)
                .name(entity.getName())
                .description(entity.getDescription())
                .category(entity.getCategory())
                .dueDate(entity.getDueDate())
                .assignedTo(entity.getAssignedTo())
                .status(entity.getStatus())
                .completionConditionJson(entity.getCompletionConditionJson())
                .isMandatory(entity.isMandatory())
                .taskOrder(entity.getTaskOrder())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .auditUser(entity.getAuditUser())
                .build();
    }
}
