package com.ho.account.closing.config;

import com.ho.account.closing.infrastructure.external.HttpFiscalPeriodControlAdapter;
import com.ho.account.contracts.masterdata.FiscalPeriodClosingStatus;
import com.ho.account.contracts.masterdata.FiscalPeriodTransitionCommand;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
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
    private static final String SECRET = "separate-closing-assertion-test-key-123456789";
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
        adapter = new HttpFiscalPeriodControlAdapter(builder, BASE,
                Duration.ofSeconds(2), Duration.ofSeconds(5), SECRET);
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
    void retiredDirectUpdateFailsBeforeAnyHttpRequest() {
        assertThatThrownBy(() -> adapter.updateClosingStatus(9L, "CLOSED", "operator"))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("operation-bound transition");
    }

    @Test
    void reopenOperationRoutesSendBoundSignedServiceAssertionAndValidateReceipt() throws Exception {
        String operationId = "2a28494e-8b9c-47e1-a443-3ce7b732973a";
        String preparePath = "/api/internal/fiscal-periods/9/transitions/" + operationId;
        String receiptPath = "/api/internal/fiscal-periods/transitions/" + operationId;
        String prepared = "{\"operationId\":\"" + operationId + "\",\"fiscalPeriodId\":9,"
                + "\"expectedStatus\":\"CLOSED\",\"targetStatus\":\"OPEN\","
                + "\"auditUser\":\"operator\",\"phase\":\"PREPARED\"}";
        String applied = prepared.replace("PREPARED", "APPLIED");
        var command = new FiscalPeriodTransitionCommand(operationId, 9L,
                FiscalPeriodClosingStatus.CLOSED, FiscalPeriodClosingStatus.OPEN, "operator");
        server.expect(requestTo(BASE + preparePath)).andExpect(method(HttpMethod.PUT))
                .andExpect(header("X-Service-Identity", "closing"))
                .andExpect(request -> verifyAssertion(request.getHeaders().getFirst("X-Service-Assertion"),
                        "PUT", preparePath, operationId, "operator"))
                .andExpect(content().json("{\"expectedStatus\":\"CLOSED\",\"targetStatus\":\"OPEN\"}"))
                .andRespond(withSuccess(prepared, MediaType.APPLICATION_JSON));
        server.expect(requestTo(BASE + receiptPath)).andExpect(method(HttpMethod.GET))
                .andExpect(request -> verifyAssertion(request.getHeaders().getFirst("X-Service-Assertion"),
                        "GET", receiptPath, operationId, "closing-service"))
                .andRespond(withSuccess(prepared, MediaType.APPLICATION_JSON));
        server.expect(requestTo(BASE + receiptPath)).andExpect(method(HttpMethod.POST))
                .andExpect(request -> verifyAssertion(request.getHeaders().getFirst("X-Service-Assertion"),
                        "POST", receiptPath, operationId, "closing-service"))
                .andRespond(withSuccess(applied, MediaType.APPLICATION_JSON));
        String confirmPath = receiptPath + "/confirm";
        server.expect(requestTo(BASE + confirmPath)).andExpect(method(HttpMethod.POST))
                .andExpect(request -> verifyAssertion(request.getHeaders().getFirst("X-Service-Assertion"),
                        "POST", confirmPath, operationId, "closing-service"))
                .andRespond(withSuccess(prepared.replace("PREPARED", "CONFIRMED"), MediaType.APPLICATION_JSON));

        assertThat(adapter.prepareClosingTransition(command).matches(command)).isTrue();
        assertThat(adapter.findTransitionReceipt(operationId).orElseThrow().matches(command)).isTrue();
        assertThat(adapter.applyClosingTransition(operationId).phase().name()).isEqualTo("APPLIED");
        assertThat(adapter.confirmClosingTransition(operationId).phase().name()).isEqualTo("CONFIRMED");
    }

    @Test
    void preparedCloseCancellationUsesItsOwnBoundOperationAndSignedActor() {
        String operationId = "e44d9d1b-6a66-4c3d-9335-462d3bb01e82";
        String preparePath = "/api/internal/fiscal-periods/9/transitions/" + operationId;
        String receiptPath = "/api/internal/fiscal-periods/transitions/" + operationId;
        String prepared = "{\"operationId\":\"" + operationId + "\",\"fiscalPeriodId\":9,"
                + "\"expectedStatus\":\"OPEN\",\"targetStatus\":\"CLOSED\","
                + "\"auditUser\":\"operator\",\"phase\":\"PREPARED\"}";
        var command = new FiscalPeriodTransitionCommand(operationId, 9L,
                FiscalPeriodClosingStatus.OPEN, FiscalPeriodClosingStatus.CLOSED, "operator");
        server.expect(requestTo(BASE + preparePath)).andExpect(method(HttpMethod.PUT))
                .andExpect(header("X-Service-Identity", "closing"))
                .andExpect(request -> verifyAssertion(request.getHeaders().getFirst("X-Service-Assertion"),
                        "PUT", preparePath, operationId, "operator", "OPEN", "CLOSED"))
                .andExpect(content().json("{\"expectedStatus\":\"OPEN\",\"targetStatus\":\"CLOSED\"}"))
                .andRespond(withSuccess(prepared, MediaType.APPLICATION_JSON));
        server.expect(requestTo(BASE + receiptPath)).andExpect(method(HttpMethod.DELETE))
                .andExpect(request -> verifyAssertion(request.getHeaders().getFirst("X-Service-Assertion"),
                        "DELETE", receiptPath, operationId, "recovery-operator"))
                .andRespond(withSuccess(prepared.replace("PREPARED", "CANCELLED"), MediaType.APPLICATION_JSON));

        assertThat(adapter.prepareClosingTransition(command).matches(command)).isTrue();
        assertThat(adapter.cancelClosingTransition(operationId, "recovery-operator").phase().name())
                .isEqualTo("CANCELLED");
    }

    private static void verifyAssertion(String assertion, String method, String path,
            String operationId, String actor) {
        verifyAssertion(assertion, method, path, operationId, actor, "CLOSED", "OPEN");
    }

    private static void verifyAssertion(String assertion, String method, String path,
            String operationId, String actor, String expectedStatus, String targetStatus) {
        try {
            assertThat(assertion).isNotBlank();
            String[] parts = assertion.split("\\.");
            assertThat(parts).hasSize(3);
            var mac = javax.crypto.Mac.getInstance("HmacSHA256");
            mac.init(new javax.crypto.spec.SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] signed = mac.doFinal((parts[0] + "." + parts[1]).getBytes(StandardCharsets.US_ASCII));
            assertThat(Base64.getUrlDecoder().decode(parts[2])).containsExactly(signed);
            var claims = new com.fasterxml.jackson.databind.ObjectMapper().readTree(
                    Base64.getUrlDecoder().decode(parts[1]));
            assertThat(claims.path("iss").asText()).isEqualTo("closing-service");
            assertThat(claims.path("aud").asText()).isEqualTo("master-data-internal");
            assertThat(claims.path("sub").asText()).isEqualTo("closing");
            assertThat(claims.path("method").asText()).isEqualTo(method);
            assertThat(claims.path("path").asText()).isEqualTo(path);
            assertThat(claims.path("operationId").asText()).isEqualTo(operationId);
            assertThat(claims.path("actor").asText()).isEqualTo(actor);
            assertThat(claims.path("serviceAuthority").asText()).isEqualTo("closing-fiscal-transition");
            assertThat(claims.path("actorRoles").isMissingNode()).isTrue();
            if ("PUT".equals(method)) {
                assertThat(claims.path("fiscalPeriodId").asLong()).isEqualTo(9L);
                assertThat(claims.path("expectedStatus").asText()).isEqualTo(expectedStatus);
                assertThat(claims.path("targetStatus").asText()).isEqualTo(targetStatus);
            }
        } catch (Exception failure) {
            throw new AssertionError("Cannot verify signed Closing service assertion", failure);
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {401, 403})
    void rejectedSignedOperationFailsWithoutRetryOrResponseLeak(int status) {
        var command = new FiscalPeriodTransitionCommand("operation-1", 9L,
                FiscalPeriodClosingStatus.OPEN, FiscalPeriodClosingStatus.CLOSED, "operator");
        server.expect(requestTo(BASE + "/api/internal/fiscal-periods/9/transitions/operation-1"))
                .andRespond(withStatus(HttpStatus.valueOf(status)).body("sensitive-fixture-detail"));
        assertThatThrownBy(() -> adapter.prepareClosingTransition(command))
                .isInstanceOf(SecurityException.class).hasMessageNotContaining("sensitive-fixture-detail").hasNoCause();
    }

    @Test
    void emptyPrepareBodyAndServerFailureCannotReportSuccessfulTransition() {
        var command = new FiscalPeriodTransitionCommand("operation-1", 9L,
                FiscalPeriodClosingStatus.OPEN, FiscalPeriodClosingStatus.CLOSED, "operator");
        String uri = BASE + "/api/internal/fiscal-periods/9/transitions/operation-1";
        server.expect(requestTo(uri)).andRespond(withSuccess());
        server.expect(requestTo(uri)).andRespond(withServerError());
        assertThatThrownBy(() -> adapter.prepareClosingTransition(command)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> adapter.prepareClosingTransition(command)).isInstanceOf(IllegalStateException.class);
    }

    @ParameterizedTest
    @ValueSource(ints = {301, 302, 303, 307, 308})
    void redirectWithReceiptBodyCannotReportSuccessfulTransition(int status) {
        var command = new FiscalPeriodTransitionCommand("operation-1", 9L,
                FiscalPeriodClosingStatus.OPEN, FiscalPeriodClosingStatus.CLOSED, "operator");
        server.expect(requestTo(BASE + "/api/internal/fiscal-periods/9/transitions/operation-1"))
                .andExpect(method(HttpMethod.PUT))
                .andRespond(withStatus(HttpStatus.valueOf(status))
                        .header("Location", "https://sensitive-fixture.invalid/closing")
                        .body(PERIOD).contentType(MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> adapter.prepareClosingTransition(command))
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
    void successfulHttpPrepareStillRejectsWrongReceiptBinding() {
        var command = new FiscalPeriodTransitionCommand("operation-1", 9L,
                FiscalPeriodClosingStatus.OPEN, FiscalPeriodClosingStatus.CLOSED, "operator");
        String receipt = "{\"operationId\":\"operation-1\",\"fiscalPeriodId\":10,"
                + "\"expectedStatus\":\"OPEN\",\"targetStatus\":\"CLOSED\","
                + "\"auditUser\":\"operator\",\"phase\":\"PREPARED\"}";
        server.expect(requestTo(BASE + "/api/internal/fiscal-periods/9/transitions/operation-1"))
                .andRespond(withSuccess(receipt, MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> adapter.prepareClosingTransition(command))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("does not match request");
    }

    @Test
    void actualTransportRejectsPrepareRedirectAndNeverFollowsGetRedirectToSuccessfulPeriod() throws Exception {
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
                    "http://127.0.0.1:" + http.getAddress().getPort(),
                    Duration.ofSeconds(2), Duration.ofSeconds(2), SECRET);
            var command = new FiscalPeriodTransitionCommand("operation-1", 9L,
                    FiscalPeriodClosingStatus.OPEN, FiscalPeriodClosingStatus.CLOSED, "operator");
            assertThatThrownBy(() -> direct.prepareClosingTransition(command))
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
    void remoteBeanCannotStartWithoutSharedAssertionSecret() {
        assertThatThrownBy(() -> new HttpFiscalPeriodControlAdapter(
                RestClient.builder(), BASE, "2s", "5s", "short"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("assertion key is not configured");
    }

    @Test
    void acceptsFiniteTimeoutBoundariesAndIsoNotation() {
        assertThatCode(() -> new HttpFiscalPeriodControlAdapter(RestClient.builder(), BASE, "1ms", "PT5S")).doesNotThrowAnyException();
        assertThatCode(() -> new HttpFiscalPeriodControlAdapter(RestClient.builder(), BASE,
                Duration.ofMillis(1), Duration.ofMillis(Integer.MAX_VALUE))).doesNotThrowAnyException();
    }
}
