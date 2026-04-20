package com.ho.account.gateway.filter;

import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

class RequestIdFilterTest {

    private final RequestIdFilter filter = new RequestIdFilter();

    @Test
    void generatesRequestIdWhenHeaderMissing() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/test").build());
        GatewayFilterChain chain = ex -> {
            ex.getResponse().setStatusCode(HttpStatus.OK);
            return Mono.empty();
        };

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        assertThat(exchange.getRequest().getHeaders().getFirst(RequestIdFilter.REQUEST_ID_HEADER)).isNotBlank();
        assertThat(exchange.getResponse().getHeaders().getFirst(RequestIdFilter.REQUEST_ID_HEADER)).isNotBlank();
    }

    @Test
    void preservesExistingRequestId() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/test")
                        .header(RequestIdFilter.REQUEST_ID_HEADER, "req-123")
                        .build());
        GatewayFilterChain chain = ex -> {
            ex.getResponse().setStatusCode(HttpStatus.OK);
            return Mono.empty();
        };

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        assertThat(exchange.getRequest().getHeaders().getFirst(RequestIdFilter.REQUEST_ID_HEADER)).isEqualTo("req-123");
        assertThat(exchange.getResponse().getHeaders().getFirst(RequestIdFilter.REQUEST_ID_HEADER)).isEqualTo("req-123");
    }
}
