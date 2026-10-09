package com.ho.account.masterdata.core.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.ho.account.masterdata.core.domain.model.BusinessPartner;
import com.ho.account.masterdata.core.infrastructure.persistence.repository.BusinessPartnerRepository;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;

/**
 * Issue #261: BusinessPartner SCD2 유효기간과 업무 활성(useYn) 상태의 과거 조회 정합성 검증 테스트입니다.
 */
@DataJpaTest(properties = "spring.flyway.target=8")
@ContextConfiguration(classes = BusinessPartnerHistoricalActiveIntegrationTest.TestApplication.class)
@Import(JpaBusinessPartnerPersistenceAdapter.class)
class BusinessPartnerHistoricalActiveIntegrationTest {

    @Autowired
    private JpaBusinessPartnerPersistenceAdapter adapter;

    @Autowired
    private BusinessPartnerRepository repository;

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EntityScan(basePackages = "com.ho.account.masterdata.core")
    static class TestApplication {
    }

    @Test
    void terminatedBusinessPartnerIsActiveDuringValidWindowAndInactiveAfterwards() {
        LocalDate validFrom = LocalDate.of(2025, 1, 1);
        LocalDate terminationDate = LocalDate.of(2026, 6, 30);
        BusinessPartner partner = BusinessPartner.create(
                "BP-HIST-01",
                "역사적거래처",
                "123-45-67890",
                "김대표",
                "도매업",
                "원자재",
                BusinessPartner.PartnerType.VENDOR,
                true,
                BusinessPartner.KycStatus.APPROVED,
                BusinessPartner.RiskRating.LOW,
                validFrom,
                BusinessPartner.OPEN_ENDED_VALID_TO);

        BusinessPartner saved = adapter.save(partner);
        saved.terminate(terminationDate);
        adapter.save(saved);
        entityManager.flush();
        entityManager.clear();

        // 1. 종료일 이전 (2025-06-01): 활성 상태 재현
        LocalDate beforeTermination = LocalDate.of(2025, 6, 1);
        assertThat(repository.findActiveByBusinessPartnerCode("BP-HIST-01", beforeTermination)).isPresent();
        assertThat(adapter.searchActiveByName("역사적", beforeTermination)).hasSize(1);

        // 2. 종료일 당일 (2026-06-30): 활성 상태 재현
        assertThat(repository.findActiveByBusinessPartnerCode("BP-HIST-01", terminationDate)).isPresent();
        assertThat(adapter.searchActiveByName("역사적", terminationDate)).hasSize(1);

        // 3. 종료일 이후 (2026-07-01): 비활성 (유효기간 초과로 조회 불가능)
        LocalDate afterTermination = terminationDate.plusDays(1);
        assertThat(repository.findActiveByBusinessPartnerCode("BP-HIST-01", afterTermination)).isEmpty();
        assertThat(adapter.searchActiveByName("역사적", afterTermination)).isEmpty();
    }

    @Test
    void flywayV7MigrationRestoresUseYnTrueForClosedHistoricalVersions() {
        // 백필 대상 레거시 데이터 직구문 직접 삽입 (valid_to가 과거이고 use_yn = false인 레거시 종결 행)
        jdbcTemplate.update("""
                INSERT INTO business_partners
                (business_partner_code, business_partner_name, partner_type, use_yn, kyc_status, risk_rating, valid_from, valid_to)
                VALUES ('BP-LEGACY-01', '과거레거시거래처', 'CUSTOMER', FALSE, 'APPROVED', 'LOW', DATE '2024-01-01', DATE '2024-12-31')
                """);

        // V7 SQL 구문 실행 검증
        jdbcTemplate.update("""
                UPDATE business_partners
                SET use_yn = TRUE
                WHERE valid_to < DATE '9999-12-31'
                  AND use_yn = FALSE
                """);

        entityManager.clear();

        // 과거 기준일(2024-06-01)로 조회 시 use_yn = true로 복원되어 당시 활성 거래처로 재현됨 확인
        LocalDate historicalDate = LocalDate.of(2024, 6, 1);
        assertThat(repository.findActiveByBusinessPartnerCode("BP-LEGACY-01", historicalDate)).isPresent();
    }
}
