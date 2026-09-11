package com.ho.account.loan.batch;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.batch.JobLauncherApplicationRunner;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.test.context.ActiveProfiles;

/**
 * [Loan Batch Local Profile Integration Test]
 *
 * <p><strong>Pedagogical Explanation & Design Intent / 상세 교육적 주석:</strong></p>
 * <ul>
 *   <li><strong>Profile Separation (프로파일 분리)</strong>:
 *       local 프로파일 활성화만으로 외부 PostgreSQL DB 접속 요구 없이 독립 런타임
 *       (Self-contained Local Runtime)으로 배치 컨텍스트를 성공적으로 로딩하는지 검증합니다.</li>
 *   <li><strong>Web Environment Disabled (web-application-type: none)</strong>:
 *       배치 프로세스 본연의 자원 효율성을 위해 내장 서블릿 컨테이너(Tomcat 등)가 구동되지 않고
 *       Web Environment 없이 최소 자원으로 동작함을 검증합니다.</li>
 *   <li><strong>Automatic Batch Scheduler Prevention (spring.batch.job.enabled: false)</strong>:
 *       애플리케이션 기동 시 등록된 배치 Job(예: {@code loanInterestAccrualJob})이 자동 구동되어
 *       시스템 자원을 점유하거나 H2 DB 데이터를 오염시키는 상황을 원천 차단합니다.
 *       배치 구동은 CLI 파라미터나 전용 트리거로만 명시적 구동되도록 제어됩니다.</li>
 *   <li><strong>Batch Meta-schema & In-memory H2 Isolation</strong>:
 *       H2 인메모리 환경에서 Spring Batch 메타데이터 테이블 생성을 위한
 *       {@code initialize-schema: always} 구성을 검증합니다.</li>
 *   <li><strong>External Infrastructure Decoupling</strong>:
 *       Config Server, Eureka, Vault 등 마이크로서비스 제어 서버 옵션을 비활성화하여
 *       로컬 환경에서 단독 오프라인 구동이 가능하도록 디커플링되었음을 검증합니다.</li>
 * </ul>
 */
@ActiveProfiles("local")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class LoanBatchLocalProfileTest {

    @Autowired
    private ConfigurableApplicationContext context;

    @Autowired
    private Environment environment;

    @Autowired
    private JobRegistry jobRegistry;

    @Autowired
    private JobExplorer jobExplorer;

    @Test
    @DisplayName("local 프로파일 구동 시 H2 메모리 DB, web-environment=none, job.enabled=false 설정이 적용되어 비동기 자동 실행 없이 컨텍스트가 로드된다")
    void startsFromTheRealLocalH2ProfileWithoutLaunchingABusinessJob() throws Exception {
        // 1. ApplicationContext 활성화 상태 및 local 프로파일 적용 검증
        assertThat(context.isActive()).isTrue();
        assertThat(environment.acceptsProfiles(Profiles.of("local"))).isTrue();

        // 2. Web Application Type NONE 및 Batch Job 자동 구동 차단 설정 검증
        assertThat(environment.getProperty("spring.main.web-application-type")).isEqualTo("none");
        assertThat(environment.getProperty("spring.batch.job.enabled", Boolean.class)).isFalse();
        assertThat(environment.getProperty("spring.batch.jdbc.initialize-schema")).isEqualTo("always");

        // 3. H2 데이터소스 isolated 인메모리 설정 검증
        assertThat(environment.getProperty("spring.datasource.url"))
                .startsWith("jdbc:h2:mem:loan_batch_db")
                .contains("MODE=PostgreSQL");
        assertThat(environment.getProperty("spring.datasource.driver-class-name"))
                .isEqualTo("org.h2.Driver");
        assertThat(environment.getProperty("spring.jpa.hibernate.ddl-auto"))
                .isEqualTo("create-drop");

        // 4. 외부 마이크로서비스 제어 서버(Config, Discovery, Vault, Eureka) 디커플링 검증
        assertThat(environment.getProperty("spring.cloud.config.enabled", Boolean.class)).isFalse();
        assertThat(environment.getProperty("spring.cloud.discovery.enabled", Boolean.class)).isFalse();
        assertThat(environment.getProperty("spring.cloud.vault.enabled", Boolean.class)).isFalse();
        assertThat(environment.getProperty("eureka.client.enabled", Boolean.class)).isFalse();

        // 5. JobLauncherApplicationRunner 비활성화 상태 및 배치 Job 등록 확인
        assertThat(context.getBeansOfType(JobLauncherApplicationRunner.class)).isEmpty();
        assertThat(jobRegistry.getJobNames()).containsExactlyInAnyOrder("loanInterestAccrualJob", "loanScheduledRepaymentJob");
        assertThat(jobExplorer.getJobInstanceCount("loanInterestAccrualJob")).isZero();
    }
}
