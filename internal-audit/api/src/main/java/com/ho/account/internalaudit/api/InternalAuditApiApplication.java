package com.ho.account.internalaudit.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * [Pedagogical Comment] Internal Audit API Application Entry Point.
 * 
 * why this design?
 * 내부통제 및 감사 로그(Audit Log) 조회를 담당하는 모듈의 Web API 진입점입니다.
 * core의 감사 엔티티 및 저장소 어댑터를 함께 스캔하도록 설정합니다.
 */
@SpringBootApplication(scanBasePackages = {"com.ho.account.internalaudit", "com.ho.account.audit"})
@EntityScan(basePackages = {"com.ho.account.internalaudit", "com.ho.account.audit"})
@EnableJpaRepositories(basePackages = {"com.ho.account.internalaudit", "com.ho.account.audit"})
public class InternalAuditApiApplication {
    public static void main(String[] args) {
        SpringApplication.run(InternalAuditApiApplication.class, args);
    }
}
