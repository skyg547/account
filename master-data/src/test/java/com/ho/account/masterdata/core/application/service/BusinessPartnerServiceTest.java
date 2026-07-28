package com.ho.account.masterdata.core.application.service;

import com.ho.account.masterdata.core.application.command.BusinessPartnerCommand;
import com.ho.account.masterdata.core.application.port.out.BusinessPartnerPersistencePort;
import com.ho.account.masterdata.core.domain.model.BusinessPartner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BusinessPartnerServiceTest {

    @Mock
    private BusinessPartnerPersistencePort businessPartnerPersistencePort;

    private BusinessPartnerService service;

    @BeforeEach
    void setUp() {
        service = new BusinessPartnerService(businessPartnerPersistencePort);
    }

    @Test
    void updateBusinessPartnerCreatesNewScd2VersionAndClosesCurrentVersion() {
        BusinessPartner current = partner(
                10L,
                "BP001",
                "Old partner",
                "111-22-33333",
                BusinessPartner.PartnerType.VENDOR,
                LocalDate.of(2026, 1, 1),
                LocalDate.of(9999, 12, 31));
        BusinessPartnerCommand command = new BusinessPartnerCommand(
                "BP001",
                "New partner",
                null,
                "New CEO",
                null,
                null,
                null,
                null,
                BusinessPartner.KycStatus.APPROVED,
                null,
                LocalDate.of(2026, 6, 1),
                null);

        when(businessPartnerPersistencePort.findById(10L)).thenReturn(Optional.of(current));
        when(businessPartnerPersistencePort.save(any(BusinessPartner.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        BusinessPartner updated = service.updateBusinessPartner(10L, command);

        ArgumentCaptor<BusinessPartner> captor = ArgumentCaptor.forClass(BusinessPartner.class);
        verify(businessPartnerPersistencePort, times(2)).save(captor.capture());

        assertThat(current.getValidTo()).isEqualTo(LocalDate.of(2026, 5, 31));
        assertThat(current.getUseYn()).isFalse();
        assertThat(captor.getAllValues().get(0)).isSameAs(current);
        assertThat(captor.getAllValues().get(1)).isSameAs(updated);
        assertThat(updated).isNotSameAs(current);
        assertThat(updated.getId()).isNull();
        assertThat(updated.getBusinessPartnerCode()).isEqualTo("BP001");
        assertThat(updated.getBusinessPartnerName()).isEqualTo("New partner");
        assertThat(updated.getRegistrationNumber()).isEqualTo("111-22-33333");
        assertThat(updated.getCeoName()).isEqualTo("New CEO");
        assertThat(updated.getPartnerType()).isEqualTo(BusinessPartner.PartnerType.VENDOR);
        assertThat(updated.getUseYn()).isTrue();
        assertThat(updated.getKycStatus()).isEqualTo(BusinessPartner.KycStatus.APPROVED);
        assertThat(updated.getRiskRating()).isEqualTo(BusinessPartner.RiskRating.LOW);
        assertThat(updated.getValidFrom()).isEqualTo(LocalDate.of(2026, 6, 1));
        assertThat(updated.getValidTo()).isEqualTo(LocalDate.of(9999, 12, 31));
    }

    @Test
    void updateBusinessPartnerRejectsCodeChange() {
        BusinessPartner current = partner(
                10L,
                "BP001",
                "Old partner",
                null,
                BusinessPartner.PartnerType.CUSTOMER,
                LocalDate.of(2026, 1, 1),
                LocalDate.of(9999, 12, 31));
        BusinessPartnerCommand command = new BusinessPartnerCommand(
                "BP999",
                "New partner",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                LocalDate.of(2026, 6, 1),
                null);

        when(businessPartnerPersistencePort.findById(10L)).thenReturn(Optional.of(current));

        assertThatThrownBy(() -> service.updateBusinessPartner(10L, command))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("거래처 코드는");
    }

    @Test
    void deleteBusinessPartnerTerminatesCurrentVersion() {
        BusinessPartner current = partner(
                10L,
                "BP001",
                "Old partner",
                null,
                BusinessPartner.PartnerType.CUSTOMER,
                LocalDate.now().minusDays(10),
                LocalDate.of(9999, 12, 31));
        when(businessPartnerPersistencePort.findById(10L)).thenReturn(Optional.of(current));

        service.deleteBusinessPartner(10L);

        assertThat(current.getValidTo()).isEqualTo(LocalDate.now());
        assertThat(current.getUseYn()).isFalse();
        verify(businessPartnerPersistencePort).save(current);
    }

    @Test
    void searchBusinessPartnersUsesTrimmedKeywordAndCurrentActiveQuery() {
        LocalDate today = LocalDate.now();
        BusinessPartner active = partner(
                10L, "BP001", "Active partner", null,
                BusinessPartner.PartnerType.VENDOR,
                today.minusDays(10), LocalDate.of(9999, 12, 31));
        when(businessPartnerPersistencePort.searchActiveByName("partner", today))
                .thenReturn(List.of(active));

        assertThat(service.searchBusinessPartnersByName("  partner  ")).containsExactly(active);

        verify(businessPartnerPersistencePort).searchActiveByName("partner", today);
    }

    @Test
    void searchBusinessPartnersRejectsBlankKeywordBeforePersistence() {
        assertThatThrownBy(() -> service.searchBusinessPartnersByName("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("검색어");

        verifyNoInteractions(businessPartnerPersistencePort);
    }

    private BusinessPartner partner(
            Long id,
            String code,
            String name,
            String registrationNumber,
            BusinessPartner.PartnerType partnerType,
            LocalDate validFrom,
            LocalDate validTo) {
        BusinessPartner partner = new BusinessPartner();
        partner.setId(id);
        partner.setBusinessPartnerCode(code);
        partner.setBusinessPartnerName(name);
        partner.setRegistrationNumber(registrationNumber);
        partner.setPartnerType(partnerType);
        partner.setUseYn(true);
        partner.setKycStatus(BusinessPartner.KycStatus.PENDING);
        partner.setRiskRating(BusinessPartner.RiskRating.LOW);
        partner.setValidFrom(validFrom);
        partner.setValidTo(validTo);
        return partner;
    }
}
