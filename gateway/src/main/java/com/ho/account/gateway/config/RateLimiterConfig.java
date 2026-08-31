package com.ho.account.gateway.config;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RateLimiter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpMethod;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * ================================================================================
 * [교육적 주석 - MSA API Gateway 트래픽 제어 및 Rate Limiter / KeyResolver 아키텍처]
 * ================================================================================
 * API Gateway는 Microservice Architecture(MSA)의 최전방 단일 진입점(Single Point of Entry)입니다.
 * 
 * 1. 검증된 요청 주체 기반 KeyResolver:
 *    - 보호 API는 JWT 필터가 ServerWebExchange 내부 attribute에 기록한 검증 주체를 사용합니다.
 *    - 공개 로그인은 BFF가 만든 불투명 사용자 해시와 짧은 유효시간 HMAC 서명을 검증합니다.
 *    - 그 밖의 요청은 직접 연결된 peer 주소를 사용하며, 임의 조작 가능한 X-Forwarded-For는 신뢰하지 않습니다.
 * 
 * 2. Rate Limiter (Token Bucket 알고리즘 기반 트래픽 제어):
 *    - 초당 요청 충전율(Replenish Rate)과 순간 최고 처리 가능량(Burst Capacity)을 기반으로
 *      백엔드 서버의 최대 처리 능력을 초과하는 순간적 트래픽 폭주(Spike)를 안정적으로 조율합니다.
 *    - Redis 연결이 제공되지 않거나 개발/단단 테스트 환경에서도 안정적으로 동작하는
 *      In-Memory Token Bucket RateLimiter를 함께 등록하여 게이트웨이 보호 기능을 완성합니다.
 *    - 로그인 사용자별 bucket과 BFF peer aggregate bucket을 함께 적용하고 bucket 저장소 크기를
 *      제한하여 사용자명 회전 우회와 무제한 heap 증가를 막습니다.
 * 
 * 3. Bounded Context 독립성 및 안정성 보장:
 *    - 특정 서비스로의 과도한 요청이나 서비스 장애 발생 시에도 Rate Limiter와 CircuitBreaker를 통해
 *      다른 Bounded Context 영역으로 장애가 연쇄 전파(Cascading Failure)되는 것을 차단합니다.
 * ================================================================================
 */
@Configuration
public class RateLimiterConfig {

    public static final String AUTHENTICATED_PRINCIPAL_ATTRIBUTE =
            RateLimiterConfig.class.getName() + ".authenticatedPrincipal";
    static final String BFF_RATE_KEY_HEADER = "X-Bff-Rate-Key";
    static final String BFF_RATE_TIMESTAMP_HEADER = "X-Bff-Rate-Timestamp";
    static final String BFF_RATE_SIGNATURE_HEADER = "X-Bff-Rate-Signature";

    private static final String LOGIN_PATH = "/api/auth/login";
    private static final long BFF_SIGNATURE_MAX_AGE_SECONDS = 30;
    private static final int MINIMUM_SHARED_SECRET_BYTES = 32;
    private static final int MAXIMUM_SHARED_SECRET_BYTES = 512;
    private static final int MAXIMUM_IN_MEMORY_BUCKETS = 10_000;
    private static final int BFF_PEER_AGGREGATE_MULTIPLIER = 10;
    private static final String BFF_LOGIN_KEY_PREFIX = "bff-login:";
    private static final String BFF_PEER_KEY_DELIMITER = ":peer:";
    private static final Pattern SHA_256_HEX_PATTERN = Pattern.compile("^[0-9a-f]{64}$");
    private static final Pattern TIMESTAMP_PATTERN = Pattern.compile("^[0-9]{1,12}$");
    private static final HexFormat HEX_FORMAT = HexFormat.of();

    private final byte[] bffSharedSecret;
    private final Clock clock;

    @Autowired
    public RateLimiterConfig(@Value("${gateway.bff.shared-secret:}") String bffSharedSecret) {
        this(bffSharedSecret, Clock.systemUTC());
    }

    RateLimiterConfig(String bffSharedSecret, Clock clock) {
        this.clock = clock;
        this.bffSharedSecret = validatedSharedSecret(bffSharedSecret);
    }

    /**
     * 검증된 사용자, 검증된 BFF 로그인 키, 직접 연결 peer 순서로 Rate Limiter 키를 선택합니다.
     *
     * @return Mono<String> 불투명한 rate-limit 식별 키
     */
    @Bean
    public KeyResolver requestRateKeyResolver() {
        return exchange -> Mono.just(resolveRequestKey(exchange));
    }

