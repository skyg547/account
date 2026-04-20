package com.ho.account.masterdata.core.application.command;

import com.ho.account.basic.domain.AccountSubject;
import java.time.LocalDate;

public record AccountSubjectCommand(
        String code,
        String name,
        String parentCode,
        AccountSubject.AccountCategory category,
        AccountSubject.BalanceType balanceType,
        String reportLine,
        boolean unsettled,
        boolean fixedAsset,
        LocalDate validFrom,
        LocalDate validTo) {

    public boolean hasParentCode() {
        return parentCode != null && !parentCode.isBlank();
    }

    public AccountSubject toEntity() {
        AccountSubject entity = new AccountSubject();
        entity.setCode(code);
        entity.setName(name);
        entity.setCategory(category);
        entity.setBalanceType(balanceType);
        entity.setReportLine(reportLine);
        entity.setUnsettled(unsettled);
        entity.setFixedAsset(fixedAsset);
        entity.setValidFrom(validFrom);
        entity.setValidTo(validTo);
        return entity;
    }
}
