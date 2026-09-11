package com.ho.account.closing.infrastructure.source;

import com.ho.account.closing.application.port.out.FxExchangeRateLookupPort;
import com.ho.account.contracts.masterdata.AccountSubjectRef;
import com.ho.account.contracts.masterdata.BusinessPartnerRef;
import com.ho.account.contracts.masterdata.DepartmentRef;
import com.ho.account.contracts.masterdata.MasterDataQueryPort;
import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;

/** Closing batch consumes dated references only from the master-data owned database. */
public final class JdbcClosingMasterDataAdapter implements FxExchangeRateLookupPort, MasterDataQueryPort {
    private final JdbcTemplate jdbc;

    public JdbcClosingMasterDataAdapter(JdbcTemplate jdbc) {
        this.jdbc = Objects.requireNonNull(jdbc, "jdbc must not be null");
    }

    @Override
    public Optional<BigDecimal> findRate(String from, String to, LocalDate effectiveDate) {
        Objects.requireNonNull(effectiveDate, "effectiveDate must not be null");
        List<BigDecimal> rates = jdbc.query("""
                SELECT rate FROM exchange_rates
                 WHERE from_currency_code = ? AND to_currency_code = ? AND effective_date <= ?
                 ORDER BY effective_date DESC FETCH FIRST 1 ROW ONLY
                """, (rs, row) -> rs.getBigDecimal("rate"), currency(from), currency(to), Date.valueOf(effectiveDate));
        return rates.stream().findFirst();
    }

    @Override
    public Optional<AccountSubjectRef> findAccountSubjectAt(String code, LocalDate effectiveDate) {
        if (code == null || code.isBlank()) throw new IllegalArgumentException("account code is required");
        Objects.requireNonNull(effectiveDate, "effectiveDate must not be null");
        List<AccountSubjectRef> accounts = jdbc.query("""
                SELECT code, name, unsettled, fixed_asset, balance_type, category FROM account_subjects
                 WHERE code = ? AND valid_from <= ? AND valid_to >= ?
                 FETCH FIRST 2 ROWS ONLY
                """, (rs, row) -> new AccountSubjectRef(rs.getString("code"), rs.getString("name"),
                rs.getBoolean("unsettled"), rs.getBoolean("fixed_asset"), rs.getString("balance_type"),
                rs.getString("category")), code.trim(), Date.valueOf(effectiveDate), Date.valueOf(effectiveDate));
        if (accounts.size() > 1) throw new IllegalStateException("Overlapping account subject effective dates");
        return accounts.stream().findFirst();
    }

    @Override
    public Optional<AccountSubjectRef> findAccountSubject(String code) {
        throw new UnsupportedOperationException("Closing batch requires an explicit account effective date");
    }

    @Override
    public Optional<BusinessPartnerRef> findBusinessPartner(String code) {
        throw new UnsupportedOperationException("Closing batch does not query business partners");
    }

    @Override
    public Optional<DepartmentRef> findDepartment(String code) {
        throw new UnsupportedOperationException("Closing batch does not query departments");
    }

    private static String currency(String value) {
        if (value == null || !value.trim().matches("[A-Za-z]{3}")) {
            throw new IllegalArgumentException("currency must be a 3-letter code");
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }
}
