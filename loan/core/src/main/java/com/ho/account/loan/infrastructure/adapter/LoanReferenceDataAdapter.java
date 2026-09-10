package com.ho.account.loan.infrastructure.adapter;

import com.ho.account.loan.application.port.out.LoanReferenceDataPort;
import com.ho.account.masterdata.core.application.port.out.AccountSubjectPersistencePort;
import com.ho.account.masterdata.core.application.port.out.BusinessPartnerPersistencePort;
import com.ho.account.masterdata.core.application.port.out.CurrencyPersistencePort;
import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDate;
import java.util.Locale;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 기존 Master Data 내부 포트를 Loan 소유 출력 포트 뒤에 격리하는 모놀리스 어댑터.
 */
@Component
@ConditionalOnProperty(prefix = "account.loan.remote", name = "enabled",
        havingValue = "false", matchIfMissing = true)
public class LoanReferenceDataAdapter implements LoanReferenceDataPort {

    private final BusinessPartnerPersistencePort businessPartnerPort;
    private final CurrencyPersistencePort currencyPort;
    private final AccountSubjectPersistencePort accountSubjectPort;

    public LoanReferenceDataAdapter(
            BusinessPartnerPersistencePort businessPartnerPort,
            CurrencyPersistencePort currencyPort,
            AccountSubjectPersistencePort accountSubjectPort) {
        this.businessPartnerPort = businessPartnerPort;
        this.currencyPort = currencyPort;
        this.accountSubjectPort = accountSubjectPort;
    }

    @Override
    public LoanReferenceSnapshot requireLoanReferences(
            Long businessPartnerId,
            String currencyCode,
            LocalDate effectiveDate) {
        if (businessPartnerId == null || businessPartnerId < 1) {
            throw new IllegalArgumentException("businessPartnerId must be positive.");
        }
        if (effectiveDate == null) {
            throw new IllegalArgumentException("effectiveDate is required.");
        }
        var partner = businessPartnerPort.findById(businessPartnerId)
                .filter(candidate -> Boolean.TRUE.equals(candidate.getUseYn()))
                .filter(candidate -> !candidate.getValidFrom().isAfter(effectiveDate))
                .filter(candidate -> !candidate.getValidTo().isBefore(effectiveDate))
                .orElseThrow(() -> new EntityNotFoundException(
                        "BusinessPartner is not effective on " + effectiveDate + ": " + businessPartnerId));

        String normalizedCurrencyCode = requireText(currencyCode, "currencyCode").toUpperCase(Locale.ROOT);
        var currency = currencyPort.findByCodeAt(normalizedCurrencyCode, effectiveDate)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Currency is not effective on " + effectiveDate + ": " + normalizedCurrencyCode));

        return new LoanReferenceSnapshot(
                partner.getId(),
                partner.getBusinessPartnerName(),
                currency.getCurrencyCode());
    }

    @Override
    public AccountReference requireAccount(String accountCode, LocalDate effectiveDate) {
        String normalizedCode = requireText(accountCode, "accountCode");
        if (effectiveDate == null) {
            throw new IllegalArgumentException("effectiveDate is required.");
        }
        var account = accountSubjectPort.findByCodeAt(normalizedCode, effectiveDate)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Account is not effective on " + effectiveDate + ": " + normalizedCode));
        return new AccountReference(account.getCode(), account.getName());
    }

    private String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required.");
        }
        return value.trim();
    }
}