    private String resolveRequestKey(ServerWebExchange exchange) {
        Object authenticatedPrincipal = exchange.getAttribute(AUTHENTICATED_PRINCIPAL_ATTRIBUTE);
        if (authenticatedPrincipal instanceof String username && !username.isBlank()) {
            return "principal:" + sha256Hex(username);
        }

        Optional<String> bffLoginKey = resolveVerifiedBffLoginKey(exchange);
        if (bffLoginKey.isPresent()) {
            return BFF_LOGIN_KEY_PREFIX + bffLoginKey.orElseThrow()
                    + BFF_PEER_KEY_DELIMITER + sha256Hex(directPeerAddress(exchange));
        }
        return "peer:" + directPeerAddress(exchange);
    }

    private Optional<String> resolveVerifiedBffLoginKey(ServerWebExchange exchange) {
        if (bffSharedSecret.length == 0
                || exchange.getRequest().getMethod() != HttpMethod.POST
                || !LOGIN_PATH.equals(exchange.getRequest().getURI().getPath())) {
            return Optional.empty();
        }

        Optional<String> rateKey = singleHeader(exchange, BFF_RATE_KEY_HEADER)
                .filter(value -> SHA_256_HEX_PATTERN.matcher(value).matches());
        Optional<String> timestamp = singleHeader(exchange, BFF_RATE_TIMESTAMP_HEADER)
                .filter(value -> TIMESTAMP_PATTERN.matcher(value).matches());
        Optional<String> signature = singleHeader(exchange, BFF_RATE_SIGNATURE_HEADER)
                .filter(value -> SHA_256_HEX_PATTERN.matcher(value).matches());
        if (rateKey.isEmpty() || timestamp.isEmpty() || signature.isEmpty()) {
            return Optional.empty();
        }

        final long signedAt;
        try {
            signedAt = Long.parseLong(timestamp.orElseThrow());
        } catch (NumberFormatException exception) {
            return Optional.empty();
        }
        long now = clock.instant().getEpochSecond();
        if (signedAt < now - BFF_SIGNATURE_MAX_AGE_SECONDS
                || signedAt > now + BFF_SIGNATURE_MAX_AGE_SECONDS) {
            return Optional.empty();
        }

        String canonicalRequest = "POST\n" + LOGIN_PATH + "\n"
                + timestamp.orElseThrow() + "\n" + rateKey.orElseThrow();
        byte[] expectedSignature = hmacSha256(canonicalRequest);
        byte[] suppliedSignature = HEX_FORMAT.parseHex(signature.orElseThrow());
        return MessageDigest.isEqual(expectedSignature, suppliedSignature)
                ? rateKey
                : Optional.empty();
    }

    private Optional<String> singleHeader(ServerWebExchange exchange, String name) {
        List<String> values = exchange.getRequest().getHeaders().get(name);
        if (values == null || values.size() != 1) {
            return Optional.empty();
        }
        return Optional.ofNullable(values.get(0));
    }

