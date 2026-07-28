package com.ho.account.masterdata.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 승인 또는 반려를 수행하는 담당자와 사유를 전달하는 HTTP 입력 DTO입니다.
 */
public class MasterDataChangeDecisionDto {

    @NotBlank
    @Size(max = 80)
    private String approver;

    @Size(max = 500)
    private String reason;

    public String getApprover() { return approver; }
    public void setApprover(String approver) { this.approver = approver; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}