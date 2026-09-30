package com.ho.account.masterdata.core.application.command;

import com.ho.account.masterdata.core.domain.model.AccountSubject;
import java.time.LocalDate;

public record AccountSubjectCommand(
        String code,
        String name,
        String parentCode,
        AccountSubject.AccountCategory category,
        AccountSubject.BalanceType balanceType,
        String reportLine,
        Boolean unsettled,
        Boolean fixedAsset,
        LocalDate validFrom,
        LocalDate validTo,
        AccountSubject.AccountType accountType,
        String regulatoryMappingCode,
        boolean clearRegulatoryMappingCode) {

    public AccountSubjectCommand {
        if (regulatoryMappingCode != null && regulatoryMappingCode.isBlank()) {
            throw new IllegalArgumentException("Regulatory mapping code must not be blank.");
        }
        // Preserve the supplied identifier exactly; reject ambiguous whitespace and values the column cannot hold.
        if (regulatoryMappingCode != null && !regulatoryMappingCode.equals(regulatoryMappingCode.strip())) {
            throw new IllegalArgumentException("Regulatory mapping code must not have surrounding whitespace.");
        }
        if (regulatoryMappingCode != null && regulatoryMappingCode.length() > 100) {
            throw new IllegalArgumentException("Regulatory mapping code must be at most 100 characters.");
        }
        if (clearRegulatoryMappingCode && regulatoryMappingCode != null) {
            throw new IllegalArgumentException("Regulatory mapping code cannot be changed and cleared together.");
        }
    }

    // Existing direct callers supply both flags; a missing JSON field stays null for partial updates.
    public AccountSubjectCommand(
            String code, String name, String parentCode, AccountSubject.AccountCategory category,
            AccountSubject.BalanceType balanceType, String reportLine, boolean unsettled,
            boolean fixedAsset, LocalDate validFrom, LocalDate validTo) {
        this(code, name, parentCode, category, balanceType, reportLine, unsettled, fixedAsset,
                validFrom, validTo, null, null, false);
    }

    public boolean hasParentCode() {
        return parentCode != null && !parentCode.isBlank();
    }

    public AccountSubject toEntity() {
        AccountSubject entity = new AccountSubject();
        entity.setCode(code);
        entity.setName(name);
        entity.setCategory(category);
        entity.setAccountType(accountType);
        entity.setBalanceType(balanceType);
        entity.setReportLine(reportLine);
        entity.setRegulatoryMappingCode(regulatoryMappingCode);
        entity.setUnsettled(Boolean.TRUE.equals(unsettled));
        entity.setFixedAsset(Boolean.TRUE.equals(fixedAsset));
        entity.setValidFrom(validFrom);
        entity.setValidTo(validTo);
        return entity;
    }
}
