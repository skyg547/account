package com.ho.account.journalledger;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import com.ho.account.shared.infrastructure.ProductionPostgresqlTlsGuard;
import com.ho.account.shared.infrastructure.SpringServiceDiscoveryRegistry;

/**
 * [Journal Ledger Microservice Composition Root / 회계 장부 마이크로서비스 진입점]
 *
 * <p><strong>Architecture & Pedagogical Design (아키텍처 및 상세 교육적 주석):</strong></p>
 * <ul>
 *   <li><strong>Explicit Multi-Module JPA Package Scanning (명시적 멀티모듈 JPA 패키지 스캐닝)</strong>:
 *       회계 장부(Journal Ledger) 마이크로서비스 진입점(Composition Root)에서 {@code com.ho.account} 전체 패키지를 모호하게 스캔(Broad Scan)하는 대신,
 *       장부 모듈 고유의 도메인/영속성 패키지 및 공유 공통 컴포넌트 패키지를 명시적으로 지정하여 빈 중복 등록 및 충돌을 예방합니다.</li>
 *   <li><strong>Entity & Repository Management (엔티티 및 리포지토리 관리 정합성)</strong>:
 *       {@link EntityScan} 및 {@link EnableJpaRepositories}를 통해 회계 전표({@code JournalEntry}), 총계정원장({@code GlBalance}),
 *       보조원장({@code SlBalance}) 및 미결 항목({@code UnsettledItem}) 관련 JPA 엔티티와 리포지토리를 정확히 컨텍스트에 등록합니다.</li>
 *   <li><strong>Isolated Local Execution & Decoupling (프로파일 기반 H2 런타임 격리 및 Kafka 브로커 분리)</strong>:
 *       외부 PostgreSQL DB나 Spring Cloud 인프라(Config Server, Eureka, Vault 등)에 의존하지 않고,
 *       로컬 개발 환경({@code local} 프로파일)에서 인메모리 H2 DB 및 비활성화된 Kafka 이벤트 브로커 환경으로 독립 단독 실행(Self-contained Local Runtime)이 가능하도록 보장합니다.</li>
 * </ul>
 */
@SpringBootApplication(scanBasePackages = {
        "com.ho.account.journalledger",
        "com.ho.account.common"
})
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.ho.account.journalledger")
@EnableJpaRepositories(basePackages = {
        "com.ho.account.journalledger.domain",
        "com.ho.account.journalledger.adapter.out.persistence"
})
@EntityScan(basePackages = {
        "com.ho.account.journalledger.domain",
        "com.ho.account.journalledger.adapter.out.persistence"
})
@Import({SpringServiceDiscoveryRegistry.class, ProductionPostgresqlTlsGuard.class})
public class JournalLedgerApplication {

    public static void main(String[] args) {
        SpringApplication.run(JournalLedgerApplication.class, args);
    }
}
