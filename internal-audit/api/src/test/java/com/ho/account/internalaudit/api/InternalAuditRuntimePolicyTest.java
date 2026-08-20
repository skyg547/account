package com.ho.account.internalaudit.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.Profiles;
import org.springframework.core.env.StandardEnvironment;

/**
 * ==============================================================================
 * Internal Audit Runtime Policy Test (내부 감사 모듈 런타임 정책 검증 테스트)
 * ==============================================================================
 * [Architecture & Pedagogical Explanation / 교육적 주석]
 *
 * 1. Profile-Based Isolation Runtime Verification (프로파일 기반 격리 런타임 검증)
 *    - 'local' 프로파일 환경에서는 외부 인프라(PostgreSQL, Config Server, Eureka, Vault 등)에
 *      의존하지 않고 단독으로 실행(Self-contained Local Runtime)될 수 있어야 합니다.
 *    - H2 인메모리 데이터베이스를 PostgreSQL 호환 모드('MODE=PostgreSQL')로 구동하고,
 *      Flyway V60 마이그레이션 스크립트를 통해 테이블 스키마를 동적으로 구성합니다.
 *    - Hibernate의 'ddl-auto=validate' 옵션으로 스키마-엔티티 간 정합성을 검증하여,
 *      로컬 개발 및 통합 테스트 시 외부 DB 준비 없이 안전하고 독립적인 검증 환경을 구축합니다.
 *
 * 2. Control Plane Decoupling & Offline Resiliency (런타임 제어 평면 비활성화 및 오프라인 자충우돌 구동)
 *    - Spring Cloud Config, Discovery (Eureka), Vault 등의 제어 평면 연동을 비활성화합니다.
 *    - 오프라인 단독 구동 환경에서 외부 커넥션 실패로 인한 애플리케이션 시작 지연 및
 *      예외 발생을 차단하여 신속한 개발 피드백 루프(Rapid Feedback Loop)를 제공합니다.
 *
 * 3. Execution Topology & Architectural Boundaries (실행 토폴로지 및 아키텍처 경계 보장)
 *    - internal-audit 모듈 내에서 실행 가능한 Spring Boot 모듈은 오직 'internal-audit:api'뿐이며,
 *      'internal-audit:core'는 pure domain/usecase/port를 소유하는 java-library 모듈이어야 합니다.
 *    - Docker Compose 및 빌드 스크립트 계약을 이 테스트를 통해 자동으로 검증함으로써
 *      잘못된 배치/API 구성이나 불필요한 의존성 누수를 사전에 차단합니다.
 * ==============================================================================
 */
class InternalAuditRuntimePolicyTest {

    /**
     * [Pedagogical Note / 교육적 주석]
     * local 프로파일 활성화 시:
     * - H2 In-Memory DB (PostgreSQL 호환 모드) 사용 여부
     * - Flyway V60 마이그레이션 적용 및 Hibernate validate 정책 작동 여부
     * - Cloud Config, Eureka Discovery, Vault 등 제어 평면 비활성화 여부
     * 를 철저하게 검증하여 독립 단독 구동(Self-contained local execution)을 보장합니다.
     */
    @Test
    @DisplayName("local 프로파일은 외부 제어 평면 없이 Flyway V60 기반 H2 인메모리 DB로 독립 구동된다")
    void localProfileUsesFlywayOwnedH2WithoutControlPlaneDependencies() {
        try (ConfigurableApplicationContext context = context("local")) {
            Environment environment = context.getEnvironment();

            // 1. 프로파일 활성화 검증
            assertThat(environment.acceptsProfiles(Profiles.of("local"))).isTrue();

            // 2. H2 PostgreSQL 호환 모드 및 드라이버 검증
            assertThat(environment.getProperty("spring.datasource.url"))
                    .startsWith("jdbc:h2:mem:")
                    .contains("MODE=PostgreSQL");
            assertThat(environment.getProperty("spring.datasource.driver-class-name"))
                    .isEqualTo("org.h2.Driver");

            // 3. Flyway V60 타깃 마이그레이션 및 JPA validate 정책 검증
            assertThat(environment.getProperty("spring.flyway.enabled", Boolean.class)).isTrue();
            assertThat(environment.getProperty("spring.flyway.locations"))
                    .isEqualTo("classpath:db/migration");
            assertThat(environment.getProperty("spring.flyway.target")).isEqualTo("60");
            assertThat(environment.getProperty("spring.jpa.hibernate.ddl-auto"))
                    .isEqualTo("validate");
            assertThat(environment.getProperty("spring.sql.init.mode")).isEqualTo("never");

            // 4. Spring Cloud 제어 평면(Config/Discovery/Vault) 비활성화 검증
            assertThat(environment.getProperty("spring.cloud.config.enabled", Boolean.class))
                    .isFalse();
            assertThat(environment.getProperty("spring.cloud.discovery.enabled", Boolean.class))
                    .isFalse();
            assertThat(environment.getProperty("spring.cloud.vault.enabled", Boolean.class))
                    .isFalse();

            // 5. Eureka 클라이언트 비활성화 검증
            assertThat(environment.getProperty("eureka.client.enabled", Boolean.class)).isFalse();
            assertThat(environment.getProperty("eureka.client.register-with-eureka", Boolean.class))
                    .isFalse();
            assertThat(environment.getProperty("eureka.client.fetch-registry", Boolean.class))
                    .isFalse();
        }
    }

