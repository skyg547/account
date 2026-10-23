package com.ho.account.closing.dto;

import com.ho.account.closing.domain.ReopenApproval;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 기간 재오픈 승인 상태 업데이트 요청 DTO
 */
@Data
public class ReopenApprovalStatusUpdateDto {
    @NotNull
    private ReopenApproval.ReopenApprovalStatus status;
}
