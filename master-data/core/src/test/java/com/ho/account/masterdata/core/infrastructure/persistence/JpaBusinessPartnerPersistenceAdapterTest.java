package com.ho.account.masterdata.core.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ho.account.masterdata.core.domain.model.BusinessPartner;
import com.ho.account.masterdata.core.domain.model.BusinessPartnerAccount;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.dao.IncorrectResultSizeDataAccessException;

/**
 * H2에 실제 행을 저장해 도메인 aggregate와 JPA aggregate 사이의 명시적 매핑을 검증합니다.
 *
 * <p>mock Repository 테스트만으로는 자식 FK, cascade, EntityGraph, JPQL 날짜 조건을 확인할 수
 * 없습니다. 이 slice 테스트는 가벼운 내장 DB를 사용하면서도 영속성 어댑터 경계의 실제 회귀를
 * 잡도록 구성했습니다.</p>
 */
@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false"
})
@ContextConfiguration(classes = JpaBusinessPartnerPersistenceAdapterTest.TestApplication.class)
@Import(JpaBusinessPartnerPersistenceAdapter.class)
class JpaBusinessPartnerPersistenceAdapterTest {

    @Autowired
    private JpaBusinessPartnerPersistenceAdapter adapter;

    @Autowired
    private TestEntityManager entityManager;

    /**
     * Core는 Issue #40 이후 실행 진입점이 없는 library입니다.
     *
     * <p>JPA slice가 API의 Spring Boot application을 역참조하면 core 단독 테스트가 API 배치
     * 구조에 결합됩니다. 테스트 전용 최소 부트 구성을 명시해 repository/entity 자동 설정만
     * 사용하고, production 모듈 의존 방향은 그대로 유지합니다.</p>
     */
    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EntityScan(basePackages = "com.ho.account.masterdata.core")
    static class TestApplication {
    }

