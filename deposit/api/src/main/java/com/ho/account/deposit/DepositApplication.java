package com.ho.account.deposit;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * [예금 마이크로서비스 HTTP API Composition Root]
 *
 * 🐣 [초보자를 위한 설명 및 아키텍처적 구조]
 * 본 클래스는 예금(Deposit) API 마이크로서비스의 Spring Boot 구동 진입점(Composition Root)입니다.
 * 헥사고날 아키텍처(Hexagonal Architecture) 원칙에 따라 웹 인바운드 어댑터(DepositController),
 * core 모듈의 애플리케이션 서비스(DepositService), 영속성 어댑터(DepositAccountPersistenceAdapter, DepositTransactionalOutboxAdapter),
 * 도메인 설정(DepositDomainConfiguration, DepositOutboxConfiguration) 및 JPA 엔티티/리포지토리를 조합하여 Spring ApplicationContext를 구성합니다.
 *
 * 1. @SpringBootApplication:
 *    - 'com.ho.account.deposit' 패키지와 하위 패키지에 존재하는 @Controller, @Service, @Component, @Configuration 빈을 스캔합니다.
 * 2. @EntityScan:
 *    - 'com.ho.account.deposit.domain' 패키지에 위치한 JPA 엔티티 객체들(DepositAccount, DepositOutboxEntity 등)을 검색하여 EntityManagerFactory에 등록합니다.
 * 3. @EnableJpaRepositories:
 *    - 'com.ho.account.deposit.infrastructure.adapter.out.persistence' 패키지의 Spring Data JPA 리포지토리 인터페이스를 탐색하여 빈을 생성합니다.
 * 4. @EnableScheduling:
 *    - DepositOutboxRelayScheduler의 백그라운드 폴링 릴레이 스케줄러를 활성화합니다.
 */
@SpringBootApplication(scanBasePackages = "com.ho.account.deposit")
@EntityScan(basePackages = "com.ho.account.deposit.domain")
@EnableJpaRepositories(basePackages = "com.ho.account.deposit.infrastructure.adapter.out.persistence")
@EnableDiscoveryClient
@EnableScheduling
public class DepositApplication {

    public static void main(String[] args) {
        SpringApplication.run(DepositApplication.class, args);
    }
}
