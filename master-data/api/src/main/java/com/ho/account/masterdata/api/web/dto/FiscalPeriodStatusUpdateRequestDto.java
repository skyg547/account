package com.ho.account.masterdata.api.web.dto;

import jakarta.validation.constraints.NotBlank;

public class FiscalPeriodStatusUpdateRequestDto {

    @NotBlank(message = "closingStatus must not be blank")
    private String closingStatus;

    @NotBlank(message = "auditUser must not be blank")
    private String auditUser;

    public FiscalPeriodStatusUpdateRequestDto() {
    }

    public FiscalPeriodStatusUpdateRequestDto(String closingStatus, String auditUser) {
        this.closingStatus = closingStatus;
        this.auditUser = auditUser;
    }

    public String getClosingStatus() {
        return closingStatus;
    }

    public void setClosingStatus(String closingStatus) {
        this.closingStatus = closingStatus;
    }

    public String getAuditUser() {
        return auditUser;
    }

    public void setAuditUser(String auditUser) {
        this.auditUser = auditUser;
    }
}
