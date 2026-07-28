package com.ho.account.masterdata.core.infrastructure.persistence.repository;

import com.ho.account.masterdata.core.domain.model.BusinessPartner;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.IncorrectResultSizeDataAccessException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest(properties = {
        "spring.profiles.active=local",
        "spring.cloud.config.enabled=false",
        "spring.cloud.vault.enabled=false",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class BusinessPartnerRepositoryScd2Test {

    @Autowired
    private BusinessPartnerRepository repository;

    @Test
    void historicalLookupUsesValidityWhileCurrentLookupAlsoRequiresUseYn() {
        LocalDate today = LocalDate.now();
        BusinessPartner historical = partner(
                "BP-001", "Historical vendor", false,
                today.minusYears(1), today.minusDays(1));
        BusinessPartner current = partner(
                "BP-001", "Current vendor", true,
                today, LocalDate.of(9999, 12, 31));
        repository.save(historical);
        repository.save(current);

        assertThat(repository.findEffectiveByBusinessPartnerCode(
                "BP-001", today.minusMonths(6)))
                .contains(historical);
        assertThat(repository.findActiveByBusinessPartnerCode(
                "BP-001", today.minusMonths(6)))
                .isEmpty();
        assertThat(repository.findByBusinessPartnerCode("BP-001"))
                .contains(current);
        assertThat(repository.searchActiveByName("vendor", today))
                .containsExactly(current);
    }

    @Test
    void overlappingScd2RowsFailClosedInsteadOfSelectingOneSilently() {
        LocalDate today = LocalDate.now();
        repository.save(partner(
                "BP-OVERLAP", "First", true,
                today.minusDays(10), LocalDate.of(9999, 12, 31)));
        repository.save(partner(
                "BP-OVERLAP", "Second", true,
                today.minusDays(5), LocalDate.of(9999, 12, 31)));
        repository.flush();

        assertThatThrownBy(() -> repository.findEffectiveByBusinessPartnerCode(
                "BP-OVERLAP", today))
                .isInstanceOf(IncorrectResultSizeDataAccessException.class);
    }

    private BusinessPartner partner(
            String code,
            String name,
            boolean useYn,
            LocalDate validFrom,
            LocalDate validTo) {
        BusinessPartner partner = new BusinessPartner();
        partner.setBusinessPartnerCode(code);
        partner.setBusinessPartnerName(name);
        partner.setPartnerType(BusinessPartner.PartnerType.VENDOR);
        partner.setUseYn(useYn);
        partner.setValidFrom(validFrom);
        partner.setValidTo(validTo);
        return partner;
    }
}