    private byte[] hmacSha256(String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(bffSharedSecret, "HmacSHA256"));
            return mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException | InvalidKeyException exception) {
            throw new IllegalStateException("HmacSHA256 is unavailable", exception);
        }
    }

    private static byte[] validatedSharedSecret(String value) {
        byte[] bytes = value == null ? new byte[0] : value.getBytes(StandardCharsets.UTF_8);
        if (bytes.length != 0
                && (bytes.length < MINIMUM_SHARED_SECRET_BYTES
                || bytes.length > MAXIMUM_SHARED_SECRET_BYTES)) {
            throw new IllegalArgumentException(
                    "gateway.bff.shared-secret must contain between 32 and 512 UTF-8 bytes");
        }
        return bytes;
    }

    private static String directPeerAddress(ServerWebExchange exchange) {
        InetSocketAddress remoteAddress = exchange.getRequest().getRemoteAddress();
        if (remoteAddress == null) {
            return "unknown";
        }
        if (remoteAddress.getAddress() != null) {
            return remoteAddress.getAddress().getHostAddress();
        }
        return remoteAddress.getHostString();
    }

    private static String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HEX_FORMAT.formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    /**
     * In-Memory 기반 Token Bucket RateLimiter Bean.
     * 
     * <p>Spring Cloud Gateway 필터 {@code RequestRateLimiter}에 주입되어
     * 검증된 식별 키별 Token Bucket 관리 및 요청 허용/거부(HTTP 429 Too Many Requests) 여부를 결정합니다.</p>
     */
    @Bean
    @Primary
    public RateLimiter<RateLimiterConfig.Config> inMemoryRateLimiter() {
        return new InMemoryRateLimiter();
    }

    /**
     * RateLimiter 구성 설정 클래스.
     */
    public static class Config {
        private int replenishRate = 10;
        private int burstCapacity = 20;

        public int getReplenishRate() {
            return replenishRate;
        }

        public void setReplenishRate(int replenishRate) {
            this.replenishRate = replenishRate;
        }

        public int getBurstCapacity() {
            return burstCapacity;
        }

        public void setBurstCapacity(int burstCapacity) {
            this.burstCapacity = burstCapacity;
        }
    }

    /**
     * 최대 bucket 수를 제한한 Thread-Safe In-Memory Token Bucket RateLimiter 구현체.
     */
    public static class InMemoryRateLimiter implements RateLimiter<Config> {

        private final Map<String, TokenBucket> buckets;
        private final int maximumBucketCount;

        public InMemoryRateLimiter() {
            this(MAXIMUM_IN_MEMORY_BUCKETS);
        }

        InMemoryRateLimiter(int maximumBucketCount) {
            if (maximumBucketCount < 2) {
                throw new IllegalArgumentException("maximumBucketCount must be at least 2");
            }
            this.maximumBucketCount = maximumBucketCount;
            this.buckets = new LinkedHashMap<>(16, 0.75f, true);
        }

        @Override
        public Mono<Response> isAllowed(String routeId, String id) {
            Config config = getConfig().getOrDefault(routeId, new Config());
            TokenBucket bucket = bucketFor(
                    routeScopedKey(routeId, id),
                    config.getBurstCapacity(),
                    config.getReplenishRate());

            boolean allowed = bucket.tryConsume();
            long tokensLeft = bucket.getTokens();
            Optional<String> aggregateKey = bffPeerAggregateKey(id);
            if (allowed && aggregateKey.isPresent()) {
                TokenBucket aggregateBucket = bucketFor(
                        routeScopedKey(routeId, aggregateKey.orElseThrow()),
                        multipliedAndCapped(config.getBurstCapacity()),
                        multipliedAndCapped(config.getReplenishRate()));
                allowed = aggregateBucket.tryConsume();
                tokensLeft = Math.min(tokensLeft, aggregateBucket.getTokens());
            }

            Map<String, String> headers = Map.of(
                "X-RateLimit-Remaining", String.valueOf(tokensLeft),
                "X-RateLimit-Replenish-Rate", String.valueOf(config.getReplenishRate()),
                "X-RateLimit-Burst-Capacity", String.valueOf(config.getBurstCapacity())
            );

            return Mono.just(new Response(allowed, headers));
        }

        private TokenBucket bucketFor(String key, int capacity, int replenishRate) {
            synchronized (buckets) {
                TokenBucket existing = buckets.get(key);
                if (existing != null) {
                    return existing;
                }
                while (buckets.size() >= maximumBucketCount) {
                    String eldestKey = buckets.keySet().iterator().next();
                    buckets.remove(eldestKey);
                }
                TokenBucket created = new TokenBucket(capacity, replenishRate);
                buckets.put(key, created);
                return created;
            }
        }

        private static Optional<String> bffPeerAggregateKey(String id) {
            if (!id.startsWith(BFF_LOGIN_KEY_PREFIX)) {
                return Optional.empty();
            }
            int delimiterIndex = id.lastIndexOf(BFF_PEER_KEY_DELIMITER);
            if (delimiterIndex < BFF_LOGIN_KEY_PREFIX.length()) {
                return Optional.empty();
            }
            String peerHash = id.substring(delimiterIndex + BFF_PEER_KEY_DELIMITER.length());
            if (!SHA_256_HEX_PATTERN.matcher(peerHash).matches()) {
                return Optional.empty();
            }
            return Optional.of("bff-peer:" + peerHash);
        }

        private static String routeScopedKey(String routeId, String id) {
            return routeId + '\n' + id;
        }

        private static int multipliedAndCapped(int value) {
            long multiplied = (long) value * BFF_PEER_AGGREGATE_MULTIPLIER;
            return (int) Math.min(multiplied, Integer.MAX_VALUE);
        }

        int bucketCountForTesting() {
            synchronized (buckets) {
                return buckets.size();
            }
        }

        @Override
        public Map<String, Config> getConfig() {
            return Map.of("default", new Config());
        }

        @Override
        public Class<Config> getConfigClass() {
            return Config.class;
        }

        @Override
        public Config newConfig() {
            return new Config();
        }
    }

    /**
     * Token Bucket 알고리즘으로 소모 및 자동 충전 처리.
     */
    private static class TokenBucket {
        private final int capacity;
        private final int replenishRate;
        private double tokens;
        private long lastRefillTimestamp;

        public TokenBucket(int capacity, int replenishRate) {
            this.capacity = capacity;
            this.replenishRate = replenishRate;
            this.tokens = capacity;
            this.lastRefillTimestamp = System.currentTimeMillis();
        }

        public synchronized boolean tryConsume() {
            refill();
            if (tokens >= 1.0) {
                tokens -= 1.0;
                return true;
            }
            return false;
        }

        public synchronized long getTokens() {
            refill();
            return (long) tokens;
        }

        private void refill() {
            long now = System.currentTimeMillis();
            double elapsedSeconds = (now - lastRefillTimestamp) / 1000.0;
            if (elapsedSeconds > 0) {
                tokens = Math.min(capacity, tokens + elapsedSeconds * replenishRate);
                lastRefillTimestamp = now;
            }
        }
    }
}
