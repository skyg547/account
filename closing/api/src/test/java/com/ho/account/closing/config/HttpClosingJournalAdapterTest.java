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
    void approveThenPostCarryActorInOrder() {
        server.expect(requestTo(BASE + "/api/journals/42/approve"))
                .andExpect(method(HttpMethod.POST)).andExpect(header("X-User-ID", "operator"))
                .andRespond(withSuccess());
        server.expect(requestTo(BASE + "/api/journals/42/post"))
                .andExpect(method(HttpMethod.POST)).andExpect(header("X-User-ID", "operator"))
                .andRespond(withSuccess());
        adapter.approveAndPost(42L, " operator ");
    }

    @Test
    void rejectedApprovalDoesNotPostOrRetryAndDoesNotLeakResponseBody() {
        server.expect(requestTo(BASE + "/api/journals/42/approve"))
                .andRespond(withStatus(HttpStatus.FORBIDDEN).body("sensitive-fixture-detail"));
        assertThatThrownBy(() -> adapter.approveAndPost(42L, "operator"))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("approval")
                .hasMessageNotContaining("sensitive-fixture-detail").hasNoCause();
    }

    @Test
    void postingFailureAfterApprovalReportsPartialProgressWithoutRetry() {
        server.expect(requestTo(BASE + "/api/journals/42/approve")).andRespond(withSuccess());
        server.expect(requestTo(BASE + "/api/journals/42/post")).andRespond(withServerError());
        assertThatThrownBy(() -> adapter.approveAndPost(42L, "operator"))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("after approval");
    }

    @Test
    void invalidInputsAndUnavailableQueriesFailWithoutRemoteCalls() {
        assertThatThrownBy(() -> adapter.getJournalSummaries(DATE.plusDays(1), DATE)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> adapter.findBySlipNo(" ")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> adapter.approveAndPost(0L, "operator")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> adapter.approveAndPost(42L, " ")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> adapter.getJournalSummary(42L)).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> adapter.getJournalDetails(42L)).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> adapter.getJournalDetailsByAccountCodes(DATE, DATE, List.of("11000"))).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> adapter.getJournalDetailAggregate(DATE, DATE, JournalSide.DEBIT)).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> adapter.getJournalDetailAggregateByAccount(DATE, DATE, JournalSide.DEBIT, "11000")).isInstanceOf(UnsupportedOperationException.class);
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
