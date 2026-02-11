package com.ho.account.basic.dto;

import com.ho.account.basic.domain.AccountSubject;

import java.time.LocalDate;

public class AccountSubjectRequestDto {

    private String code;
    private String name;
    private String parentCode;
    private AccountSubject.AccountCategory category;
    private AccountSubject.BalanceType balanceType;
    private String reportLine;
    private boolean unsettled;
    private boolean fixedAsset;
    private LocalDate validFrom;
    private LocalDate validTo;

    // Default constructor for JSON deserialization
    public AccountSubjectRequestDto() {
    }
    
    // toEntity method to convert DTO to domain object
    public AccountSubject toEntity() {
        AccountSubject entity = new AccountSubject();
        entity.setCode(this.code);
        entity.setName(this.name);
        // parent will be set in the service
        entity.setCategory(this.category);
        entity.setBalanceType(this.balanceType);
        entity.setReportLine(this.reportLine);
        entity.setUnsettled(this.unsettled);
        entity.setFixedAsset(this.fixedAsset);
        entity.setValidFrom(this.validFrom);
        entity.setValidTo(this.validTo);
        return entity;
    }

    // Getters
    public String getCode() { return code; }
    public String getName() { return name; }
    public String getParentCode() { return parentCode; }
    public AccountSubject.AccountCategory getCategory() { return category; }
    public AccountSubject.BalanceType getBalanceType() { return balanceType; }
    public String getReportLine() { return reportLine; }
    public boolean isUnsettled() { return unsettled; }
    public boolean isFixedAsset() { return fixedAsset; }
    public LocalDate getValidFrom() { return validFrom; }
    public LocalDate getValidTo() { return validTo; }

    // Setters
    public void setCode(String code) { this.code = code; }
    public void setName(String name) { this.name = name; }
    public void setParentCode(String parentCode) { this.parentCode = parentCode; }
    public void setCategory(AccountSubject.AccountCategory category) { this.category = category; }
    public void setBalanceType(AccountSubject.BalanceType balanceType) { this.balanceType = balanceType; }
    public void setReportLine(String reportLine) { this.reportLine = reportLine; }
    public void setUnsettled(boolean unsettled) { this.unsettled = unsettled; }
    public void setFixedAsset(boolean fixedAsset) { this.fixedAsset = fixedAsset; }
    public void setValidFrom(LocalDate validFrom) { this.validFrom = validFrom; }
    public void setValidTo(LocalDate validTo) { this.validTo = validTo; }
}
