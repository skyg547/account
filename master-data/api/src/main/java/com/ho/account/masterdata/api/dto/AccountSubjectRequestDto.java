package com.ho.account.masterdata.api.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.ho.account.masterdata.core.domain.model.AccountSubject;
import com.ho.account.masterdata.core.application.command.AccountSubjectCommand;

import java.time.LocalDate;

public class AccountSubjectRequestDto {

    private String code;
    private String name;
    private String parentCode;
    private AccountSubject.AccountCategory category;
    private AccountSubject.BalanceType balanceType;
    private String reportLine;
    private Boolean unsettled;
    private Boolean fixedAsset;
    private LocalDate validFrom;
    private LocalDate validTo;

    public AccountSubjectRequestDto() {
    }

    public AccountSubjectCommand toCommand() {
        return new AccountSubjectCommand(
                code,
                name,
                parentCode,
                category,
                balanceType,
                reportLine,
                unsettled,
                fixedAsset,
                validFrom,
                validTo,
                null,
                null,
                false);
    }

    // Direct writes cannot bypass the approval audit for classification changes.
    @JsonAnySetter
    public void rejectUnsupportedField(String field, Object value) {
        throw new IllegalArgumentException("Unsupported account subject field: " + field);
    }

    // Getter
    public String getCode() { return code; }
    public String getName() { return name; }
    public String getParentCode() { return parentCode; }
    public AccountSubject.AccountCategory getCategory() { return category; }
    public AccountSubject.BalanceType getBalanceType() { return balanceType; }
    public String getReportLine() { return reportLine; }
    public Boolean getUnsettled() { return unsettled; }
    public Boolean getFixedAsset() { return fixedAsset; }
    public LocalDate getValidFrom() { return validFrom; }
    public LocalDate getValidTo() { return validTo; }

    // Setters
    public void setCode(String code) { this.code = code; }
    public void setName(String name) { this.name = name; }
    public void setParentCode(String parentCode) { this.parentCode = parentCode; }
    public void setCategory(AccountSubject.AccountCategory category) { this.category = category; }
    public void setBalanceType(AccountSubject.BalanceType balanceType) { this.balanceType = balanceType; }
    public void setReportLine(String reportLine) { this.reportLine = reportLine; }
    public void setUnsettled(Boolean unsettled) { this.unsettled = unsettled; }
    public void setFixedAsset(Boolean fixedAsset) { this.fixedAsset = fixedAsset; }
    public void setValidFrom(LocalDate validFrom) { this.validFrom = validFrom; }
    public void setValidTo(LocalDate validTo) { this.validTo = validTo; }
}
