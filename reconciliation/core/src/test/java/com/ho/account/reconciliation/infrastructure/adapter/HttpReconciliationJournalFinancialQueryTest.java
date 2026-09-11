package com.ho.account.reconciliation.infrastructure.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.ho.account.contracts.journal.JournalSide;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class HttpReconciliationJournalFinancialQueryTest {
    private static final String ROOT = "http://journal.invalid";
    private static final LocalDate DATE = LocalDate.of(2026, 9, 10);
    private static final String RANGE = ROOT + "/api/journals?startDate=2026-09-10&endDate=2026-09-10";
    private static final String LINES = """
            [{"id":81,"side":"DEBIT","accountCode":"11000","amount":10,
              "baseAmount":99999999999999999.90,"departmentCode":"FIN",
              "businessPartnerCode":"SYNTHETIC-PARTNER","description":"first"},
             {"id":82,"side":"DEBIT","accountCode":"11000","amount":0.09,
              "baseAmount":null,"description":"second"},
             {"id":83,"side":"CREDIT","accountCode":"11000","amount":2,"baseAmount":3},
             {"id":84,"side":"DEBIT","accountCode":"22000","amount":4,"baseAmount":5}]
            """;
    private MockRestServiceServer server;
    private HttpReconciliationJournalAdapter adapter;

    @BeforeEach
    void setUp() {
        var builder = RestClient.builder().baseUrl(ROOT);
        server = MockRestServiceServer.bindTo(builder).build();
        adapter = new HttpReconciliationJournalAdapter(builder.build());
    }

    @Test
    void aggregatesOnlyPostedAccountingDateAccountAndSideUsingExactBaseAmounts() {
        String response = "[" + journal(1, "POSTED", "2026-09-10", LINES) + ","
                + journal(2, "DRAFT", "2026-09-10", LINES) + ","
                + journal(3, "APPROVED", "2026-09-10", LINES) + ","
                + journal(4, "POSTED", "2026-09-09", LINES) + ","
                + journal(5, "POSTED", "2026-09-11", LINES) + ","
                + journal(6, "REQUESTED", "2026-09-10", LINES) + "]";
        server.expect(requestTo(RANGE)).andRespond(withSuccess(response, MediaType.APPLICATION_JSON));
        var aggregate = adapter.getJournalDetailAggregateByAccount(DATE, DATE, JournalSide.DEBIT, "11000");
        assertThat(aggregate.getDetailCount()).isEqualTo(2);
        assertThat(aggregate.getTotalAmount()).isEqualByComparingTo("99999999999999999.99");
        server.verify(); // Exactly one range call; no per-header detail requests.
    }

    @Test
    void allAccountAggregateAndCreditAggregatePreserveFilters() {
        String response = "[" + journal(1, "POSTED", "2026-09-10", LINES) + "]";
        server.expect(requestTo(RANGE)).andRespond(withSuccess(response, MediaType.APPLICATION_JSON));
        server.expect(requestTo(RANGE)).andRespond(withSuccess(response, MediaType.APPLICATION_JSON));
        var all = adapter.getJournalDetailAggregate(DATE, DATE, JournalSide.DEBIT);
        assertThat(all.getDetailCount()).isEqualTo(3);
        assertThat(all.getTotalAmount()).isEqualByComparingTo("100000000000000004.99");
        var credits = adapter.getJournalDetailAggregateByAccount(DATE, DATE, JournalSide.CREDIT, "11000");
        assertThat(credits.getDetailCount()).isEqualTo(1);
        assertThat(credits.getTotalAmount()).isEqualByComparingTo("3");
        server.verify();
    }

    @Test
    void mapsItemLevelDetailsIncludingSourceIdentityAndDescriptions() {
        server.expect(requestTo(RANGE)).andRespond(withSuccess("["
                + journal(1, "POSTED", "2026-09-10", LINES) + "]", MediaType.APPLICATION_JSON));
        var details = adapter.getJournalDetailsByAccountCodes(DATE, DATE, List.of("11000"));
        assertThat(details).hasSize(3);
        var first = details.get(0);
        assertThat(first.getId()).isEqualTo(81);
        assertThat(first.getSide()).isEqualTo(JournalSide.DEBIT);
        assertThat(first.getAccountCode()).isEqualTo("11000");
        assertThat(first.getAccountingDate()).isEqualTo(DATE);
        assertThat(first.getSlipNo()).isEqualTo("SYNTHETIC-1");
        assertThat(first.getHeaderDescription()).isEqualTo("source header");
        assertThat(first.getDetailDescription()).isEqualTo("first");
        assertThat(first.getDepartmentCode()).isEqualTo("FIN");
        assertThat(first.getBusinessPartnerCode()).isEqualTo("SYNTHETIC-PARTNER");
        assertThat(first.getAmount()).isEqualByComparingTo("10");
        assertThat(first.getBaseAmount()).isEqualByComparingTo("99999999999999999.90");
        server.verify();
    }

    @Test
    void byIdDetailsUseExplicitProviderRouteAndRejectWrongIdentity() {
        server.expect(requestTo(ROOT + "/api/journals/by-id/1"))
                .andRespond(withSuccess(journal(1, "DRAFT", "2026-09-10", LINES), MediaType.APPLICATION_JSON));
        server.expect(requestTo(ROOT + "/api/journals/by-id/2"))
                .andRespond(withSuccess(journal(1, "POSTED", "2026-09-10", LINES), MediaType.APPLICATION_JSON));
        assertThat(adapter.getJournalDetails(1L)).hasSize(4);
        assertThatThrownBy(() -> adapter.getJournalDetails(2L)).isInstanceOf(IllegalStateException.class);
        server.verify();
    }

    @Test
    void explicitEmptyArrayMeansZeroButInvalidArgumentsDoNotRequestProvider() {
        server.expect(requestTo(RANGE)).andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));
        var result = adapter.getJournalDetailAggregate(DATE, DATE, JournalSide.DEBIT);
        assertThat(result.getDetailCount()).isZero();
        assertThat(result.getTotalAmount()).isZero();
        assertThat(adapter.getJournalDetailsByAccountCodes(DATE, DATE, List.of())).isEmpty();
        assertThatThrownBy(() -> adapter.getJournalDetailAggregate(null, DATE, JournalSide.DEBIT))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> adapter.getJournalDetailAggregate(DATE.plusDays(1), DATE, JournalSide.DEBIT))
                .isInstanceOf(IllegalArgumentException.class);
        server.verify();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "null", "{}", "not-json", "[null]", "[{}]"})
    void missingOrMalformedResponseCannotBecomeZeroBalance(String body) {
        server.expect(requestTo(RANGE)).andRespond(withSuccess(body, MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> adapter.getJournalDetailAggregate(DATE, DATE, JournalSide.DEBIT))
                .isInstanceOf(IllegalStateException.class).hasNoCause();
        server.verify();
    }

    @ParameterizedTest
    @ValueSource(strings = {"null", "[]", "[null]", "[{}]",
            "[{\"id\":1,\"side\":\"OTHER\",\"accountCode\":\"11000\",\"amount\":1}]",
            "[{\"id\":1,\"side\":\"DEBIT\",\"accountCode\":\"11000\"}]"})
    void incompletePostedLinesFailInsteadOfSkippingFinancialData(String lines) {
        server.expect(requestTo(RANGE)).andRespond(withSuccess("["
                + journal(1, "POSTED", "2026-09-10", lines) + "]", MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> adapter.getJournalDetailAggregate(DATE, DATE, JournalSide.DEBIT))
                .isInstanceOf(IllegalStateException.class).hasNoCause();
        server.verify();
    }

    @Test
    void duplicateJournalOrLineIdentityCannotDoubleCount() {
        String entry = journal(1, "POSTED", "2026-09-10", LINES);
        server.expect(requestTo(RANGE)).andRespond(withSuccess("[" + entry + "," + entry + "]", MediaType.APPLICATION_JSON));
        server.expect(requestTo(RANGE)).andRespond(withSuccess("[" + entry + ","
                + journal(2, "POSTED", "2026-09-10", LINES) + "]", MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> adapter.getJournalDetailAggregate(DATE, DATE, JournalSide.DEBIT))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> adapter.getJournalDetailsByAccountCodes(DATE, DATE, List.of("11000")))
                .isInstanceOf(IllegalStateException.class);
        server.verify();
    }

    @ParameterizedTest
    @ValueSource(ints = {204, 300, 301, 302, 303, 304, 307, 308, 400, 401, 403, 404, 500, 503})
    void unsuccessfulOrEmptyFinancialReadNeverReturnsZeroOrLeaksProviderDetails(int status) {
        server.expect(requestTo(RANGE)).andRespond(withStatus(HttpStatus.valueOf(status))
                .header("Location", "http://sensitive-provider.invalid/private")
                .body("sensitive-provider-detail"));
        assertThatThrownBy(() -> adapter.getJournalDetailAggregate(DATE, DATE, JournalSide.DEBIT))
                .isInstanceOf(IllegalStateException.class).hasMessageNotContaining("sensitive-provider").hasNoCause();
        server.verify();
    }

    @ParameterizedTest
    @ValueSource(ints = {300, 301, 302, 303, 304, 307, 308})
    void realTransportDoesNotFollowFinancialReadRedirect(int status) throws Exception {
        var http = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        var followed = new AtomicInteger();
        http.createContext("/api/journals", exchange -> {
            exchange.getResponseHeaders().set("Location", "/unexpected");
            exchange.sendResponseHeaders(status, -1);
            exchange.close();
        });
        http.createContext("/unexpected", exchange -> {
            followed.incrementAndGet();
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
        });
        http.start();
        try {
            var real = new HttpReconciliationJournalAdapter(RestClient.builder(),
                    "http://127.0.0.1:" + http.getAddress().getPort(), "2s", "2s");
            assertThatThrownBy(() -> real.getJournalDetailAggregate(DATE, DATE, JournalSide.DEBIT))
                    .isInstanceOf(IllegalStateException.class).hasMessageContaining("HTTP " + status).hasNoCause();
            assertThat(followed.get()).isZero();
        } finally {
            http.stop(0);
        }
    }

    private static String journal(long id, String status, String date, String lines) {
        // Deliberately different slipDate: accountingDate is the financial selection boundary.
        return "{\"id\":" + id + ",\"slipNo\":\"SYNTHETIC-" + id + "\",\"slipDate\":\"2026-09-01\","
                + "\"accountingDate\":\"" + date + "\",\"status\":\"" + status
                + "\",\"description\":\"source header\",\"lines\":" + lines + "}";
    }
}
