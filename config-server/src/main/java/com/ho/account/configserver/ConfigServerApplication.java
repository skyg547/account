package com.ho.account.configserver;

import com.ho.account.configserver.health.ConfigRepositoryProbeProperties;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.config.server.EnableConfigServer;

/**
 * 중앙 설정 서버 (Config Server)
 * 모든 MSA 마이크로서비스들의 application.yml 파일들을 한 곳(config-repo)에서 중앙 집중식으로 관리하고 나누어줍니다.
 *
 * <p>이 클래스가 작은 이유는 스켈레톤이라서가 아닙니다. 설정 파일 탐색, profile 병합, HTTP 응답 생성은
 * Spring Cloud Config가 검증된 인프라 어댑터로 수행합니다. 이 모듈은 저장소 위치, readiness, 보안/배포 정책과
 * 설정 조회 계약 테스트를 소유하며 회계 업무 규칙은 각 업무 모듈의 core에 남겨 둡니다.</p>
 */
@SpringBootApplication
@EnableConfigServer // 이 어노테이션 하나가 프랜차이즈 본사(설정 서버) 역할을 부여합니다!
@EnableConfigurationProperties(ConfigRepositoryProbeProperties.class)
public class ConfigServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConfigServerApplication.class, args);
    }
}