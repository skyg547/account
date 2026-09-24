package com.ho.account.closing.application.service;

import com.ho.account.contracts.masterdata.AccountSubjectRef;
import com.ho.account.contracts.masterdata.MasterDataQueryPort;
import lombok.RequiredArgsConstructor;

import java.time.LocalDate;
import java.util.Objects;

/** Same dated eligibility gate for source discovery, cursor reads, and direct service invocations. */
@RequiredArgsConstructor
public class FxValuationEligibilityResolver {
    private final ClosingAccountingProperties accountingProperties;
    private final MasterDataQueryPort masterDataQueryPort;

    public boolean isEligible(String accountCode, LocalDate valuationDate) {
        Objects.requireNonNull(valuationDate, "valuationDate must not be null");
        AccountSubjectRef account = masterDataQueryPort.findAccountSubjectAt(accountCode, valuationDate)
                .orElseThrow(() -> new IllegalStateException(
                        "Account subject is missing for " + accountCode + " on " + valuationDate));
        if (!accountCode.equals(account.code())) {
            throw new IllegalStateException("FX account metadata code does not match requested account " + accountCode);
        }
        return accountingProperties.fxValuationPolicy().isEligible(
                accountCode, valuationDate, account.accountCategory(), account.fixedAsset());
    }
}
