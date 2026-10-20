package com.ho.account.journalledger;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.jdbc.core.JdbcTemplate;

import com.ho.account.journalledger.infrastructure.persistence.repository.JournalEntryRepository;
import com.ho.account.journalledger.infrastructure.persistence.repository.GlBalanceRepository;
import com.ho.account.journalledger.infrastructure.persistence.repository.UnsettledItemRepository;
import com.ho.account.journalledger.application.service.ledger.LedgerService;

import java.time.LocalDate;

/**
 * [Journal Ledger API Local Profile & Application Context Integration Test]
 *
 * <p><strong>Pedagogical Explanation & Design Intent / 상세 교육적 주석:</strong></p>
 * <ul>
 *   <li><strong>Profile Separation & Isolated Local Execution (프로파일 분리 및 독립 실행)</strong>:
 *       {@code local} 프로파일 활성화 시 외부 PostgreSQL 접속이나 Cloud Config Server/Eureka 연동 없이
 *       H2 인메모리 DB 기반으로 Journal Ledger API Spring ApplicationContext가 안정적으로 초기화됨을 검증합니다.</li>
 *   <li><strong>Multi-Module Persistence Scanning Verification (멀티모듈 영속성 스캐닝 검증)</strong>:
 *       Composition Root({@link JournalLedgerApplication})의 {@code @EntityScan} 및 {@code @EnableJpaRepositories} 설정을 통해
 *       {@code JournalEntry}, {@code GlBalance}, {@code UnsettledItem} 엔티티와 해당 저장소 빈들이 JPA Metamodel 및 ApplicationContext에
 *       올바르게 등록되었음을 검증합니다.</li>
 *   <li><strong>External Infrastructure & Broker Decoupling (외부 서비스 및 메시지 브로커 디커플링 검증)</strong>:
 *       Eureka Discovery, Spring Cloud Config, Vault, Tracing뿐만 아니라 Kafka 메시지 브로커 이벤트 리스너({@code spring.kafka.listener.auto-startup=false})가
 *       비활성화되어 외부 브로커(localhost:9092) 접속 시도 없이 오프라인 환경에서도 안전하게 단독 구동될 수 있는지 검증합니다.</li>
 * </ul>
 */
@ActiveProfiles("local")
@SpringBootTest(
        classes = JournalLedgerApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.MOCK
)
class JournalLedgerApiLocalProfileTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private Environment environment;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Autowired
    private JournalEntryRepository journalEntryRepository;

    @Autowired
    private GlBalanceRepository glBalanceRepository;

    @Autowired
    private UnsettledItemRepository unsettledItemRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private LedgerService ledgerService;

    @Test
    @DisplayName("local 프로파일 구동 시 H2 인메모리 DB로 ApplicationContext가 정상 로드되고 엔티티 및 저장소 빈이 관리된다")
    void localProfileStartsWithH2AndCorePersistenceManaged() {
        // 1. 프로파일 활성화 상태 및 ApplicationContext 로딩 검증
        assertThat(environment.acceptsProfiles(Profiles.of("local"))).isTrue();
        assertThat(applicationContext).isNotNull();

        // 2. H2 데이터소스, 모듈 Flyway migration 및 Hibernate validate 검증
        assertThat(environment.getProperty("spring.datasource.url"))
                .startsWith("jdbc:h2:mem:journal_ledger_api_db")
                .contains("MODE=PostgreSQL");
        assertThat(environment.getProperty("spring.datasource.driver-class-name"))
                .isEqualTo("org.h2.Driver");
        assertThat(environment.getProperty("spring.jpa.hibernate.ddl-auto"))
                .isEqualTo("validate");

        // 3. 외부 마이크로서비스 제어 서버 및 Kafka 브로커 연동 비활성화 검증
        assertThat(environment.getProperty("spring.cloud.config.enabled", Boolean.class)).isFalse();
        assertThat(environment.getProperty("spring.cloud.discovery.enabled", Boolean.class)).isFalse();
        assertThat(environment.getProperty("spring.cloud.vault.enabled", Boolean.class)).isFalse();
        assertThat(environment.getProperty("eureka.client.enabled", Boolean.class)).isFalse();
        assertThat(environment.getProperty("eureka.client.register-with-eureka", Boolean.class)).isFalse();
        assertThat(environment.getProperty("eureka.client.fetch-registry", Boolean.class)).isFalse();
        assertThat(environment.getProperty("management.tracing.enabled", Boolean.class)).isFalse();
        assertThat(environment.getProperty("spring.kafka.listener.auto-startup", Boolean.class)).isFalse();
        // local은 V15 같은 비-JPA 테이블까지 단일 migration chain으로 만든다.
        // dev/prod의 외부 migration-runner 정책과 달리 합성 H2만 runtime Flyway를 사용한다.
        assertThat(environment.getProperty("spring.flyway.enabled", Boolean.class)).isTrue();

        // 4. JPA Metamodel Managed Types (엔티티 스캔) 검증
        assertThat(entityManagerFactory.getMetamodel().getManagedTypes())
                .extracting(type -> type.getJavaType().getName())
                .contains(
                        "com.ho.account.journalledger.domain.journal.domain.JournalEntry",
                        "com.ho.account.journalledger.domain.journal.domain.JournalEventQuarantine",
                        "com.ho.account.journalledger.domain.ledger.domain.GlBalance",
                        "com.ho.account.journalledger.domain.unsettled.UnsettledItem"
                );

        // 5. 핵심 JPA Repositories 빈 주입 검증
        assertThat(journalEntryRepository).isNotNull();
        assertThat(glBalanceRepository).isNotNull();
        assertThat(unsettledItemRepository).isNotNull();
    }

    @Test
    @DisplayName("실제 local Flyway/validate 스키마는 V15 제어 singleton을 만들고 fenced 잔액 조회를 지원한다")
    void localMigratedSchemaSupportsFencedBalanceReads() {
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM ledger_reaggregation_control WHERE control_id = 1 AND status = 'OPEN'",
                Integer.class)).isOne();

        LocalDate date = LocalDate.of(2026, 9, 24);
        assertThat(ledgerService.getGlBalances(date, date, null, null)).isEmpty();
    }
}
