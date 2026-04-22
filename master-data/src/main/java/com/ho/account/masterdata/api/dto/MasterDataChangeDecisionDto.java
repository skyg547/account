package com.ho.account.masterdata.api.dto;

public class MasterDataChangeDecisionDto {

    private String approver;
    private String reason;

    public String getApprover() { return approver; }
    public void setApprover(String approver) { this.approver = approver; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
