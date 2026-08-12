package com.ho.account.gateway.config;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RateLimiter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import reactor.core.publisher.Mono;

/**
 * ================================================================================
 * [교육적 주석 - MSA API Gateway 트래픽 제어 및 Rate Limiter / KeyResolver 아키텍처]
 * ================================================================================
 * API Gateway는 Microservice Architecture(MSA)의 최전방 단일 진입점(Single Point of Entry)입니다.
 * 
 * 1. IP 기반 KeyResolver (DDoS 방어 및 트래픽 제어):
 *    - 클라이언트의 실제 IP 주소(L7 프록시 및 로드밸런서의 X-Forwarded-For 헤더 포함)를 식별 키로 사용합니다.
 *    - 악의적인 단일 IP의 과도한 API 요청(DDoS, Credential Stuffing 등)을 게이트웨이 최전방에서 차단하여
 *      내부 Bounded Context 도메인 서비스(Auth, Master-Data, Journal-Ledger 등)의 자원 고갈을 막습니다.
 * 
 * 2. Rate Limiter (Token Bucket 알고리즘 기반 트래픽 제어):
 *    - 초당 요청 충전율(Replenish Rate)과 순간 최고 처리 가능량(Burst Capacity)을 기반으로
 *      백엔드 서버의 최대 처리 능력을 초과하는 순간적 트래픽 폭주(Spike)를 안정적으로 조율합니다.
 *    - Redis 연결이 제공되지 않거나 개발/단단 테스트 환경에서도 안정적으로 동작하는
 *      In-Memory Token Bucket RateLimiter를 함께 등록하여 게이트웨이 보호 기능을 완성합니다.
 * 
 * 3. Bounded Context 독립성 및 안정성 보장:
 *    - 특정 서비스로의 과도한 요청이나 서비스 장애 발생 시에도 Rate Limiter와 CircuitBreaker를 통해
 *      다른 Bounded Context 영역으로 장애가 연쇄 전파(Cascading Failure)되는 것을 차단합니다.
 * ================================================================================
 */
@Configuration
public class RateLimiterConfig {

    /**
     * 클라이언트 IP 주소를 추출하여 Rate Limiter의 요청 식별 키(Key)로 제공하는 KeyResolver Bean.
     * 
     * <p>프록시 또는 Load Balancer(ALB, NGINX 등)를 통과한 요청의 클라이언트 실제 IP를 보장하기 위해
     * X-Forwarded-For 헤더의 첫 번째 IP를 우선 추출하며, 미존재 시 RemoteAddress를 사용합니다.</p>
     *
     * @return Mono<String> 클라이언트 IP 주소
     */
    @Bean
    public KeyResolver ipKeyResolver() {
        return exchange -> {
            String forwardedFor = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
            if (forwardedFor != null && !forwardedFor.isBlank()) {
                String clientIp = forwardedFor.split(",")[0].trim();
                return Mono.just(clientIp);
            }
            
            if (exchange.getRequest().getRemoteAddress() != null 
                    && exchange.getRequest().getRemoteAddress().getAddress() != null) {
                return Mono.just(exchange.getRequest().getRemoteAddress().getAddress().getHostAddress());
            }
            
            return Mono.just("127.0.0.1");
        };
    }

    /**
     * In-Memory 기반 Token Bucket RateLimiter Bean.
     * 
     * <p>Spring Cloud Gateway 필터 {@code RequestRateLimiter}에 주입되어
     * IP별 Token Bucket 관리 및 요청 허용/거부(HTTP 429 Too Many Requests) 여부를 결정합니다.</p>
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
     * ConcurrentHashMap 기반 Thread-Safe In-Memory Token Bucket RateLimiter 구현체.
     */
    public static class InMemoryRateLimiter implements RateLimiter<Config> {

        private final Map<String, TokenBucket> buckets = new ConcurrentHashMap<>();

        @Override
        public Mono<Response> isAllowed(String routeId, String id) {
            Config config = getConfig().getOrDefault(routeId, new Config());
            TokenBucket bucket = buckets.computeIfAbsent(id, k -> new TokenBucket(config.getBurstCapacity(), config.getReplenishRate()));
            
            boolean allowed = bucket.tryConsume();
            long tokensLeft = bucket.getTokens();

            Map<String, String> headers = Map.of(
                "X-RateLimit-Remaining", String.valueOf(tokensLeft),
                "X-RateLimit-Replenish-Rate", String.valueOf(config.getReplenishRate()),
                "X-RateLimit-Burst-Capacity", String.valueOf(config.getBurstCapacity())
            );

            return Mono.just(new Response(allowed, headers));
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
