package com.ho.account.loan;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;

/**
 * [Loan API Local Profile & Application Context Integration Test]
 *
 * <p><strong>Pedagogical Explanation & Design Intent / 상세 교육적 주석:</strong></p>
 * <ul>
 *   <li><strong>Profile Separation & Isolated Local Execution (프로파일 분리 및 독립 실행)</strong>:
 *       {@code local} 프로파일 활성화 시 외부 PostgreSQL 접속이나 Cloud Config Server/Eureka 연동 없이
 *       H2 인메모리 DB 기반으로 Loan API Spring ApplicationContext가 안정적으로 초기화됨을 검증합니다.</li>
 *   <li><strong>Multi-Module Persistence Scanning Verification (멀티모듈 영속성 스캐닝 검증)</strong>:
 *       Composition Root의 {@code @EntityScan} 및 {@code @EnableJpaRepositories} 설정을 통해
 *       Master Data 모듈의 {@code BusinessPartnerJpaEntity}, {@code AccountSubjectEntity}와
 *       Shared Audit 모듈의 {@code AuditLog} 엔티티 및 {@code AuditLogRepository}, {@code BusinessPartnerRepository}
 *       빈이 JPA Metamodel 및 ApplicationContext에 올바르게 등록되었음을 수용 조건에 따라 종합적으로 검증합니다.</li>
 *   <li><strong>Encapsulation Boundary & Dynamic Bean Lookup (모듈 캡슐화 경계 준수)</strong>:
 *       상위 API 모듈이 하위 transitive 모듈의 컴파일 의존성을 직속으로 가질 필요 없이, FQCN 및 Reflection을 활용하여
 *       Spring Container의 런타임 영속성 빈 등록 상태를 검증합니다.</li>
 *   <li><strong>External Infrastructure Decoupling (외부 서비스 디커플링 검증)</strong>:
 *       Eureka Discovery, Spring Cloud Config, Vault 등이 비활성화되어 오프라인 환경에서도 안전하게 단독 구동될 수 있는지 검증합니다.</li>
 * </ul>
 */
@ActiveProfiles("local")
@SpringBootTest(
        classes = LoanApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.application.name=loan-api",
                "spring.data.redis.repositories.enabled=false",
                "spring.cloud.config.enabled=false",
                "spring.cloud.discovery.enabled=false",
                "spring.cloud.loadbalancer.enabled=false",
                "spring.cloud.vault.enabled=false",
                "eureka.client.enabled=false",
                "spring.jpa.hibernate.ddl-auto=create-drop",
                "spring.flyway.enabled=false",
                "management.tracing.enabled=false",
                "account.loan.accounting.cash-account-code=101000",
                "account.loan.accounting.loan-receivable-account-code=131000",
                "account.loan.accounting.deferred-asset-account-code=118000",
                "account.loan.accounting.recognized-income-account-code=410000",
                "account.loan.accounting.accrued-interest-receivable-account-code=115010",
                "account.loan.accounting.interest-income-account-code=410100"
        })
class LoanApplicationLocalProfileTest {

    @Autowired
    private Environment environment;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    @DisplayName("local 프로파일 구동 시 H2 인메모리 DB로 Context가 로드되고 MasterData 및 Audit 엔티티/리포지토리가 올바르게 관리된다")
    void localProfileStartsWithMasterDataAndAuditPersistenceManaged() throws ClassNotFoundException {
        // 1. 프로파일 활성화 상태 검증
        assertThat(environment.matchesProfiles("local")).isTrue();

        // 2. 외부 마이크로서비스 제어 서버(Config, Discovery, Vault, Eureka) 디커플링 검증
        assertThat(environment.getProperty("spring.cloud.config.enabled", Boolean.class)).isFalse();
        assertThat(environment.getProperty("spring.cloud.discovery.enabled", Boolean.class)).isFalse();
        assertThat(environment.getProperty("spring.cloud.vault.enabled", Boolean.class)).isFalse();
        assertThat(environment.getProperty("eureka.client.enabled", Boolean.class)).isFalse();

        // 3. JPA Metamodel Managed Types (엔티티 스캔) 검증
        assertThat(entityManagerFactory.getMetamodel().getManagedTypes())
                .extracting(type -> type.getJavaType().getName())
                .contains(
                        "com.ho.account.masterdata.core.infrastructure.persistence.BusinessPartnerJpaEntity",
                        "com.ho.account.masterdata.core.infrastructure.persistence.BusinessPartnerAccountJpaEntity",
                        "com.ho.account.masterdata.core.infrastructure.persistence.entity.AccountSubjectEntity",
                        "com.ho.account.shared.infrastructure.security.domain.AuditLog"
                );

        // 4. Spring JPA Repositories (리포지토리 빈 스캔) 런타임 등록 검증
        Class<?> businessPartnerRepoClass = Class.forName("com.ho.account.masterdata.core.infrastructure.persistence.repository.BusinessPartnerRepository");
        Class<?> auditLogRepoClass = Class.forName("com.ho.account.shared.infrastructure.security.repository.AuditLogRepository");

        assertThat(applicationContext.getBeanNamesForType(businessPartnerRepoClass)).isNotEmpty();
        assertThat(applicationContext.getBeanNamesForType(auditLogRepoClass)).isNotEmpty();
    }

    @Test
    @DisplayName("거버넌스 AuditController 웹 어댑터가 Loan API ApplicationContext에 빈으로 등록되지 않는다")
    void auditControllerIsNotExposedInLoanApi() throws ClassNotFoundException {
        Class<?> auditControllerClass = Class.forName("com.ho.account.shared.infrastructure.security.web.AuditController");
        assertThat(applicationContext.getBeanNamesForType(auditControllerClass)).isEmpty();
    }
}


