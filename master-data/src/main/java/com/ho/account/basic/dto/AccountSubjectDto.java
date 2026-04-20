package com.ho.account.masterdata.api.dto;

import com.ho.account.basic.domain.AccountSubject;
import java.time.LocalDate;

public class AccountSubjectDto {
    private final String code;
    private final String name;
    private final String parentCode;
    private final AccountSubject.AccountCategory category;
    private final AccountSubject.BalanceType balanceType;
    private final String reportLine;
    private final boolean unsettled;
    private final boolean fixedAsset;
    private final LocalDate validFrom;
    private final LocalDate validTo;

    public AccountSubjectDto(String code, String name, String parentCode, AccountSubject.AccountCategory category,
            AccountSubject.BalanceType balanceType, String reportLine, boolean unsettled, boolean fixedAsset,
            LocalDate validFrom, LocalDate validTo) {
        this.code = code;
        this.name = name;
        this.parentCode = parentCode;
        this.category = category;
        this.balanceType = balanceType;
        this.reportLine = reportLine;
        this.unsettled = unsettled;
        this.fixedAsset = fixedAsset;
        this.validFrom = validFrom;
        this.validTo = validTo;
    }

    public static AccountSubjectDto fromEntity(AccountSubject entity) {
        if (entity == null) {
            return null;
        }

        String parentCode = (entity.getParent() != null) ? entity.getParent().getCode() : null;

        return new AccountSubjectDto(
                entity.getCode(),
                entity.getName(),
                parentCode,
                entity.getCategory(),
                entity.getBalanceType(),
                entity.getReportLine(),
                entity.isUnsettled(),
                entity.isFixedAsset(),
                entity.getValidFrom(),
                entity.getValidTo());
    }

    // Getter
    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getParentCode() {
        return parentCode;
    }

    public AccountSubject.AccountCategory getCategory() {
        return category;
    }

    public AccountSubject.BalanceType getBalanceType() {
        return balanceType;
    }

    public String getReportLine() {
        return reportLine;
    }

    public boolean isUnsettled() {
        return unsettled;
    }

    public boolean isFixedAsset() {
        return fixedAsset;
    }

    public LocalDate getValidFrom() {
        return validFrom;
    }

    public LocalDate getValidTo() {
        return validTo;
    }
}
