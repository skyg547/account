package com.ho.account.reconciliation.infrastructure.adapter;

import com.ho.account.contracts.ledger.LedgerAggregateSummary;
import com.ho.account.contracts.ledger.LedgerBalanceSummary;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class HttpReconciliationLedgerAdapterTest {

    @Test
    void getGlBalanceSummariesQueriesEndpointAndMapsResult() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://journal-ledger.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        HttpReconciliationLedgerAdapter adapter = new HttpReconciliationLedgerAdapter(builder.build());

        server.expect(requestTo("http://journal-ledger.test/api/v1/ledger/gl/balances?startDate=2026-09-01&endDate=2026-09-07&accountCode=1100&currencyCode=KRW"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        [
                          {
                            "accountCode": "1100",
                            "currencyCode": "KRW",
                            "debitAmount": 1000.00,
                            "creditAmount": 200.00,
                            "endingBalance": 800.00
                          }
                        ]
                        """, MediaType.APPLICATION_JSON));

        List<LedgerBalanceSummary> results = adapter.getGlBalanceSummaries(
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 7),
                "1100",
                "KRW"
        );

        assertThat(results).hasSize(1);
        LedgerBalanceSummary summary = results.get(0);
        assertThat(summary.getAccountCode()).isEqualTo("1100");
        assertThat(summary.getCurrencyCode()).isEqualTo("KRW");
        assertThat(summary.getDebitAmount()).isEqualByComparingTo(new BigDecimal("1000.00"));
        assertThat(summary.getCreditAmount()).isEqualByComparingTo(new BigDecimal("200.00"));
        assertThat(summary.getEndingBalance()).isEqualByComparingTo(new BigDecimal("800.00"));
        server.verify();
    }

    @Test
    void getSlBalanceSummariesQueriesEndpointWithDeptCodeAndMapsResult() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://journal-ledger.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        HttpReconciliationLedgerAdapter adapter = new HttpReconciliationLedgerAdapter(builder.build());

        server.expect(requestTo("http://journal-ledger.test/api/v1/ledger/sl/balances?startDate=2026-09-01&endDate=2026-09-07&accountCode=1100&businessPartnerCode=BP-01&deptCode=D-01&currencyCode=KRW"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        [
                          {
                            "accountCode": "1100",
                            "businessPartnerCode": "BP-01",
                            "departmentCode": "D-01",
                            "currencyCode": "KRW",
                            "debitAmount": 500.00,
                            "creditAmount": 100.00,
                            "endingBalance": 400.00
                          }
                        ]
                        """, MediaType.APPLICATION_JSON));

        List<LedgerBalanceSummary> results = adapter.getSlBalanceSummaries(
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 7),
                "1100",
                "BP-01",
                "D-01",
                "KRW"
        );

        assertThat(results).hasSize(1);
        LedgerBalanceSummary summary = results.get(0);
        assertThat(summary.getAccountCode()).isEqualTo("1100");
        assertThat(summary.getBusinessPartnerCode()).isEqualTo("BP-01");
        assertThat(summary.getDepartmentCode()).isEqualTo("D-01");
        assertThat(summary.getEndingBalance()).isEqualByComparingTo(new BigDecimal("400.00"));
        server.verify();
    }

    @Test
    void calculateLedgerSummaryAggregatesCorrectlyBasedOnAmountBasis() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://journal-ledger.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        HttpReconciliationLedgerAdapter adapter = new HttpReconciliationLedgerAdapter(builder.build());

        server.expect(requestTo("http://journal-ledger.test/api/v1/ledger/gl/balances?startDate=2026-09-01&endDate=2026-09-07&accountCode=1100"))
                .andRespond(withSuccess("""
                        [
                          {"accountCode":"1100","currencyCode":"KRW","debitAmount":1000,"creditAmount":200,"endingBalance":800},
                          {"accountCode":"1100","currencyCode":"KRW","debitAmount":500,"creditAmount":100,"endingBalance":-400}
                        ]
                        """, MediaType.APPLICATION_JSON));

        LedgerAggregateSummary debitSummary = adapter.calculateLedgerSummary(
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 7),
                "1100",
                null,
                "DEBIT"
        );
        assertThat(debitSummary.getCount()).isEqualTo(2L);
        assertThat(debitSummary.getTotalAmount()).isEqualByComparingTo(new BigDecimal("1500"));

        server.reset();
        server.expect(requestTo("http://journal-ledger.test/api/v1/ledger/gl/balances?startDate=2026-09-01&endDate=2026-09-07&accountCode=1100"))
                .andRespond(withSuccess("""
                        [
                          {"accountCode":"1100","currencyCode":"KRW","debitAmount":1000,"creditAmount":200,"endingBalance":800},
                          {"accountCode":"1100","currencyCode":"KRW","debitAmount":500,"creditAmount":100,"endingBalance":-400}
                        ]
                        """, MediaType.APPLICATION_JSON));

        LedgerAggregateSummary creditSummary = adapter.calculateLedgerSummary(
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 7),
                "1100",
                null,
                "CREDIT"
        );
        assertThat(creditSummary.getCount()).isEqualTo(2L);
        assertThat(creditSummary.getTotalAmount()).isEqualByComparingTo(new BigDecimal("300"));

        server.reset();
        server.expect(requestTo("http://journal-ledger.test/api/v1/ledger/gl/balances?startDate=2026-09-01&endDate=2026-09-07&accountCode=1100"))
                .andRespond(withSuccess("""
                        [
                          {"accountCode":"1100","currencyCode":"KRW","debitAmount":1000,"creditAmount":200,"endingBalance":800},
                          {"accountCode":"1100","currencyCode":"KRW","debitAmount":500,"creditAmount":100,"endingBalance":-400}
                        ]
                        """, MediaType.APPLICATION_JSON));

        LedgerAggregateSummary absSummary = adapter.calculateLedgerSummary(
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 7),
                "1100",
                null,
                "ABS_ENDING_BALANCE"
        );
        assertThat(absSummary.getCount()).isEqualTo(2L);
        assertThat(absSummary.getTotalAmount()).isEqualByComparingTo(new BigDecimal("1200"));
    }

    @Test
    void getBalancesReturnsEmptyOn404OrNullDates() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://journal-ledger.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        HttpReconciliationLedgerAdapter adapter = new HttpReconciliationLedgerAdapter(builder.build());

        server.expect(requestTo("http://journal-ledger.test/api/v1/ledger/gl/balances?startDate=2026-09-01&endDate=2026-09-07"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThat(adapter.getGlBalanceSummaries(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 7), null, null)).isEmpty();
        assertThat(adapter.getGlBalanceSummaries(null, LocalDate.now(), null, null)).isEmpty();
        assertThat(adapter.getSlBalanceSummaries(null, LocalDate.now(), null, null, null, null)).isEmpty();
    }

    @Test
    void failsClosedWithinConfiguredReadTimeout() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/v1/ledger/gl/balances", exchange -> {
            try {
                Thread.sleep(500);
                byte[] body = "[]".getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
                exchange.sendResponseHeaders(200, body.length);
                exchange.getResponseBody().write(body);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            } catch (Exception ignored) {
            } finally {
                exchange.close();
            }
        });
        server.start();

        try {
            HttpReconciliationLedgerAdapter adapter = new HttpReconciliationLedgerAdapter(
                    RestClient.builder(),
                    "http://127.0.0.1:" + server.getAddress().getPort(),
                    Duration.ofMillis(100),
                    Duration.ofMillis(100)
            );
            long startedAt = System.nanoTime();

            assertThatThrownBy(() -> adapter.getGlBalanceSummaries(
                    LocalDate.of(2026, 9, 1),
                    LocalDate.of(2026, 9, 7),
                    "1100",
                    "KRW"
            )).isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Remote GL balances lookup failed");

            assertThat(Duration.ofNanos(System.nanoTime() - startedAt))
                    .isLessThan(Duration.ofSeconds(2));
        } finally {
            server.stop(0);
        }
    }

    @Test
    void rejectsInvalidTimeout() {
        assertThatThrownBy(() -> new HttpReconciliationLedgerAdapter(
                RestClient.builder(),
                "http://journal-ledger.test",
                Duration.ofNanos(1),
                Duration.ofSeconds(1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("connect-timeout");

        assertThatThrownBy(() -> new HttpReconciliationLedgerAdapter(
                RestClient.builder(),
                "http://journal-ledger.test",
                Duration.ofSeconds(1),
                Duration.ofNanos(1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("read-timeout");
    }
}
