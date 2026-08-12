package com.ho.account.loan;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * [대출 관리 마이크로서비스 API 실행 애플리케이션 (Loan Microservice Composition Root)]
 *
 * <p><strong>Architecture & Pedagogical Design (아키텍처 및 상세 교육적 주석):</strong></p>
 * <ul>
 *   <li><strong>Explicit Multi-Module JPA Package Scanning (명시적 멀티모듈 JPA 패키지 스캐닝)</strong>:
 *       마이크로서비스 진입점(Composition Root)에서 {@code com.ho.account} 전체 패키지를 모호하게 스캔(Broad Scan)하는 대신,
 *       대출(Loan) 마이크로서비스가 의존하는 원장(Journal Ledger), 기준정보(Master Data Core), 공통 보안/감사(Shared Security Audit)
 *       모듈의 JPA 엔티티 및 데이터 리포지토리 패키지를 명시적으로 구성합니다.</li>
 *   <li><strong>Entity & Repository Management (엔티티 및 리포지토리 관리 정합성)</strong>:
 *       <ul>
 *         <li>{@link EntityScan}: 기준정보 persistence 엔티티 패키지({@code com.ho.account.masterdata.core.infrastructure.persistence.entity} 및 {@code com.ho.account.masterdata.core.infrastructure.persistence})와
 *             공통 감사 엔티티 패키지({@code com.ho.account.shared.infrastructure.security.domain})를 명시 등록하여 {@code BusinessPartnerJpaEntity} 등의 {@code Not a managed type} 오류를 원천 방지합니다.</li>
 *         <li>{@link EnableJpaRepositories}: 기준정보 persistence repository 패키지({@code com.ho.account.masterdata.core.infrastructure.persistence.repository})와
 *             공통 감사 repository 패키지({@code com.ho.account.shared.infrastructure.security.repository})를 포함시켜 {@code AuditLogRepository} 등 핵심 JPA 빈의 등록 실패를 해결합니다.</li>
 *       </ul>
 *   </li>
 *   <li><strong>Isolated Local Execution & Decoupling (프로파일 기반 H2 런타임 격리)</strong>:
 *       외부 PostgreSQL DB나 Spring Cloud 인프라(Config Server, Eureka, Vault 등)에 의존하지 않고,
 *       로컬 개발 환경({@code local} 프로파일)에서 인메모리 H2 DB로 독립 실행 가능하도록 설계되었습니다.</li>
 * </ul>
 */
@SpringBootApplication(scanBasePackages = {
        "com.ho.account.loan",
        "com.ho.account.journalledger",
        "com.ho.account.common",
        "com.ho.account.shared",
        "com.ho.account.masterdata.core"
})
@EnableDiscoveryClient
@EntityScan(basePackages = {
        "com.ho.account.loan.domain",
        "com.ho.account.journalledger.domain",
        "com.ho.account.masterdata.core.domain",
        "com.ho.account.masterdata.core.infrastructure.persistence",
        "com.ho.account.masterdata.core.infrastructure.persistence.entity",
        "com.ho.account.shared.infrastructure.security.domain"
})
@EnableJpaRepositories(basePackages = {
        "com.ho.account.loan.infrastructure.persistence",
        "com.ho.account.journalledger.domain",
        "com.ho.account.journalledger.adapter.out.persistence",
        "com.ho.account.masterdata.core.infrastructure.persistence",
        "com.ho.account.shared.infrastructure.security.repository"
})
public class LoanApplication {

    public static void main(String[] args) {
        SpringApplication.run(LoanApplication.class, args);
    }
}

