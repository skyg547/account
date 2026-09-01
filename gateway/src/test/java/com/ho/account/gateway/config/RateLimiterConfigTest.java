package com.ho.account.gateway.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
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

    private static final String SHARED_SECRET = "test-bff-gateway-secret-that-is-long-enough";
    private static final Instant NOW = Instant.parse("2030-01-02T03:04:05Z");

    private RateLimiterConfig rateLimiterConfig;
    private KeyResolver requestRateKeyResolver;
    private RateLimiter<RateLimiterConfig.Config> rateLimiter;

    @BeforeEach
    void setUp() {
        rateLimiterConfig = new RateLimiterConfig(
                SHARED_SECRET,
                Clock.fixed(NOW, ZoneOffset.UTC));
        requestRateKeyResolver = rateLimiterConfig.requestRateKeyResolver();
        rateLimiter = rateLimiterConfig.inMemoryRateLimiter();
    }

    @Test
    @DisplayName("JWT 필터가 검증한 내부 attribute만 사용자 rate key로 신뢰한다")
    void resolveKeyFromVerifiedPrincipalAttribute() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/test")
                .header("X-Auth-User", "attacker-controlled")
                .header("X-Forwarded-For", "203.0.113.195")
                .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);
        exchange.getAttributes().put(
                RateLimiterConfig.AUTHENTICATED_PRINCIPAL_ATTRIBUTE,
                "verified-user");

        StepVerifier.create(requestRateKeyResolver.resolve(exchange))
                .expectNext("principal:" + sha256Hex("verified-user"))
                .verifyComplete();
    }

    @Test
    @DisplayName("서명된 BFF 로그인 키는 직접 peer가 같아도 사용자별 불투명 bucket을 만든다")
    void resolveDistinctVerifiedBffLoginKeys() {
        String firstRateKey = sha256Hex("first-user");
        String secondRateKey = sha256Hex("second-user");
        ServerWebExchange first = signedLoginExchange(firstRateKey, NOW.getEpochSecond());
        ServerWebExchange second = signedLoginExchange(secondRateKey, NOW.getEpochSecond());
        String peerHash = sha256Hex("198.51.100.1");

        StepVerifier.create(requestRateKeyResolver.resolve(first))
                .expectNext("bff-login:" + firstRateKey + ":peer:" + peerHash)
                .verifyComplete();
        StepVerifier.create(requestRateKeyResolver.resolve(second))
                .expectNext("bff-login:" + secondRateKey + ":peer:" + peerHash)
                .verifyComplete();
    }

    @Test
    @DisplayName("위조된 BFF와 X-Forwarded-For 헤더는 무시하고 직접 peer를 사용한다")
    void spoofedHeadersFallBackToDirectPeer() {
        MockServerHttpRequest request = MockServerHttpRequest.post("/api/auth/login")
                .remoteAddress(new InetSocketAddress("198.51.100.1", 8080))
                .header("X-Forwarded-For", "203.0.113.195")
                .header(RateLimiterConfig.BFF_RATE_KEY_HEADER, sha256Hex("victim"))
                .header(RateLimiterConfig.BFF_RATE_TIMESTAMP_HEADER, Long.toString(NOW.getEpochSecond()))
                .header(RateLimiterConfig.BFF_RATE_SIGNATURE_HEADER, "0".repeat(64))
                .build();

        StepVerifier.create(requestRateKeyResolver.resolve(MockServerWebExchange.from(request)))
                .expectNext("peer:198.51.100.1")
                .verifyComplete();
    }

    @Test
    @DisplayName("만료된 서명은 직접 peer로 안전하게 대체한다")
    void expiredBffSignatureFallsBackToDirectPeer() {
        String rateKey = sha256Hex("expired-user");
        ServerWebExchange exchange = signedLoginExchange(
                rateKey,
                NOW.minusSeconds(31).getEpochSecond());

        StepVerifier.create(requestRateKeyResolver.resolve(exchange))
                .expectNext("peer:198.51.100.1")
                .verifyComplete();
    }

    @Test
    @DisplayName("BFF 헤더가 로그인 경로 밖에 재사용되면 직접 peer로 대체한다")
    void signedBffHeadersCannotBeReplayedOnAnotherPath() {
        String rateKey = sha256Hex("path-user");
        long timestamp = NOW.getEpochSecond();
        MockServerHttpRequest request = MockServerHttpRequest.post("/api/basic/accounts")
                .remoteAddress(new InetSocketAddress("198.51.100.1", 8080))
                .header(RateLimiterConfig.BFF_RATE_KEY_HEADER, rateKey)
                .header(RateLimiterConfig.BFF_RATE_TIMESTAMP_HEADER, Long.toString(timestamp))
                .header(
                        RateLimiterConfig.BFF_RATE_SIGNATURE_HEADER,
                        loginSignature(rateKey, timestamp))
                .build();

        StepVerifier.create(requestRateKeyResolver.resolve(MockServerWebExchange.from(request)))
                .expectNext("peer:198.51.100.1")
                .verifyComplete();
    }

    @Test
    @DisplayName("잘못된 길이의 공유 secret은 시작 시 거부한다")
    void rejectInvalidSharedSecretLength() {
        assertThatThrownBy(() -> new RateLimiterConfig("too-short", Clock.systemUTC()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("32");
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

    @Test
    @DisplayName("사용자명을 회전해도 같은 BFF peer의 aggregate burst 제한을 우회하지 못한다")
    void distinctLoginKeysShareABoundedPeerAggregate() {
        String peerHash = sha256Hex("198.51.100.1");
        int rejectedRequests = 0;
        for (int index = 0; index < 500; index++) {
            String resolvedKey = "bff-login:" + sha256Hex("rotating-user-" + index)
                    + ":peer:" + peerHash;
            RateLimiter.Response response = rateLimiter.isAllowed("default", resolvedKey).block();
            assertThat(response).isNotNull();
            if (!response.isAllowed()) {
                rejectedRequests++;
            }
        }

        assertThat(rejectedRequests).isPositive();
    }

    @Test
    @DisplayName("고유 key가 계속 유입되어도 in-memory bucket 수는 설정 상한을 넘지 않는다")
    void bucketStoreRemainsBounded() {
        RateLimiterConfig.InMemoryRateLimiter boundedRateLimiter =
                new RateLimiterConfig.InMemoryRateLimiter(4);

        for (int index = 0; index < 20; index++) {
            boundedRateLimiter.isAllowed("default", "principal:" + index).block();
        }

        assertThat(boundedRateLimiter.bucketCountForTesting()).isEqualTo(4);
    }

    private ServerWebExchange signedLoginExchange(String rateKey, long timestamp) {
        MockServerHttpRequest request = MockServerHttpRequest.post("/api/auth/login")
                .remoteAddress(new InetSocketAddress("198.51.100.1", 8080))
                .header(RateLimiterConfig.BFF_RATE_KEY_HEADER, rateKey)
                .header(RateLimiterConfig.BFF_RATE_TIMESTAMP_HEADER, Long.toString(timestamp))
                .header(
                        RateLimiterConfig.BFF_RATE_SIGNATURE_HEADER,
                        loginSignature(rateKey, timestamp))
                .build();
        return MockServerWebExchange.from(request);
    }

    private String loginSignature(String rateKey, long timestamp) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(SHARED_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            String canonical = "POST\n/api/auth/login\n" + timestamp + "\n" + rateKey;
            return HexFormat.of().formatHex(mac.doFinal(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new AssertionError(exception);
        }
    }

    private static String sha256Hex(String value) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256")
                            .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new AssertionError(exception);
        }
    }
}
