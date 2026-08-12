package com.ho.account.reporting.batch;

import static org.assertj.core.api.Assertions.assertThat;

import com.ho.account.reporting.application.port.out.LoadLedgerPort;
import com.ho.account.reporting.infrastructure.persistence.InMemoryLedgerBalanceAdapter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.test.context.ActiveProfiles;

/**
 * [Reporting Batch ApplicationContext Loading Integration Test]
 *
 * <p><strong>Pedagogical Explanation & Design Intent:</strong></p>
 * <ul>
 *   <li><strong>Context Loading Verification (애플리케이션 컨텍스트 로딩 검증)</strong>:
 *       Spring Boot 배치 애플리케이션의 핵심 구성 요소 및 의존성 주입이 정상 작동하며
 *       Spring Container(ApplicationContext)가 오류 없이 성공적으로 구동되는지 검증합니다.</li>
 *   <li><strong>Profile Isolation (local 프로파일 테스트 격리)</strong>:
 *       {@code @ActiveProfiles("local")}을 활성화하여 테스트 실행 시 외부 데이터베이스나 Cloud 인프라 연동 없이
 *       H2 인메모리 DB 기반으로 독립 실행(Self-contained Test Environment)될 수 있도록 구성합니다.</li>
 *   <li><strong>Batch Infra & Domain Bean Injection (배치 인프라 및 도메인 빈 검증)</strong>:
 *       {@link JobExplorer} 및 배치 연동 도메인 어댑터({@link LoadLedgerPort} - {@link InMemoryLedgerBalanceAdapter})가
 *       정상 등록되어 주입되는지 확인합니다.</li>
 * </ul>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("local")
class ReportingBatchApplicationTests {

    @Autowired
    private ConfigurableApplicationContext context;

    @Autowired
    private Environment environment;

    @Autowired
    private JobExplorer jobExplorer;

    @Autowired
    private LoadLedgerPort loadLedgerPort;

    @Test
    @DisplayName("local 프로파일 환경에서 Reporting Batch 애플리케이션의 ApplicationContext가 성공적으로 로드된다")
    void contextLoads() {
        // 1. Context Activation & Active Profile Verification
        assertThat(context.isActive()).isTrue();
        assertThat(environment.acceptsProfiles(Profiles.of("local"))).isTrue();

        // 2. Batch Infrastructure Bean Registration Verification
        assertThat(jobExplorer).isNotNull();

        // 3. Domain Persistence Adapter Verification (Memory mode)
        assertThat(loadLedgerPort).isInstanceOf(InMemoryLedgerBalanceAdapter.class);

        // 4. In-Memory H2 Datasource Verification
        assertThat(environment.getProperty("spring.datasource.url"))
                .contains("jdbc:h2:mem:")
                .contains("MODE=PostgreSQL");
        assertThat(environment.getProperty("spring.datasource.driver-class-name"))
                .isEqualTo("org.h2.Driver");
    }
}
