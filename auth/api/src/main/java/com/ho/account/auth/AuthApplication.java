package com.ho.account.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 인증(Auth) 서비스 - "중앙 출입증 발급 센터"
 * 고객의 로그인 요청을 받아 확인하고 위조 불가능한 출입증(JWT 토큰)을 발급해주는 전담 서비스입니다.
 */
@SpringBootApplication
@EnableDiscoveryClient
public class AuthApplication {
    public static void main(String[] args) {
        SpringApplication.run(AuthApplication.class, args);
    }
}
