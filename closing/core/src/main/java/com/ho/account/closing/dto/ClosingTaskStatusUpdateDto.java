package com.ho.account.closing.dto;

import com.ho.account.closing.domain.ClosingTask;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 결산 태스크 상태 업데이트 요청 DTO
 */
@Data
public class ClosingTaskStatusUpdateDto {
    @NotNull
    private ClosingTask.ClosingTaskStatus status;

    @NotBlank
    private String user;
}
