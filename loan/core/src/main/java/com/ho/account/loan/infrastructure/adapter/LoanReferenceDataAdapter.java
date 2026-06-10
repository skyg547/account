package com.ho.account.loan.infrastructure.adapter;

import com.ho.account.loan.application.port.out.LoanReferenceDataPort;
import com.ho.account.loan.domain.Loan;
import com.ho.account.masterdata.core.application.port.out.AccountSubjectPersistencePort;
import com.ho.account.masterdata.core.application.port.out.BusinessPartnerPersistencePort;
import com.ho.account.masterdata.core.application.port.out.CurrencyPersistencePort;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Component;

/**
 * 기존 Master Data 내부 포트를 Loan 소유 출력 포트 뒤에 격리하는 모놀리스 어댑터.
 */
@Component
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
    public void attachValidatedLoanReferences(Loan loan) {
        Long partnerId = loan.getBusinessPartnerId();
        String currencyCode = loan.getCurrencyCode();
        loan.setBusinessPartner(businessPartnerPort.findById(partnerId)
                .orElseThrow(() -> new EntityNotFoundException("BusinessPartner not found: " + partnerId)));
        loan.setCurrency(currencyPort.findByCode(currencyCode)
                .orElseThrow(() -> new EntityNotFoundException("Currency not found: " + currencyCode)));
    }

    @Override
    public String requireAccountCode(String accountCode) {
        if (accountCode == null || accountCode.isBlank()) {
            throw new IllegalArgumentException("accountCode is required.");
        }
        return accountSubjectPort.findByCode(accountCode.trim())
                .orElseThrow(() -> new EntityNotFoundException("Account not found: " + accountCode))
                .getCode();
    }
}
