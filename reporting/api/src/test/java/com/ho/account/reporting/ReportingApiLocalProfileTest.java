package com.ho.account.reporting;

import static org.assertj.core.api.Assertions.assertThat;

import com.ho.account.reporting.application.port.out.LoadLedgerPort;
import com.ho.account.reporting.infrastructure.persistence.InMemoryLedgerBalanceAdapter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.test.context.ActiveProfiles;

/**
 * [Reporting API Local Profile Integration Test]
 *
 * <p><strong>Pedagogical Explanation & Design Intent:</strong></p>
 * <ul>
 *   <li><strong>Profile Separation (프로파일 분리)</strong>:
 *       CLI 오버라이드 없이 {@code ActiveProfiles("local")} 지정만으로 local 전용
 *       {@code application-local.yml} 프로파일 설정이 정상 로드됨을 보장합니다.
 *       이를 통해 dev/prod용 외부 PostgreSQL 인프라 연동 의존성 없이 로컬 환경에서 단독 구동
 *       (Self-contained Local Runtime)이 가능해집니다.</li>
 *   <li><strong>In-memory H2 DB & Memory Persistence Mode Isolation</strong>:
 *       로컬 개발/테스트 시 H2 인메모리 DB와 메모리 퍼시스턴스 모드({@code account.reporting.persistence.mode=memory})를
 *       사용함으로써 외부 데이터베이스의 상태나 타 모듈 서비스의 가용성에 영향받지 않으며,
 *       JPA {@code ddl-auto=create-drop} 구성을 통해 테스트 간 데이터 격리성을 완벽히 보장합니다.</li>
 *   <li><strong>External Cloud Infrastructure Decoupling (외부 제어 서버 분리)</strong>:
 *       Config Server, Eureka, Vault 등 마이크로서비스 외부 제어 서버 연동이 미구동 시에도
 *       로컬 애플리케이션 컨텍스트 생성이 실패하지 않도록 제어 프레임워크가 비활성화됨을 검증합니다.</li>
 * </ul>
 */
@ActiveProfiles("local")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class ReportingApiLocalProfileTest {

    @Autowired
    private ConfigurableApplicationContext context;

    @Autowired
    private Environment environment;

    @Autowired
    private LoadLedgerPort loadLedgerPort;

    @Test
    @DisplayName("local 프로파일 구동 시 H2 메모리 DB와 memory 퍼시스턴스 어댑터로 API 컨텍스트가 성공적으로 로딩된다")
    void startsFromRealLocalH2ProfileWithoutExternalDependencies() {
        // 1. Context status and active profile verification
        assertThat(context.isActive()).isTrue();
        assertThat(environment.acceptsProfiles(Profiles.of("local"))).isTrue();

        // 2. Self-contained H2 datasource & memory persistence mode verification
        assertThat(environment.getProperty("account.reporting.persistence.mode")).isEqualTo("memory");
        assertThat(loadLedgerPort).isInstanceOf(InMemoryLedgerBalanceAdapter.class);
        assertThat(environment.getProperty("spring.datasource.url"))
                .startsWith("jdbc:h2:mem:reporting_api_db")
                .contains("MODE=PostgreSQL");
        assertThat(environment.getProperty("spring.datasource.driver-class-name"))
                .isEqualTo("org.h2.Driver");
        assertThat(environment.getProperty("spring.jpa.hibernate.ddl-auto"))
                .isEqualTo("create-drop");

        // 3. External Infrastructure decoupling verification
        assertThat(environment.getProperty("spring.cloud.config.enabled", Boolean.class)).isFalse();
        assertThat(environment.getProperty("spring.cloud.discovery.enabled", Boolean.class)).isFalse();
        assertThat(environment.getProperty("spring.cloud.vault.enabled", Boolean.class)).isFalse();
        assertThat(environment.getProperty("eureka.client.enabled", Boolean.class)).isFalse();
        assertThat(environment.getProperty("eureka.client.register-with-eureka", Boolean.class)).isFalse();
        assertThat(environment.getProperty("eureka.client.fetch-registry", Boolean.class)).isFalse();
        assertThat(environment.getProperty("management.tracing.enabled", Boolean.class)).isFalse();
    }
}
