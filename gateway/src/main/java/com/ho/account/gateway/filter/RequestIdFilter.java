package com.ho.account.gateway.filter;

import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 요청 하나를 로그와 서비스 사이에서 추적할 수 있는 식별자를 정규화하는 전역 필터입니다.
 *
 * <p>클라이언트 식별자는 분산 추적 연계를 위해 보존하지만, 로그 오염과 과도한 헤더 크기를 막기 위해
 * 안전한 문자와 128자 길이만 허용합니다. 조건을 벗어나면 Gateway가 새 UUID를 발급합니다.</p>
 */
@Component
public class RequestIdFilter implements GlobalFilter, Ordered {

    public static final String REQUEST_ID_HEADER = "X-Request-Id";

    private static final int MAX_REQUEST_ID_LENGTH = 128;
    private static final Pattern SAFE_REQUEST_ID = Pattern.compile("[A-Za-z0-9._:-]+");

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String requestId = resolveRequestId(
                exchange.getRequest().getHeaders().getFirst(REQUEST_ID_HEADER));

        ServerHttpRequest mutatedRequest = exchange.getRequest()
                .mutate()
                .headers(headers -> headers.set(REQUEST_ID_HEADER, requestId))
                .build();

        exchange.getResponse().getHeaders().set(REQUEST_ID_HEADER, requestId);
        return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }

    private String resolveRequestId(String candidate) {
        if (candidate == null || candidate.isBlank()) {
            return UUID.randomUUID().toString();
        }
        String normalized = candidate.trim();
        if (normalized.length() > MAX_REQUEST_ID_LENGTH || !SAFE_REQUEST_ID.matcher(normalized).matches()) {
            return UUID.randomUUID().toString();
        }
        return normalized;
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
