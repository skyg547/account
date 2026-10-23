package com.ho.account.masterdata.core.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.ho.account.masterdata.core.domain.model.BusinessPartner;
import com.ho.account.masterdata.core.domain.model.BusinessPartnerScreening;
import com.ho.account.masterdata.core.infrastructure.persistence.repository.BusinessPartnerScreeningAuditRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;

/** Checks the current snapshot and historical audit against actual JPA rows. */
@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false"
})
@ContextConfiguration(classes = JpaBusinessPartnerScreeningAdapterTest.TestApplication.class)
@Import(JpaBusinessPartnerScreeningAdapter.class)
class JpaBusinessPartnerScreeningAdapterTest {

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EntityScan(basePackages = "com.ho.account.masterdata.core")
    static class TestApplication {
    }

    @Autowired
    private JpaBusinessPartnerScreeningAdapter adapter;

    @Autowired
    private BusinessPartnerScreeningAuditRepository audits;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void decisionAndResubmissionKeepPriorEvidenceAndReviewerInImmutableAuditRows() {
        LocalDateTime submittedAt = LocalDateTime.of(2026, 10, 1, 9, 0);
        LocalDate expiry = LocalDate.of(2026, 10, 31);
        BusinessPartnerScreening pending = BusinessPartnerScreening.submit("BP-923", "KR",
                "BUSINESS_REGISTRATION", "1234567890", "synthetic-evidence-923", expiry,
                "submitter", submittedAt);
        adapter.save(pending);
        adapter.appendAudit(pending);
        BusinessPartnerScreening approved = pending.decide(BusinessPartner.KycStatus.APPROVED,
                BusinessPartner.RiskRating.MEDIUM, "Evidence checked", "reviewer", submittedAt.plusHours(1));
        adapter.save(approved);
        adapter.appendAudit(approved);
        BusinessPartnerScreening resubmitted = BusinessPartnerScreening.submit("BP-923", "KR",
                "BUSINESS_REGISTRATION", "1234567890", "new-synthetic-evidence-923", expiry,
                "submitter", submittedAt.plusDays(1));
        adapter.save(resubmitted);
        adapter.appendAudit(resubmitted);

        entityManager.flush();
        entityManager.clear();

        BusinessPartnerScreening current = adapter.findByBusinessPartnerCode("BP-923").orElseThrow();
        assertThat(current.status()).isEqualTo(BusinessPartner.KycStatus.PENDING);
        assertThat(current.evidenceReference()).isEqualTo("new-synthetic-evidence-923");
        assertThat(current.reviewedBy()).isNull();
        assertThat(adapter.findLatestAt("BP-923", submittedAt.toLocalDate()).orElseThrow().status())
                .isEqualTo(BusinessPartner.KycStatus.APPROVED);
        assertThat(adapter.findLatestAt("BP-923", submittedAt.plusDays(1).toLocalDate()).orElseThrow().status())
                .isEqualTo(BusinessPartner.KycStatus.PENDING);
        assertThat(adapter.findLatestAt("BP-923", submittedAt.minusDays(1).toLocalDate())).isEmpty();
        var auditHistory = audits.findAll().stream()
                .sorted(Comparator.comparing(BusinessPartnerScreeningAuditJpaEntity::getId))
                .toList();
        assertThat(auditHistory).hasSize(3)
                .extracting(BusinessPartnerScreeningAuditJpaEntity::getStatus)
                .containsExactly(BusinessPartner.KycStatus.PENDING,
                        BusinessPartner.KycStatus.APPROVED, BusinessPartner.KycStatus.PENDING);
        BusinessPartnerScreeningAuditJpaEntity decisionAudit = auditHistory.get(1);
        assertThat(decisionAudit.getEvidenceReference()).isEqualTo("synthetic-evidence-923");
        assertThat(decisionAudit.getReviewedBy()).isEqualTo("reviewer");
        assertThat(decisionAudit.getDecisionReason()).isEqualTo("Evidence checked");
        assertThat(decisionAudit.getDecidedAt()).isEqualTo(submittedAt.plusHours(1));
    }
}
