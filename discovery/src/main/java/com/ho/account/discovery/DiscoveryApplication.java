package com.ho.account.discovery;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

/**
 * 서비스 등록, heartbeat, 조회를 제공하는 Eureka Server 실행 진입점입니다.
 *
 * <p>초보자 설명: 이 클래스가 짧은 것은 업무 코드가 빠진 스켈레톤이어서가 아닙니다. Discovery의 핵심
 * 알고리즘은 검증된 Eureka Server 라이브러리가 담당하고, 이 모듈은 서버 역할 선언과 포트·readiness·
 * self-preservation·보안/HA 운영 정책을 설정과 테스트로 소유합니다. 회계 도메인 규칙은 넣지 않습니다.</p>
 */
@SpringBootApplication
@EnableEurekaServer
public class DiscoveryApplication {

    public static void main(String[] args) {
        SpringApplication.run(DiscoveryApplication.class, args);
    }
}
