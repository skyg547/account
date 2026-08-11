package com.ho.account.reporting.infrastructure.config;

import com.ho.account.reporting.core.domain.service.FinancialStatementEngine;
import com.ho.account.reporting.core.domain.service.IfrsDisclosureNotesEngine;
import com.ho.account.reporting.core.domain.service.RwaCalculator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * [도메인 서비스 스프링 빈 등록 설정 (Reporting Domain Configuration)]
 *
 * 💡 [설계 배경 및 헥사고날 아키텍처 (Hexagonal Architecture) 원칙]
 * 헥사고날 아키텍처(포트와 어댑터 패턴) 및 Domain-Driven Design(DDD)의 핵심 원칙 중 하나는
 * **"도메인 계층(Domain Layer)의 기술 및 프레임워크 독립성"**입니다.
 *
 * 📌 [왜 도메인 서비스에서 `@Component`, `@Service` 어노테이션을 전면 제거했는가?]
 * 1. **프레임워크 종속성 제거 (Pure POJO 유지)**:
 *    도메인 계층에 `@Component`나 `@Service` 같은 Spring 어노테이션을 붙이면,
 *    도메인 로직이 Spring 프레임워크에 직접적으로 결합(Coupling)됩니다.
 *    어노테이션을 제거하고 순수 Java 객체(POJO)로 만듦으로써 도메인 코드를 외부 프레임워크나 라이브러리 없이 독립적으로 유지할 수 있습니다.
 *
 * 2. **단위 테스트(Unit Test)의 단순화 및 고속화**:
 *    도메인 서비스가 Pure POJO이면 Spring Context를 띄우지 않고도(`@SpringBootTest` 없이)
 *    일반 자바 객체 생성(`new FinancialStatementEngine()`)만으로 매우 빠른 단위 테스트 작성이 가능합니다.
 *
 * 3. **수동 명시적 Bean 등록(Explicit Bean Configuration)의 이점**:
 *    인프라스트럭처(Infrastructure) 계층인 본 `@Configuration` 클래스에서 도메인 서비스들을 수동으로 `@Bean` 등록합니다.
 *    이를 통해 Spring IOC 컨테이너의 의존성 주입(DI) 이점을 100% 누리면서도 도메인 코드의 순수성을 지킬 수 있습니다.
 *    의존성 방향: Infrastructure 계층 -> Domain 계층 (도메인 계층은 인프라 계층을 참조하지 않음).
 */
@Configuration
public class ReportingDomainConfiguration {

    /**
     * 재무상태표(B/S), 손익계산서(I/S), 현금흐름표(C/F) 집계 엔진 도메인 서비스 빈 등록
     */
    @Bean
    public FinancialStatementEngine financialStatementEngine() {
        return new FinancialStatementEngine();
    }

    /**
     * IFRS 주석(Disclosure Notes) 공시 집계 엔진 도메인 서비스 빈 등록
     */
    @Bean
    public IfrsDisclosureNotesEngine ifrsDisclosureNotesEngine() {
        return new IfrsDisclosureNotesEngine();
    }

    /**
     * 바젤 III 규제 위험가중자산(RWA) 산출 계산기 도메인 서비스 빈 등록
     */
    @Bean
    public RwaCalculator rwaCalculator() {
        return new RwaCalculator();
    }
}
