package com.ho.account.loan.infrastructure.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.ho.account.loan.application.port.out.LoanReferenceDataPort.AccountReference;
import com.ho.account.loan.application.port.out.LoanReferenceDataPort.LoanReferenceSnapshot;
import com.ho.account.masterdata.core.application.port.out.AccountSubjectPersistencePort;
import com.ho.account.masterdata.core.application.port.out.BusinessPartnerPersistencePort;
import com.ho.account.masterdata.core.application.port.out.CurrencyPersistencePort;
import com.ho.account.masterdata.core.domain.model.AccountSubject;
import com.ho.account.masterdata.core.domain.model.BusinessPartner;
import com.ho.account.masterdata.core.domain.model.Currency;
import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LoanReferenceDataAdapterTest {

    @Mock private BusinessPartnerPersistencePort businessPartnerPort;
    @Mock private CurrencyPersistencePort currencyPort;
    @Mock private AccountSubjectPersistencePort accountSubjectPort;

    private LoanReferenceDataAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new LoanReferenceDataAdapter(
                businessPartnerPort, currencyPort, accountSubjectPort);
    }

    @Test
    void resolvesOnlyReferencesEffectiveOnBusinessDate() {
        LocalDate effectiveDate = LocalDate.of(2026, 5, 10);
        BusinessPartner partner = partner(100L, "차주 A", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31));
        Currency currency = currency("KRW", LocalDate.of(2020, 1, 1), LocalDate.of(9999, 12, 31));
        when(businessPartnerPort.findById(100L)).thenReturn(Optional.of(partner));
        when(currencyPort.findByCodeAt("KRW", effectiveDate)).thenReturn(Optional.of(currency));

        LoanReferenceSnapshot snapshot = adapter.requireLoanReferences(
                100L, " krw ", effectiveDate);

        assertThat(snapshot.businessPartnerId()).isEqualTo(100L);
        assertThat(snapshot.businessPartnerName()).isEqualTo("차주 A");
        assertThat(snapshot.currencyCode()).isEqualTo("KRW");
    }

    @Test
    void invalidPartnerIdFailsBeforeCallingAnyProvider() {
        assertThatThrownBy(() -> adapter.requireLoanReferences(
                null, "KRW", LocalDate.of(2026, 5, 10)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("businessPartnerId");
        verifyNoInteractions(businessPartnerPort, currencyPort, accountSubjectPort);
    }

    @Test
    void expiredPartnerStopsBeforeCurrencyLookup() {
        LocalDate effectiveDate = LocalDate.of(2026, 5, 10);
        when(businessPartnerPort.findById(100L)).thenReturn(Optional.of(partner(
                100L, "차주 A", LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31))));

        assertThatThrownBy(() -> adapter.requireLoanReferences(100L, "KRW", effectiveDate))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("not effective");
        verify(currencyPort, never()).findByCodeAt("KRW", effectiveDate);
    }

    @Test
    void accountLookupUsesEffectiveDatedMasterPort() {
        LocalDate effectiveDate = LocalDate.of(2026, 5, 10);
        AccountSubject account = new AccountSubject();
        account.setCode("131999");
        account.setName("Loan receivable");
        when(accountSubjectPort.findByCodeAt("131999", effectiveDate))
                .thenReturn(Optional.of(account));

        AccountReference reference = adapter.requireAccount(" 131999 ", effectiveDate);

        assertThat(reference.code()).isEqualTo("131999");
        assertThat(reference.name()).isEqualTo("Loan receivable");
        verify(accountSubjectPort).findByCodeAt("131999", effectiveDate);
    }

    private BusinessPartner partner(
            Long id, String name, LocalDate validFrom, LocalDate validTo) {
        BusinessPartner partner = new BusinessPartner();
        partner.setId(id);
        partner.setBusinessPartnerName(name);
        partner.setUseYn(true);
        partner.setValidFrom(validFrom);
        partner.setValidTo(validTo);
        return partner;
    }

    private Currency currency(String code, LocalDate validFrom, LocalDate validTo) {
        Currency currency = new Currency();
        currency.setCurrencyCode(code);
        currency.setValidFrom(validFrom);
        currency.setValidTo(validTo);
        return currency;
    }
}
