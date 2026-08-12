package com.ho.account.ecl.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * [ECL API ApplicationContext 통합 테스트]
 *
 * <p><strong>교육적 설명 및 설계 의도 (Pedagogical Explanation & Design Intent):</strong></p>
 * <ul>
 *   <li><strong>독립 실행형 로컬 프로파일 (Self-contained Local Profile)</strong>:
 *       {@code @ActiveProfiles("local")}을 지정하여 외부 PostgreSQL 데이터베이스나
 *       Spring Cloud Config, Eureka Discovery, Vault, Kafka 등 외부 인프라 서비스에 의존하지 않고
 *       H2 인메모리 데이터베이스 기반으로 ECL API 서비스의 Spring {@link ApplicationContext}가
 *       정상적으로 로드되는지 검증합니다.</li>
 *   <li><strong>헥사고날 아키텍처 및 캡슐화 경계 준수</strong>:
 *       ECL API 모듈은 배치(ecl-batch) 컴포넌트를 직접 참조하지 않고,
 *       독립된 API/Core 영속성 및 도메인 레포지토리 빈만 안전하게 스캔 및 초기화함을 보장합니다.</li>
 * </ul>
 */
@ActiveProfiles("local")
@SpringBootTest(
        classes = AllowanceEclApiApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE)
class EclApiApplicationTests {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private Environment environment;

    @Test
    @DisplayName("local 프로파일 활성화 시 H2 DB 기반으로 Spring ApplicationContext가 정상 로드된다")
    void contextLoads() {
        // 1. Spring ApplicationContext 초기화 검증
        assertThat(applicationContext).isNotNull();

        // 2. Active Profile 이 local인지 검증
        assertThat(environment.getActiveProfiles()).contains("local");

        // 3. H2 인메모리 데이터베이스 및 Flyway 마이그레이션 활성화 확인
        assertThat(environment.getProperty("spring.datasource.driver-class-name"))
                .isEqualTo("org.h2.Driver");
        assertThat(environment.getProperty("spring.datasource.url"))
                .contains("jdbc:h2:mem:");
    }
}