    /**
     * [Pedagogical Note / 교육적 주석]
     * dev 및 prod 프로파일은 외부 주석된 PostgreSQL DB를 직접 사용하며,
     * 애플리케이션 시작 시 스키마 변경(ddl-auto write)을 수행하지 않고 validate 검증만 수행함을 확인합니다.
     */
    @Test
    @DisplayName("dev 및 prod 프로파일은 주입된 PostgreSQL을 사용하며 스키마 임의 변경을 금지한다")
    void developmentAndProductionUseInjectedPostgresqlWithoutRuntimeSchemaWrites() {
        for (String profile : new String[] {"dev", "prod"}) {
            try (ConfigurableApplicationContext context = context(profile)) {
                Environment environment = context.getEnvironment();
                assertThat(environment.getProperty("spring.datasource.url"))
                        .startsWith("jdbc:postgresql://");
                assertThat(environment.getProperty("spring.datasource.username"))
                        .isEqualTo(profile + "_internal_audit_app");
                assertThat(environment.getProperty("spring.datasource.password"))
                        .isEqualTo("injected-test-password");
                assertThat(environment.getProperty("spring.flyway.enabled", Boolean.class))
                        .isFalse();
                assertThat(environment.getProperty("spring.jpa.hibernate.ddl-auto"))
                        .isEqualTo("validate");
                assertThat(environment.getProperty("spring.sql.init.mode")).isEqualTo("never");
            }
        }
    }

    /**
     * [Pedagogical Note / 교육적 주석]
     * internal-audit의 실행 구조(Topology) 경계 검증:
     * - internal-audit:api만 실행 가능(bootJar)한 모듈이어야 함
     * - internal-audit:core는 pure java-library로 경계 분리 보장
     * - docker-compose가 표준 컨테이너 계약을 이행하고 있는지 확인
     */
    @Test
    @DisplayName("api 모듈만 유일한 실행 가능 어플리케이션이며 Compose 계약을 준수한다")
    void onlyApiIsExecutableAndComposeUsesTheCanonicalContainerContract() throws Exception {
        Path root = repositoryRoot();
        String settings = Files.readString(root.resolve("settings.gradle"));
        String coreBuild = Files.readString(root.resolve("internal-audit/core/build.gradle"));
        String compose = Files.readString(root.resolve("internal-audit/docker-compose.yml"));
        Path coreSource = root.resolve("internal-audit/core/src/main/java/com/ho/account");

        assertThat(settings)
                .contains("'internal-audit:core', 'internal-audit:api'")
                .doesNotContain("internal-audit:batch", "auth:batch");
        assertThat(coreBuild)
                .contains("id 'java-library'")
                .doesNotContain("id 'org.springframework.boot'", "spring-boot-starter-web");
        assertThat(coreSource.resolve("internalaudit/InternalAuditApplication.java")).doesNotExist();
        assertThat(javaSources(coreSource.resolve("internalaudit/core/infrastructure/api"))).isEmpty();
        assertThat(javaSources(coreSource.resolve("audit"))).isEmpty();
        assertThat(javaSources(coreSource.resolve("security"))).isEmpty();
        assertThat(root.resolve(
                        "internal-audit/api/src/main/java/com/ho/account/internalaudit/api/adapter/in/web/RcmController.java"))
                .exists();
        assertThat(compose)
                .contains("dockerfile: internal-audit/api/Dockerfile")
                .contains("${INTERNAL_AUDIT_DB_URL:?set INTERNAL_AUDIT_DB_URL}")
                .contains("name: ${ACCOUNT_NETWORK_NAME:-account-dev-network}")
                .doesNotContain(
                        "internal-audit-batch",
                        "jdbc:h2:",
                        "GRADLE_PROJECT",
                        "JAR_DIRECTORY");
    }

    private ConfigurableApplicationContext context(String profile) {
        Map<String, Object> overrides = new LinkedHashMap<>();
        overrides.put("spring.profiles.active", profile);
        if (!"local".equals(profile)) {
            overrides.put("spring.cloud.config.enabled", false);
            overrides.put("spring.cloud.discovery.enabled", false);
            overrides.put("eureka.client.enabled", false);
        }
        overrides.put("DEV_DB_HOST", "dev-db.invalid");
        overrides.put("DEV_DB_NAME", "internal_audit_dev");
        overrides.put("DEV_DB_USER", "dev_internal_audit_app");
        overrides.put("DEV_DB_PASSWORD", "injected-test-password");
        overrides.put(
                "PROD_DB_URL",
                "jdbc:postgresql://prod-db.invalid:5432/internal_audit_prod?sslmode=verify-full");
        overrides.put("PROD_DB_USER", "prod_internal_audit_app");
        overrides.put("PROD_DB_PASSWORD", "injected-test-password");
        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource("testOverrides", overrides));

        return new SpringApplicationBuilder(Probe.class)
                .environment(environment)
                .web(WebApplicationType.NONE)
                .registerShutdownHook(false)
                .properties(
                        "spring.config.location="
                                + repositoryRoot()
                                        .resolve("internal-audit/api/src/main/resources")
                                        .toUri(),
                        "spring.main.banner-mode=off")
                .run();
    }

    private Path repositoryRoot() {
        Path candidate = Path.of("").toAbsolutePath();
        while (candidate != null && !Files.exists(candidate.resolve("settings.gradle"))) {
            candidate = candidate.getParent();
        }
        if (candidate == null) {
            throw new IllegalStateException("repository root was not found");
        }
        return candidate;
    }

    private List<Path> javaSources(Path root) throws Exception {
        if (!Files.isDirectory(root)) {
            return List.of();
        }
        try (var paths = Files.walk(root)) {
            return paths.filter(path -> path.toString().endsWith(".java")).toList();
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class Probe {}
}
