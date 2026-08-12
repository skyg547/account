package com.ho.account.gateway.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.InetSocketAddress;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RateLimiter;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.test.StepVerifier;

class RateLimiterConfigTest {

    private RateLimiterConfig rateLimiterConfig;
    private KeyResolver ipKeyResolver;
    private RateLimiter<RateLimiterConfig.Config> rateLimiter;

    @BeforeEach
    void setUp() {
        rateLimiterConfig = new RateLimiterConfig();
        ipKeyResolver = rateLimiterConfig.ipKeyResolver();
        rateLimiter = rateLimiterConfig.inMemoryRateLimiter();
    }

    @Test
    @DisplayName("X-Forwarded-For 헤더가 존재하는 경우 첫 번째 프록시 클라이언트 IP를 추출한다")
    void resolveKeyFromXForwardedForHeader() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/test")
                .header("X-Forwarded-For", "203.0.113.195, 70.41.3.18, 150.172.238.178")
                .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(ipKeyResolver.resolve(exchange))
                .expectNext("203.0.113.195")
                .verifyComplete();
    }

    @Test
    @DisplayName("X-Forwarded-For 헤더가 없으면 RemoteAddress IP를 추출한다")
    void resolveKeyFromRemoteAddress() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/test")
                .remoteAddress(new InetSocketAddress("198.51.100.1", 8080))
                .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(ipKeyResolver.resolve(exchange))
                .expectNext("198.51.100.1")
                .verifyComplete();
    }

    @Test
    @DisplayName("In-Memory RateLimiter는 허용 및 토큰 소모 트래픽 제어를 정상적으로 수행한다")
    void inMemoryRateLimiterAllowsAndLimitsRequests() {
        String routeId = "default";
        String clientIp = "192.168.1.50";

        // 최초 20개 소모는 burstCapacity 이내이므로 모두 허용되어야 함
        for (int i = 0; i < 20; i++) {
            StepVerifier.create(rateLimiter.isAllowed(routeId, clientIp))
                    .assertNext(response -> assertThat(response.isAllowed()).isTrue())
                    .verifyComplete();
        }

        // 21번째 연속 소모 시 토큰 소진으로 허용 거부(allowed = false)
        StepVerifier.create(rateLimiter.isAllowed(routeId, clientIp))
                .assertNext(response -> assertThat(response.isAllowed()).isFalse())
                .verifyComplete();
    }
}
