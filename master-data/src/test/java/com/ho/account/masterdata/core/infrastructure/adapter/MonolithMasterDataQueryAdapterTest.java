package com.ho.account.masterdata.core.infrastructure.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.ho.account.contracts.masterdata.AccountSubjectRef;
import com.ho.account.contracts.masterdata.BusinessPartnerRef;
import com.ho.account.masterdata.core.domain.model.AccountSubject;
import com.ho.account.masterdata.core.domain.model.BusinessPartner;
import com.ho.account.masterdata.core.infrastructure.persistence.repository.AccountSubjectRepository;
import com.ho.account.masterdata.core.infrastructure.persistence.repository.BusinessPartnerRepository;
import com.ho.account.masterdata.core.infrastructure.persistence.repository.DepartmentRepository;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class MonolithMasterDataQueryAdapterTest {

    private final AccountSubjectRepository accountSubjectRepository =
            mock(AccountSubjectRepository.class);
    private final BusinessPartnerRepository businessPartnerRepository =
            mock(BusinessPartnerRepository.class);
    private final DepartmentRepository departmentRepository =
            mock(DepartmentRepository.class);
    private final MonolithMasterDataQueryAdapter adapter = new MonolithMasterDataQueryAdapter(
            accountSubjectRepository,
            businessPartnerRepository,
            departmentRepository);

    @Test
    void queriesTheAccountVersionEffectiveOnTheRequestedDate() {
        LocalDate valuationDate = LocalDate.of(2025, 12, 31);
        AccountSubject borrowing = new AccountSubject();
        borrowing.setCode("221000");
        borrowing.setName("Foreign borrowing");
        borrowing.setBalanceType(AccountSubject.BalanceType.CREDIT);
        when(accountSubjectRepository.findActiveByCode("221000", valuationDate))
                .thenReturn(Optional.of(borrowing));

        AccountSubjectRef result =
                adapter.findAccountSubjectAt("221000", valuationDate).orElseThrow();

        assertThat(result.code()).isEqualTo("221000");
        assertThat(result.normalBalanceSide()).isEqualTo("CREDIT");
        verify(accountSubjectRepository).findActiveByCode("221000", valuationDate);
    }

    @Test
    void rejectsMissingEffectiveDateBeforeCallingPersistence() {
        assertThatThrownBy(() -> adapter.findAccountSubjectAt("221000", null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("effectiveDate");
        verifyNoInteractions(accountSubjectRepository);
    }

    @Test
    void historicalPartnerLookupUsesValidityWindowEvenAfterCurrentFlagWasClosed() {
        LocalDate invoiceDate = LocalDate.of(2025, 12, 31);
        BusinessPartner historical = new BusinessPartner();
        historical.setBusinessPartnerCode("BP-001");
        historical.setBusinessPartnerName("Historical vendor");
        historical.setPartnerType(BusinessPartner.PartnerType.VENDOR);
        historical.setUseYn(false);
        when(businessPartnerRepository.findEffectiveByBusinessPartnerCode("BP-001", invoiceDate))
                .thenReturn(Optional.of(historical));

        BusinessPartnerRef result = adapter.findBusinessPartnerAt("BP-001", invoiceDate).orElseThrow();

        assertThat(result.name()).isEqualTo("Historical vendor");
        verify(businessPartnerRepository).findEffectiveByBusinessPartnerCode("BP-001", invoiceDate);
    }
}
