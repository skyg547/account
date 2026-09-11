package com.ho.account.closing.config;

import com.ho.account.closing.infrastructure.external.HttpFiscalPeriodControlAdapter;
import java.time.Duration;
import java.time.LocalDate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class HttpFiscalPeriodControlAdapterTest {
    private static final String BASE = "http://master-data.test";
    private static final String PERIOD = """
            {"id":9,"fiscalYear":"2026","fiscalPeriod":"09",
             "startDate":"2026-09-01","endDate":"2026-09-30","closingStatus":"CLOSED"}
            """;
    private MockRestServiceServer server;
    private HttpFiscalPeriodControlAdapter adapter;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = spy(RestClient.builder());
        server = MockRestServiceServer.bindTo(builder).build();
        doReturn(builder).when(builder).requestFactory(any(ClientHttpRequestFactory.class));
        adapter = new HttpFiscalPeriodControlAdapter(builder, BASE, "2s", "5s");
    }

    @AfterEach
    void verifyRequests() { server.verify(); }

    @Test
    void lookupUsesMasterDataRoutesAndMapsPeriodDates() {
        server.expect(requestTo(BASE + "/api/basic/fiscal-periods/id/9"))
                .andExpect(method(HttpMethod.GET)).andRespond(withSuccess(PERIOD, MediaType.APPLICATION_JSON));
        server.expect(requestTo(BASE + "/api/basic/fiscal-periods/2026/09"))
                .andExpect(method(HttpMethod.GET)).andRespond(withSuccess(PERIOD, MediaType.APPLICATION_JSON));
        var byId = adapter.findFiscalPeriodById(9L).orElseThrow();
        assertThat(byId.startDate()).isEqualTo(LocalDate.of(2026, 9, 1));
        assertThat(byId.endDate()).isEqualTo(LocalDate.of(2026, 9, 30));
        assertThat(adapter.findFiscalPeriod(" 2026 ", " 09 ")).contains(byId);
    }

    @Test
    void notFoundIsEmptyButServerFailureIsNotMissingPeriod() {
        server.expect(requestTo(BASE + "/api/basic/fiscal-periods/id/9")).andRespond(withResourceNotFound());
        server.expect(requestTo(BASE + "/api/basic/fiscal-periods/2026/09")).andRespond(withResourceNotFound());
        server.expect(requestTo(BASE + "/api/basic/fiscal-periods/id/9")).andRespond(withServerError());
        server.expect(requestTo(BASE + "/api/basic/fiscal-periods/2026/09")).andRespond(withServerError());
        assertThat(adapter.findFiscalPeriodById(9L)).isEmpty();
        assertThat(adapter.findFiscalPeriod("2026", "09")).isEmpty();
        assertThatThrownBy(() -> adapter.findFiscalPeriodById(9L)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> adapter.findFiscalPeriod("2026", "09")).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void emptyLookupBodyRetainsOptionalContract() {
        server.expect(requestTo(BASE + "/api/basic/fiscal-periods/id/9")).andRespond(withSuccess());
        assertThat(adapter.findFiscalPeriodById(9L)).isEmpty();
        assertThat(adapter.findFiscalPeriodById(null)).isEmpty();
        assertThat(adapter.findFiscalPeriod(" ", "09")).isEmpty();
    }

    @Test
    void statusUpdateCarriesServiceIdentityAndAuditActor() {
        server.expect(requestTo(BASE + "/api/internal/fiscal-periods/9/closing-status"))
                .andExpect(method(HttpMethod.PUT))
                .andExpect(header("X-Service-Identity", "closing"))
                .andExpect(content().json("{\"closingStatus\":\"CLOSED\",\"auditUser\":\"operator\"}"))
                .andRespond(withSuccess(PERIOD, MediaType.APPLICATION_JSON));
        assertThat(adapter.updateClosingStatus(9L, "CLOSED", "operator").closingStatus()).isEqualTo("CLOSED");
    }

    @ParameterizedTest
    @ValueSource(ints = {401, 403})
    void rejectedServiceIdentityFailsWithoutRetryOrResponseLeak(int status) {
        server.expect(requestTo(BASE + "/api/internal/fiscal-periods/9/closing-status"))
                .andRespond(withStatus(HttpStatus.valueOf(status)).body("sensitive-fixture-detail"));
        assertThatThrownBy(() -> adapter.updateClosingStatus(9L, "CLOSED", "operator"))
                .isInstanceOf(SecurityException.class).hasMessageNotContaining("sensitive-fixture-detail").hasNoCause();
    }

    @Test
    void emptyUpdateBodyAndServerFailureCannotReportSuccessfulTransition() {
        String uri = BASE + "/api/internal/fiscal-periods/9/closing-status";
        server.expect(requestTo(uri)).andRespond(withSuccess());
        server.expect(requestTo(uri)).andRespond(withServerError());
        assertThatThrownBy(() -> adapter.updateClosingStatus(9L, "CLOSED", "operator")).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> adapter.updateClosingStatus(9L, "CLOSED", "operator")).isInstanceOf(IllegalStateException.class);
    }

    @ParameterizedTest
    @ValueSource(ints = {301, 302, 303, 307, 308})
    void redirectWithValidPeriodBodyCannotReportSuccessfulTransition(int status) {
        server.expect(requestTo(BASE + "/api/internal/fiscal-periods/9/closing-status"))
                .andExpect(method(HttpMethod.PUT))
                .andRespond(withStatus(HttpStatus.valueOf(status))
                        .header("Location", "https://sensitive-fixture.invalid/closing")
                        .body(PERIOD).contentType(MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> adapter.updateClosingStatus(9L, "CLOSED", "operator"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("HTTP " + status)
                .hasMessageNotContaining("sensitive-fixture").hasNoCause();
        // An extra PUT or retry is rejected as an unexpected mock-server request.
    }

    @Test
    void lookupRedirectWithValidPeriodBodyIsNotAnExistingPeriod() {
        server.expect(requestTo(BASE + "/api/basic/fiscal-periods/id/9"))
                .andRespond(withStatus(HttpStatus.FOUND).body(PERIOD).contentType(MediaType.APPLICATION_JSON));
        server.expect(requestTo(BASE + "/api/basic/fiscal-periods/2026/09"))
                .andRespond(withStatus(HttpStatus.FOUND).body(PERIOD).contentType(MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> adapter.findFiscalPeriodById(9L))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("HTTP 302");
        assertThatThrownBy(() -> adapter.findFiscalPeriod("2026", "09"))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("HTTP 302");
    }

    @Test
    void successfulHttpLookupStillRejectsWrongPeriodIdentity() {
        server.expect(requestTo(BASE + "/api/basic/fiscal-periods/id/10"))
                .andRespond(withSuccess(PERIOD, MediaType.APPLICATION_JSON));
        server.expect(requestTo(BASE + "/api/basic/fiscal-periods/2027/09"))
                .andRespond(withSuccess(PERIOD, MediaType.APPLICATION_JSON));
        server.expect(requestTo(BASE + "/api/basic/fiscal-periods/2026/10"))
                .andRespond(withSuccess(PERIOD, MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> adapter.findFiscalPeriodById(10L))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("different fiscal period ID");
        assertThatThrownBy(() -> adapter.findFiscalPeriod("2027", "09"))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("different fiscal year or period");
        assertThatThrownBy(() -> adapter.findFiscalPeriod("2026", "10"))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("different fiscal year or period");
    }

    @Test
    void successfulHttpUpdateStillRequiresRequestedPeriodAndTargetStatus() {
        server.expect(requestTo(BASE + "/api/internal/fiscal-periods/10/closing-status"))
                .andRespond(withSuccess(PERIOD, MediaType.APPLICATION_JSON));
        server.expect(requestTo(BASE + "/api/internal/fiscal-periods/9/closing-status"))
                .andRespond(withSuccess(PERIOD, MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> adapter.updateClosingStatus(10L, "CLOSED", "operator"))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("different period or status");
        assertThatThrownBy(() -> adapter.updateClosingStatus(9L, "OPEN", "operator"))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("different period or status");
    }

    @Test
    void actualTransportRejectsPutRedirectAndNeverFollowsGetRedirectToSuccessfulPeriod() throws Exception {
        var forwardedRequests = new java.util.concurrent.atomic.AtomicInteger();
        var originalRequests = new java.util.concurrent.atomic.AtomicInteger();
        var http = com.sun.net.httpserver.HttpServer.create(new java.net.InetSocketAddress("127.0.0.1", 0), 0);
        http.createContext("/api", exchange -> {
            originalRequests.incrementAndGet();
            try (var body = exchange.getRequestBody()) { body.readAllBytes(); }
            byte[] response = PERIOD.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Location", "/redirected");
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(302, response.length);
            try (var output = exchange.getResponseBody()) { output.write(response); }
        });
        http.createContext("/redirected", exchange -> {
            forwardedRequests.incrementAndGet();
            byte[] response = PERIOD.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            try (var output = exchange.getResponseBody()) { output.write(response); }
        });
        http.start();
        try {
            var direct = new HttpFiscalPeriodControlAdapter(RestClient.builder(),
                    "http://127.0.0.1:" + http.getAddress().getPort(), "2s", "2s");
            assertThatThrownBy(() -> direct.updateClosingStatus(9L, "CLOSED", "operator"))
                    .isInstanceOf(IllegalStateException.class).hasMessageContaining("HTTP 302");
            assertThatThrownBy(() -> direct.findFiscalPeriodById(9L))
                    .isInstanceOf(IllegalStateException.class).hasMessageContaining("HTTP 302");
            assertThat(originalRequests.get()).isEqualTo(2);
            assertThat(forwardedRequests.get()).isZero();
        } finally {
            http.stop(0);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"0ms", "-1s", "2147483648ms", "PT0.000001S", "garbage", ""})
    void rejectsInvalidOrUnboundedTimeouts(String timeout) {
        assertThatThrownBy(() -> new HttpFiscalPeriodControlAdapter(RestClient.builder(), BASE, timeout, "5s"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new HttpFiscalPeriodControlAdapter(RestClient.builder(), BASE, "2s", timeout))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void acceptsFiniteTimeoutBoundariesAndIsoNotation() {
        assertThatCode(() -> new HttpFiscalPeriodControlAdapter(RestClient.builder(), BASE, "1ms", "PT5S")).doesNotThrowAnyException();
        assertThatCode(() -> new HttpFiscalPeriodControlAdapter(RestClient.builder(), BASE,
                Duration.ofMillis(1), Duration.ofMillis(Integer.MAX_VALUE))).doesNotThrowAnyException();
    }
}
