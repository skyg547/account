package com.ho.account.configserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.config.server.EnableConfigServer;

/**
 * 중앙 설정 서버 (Config Server)
 * 모든 MSA 마이크로서비스들의 application.yml 파일들을 한 곳(config-repo)에서 중앙 집중식으로 관리하고 나누어줍니다.
 */
@SpringBootApplication
@EnableConfigServer // 이 어노테이션 하나가 프랜차이즈 본사(설정 서버) 역할을 부여합니다!
public class ConfigServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(ConfigServerApplication.class, args);
    }
}
