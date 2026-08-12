package com.ho.account.ecl.batch;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * 🧪 [ApplicationContext 로드 검증 테스트]
 * 
 * 💡 [설계 이유 및 초보자 가이드]
 * - 본 테스트 클래스는 ecl-batch 모듈의 Spring Boot ApplicationContext가
 *   로컬(local) 환경 프로파일에서 정상적으로 로드되는지 검증합니다.
 * - @SpringBootTest: 전체 스프링 애플리케이션 컨텍스트를 로드하여 모든 빈(Bean)의 의존성 주입이
 *   올바르게 설정되었는지 확인합니다.
 * - @ActiveProfiles("local"): test/resources/application-local.yml 내의 H2 메모리 DB
 *   및 로컬 전용 인프라 설정을 활성화하여 외부 의존성(PostgreSQL, Eureka, Cloud Config 등) 없이
 *   독립적으로 실행 가능하도록 보장합니다.
 */
@SpringBootTest(classes = AllowanceEclBatchApplication.class)
@ActiveProfiles("local")
class EclBatchApplicationTests {

    /**
     * 애플리케이션 컨텍스트 로딩 테스트
     * 
     * Context가 정상적으로 로드되면 이 테스트는 성공합니다.
     * 의존성 주입 실패, DB 접속 실패, 잘못된 프로파일 설정 등이 있을 경우 로딩 중 예외가 발생합니다.
     */
    @Test
    @DisplayName("local 프로파일 환경에서 AllowanceEclBatchApplication의 ApplicationContext가 정상적으로 로드된다")
    void contextLoads() {
        // ApplicationContext 로드 자체를 검증하므로 본문은 비어 있습니다.
    }
}
