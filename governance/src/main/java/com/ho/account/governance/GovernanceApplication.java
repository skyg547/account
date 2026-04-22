package com.ho.account.governance;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 거버넌스(Governance) 서비스 - 내부회계 관리, 권한, 감사, 승인 프로세스를 담당하는 핵심 모듈입니다.
 */
@SpringBootApplication
@EnableDiscoveryClient
public class GovernanceApplication {
    public static void main(String[] args) {
        SpringApplication.run(GovernanceApplication.class, args);
    }
}
