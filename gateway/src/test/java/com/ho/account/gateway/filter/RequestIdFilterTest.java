package com.ho.account.gateway.filter;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class RequestIdFilterTest {

    private final RequestIdFilter filter = new RequestIdFilter();

    @Test
    void generatesRequestIdWhenHeaderMissing() {
        AtomicReference<ServerWebExchange> forwarded = new AtomicReference<>();
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/test").build());

        StepVerifier.create(filter.filter(exchange, capture(forwarded))).verifyComplete();

        String requestId = forwarded.get().getRequest().getHeaders()
                .getFirst(RequestIdFilter.REQUEST_ID_HEADER);
        assertThat(requestId).isNotBlank();
        assertThat(exchange.getResponse().getHeaders().getFirst(RequestIdFilter.REQUEST_ID_HEADER))
                .isEqualTo(requestId);
    }

    @Test
    void preservesSafeExistingRequestId() {
        AtomicReference<ServerWebExchange> forwarded = new AtomicReference<>();
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/test")
                        .header(RequestIdFilter.REQUEST_ID_HEADER, "req-123")
                        .build());

        StepVerifier.create(filter.filter(exchange, capture(forwarded))).verifyComplete();

        assertThat(forwarded.get().getRequest().getHeaders().getFirst(RequestIdFilter.REQUEST_ID_HEADER))
                .isEqualTo("req-123");
        assertThat(exchange.getResponse().getHeaders().getFirst(RequestIdFilter.REQUEST_ID_HEADER))
                .isEqualTo("req-123");
    }

    @Test
    void replacesOverlongClientRequestIdToProtectHeadersAndLogs() {
        AtomicReference<ServerWebExchange> forwarded = new AtomicReference<>();
        String overlongRequestId = "x".repeat(129);
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/test")
                        .header(RequestIdFilter.REQUEST_ID_HEADER, overlongRequestId)
                        .build());

        StepVerifier.create(filter.filter(exchange, capture(forwarded))).verifyComplete();

        String resolved = forwarded.get().getRequest().getHeaders().getFirst(RequestIdFilter.REQUEST_ID_HEADER);
        assertThat(resolved).isNotEqualTo(overlongRequestId).hasSize(36);
        assertThat(exchange.getResponse().getHeaders().getFirst(RequestIdFilter.REQUEST_ID_HEADER))
                .isEqualTo(resolved);
    }

    private GatewayFilterChain capture(AtomicReference<ServerWebExchange> forwarded) {
        return exchange -> {
            forwarded.set(exchange);
            exchange.getResponse().setStatusCode(HttpStatus.OK);
            return Mono.empty();
        };
    }
}
