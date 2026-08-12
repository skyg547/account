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

import com.ho.account.journalledger.domain.journal.repository.JournalEntryRepository;
import com.ho.account.journalledger.domain.ledger.repository.GlBalanceRepository;
import com.ho.account.journalledger.adapter.out.persistence.unsettled.UnsettledItemRepository;

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

    @Test
    @DisplayName("local 프로파일 구동 시 H2 인메모리 DB로 ApplicationContext가 정상 로드되고 엔티티 및 저장소 빈이 관리된다")
    void localProfileStartsWithH2AndCorePersistenceManaged() {
        // 1. 프로파일 활성화 상태 및 ApplicationContext 로딩 검증
        assertThat(environment.acceptsProfiles(Profiles.of("local"))).isTrue();
        assertThat(applicationContext).isNotNull();

        // 2. H2 데이터소스 및 JPA ddl-auto=create-drop 검증
        assertThat(environment.getProperty("spring.datasource.url"))
                .startsWith("jdbc:h2:mem:journal_ledger_api_db")
                .contains("MODE=PostgreSQL");
        assertThat(environment.getProperty("spring.datasource.driver-class-name"))
                .isEqualTo("org.h2.Driver");
        assertThat(environment.getProperty("spring.jpa.hibernate.ddl-auto"))
                .isEqualTo("create-drop");

        // 3. 외부 마이크로서비스 제어 서버 및 Kafka 브로커 연동 비활성화 검증
        assertThat(environment.getProperty("spring.cloud.config.enabled", Boolean.class)).isFalse();
        assertThat(environment.getProperty("spring.cloud.discovery.enabled", Boolean.class)).isFalse();
        assertThat(environment.getProperty("spring.cloud.vault.enabled", Boolean.class)).isFalse();
        assertThat(environment.getProperty("eureka.client.enabled", Boolean.class)).isFalse();
        assertThat(environment.getProperty("eureka.client.register-with-eureka", Boolean.class)).isFalse();
        assertThat(environment.getProperty("eureka.client.fetch-registry", Boolean.class)).isFalse();
        assertThat(environment.getProperty("management.tracing.enabled", Boolean.class)).isFalse();
        assertThat(environment.getProperty("spring.kafka.listener.auto-startup", Boolean.class)).isFalse();

        // 4. JPA Metamodel Managed Types (엔티티 스캔) 검증
        assertThat(entityManagerFactory.getMetamodel().getManagedTypes())
                .extracting(type -> type.getJavaType().getName())
                .contains(
                        "com.ho.account.journalledger.domain.journal.domain.JournalEntry",
                        "com.ho.account.journalledger.domain.ledger.domain.GlBalance",
                        "com.ho.account.journalledger.domain.unsettled.UnsettledItem"
                );

        // 5. 핵심 JPA Repositories 빈 주입 검증
        assertThat(journalEntryRepository).isNotNull();
        assertThat(glBalanceRepository).isNotNull();
        assertThat(unsettledItemRepository).isNotNull();
    }
}
