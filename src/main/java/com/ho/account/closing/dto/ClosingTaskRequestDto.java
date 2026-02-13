package com.ho.account.closing.dto;

import com.ho.account.closing.domain.ClosingTask;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 결산 태스크 (ClosingTask) 요청 DTO
 */
@Data
public class ClosingTaskRequestDto {
    @NotNull
    private Long calendarId;

    @NotBlank
    @Size(max = 200)
    private String name;

    @Size(max = 1000)
    private String description;

    @NotNull
    private ClosingTask.ClosingTaskCategory category;

    private LocalDateTime dueDate;

    @Size(max = 50)
    private String assignedTo;

    private String completionConditionJson;

    private boolean isMandatory = true;

    @NotNull
    @Min(0)
    private Integer taskOrder;

    public ClosingTask toEntity() {
        ClosingTask task = new ClosingTask();
        // ClosingCalendar will be set in service layer
        task.setName(this.name);
        task.setDescription(this.description);
        task.setCategory(this.category);
        task.setDueDate(this.dueDate);
        task.setAssignedTo(this.assignedTo);
        task.setCompletionConditionJson(this.completionConditionJson);
        task.setMandatory(this.isMandatory);
        task.setTaskOrder(this.taskOrder);
        return task;
    }
}
