package com.ho.account.mart.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * 🧪 [ApplicationContext 로드 검증 테스트]
 * 
 * 💡 [설계 이유 및 초보자 가이드]
 * - 본 테스트 클래스는 account-mart:mart-api 모듈의 Spring Boot ApplicationContext가
 *   로컬(local) 환경 프로파일에서 정상적으로 로드되는지 검증합니다.
 * - @SpringBootTest: 전체 스프링 애플리케이션 컨텍스트를 로드하여 모든 빈(Bean)의 의존성 주입 및 설정이
 *   올바르게 이루어졌는지 통합 검증합니다.
 * - @ActiveProfiles("local"): test/resources/application-local.yml 내의 H2 인메모리 DB
 *   및 로컬 전용 인프라 설정을 활성화하여 외부 의존성(PostgreSQL, Eureka, Vault 등) 없이
 *   독립적으로 테스트를 수행하도록 격리합니다.
 */
@SpringBootTest(classes = AllowanceMartApiApplication.class)
@ActiveProfiles("local")
class AccountMartApiApplicationTests {

    /**
     * 애플리케이션 컨텍스트 로딩 테스트
     * 
     * Context가 정상적으로 로드되면 이 테스트는 성공합니다.
     * 빈 생성 실패, DB 미설정, 프로파일 미적용 등의 문제가 발생할 경우 로딩 단계에서 예외가 발생하여 실패합니다.
     */
    @Test
    @DisplayName("local 프로파일 환경에서 AllowanceMartApiApplication의 ApplicationContext가 정상적으로 로드된다")
    void contextLoads() {
        // ApplicationContext 로드 자체를 검증하므로 본문은 비어 있습니다.
    }
}
