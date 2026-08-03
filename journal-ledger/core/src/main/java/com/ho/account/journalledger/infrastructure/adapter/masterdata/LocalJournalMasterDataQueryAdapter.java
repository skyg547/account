package com.ho.account.journalledger.infrastructure.adapter.masterdata;

import com.ho.account.contracts.masterdata.AccountSubjectRef;
import com.ho.account.contracts.masterdata.BusinessPartnerRef;
import com.ho.account.contracts.masterdata.DepartmentRef;
import com.ho.account.contracts.masterdata.FiscalPeriodControlPort;
import com.ho.account.contracts.masterdata.FiscalPeriodRef;
import com.ho.account.contracts.masterdata.MasterDataQueryPort;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Objects;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Local-only reference data for running Journal Ledger without another service. */
@Component
@ConditionalOnProperty(
        prefix = "journal-ledger.master-data.local-adapter",
        name = "enabled",
        havingValue = "true")
public class LocalJournalMasterDataQueryAdapter
        implements MasterDataQueryPort, FiscalPeriodControlPort {

    @Override
    public Optional<AccountSubjectRef> findAccountSubject(String accountCode) {
        return normalized(accountCode).map(code -> new AccountSubjectRef(
                code, "Local account " + code, false, false, "DEBIT", null));
    }

    @Override
    public Optional<AccountSubjectRef> findAccountSubjectAt(
            String accountCode,
            LocalDate effectiveDate) {
        Objects.requireNonNull(effectiveDate, "effectiveDate must not be null");
        return findAccountSubject(accountCode);
    }

    @Override
    public Optional<BusinessPartnerRef> findBusinessPartner(String businessPartnerCode) {
        return normalized(businessPartnerCode).map(code -> new BusinessPartnerRef(
                code, "Local partner " + code, "OTHER_BP", true));
    }

    @Override
    public Optional<BusinessPartnerRef> findBusinessPartnerAt(
            String businessPartnerCode,
            LocalDate effectiveDate) {
        Objects.requireNonNull(effectiveDate, "effectiveDate must not be null");
        return findBusinessPartner(businessPartnerCode);
    }

    @Override
    public Optional<DepartmentRef> findDepartment(String departmentCode) {
        return normalized(departmentCode).map(code -> new DepartmentRef(
                code, "Local department " + code, "OTHER"));
    }

    @Override
    public Optional<DepartmentRef> findDepartmentAt(
            String departmentCode,
            LocalDate effectiveDate) {
        Objects.requireNonNull(effectiveDate, "effectiveDate must not be null");
        return findDepartment(departmentCode);
    }

    @Override
    public Optional<FiscalPeriodRef> findFiscalPeriodById(Long id) {
        return Optional.empty();
    }

    @Override
    public Optional<FiscalPeriodRef> findFiscalPeriod(String fiscalYear, String fiscalPeriod) {
        try {
            int year = Integer.parseInt(fiscalYear);
            int month = Integer.parseInt(fiscalPeriod);
            YearMonth yearMonth = YearMonth.of(year, month);
            return Optional.of(new FiscalPeriodRef(
                    null,
                    String.format("%04d", year),
                    String.format("%02d", month),
                    yearMonth.atDay(1),
                    yearMonth.atEndOfMonth(),
                    "OPEN"));
        } catch (RuntimeException exception) {
            return Optional.empty();
        }
    }

    @Override
    public FiscalPeriodRef updateClosingStatus(
            Long fiscalPeriodId,
            String closingStatus,
            String auditUser) {
        throw new UnsupportedOperationException("local fiscal periods are read-only");
    }

    private Optional<String> normalized(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(value.trim());
    }
}
