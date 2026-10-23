package com.ho.account.closing.config;

import com.ho.account.closing.infrastructure.external.HttpClosingJournalAdapter;

import com.ho.account.contracts.journal.JournalEntryCommand;
import com.ho.account.contracts.journal.JournalLineCommand;
import com.ho.account.contracts.journal.JournalSide;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
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

class HttpClosingJournalAdapterTest {
    private static final String BASE = "http://journal-ledger.test";
    private static final LocalDate DATE = LocalDate.of(2026, 9, 10);
    private static final String VIEW = """
            {"id":42,"slipNo":"CLOSE-42","slipDate":"2026-09-10",
             "accountingDate":"2026-09-10","description":"closing","entryType":"ADJUSTMENT",
             "status":"DRAFT","currencyCode":"KRW","lineageSourceType":"CLOSING",
             "lineageSourceId":"42","details":[]}
            """;
    private MockRestServiceServer server;
    private HttpClosingJournalAdapter adapter;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = spy(RestClient.builder());
        server = MockRestServiceServer.bindTo(builder).build();
        // Preserve the mock transport when production configures its bounded timeouts.
        doReturn(builder).when(builder).requestFactory(any(ClientHttpRequestFactory.class));
        adapter = new HttpClosingJournalAdapter(builder, BASE, "2s", "5s");
    }

    @AfterEach
    void verifyRequests() { server.verify(); }

    @Test
    void listAndSlipLookupMapProviderResponseIncludingLineage() {
        server.expect(requestTo(BASE + "/api/journals?startDate=2026-09-10&endDate=2026-09-10"))
                .andExpect(method(HttpMethod.GET)).andRespond(withSuccess("[" + VIEW + "]", MediaType.APPLICATION_JSON));
        server.expect(requestTo(BASE + "/api/journals/CLOSE-42"))
                .andRespond(withSuccess(VIEW, MediaType.APPLICATION_JSON));
        var summary = adapter.getJournalSummaries(DATE, DATE).get(0);
        assertThat(summary.getId()).isEqualTo(42L);
        assertThat(summary.getAccountingDate()).isEqualTo(DATE);
        assertThat(summary.getCurrencyCode()).isEqualTo("KRW");
        assertThat(summary.getLineageSourceType()).isEqualTo("CLOSING");
        assertThat(summary.getLineageSourceId()).isEqualTo("42");
        assertThat(adapter.findBySlipNo("CLOSE-42")).get()
                .usingRecursiveComparison().isEqualTo(summary);
    }

    @Test
    void onlyExplicitNotFoundMeansMissingSlip() {
        server.expect(requestTo(BASE + "/api/journals/missing")).andRespond(withResourceNotFound());
        server.expect(requestTo(BASE + "/api/journals/unavailable")).andRespond(withServerError());
        server.expect(requestTo(BASE + "/api/journals/empty")).andRespond(withSuccess());
        assertThat(adapter.findBySlipNo("missing")).isEmpty();
        assertThatThrownBy(() -> adapter.findBySlipNo("unavailable")).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> adapter.findBySlipNo("empty")).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void emptyListIsValidButMissingBodyAndMismatchedSlipFail() {
        String uri = BASE + "/api/journals?startDate=2026-09-10&endDate=2026-09-10";
        server.expect(requestTo(uri)).andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));
        server.expect(requestTo(uri)).andRespond(withSuccess());
        server.expect(requestTo(BASE + "/api/journals/OTHER")).andRespond(withSuccess(VIEW, MediaType.APPLICATION_JSON));
        assertThat(adapter.getJournalSummaries(DATE, DATE)).isEmpty();
        assertThatThrownBy(() -> adapter.getJournalSummaries(DATE, DATE)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> adapter.findBySlipNo("OTHER")).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void draftPostingPreservesDecimalAmountsDeterministicSlipAndLineage() {
        server.expect(requestTo(BASE + "/api/v1/journals/posting"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.slipNo").value("CLOSE-42"))
                .andExpect(jsonPath("$.lineageSourceId").value("42"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("123456789012345.6789")))
                .andRespond(withSuccess("{\"journalEntryId\":42,\"slipNo\":\"CLOSE-42\",\"status\":\"DRAFT\"}", MediaType.APPLICATION_JSON));
        var result = adapter.createDraftEntry(command());
        assertThat(result.journalEntryId()).isEqualTo(42L);
        assertThat(result.slipNo()).isEqualTo("CLOSE-42");
        assertThat(result.status()).isEqualTo("DRAFT");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "{}", "{\"journalEntryId\":0,\"slipNo\":\"CLOSE-42\",\"status\":\"DRAFT\"}"})
    void postingNeverFabricatesSuccessFromMissingOrInvalidResponse(String body) {
        server.expect(requestTo(BASE + "/api/v1/journals/posting"))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> adapter.createDraftEntry(command())).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void remoteApprovalAndPostingRejectBeforeAnyHttpRequest() {
        // Journal requires maker request, a distinct checker, and a poster. Closing has no
        // trusted service-principal workflow for these transitions.
        assertThatThrownBy(() -> adapter.approveAndPost(42L, "operator"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("auto-post");
    }

    @Test
    void redirectedQueryCannotBeAcceptedAsSuccessfulFinancialContent() {
        server.expect(requestTo(BASE + "/api/journals/CLOSE-42"))
                .andRespond(withStatus(HttpStatus.FOUND).body(VIEW).contentType(MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> adapter.findBySlipNo("CLOSE-42"))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("HTTP 302");
    }

    @Test
    void actualHttpTransportDoesNotFollowGetRedirects() throws Exception {
        var forwardedRequests = new java.util.concurrent.atomic.AtomicInteger();
        var http = com.sun.net.httpserver.HttpServer.create(new java.net.InetSocketAddress("127.0.0.1", 0), 0);
        http.createContext("/api/journals/CLOSE-42", exchange -> {
            exchange.getResponseHeaders().set("Location", "/redirected");
            exchange.sendResponseHeaders(302, -1);
            exchange.close();
        });
        http.createContext("/redirected", exchange -> {
            forwardedRequests.incrementAndGet();
            byte[] body = VIEW.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            try (var output = exchange.getResponseBody()) { output.write(body); }
        });
        http.start();
        try {
            var direct = new HttpClosingJournalAdapter(RestClient.builder(),
                    "http://127.0.0.1:" + http.getAddress().getPort(), "2s", "2s");
            assertThatThrownBy(() -> direct.findBySlipNo("CLOSE-42"))
                    .isInstanceOf(IllegalStateException.class).hasMessageContaining("HTTP 302");
            assertThat(forwardedRequests.get()).isZero();
        } finally {
            http.stop(0);
        }
    }

    @Test
    void invalidInputsAndUnavailableQueriesFailWithoutRemoteCalls() {
        assertThatThrownBy(() -> adapter.getJournalSummaries(DATE.plusDays(1), DATE)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> adapter.findBySlipNo(" ")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> adapter.approveAndPost(0L, "operator")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> adapter.approveAndPost(42L, " ")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> adapter.getJournalSummary(0L)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> adapter.getJournalDetails(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> adapter.getJournalDetailsByAccountCodes(DATE, DATE, List.of("11000"))).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> adapter.getJournalDetailAggregate(DATE, DATE, JournalSide.DEBIT)).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> adapter.getJournalDetailAggregateByAccount(DATE, DATE, JournalSide.DEBIT, "11000")).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void idAndDetailsPreserveClosingRerunContentWithoutDecimalLoss() {
        String view = VIEW.replace("\"details\":[]", """
                "lines":[{"id":101,"side":"CREDIT","accountCode":"21000",
                "amount":123456789012345.67,"baseAmount":123456789012345.67,
                "departmentCode":"D1","businessPartnerCode":"BP1","description":"provision"}]
                """);
        server.expect(requestTo(BASE + "/api/journals/by-id/42"))
                .andRespond(withSuccess(view, MediaType.APPLICATION_JSON));
        server.expect(requestTo(BASE + "/api/journals/by-id/42"))
                .andRespond(withSuccess(view, MediaType.APPLICATION_JSON));
        assertThat(adapter.getJournalSummary(42L).getLineageSourceId()).isEqualTo("42");
        var line = adapter.getJournalDetails(42L).get(0);
        assertThat(line.getAmount()).isEqualByComparingTo("123456789012345.67");
        assertThat(line.getBaseAmount()).isEqualByComparingTo("123456789012345.67");
        assertThat(line.getSide()).isEqualTo(JournalSide.CREDIT);
        assertThat(line.getDepartmentCode()).isEqualTo("D1");
        assertThat(line.getBusinessPartnerCode()).isEqualTo("BP1");
        assertThat(line.getDetailDescription()).isEqualTo("provision");
        assertThat(line.getSlipNo()).isEqualTo("CLOSE-42");
    }

    @Test
    void idLookupRejectsWrongIdMissingLinesAndProviderFailure() {
        server.expect(requestTo(BASE + "/api/journals/by-id/43"))
                .andRespond(withSuccess(VIEW, MediaType.APPLICATION_JSON));
        server.expect(requestTo(BASE + "/api/journals/by-id/42"))
                .andRespond(withSuccess(VIEW, MediaType.APPLICATION_JSON));
        server.expect(requestTo(BASE + "/api/journals/by-id/42"))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR).body("sensitive-fixture-detail"));
        assertThatThrownBy(() -> adapter.getJournalSummary(43L))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("different journal ID");
        assertThatThrownBy(() -> adapter.getJournalDetails(42L))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("no journal lines");
        assertThatThrownBy(() -> adapter.getJournalDetails(42L))
                .isInstanceOf(IllegalStateException.class).hasNoCause()
                .hasMessageNotContaining("sensitive-fixture-detail");
    }

    @ParameterizedTest
    @ValueSource(strings = {"0ms", "-1s", "2147483648ms", "PT0.000001S", "garbage", ""})
    void rejectsUnboundedOrInvalidTimeouts(String timeout) {
        assertThatThrownBy(() -> new HttpClosingJournalAdapter(RestClient.builder(), BASE, timeout, "5s"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new HttpClosingJournalAdapter(RestClient.builder(), BASE, "2s", timeout))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void acceptsFiniteTimeoutBoundariesAndIsoNotation() {
        assertThatCode(() -> new HttpClosingJournalAdapter(RestClient.builder(), BASE, "1ms", "PT5S")).doesNotThrowAnyException();
        assertThatCode(() -> new HttpClosingJournalAdapter(RestClient.builder(), BASE,
                Duration.ofMillis(1), Duration.ofMillis(Integer.MAX_VALUE))).doesNotThrowAnyException();
    }

    private JournalEntryCommand command() {
        BigDecimal amount = new BigDecimal("123456789012345.6789");
        return new JournalEntryCommand(DATE, DATE, "closing", "ADJUSTMENT", "KRW", BigDecimal.ONE,
                "operator", "operator", "CLOSING", "42", "CLOSE-42", List.of(
                new JournalLineCommand("DEBIT", "11000", amount, amount, null, null, "debit"),
                new JournalLineCommand("CREDIT", "21000", amount, amount, null, null, "credit")));
    }
}
