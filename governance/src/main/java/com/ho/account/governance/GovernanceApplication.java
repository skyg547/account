package com.ho.account.governance;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * 거버넌스(Governance) 서비스 - 내부회계 관리, 권한, 감사, 승인 프로세스를 담당하는 핵심 모듈입니다.
 *
 * <p>초보자 설명: 실행 클래스의 패키지와 실제 감사 기능 패키지가 서로 다르기 때문에,
 * 스캔 범위를 명시하지 않으면 서버만 뜨고 Controller/Service가 없는 빈 애플리케이션이 됩니다.
 * governance가 직접 사용하는 audit 계층과 master-data 내부 적용 어댑터만 좁게 등록합니다.</p>
 */
@SpringBootApplication(scanBasePackages = {
        "com.ho.account.governance",
        "com.ho.account.audit",
        "com.ho.account.masterdata.core.application.service",
        "com.ho.account.masterdata.core.infrastructure.adapter",
        "com.ho.account.masterdata.core.infrastructure.persistence"
})
@EntityScan(basePackages = {
        "com.ho.account.audit.domain",
        "com.ho.account.masterdata.core.domain"
})
@EnableJpaRepositories(basePackages = {
        "com.ho.account.audit.repository",
        "com.ho.account.masterdata.core.infrastructure.persistence"
})
@EnableDiscoveryClient
public class GovernanceApplication {

    public static void main(String[] args) {
        SpringApplication.run(GovernanceApplication.class, args);
    }
}