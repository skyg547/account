package com.ho.account.reporting.infrastructure.external;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class HttpReportingLedgerAdapterTest {
    private static final LocalDate DATE = LocalDate.of(2026, 9, 10);
    private static final String ROOT = "http://ledger.invalid/api/v1/ledger/";
    private static final String DATES = "startDate=2026-09-10&endDate=2026-09-10";
    private static final String BALANCE = """
            {"id":1,"accountCode":"101","currencyCode":"KRW","balanceDate":"2026-09-10",
             "period":"2026-09","beginningBalance":0,"debitAmount":123456789012345.6789,
             "creditAmount":0.0001,"endingBalance":123456789012345.6788,
             "businessPartnerCode":"BP1","departmentCode":"D1"}
            """;
    private MockRestServiceServer server;
    private HttpReportingLedgerAdapter adapter;

    @BeforeEach
    void setup() {
        var builder = RestClient.builder().baseUrl("http://ledger.invalid");
        server = MockRestServiceServer.bindTo(builder).build();
        adapter = new HttpReportingLedgerAdapter(builder.build());
    }

    @ParameterizedTest
    @ValueSource(ints = {302, 303, 307, 308})
    void productionTransportRejectsRedirectInsteadOfAcceptingBalances(int status) throws Exception {
        HttpServer provider = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        AtomicInteger requests = new AtomicInteger();
        AtomicInteger followed = new AtomicInteger();
        provider.createContext("/", exchange -> {
            requests.incrementAndGet();
            if (exchange.getRequestURI().getPath().equals("/redirect-target")) {
                followed.incrementAndGet();
                byte[] body = ("[" + BALANCE + "]").getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, body.length);
                exchange.getResponseBody().write(body);
            } else {
                exchange.getResponseHeaders().set("Location", "/redirect-target");
                exchange.sendResponseHeaders(status, -1);
            }
            exchange.close();
        });
        provider.start();
        try {
            var remote = new HttpReportingLedgerAdapter(RestClient.builder(),
                    "http://127.0.0.1:" + provider.getAddress().getPort(),
                    Duration.ofSeconds(2), Duration.ofSeconds(2));
            // Use the production request factory: a mock response cannot reveal automatic redirects.
            assertThatThrownBy(() -> remote.getGlBalanceSummaries(DATE, DATE, null, null)).isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("HTTP " + status).hasNoCause();
            assertThat(requests.get()).isEqualTo(1);
            assertThat(followed.get()).isZero();
        } finally {
            provider.stop(0);
        }
    }

    @Test
    void glUsesActualDateAndFilterKeysAndPreservesDecimalPrecision() {
        expect("gl/balances?" + DATES + "&accountCode=101&currencyCode=KRW", "[" + BALANCE + "]");
        var result = adapter.getGlBalanceSummaries(DATE, DATE, "101", "KRW");
        assertThat(result).hasSize(1);
        var balance = result.get(0);
        assertThat(balance.getAccountCode()).isEqualTo("101");
        assertThat(balance.getCurrencyCode()).isEqualTo("KRW");
        assertThat(balance.getDebitAmount()).isEqualByComparingTo("123456789012345.6789");
        assertThat(balance.getCreditAmount()).isEqualByComparingTo("0.0001");
        assertThat(balance.getEndingBalance()).isEqualByComparingTo("123456789012345.6788");
        server.verify();
    }

    @Test
    void slUsesDeptCodeQueryButDepartmentCodeResponse() {
        expect("sl/balances?" + DATES + "&accountCode=101&businessPartnerCode=BP1&deptCode=D1&currencyCode=KRW", "[" + BALANCE + "]");
        var result = adapter.getSlBalanceSummaries(DATE, DATE, "101", "BP1", "D1", "KRW");
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getBusinessPartnerCode()).isEqualTo("BP1");
        assertThat(result.get(0).getDepartmentCode()).isEqualTo("D1");
        server.verify();
    }

    @Test
    void omittedFiltersAreAbsentAndEmptyArrayMeansNoBalances() {
        expect("gl/balances?" + DATES, "[]");
        expect("sl/balances?" + DATES, "[]");
        assertThat(adapter.getGlBalanceSummaries(DATE, DATE, null, null)).isEmpty();
        assertThat(adapter.getSlBalanceSummaries(DATE, DATE, null, null, null, null)).isEmpty();
        server.verify();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "null", "{}", "[null]", "[{}]", "not-json"})
    void malformedResponsesCannotBecomeZeroBalances(String body) {
        expect("gl/balances?" + DATES, body);
        assertThatThrownBy(() -> adapter.getGlBalanceSummaries(DATE, DATE, null, null)).isInstanceOf(RuntimeException.class);
        server.verify();
    }

    @ParameterizedTest
    @ValueSource(strings = {"accountCode", "currencyCode", "balanceDate", "debitAmount", "creditAmount", "endingBalance"})
    void missingRequiredBalanceFieldsFailClosed(String field) throws Exception {
        var node = (com.fasterxml.jackson.databind.node.ObjectNode) new com.fasterxml.jackson.databind.ObjectMapper().readTree(BALANCE);
        node.remove(field);
        expect("gl/balances?" + DATES, "[" + node + "]");
        assertThatThrownBy(() -> adapter.getGlBalanceSummaries(DATE, DATE, null, null)).isInstanceOf(RuntimeException.class);
        server.verify();
    }

    @ParameterizedTest
    @ValueSource(strings = {"accountCode", "currencyCode", "balanceDate", "businessPartnerCode", "departmentCode"})
    void balancesOutsideRequestedFiltersAreRejected(String field) throws Exception {
        var node = (com.fasterxml.jackson.databind.node.ObjectNode) new com.fasterxml.jackson.databind.ObjectMapper().readTree(BALANCE);
        node.put(field, field.equals("balanceDate") ? "2026-09-11" : "OTHER");
        expect("sl/balances?" + DATES + "&accountCode=101&businessPartnerCode=BP1&deptCode=D1&currencyCode=KRW", "[" + node + "]");
        assertThatThrownBy(() -> adapter.getSlBalanceSummaries(DATE, DATE, "101", "BP1", "D1", "KRW")).isInstanceOf(RuntimeException.class);
        server.verify();
    }

    @ParameterizedTest
    @ValueSource(ints = {302, 404, 500, 503})
    void httpFailuresAreNotEmptyBalances(int status) {
        server.expect(requestTo(ROOT + "gl/balances?" + DATES))
                .andRespond(withStatus(HttpStatus.valueOf(status)).body("sensitive-provider-detail"));
        assertThatThrownBy(() -> adapter.getGlBalanceSummaries(DATE, DATE, null, null))
                .isInstanceOf(RuntimeException.class).hasMessageNotContaining("sensitive-provider-detail");
        server.verify();
    }

    @Test
    void unsupportedAggregationAndInvalidDatesDoNotSendHttpRequests() {
        assertThatThrownBy(() -> adapter.calculateLedgerSummary(DATE, DATE, "101", "KRW", "DEBIT"))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> adapter.getGlBalanceSummaries(DATE.plusDays(1), DATE, null, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> adapter.getGlBalanceSummaries(null, DATE, null, null))
                .isInstanceOf(IllegalArgumentException.class);
        server.verify();
    }

    @ParameterizedTest
    @ValueSource(strings = {"debitAmount", "creditAmount"})
    void negativeMovementsAreRejected(String field) throws Exception {
        var node = (com.fasterxml.jackson.databind.node.ObjectNode) new com.fasterxml.jackson.databind.ObjectMapper().readTree(BALANCE);
        node.put(field, -1);
        expect("gl/balances?" + DATES, "[" + node + "]");
        assertThatThrownBy(() -> adapter.getGlBalanceSummaries(DATE, DATE, null, null)).isInstanceOf(RuntimeException.class);
        server.verify();
    }

    @Test
    void creditBalanceCanHaveNegativeEndingAmount() {
        expect("gl/balances?" + DATES, "[" + BALANCE.replace("123456789012345.6788", "-10.0001") + "]");
        assertThat(adapter.getGlBalanceSummaries(DATE, DATE, null, null).get(0).getEndingBalance())
                .isEqualByComparingTo("-10.0001");
        server.verify();
    }

    @ParameterizedTest
    @org.junit.jupiter.params.provider.CsvSource({"dev,missing", "dev,false", "local,missing", "local,true", "prod,true"})
    void unselectedRemoteAdaptersPreserveMemoryPortsWithoutHttpConfiguration(String profile, String remote) {
        var runner = new org.springframework.boot.test.context.runner.ApplicationContextRunner()
                .withInitializer(context -> context.getEnvironment().setActiveProfiles(profile))
                .withPropertyValues("account.reporting.persistence.mode=memory")
                .withUserConfiguration(HttpReportingLedgerAdapter.class, HttpReportingJournalAdapter.class,
                        com.ho.account.reporting.infrastructure.persistence.InMemoryLedgerBalanceAdapter.class,
                        com.ho.account.reporting.infrastructure.persistence.InMemoryJournalQueryAdapter.class,
                        com.ho.account.reporting.infrastructure.persistence.LedgerClientAdapter.class);
        if (!remote.equals("missing")) runner = runner.withPropertyValues("account.reporting.remote.enabled=" + remote);
        // No RestClient.Builder, URL, or foreign Ledger provider is supplied.
        runner.run(context -> {
            assertThat(context).hasNotFailed().doesNotHaveBean(HttpReportingLedgerAdapter.class)
                    .doesNotHaveBean(HttpReportingJournalAdapter.class)
                    .doesNotHaveBean(com.ho.account.reporting.infrastructure.persistence.LedgerClientAdapter.class);
            assertThat(context).hasSingleBean(com.ho.account.reporting.application.port.out.LoadLedgerPort.class)
                    .hasSingleBean(com.ho.account.contracts.journal.JournalQueryPort.class);
            assertThat(context.getBean(com.ho.account.reporting.application.port.out.LoadLedgerPort.class))
                    .isInstanceOf(com.ho.account.reporting.infrastructure.persistence.InMemoryLedgerBalanceAdapter.class);
            assertThat(context.getBean(com.ho.account.contracts.journal.JournalQueryPort.class))
                    .isInstanceOf(com.ho.account.reporting.infrastructure.persistence.InMemoryJournalQueryAdapter.class);
        });
    }

    @Test
    void transportTimeoutFailsWithoutLeakingCauseOrReturningEmptyBalances() {
        server.expect(requestTo(ROOT + "gl/balances?" + DATES)).andRespond(request -> {
            throw new java.net.SocketTimeoutException("sensitive-provider-detail");
        });
        assertThatThrownBy(() -> adapter.getGlBalanceSummaries(DATE, DATE, null, null))
                .isInstanceOf(IllegalStateException.class).hasMessageNotContaining("sensitive-provider-detail").hasNoCause();
        server.verify();
    }

    private void expect(String uri, String body) {
        server.expect(requestTo(ROOT + uri)).andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));
    }
}