    @Test
    void savesAndReloadsDomainAggregateWithChildAccount() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 1, 2, 9, 30);
        BusinessPartnerAccount account = BusinessPartnerAccount.reconstitute(
                null,
                "한국은행",
                "110-123-456789",
                "새봄상사",
                "BOKRKRSE",
                true,
                createdAt,
                createdAt);
        BusinessPartner original = reconstitute(
                "BP-ROUNDTRIP",
                "새봄상사",
                true,
                LocalDate.of(2026, 1, 1),
                BusinessPartner.OPEN_ENDED_VALID_TO,
                List.of(account));

        BusinessPartner saved = adapter.save(original);
        entityManager.flush();
        entityManager.clear();
        BusinessPartner restored = adapter.findById(saved.getId()).orElseThrow();

        // 부모와 자식을 모두 다시 읽어야 JPA Entity를 외부로 노출하지 않는 매핑 계약이 증명됩니다.
        assertThat(restored.getId()).isNotNull();
        assertThat(restored.getBusinessPartnerCode()).isEqualTo("BP-ROUNDTRIP");
        assertThat(restored.getRegistrationNumber()).isEqualTo("123-45-67890");
        assertThat(restored.getAuditUser()).isEqualTo("tester");
        assertThat(restored.getAccounts()).hasSize(1);
        BusinessPartnerAccount restoredAccount = restored.getAccounts().get(0);
        assertThat(restoredAccount.getId()).isNotNull();
        assertThat(restoredAccount.getBankName()).isEqualTo("한국은행");
        assertThat(restoredAccount.getAccountNumber()).isEqualTo("110-123-456789");
        assertThat(restoredAccount.getAccountHolder()).isEqualTo("새봄상사");
        assertThat(restoredAccount.getSwiftCode()).isEqualTo("BOKRKRSE");
        assertThat(restoredAccount.isMainAccount()).isTrue();
        assertThat(restoredAccount.getBusinessPartner()).isSameAs(restored);
    }

    @Test
    void resolvesCurrentAndHistoricalScd2VersionsAtInclusiveBoundaries() {
        LocalDate historicalFrom = LocalDate.of(2024, 1, 1);
        LocalDate historicalTo = LocalDate.of(2024, 12, 31);
        LocalDate currentFrom = LocalDate.now().minusDays(30);
        adapter.save(reconstitute(
                "BP-SCD2",
                "과거 상호",
                false,
                historicalFrom,
                historicalTo,
                List.of()));
        adapter.save(reconstitute(
                "BP-SCD2",
                "현재 상호",
                true,
                currentFrom,
                BusinessPartner.OPEN_ENDED_VALID_TO,
                List.of()));
        entityManager.flush();
        entityManager.clear();

        BusinessPartner current = adapter.findByBusinessPartnerCode("BP-SCD2").orElseThrow();
        BusinessPartner historicalAtStart =
                adapter.findEffectiveByBusinessPartnerCode("BP-SCD2", historicalFrom).orElseThrow();
        BusinessPartner historicalAtEnd =
                adapter.findEffectiveByBusinessPartnerCode("BP-SCD2", historicalTo).orElseThrow();

        // 현재 조회는 useYn을 보지만 과거 기준일 조회는 종료된 SCD2 행도 복원해야 합니다.
        assertThat(current.getBusinessPartnerName()).isEqualTo("현재 상호");
        assertThat(historicalAtStart.getBusinessPartnerName()).isEqualTo("과거 상호");
        assertThat(historicalAtEnd.getBusinessPartnerName()).isEqualTo("과거 상호");
        assertThat(adapter.findEffectiveByBusinessPartnerCode(
                "BP-SCD2", historicalTo.plusDays(1))).isEmpty();
    }

    @Test
    void existenceIncludesFutureAndExpiredRowsRegardlessOfUseFlag() {
        LocalDate today = LocalDate.now();
        adapter.save(reconstitute("BP-FUTURE-EXISTS", "미래 거래처", true,
                today.plusDays(21), today.plusDays(30), List.of()));
        adapter.save(reconstitute("BP-HISTORY-EXISTS", "과거 거래처", false,
                today.minusDays(60), today.minusDays(30), List.of()));
        entityManager.flush();
        entityManager.clear();

        assertThat(adapter.existsByBusinessPartnerCode("BP-FUTURE-EXISTS")).isTrue();
        assertThat(adapter.existsByBusinessPartnerCode("BP-HISTORY-EXISTS")).isTrue();
        assertThat(adapter.existsByBusinessPartnerCode("BP-NEW-KEY")).isFalse();
    }

    @Test
    void savesCopiedAccountsAsDifferentRowsForEachScd2Version() {
        LocalDate today = LocalDate.now();
        LocalDateTime timestamp = LocalDateTime.of(2026, 1, 2, 9, 30);
        BusinessPartnerAccount account = BusinessPartnerAccount.reconstitute(
                null,
                "한국은행",
                "110-123-456789",
                "새봄상사",
                "BOKRKRSE",
                true,
                timestamp,
                timestamp);
        BusinessPartner current = adapter.save(reconstitute(
                "BP-ACCOUNT-SCD2",
                "새봄상사",
                true,
                today.minusDays(30),
                BusinessPartner.OPEN_ENDED_VALID_TO,
                List.of(account)));
        Long historicalPartnerId = current.getId();
        Long historicalAccountId = current.getAccounts().get(0).getId();
        LocalDate nextValidFrom = today.plusDays(1);

        BusinessPartner next = current.createNextVersion(
                "BP-ACCOUNT-SCD2",
                "새봄상사 신사명",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                nextValidFrom,
                BusinessPartner.OPEN_ENDED_VALID_TO);
        current.closeVersion(nextValidFrom.minusDays(1));
        adapter.save(current);
        BusinessPartner savedNext = adapter.save(next);
        entityManager.flush();
        entityManager.clear();

        BusinessPartner historical = adapter.findById(historicalPartnerId).orElseThrow();
        BusinessPartner effectiveNext =
                adapter.findEffectiveByBusinessPartnerCode("BP-ACCOUNT-SCD2", nextValidFrom).orElseThrow();

        // 같은 계좌 업무 값은 두 버전에 남지만 PK/FK 행은 달라 과거 aggregate가 훼손되지 않습니다.
        assertThat(historical.getAccounts()).singleElement()
                .extracting(BusinessPartnerAccount::getId)
                .isEqualTo(historicalAccountId);
        assertThat(effectiveNext.getId()).isEqualTo(savedNext.getId());
        assertThat(effectiveNext.getAccounts()).singleElement()
                .satisfies(copied -> {
                    assertThat(copied.getId()).isNotNull().isNotEqualTo(historicalAccountId);
                    assertThat(copied.getAccountNumber()).isEqualTo("110-123-456789");
                    assertThat(copied.getBusinessPartner()).isSameAs(effectiveNext);
                });
    }

    @Test
    void failsClosedWhenScd2RowsOverlapForTheSameBusinessDate() {
        LocalDate today = LocalDate.now();
        adapter.save(reconstitute(
                "BP-OVERLAP",
                "중복 버전 A",
                true,
                today.minusDays(10),
                BusinessPartner.OPEN_ENDED_VALID_TO,
                List.of()));
        adapter.save(reconstitute(
                "BP-OVERLAP",
                "중복 버전 B",
                true,
                today.minusDays(5),
                BusinessPartner.OPEN_ENDED_VALID_TO,
                List.of()));
        entityManager.flush();
        entityManager.clear();

        // Optional query가 겹치는 행 중 하나를 임의 선택하면 손상된 SCD2를 정상 데이터처럼 숨기게 됩니다.
        assertThatThrownBy(() -> adapter.findByBusinessPartnerCode("BP-OVERLAP"))
                .isInstanceOf(IncorrectResultSizeDataAccessException.class);
        assertThatThrownBy(() -> adapter.findEffectiveByBusinessPartnerCode("BP-OVERLAP", today))
                .isInstanceOf(IncorrectResultSizeDataAccessException.class);
    }

    private BusinessPartner reconstitute(
            String code,
            String name,
            boolean useYn,
            LocalDate validFrom,
            LocalDate validTo,
            List<BusinessPartnerAccount> accounts) {
        LocalDateTime timestamp = LocalDateTime.of(2026, 1, 2, 9, 30);
        return BusinessPartner.reconstitute(
                null,
                code,
                name,
                "123-45-67890",
                "김새봄",
                "도매업",
                "원자재",
                BusinessPartner.PartnerType.VENDOR,
                useYn,
                BusinessPartner.KycStatus.APPROVED,
                BusinessPartner.RiskRating.MEDIUM,
                validFrom,
                validTo,
                timestamp,
                timestamp,
                "tester",
                accounts);
    }
}
