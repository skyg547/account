package com.ho.account.reporting.infrastructure.external;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class HttpReportingJournalAdapterTest {
    private static final LocalDate DATE = LocalDate.of(2026, 9, 10);
    private static final String ROOT = "http://journal.invalid/api/journals";
    private static final String VIEW = """
            {"id":42,"slipNo":"J-42","slipDate":"2026-09-10","accountingDate":"2026-09-10",
             "status":"POSTED","currencyCode":"KRW","entryType":"NORMAL","description":"Accrual",
             "lineageSourceType":"LOAN","lineageSourceId":"7","lines":[]}
            """;
    private MockRestServiceServer server;
    private HttpReportingJournalAdapter adapter;

    @BeforeEach
    void setup() {
        var builder = RestClient.builder().baseUrl("http://journal.invalid");
        server = MockRestServiceServer.bindTo(builder).build();
        adapter = new HttpReportingJournalAdapter(builder.build());
    }

    @Test
    void summaryRangeAndSlipLookupUseActualJournalViewContract() {
        expect("?startDate=2026-09-10&endDate=2026-09-10", "[" + VIEW + "]");
        expect("/J-42", VIEW);
        var result = adapter.getJournalSummaries(DATE, DATE);
        assertThat(result).hasSize(1);
        var summary = result.get(0);
        assertThat(summary.getId()).isEqualTo(42L);
        assertThat(summary.getSlipNo()).isEqualTo("J-42");
        assertThat(summary.getSlipDate()).isEqualTo(DATE);
        assertThat(summary.getAccountingDate()).isEqualTo(DATE);
        assertThat(summary.getStatus()).isEqualTo("POSTED");
        assertThat(summary.getCurrencyCode()).isEqualTo("KRW");
        assertThat(summary.getLineageSourceType()).isEqualTo("LOAN");
        assertThat(summary.getLineageSourceId()).isEqualTo("7");
        assertThat(adapter.findBySlipNo("J-42")).hasValueSatisfying(value -> assertThat(value.getId()).isEqualTo(42L));
        server.verify();
    }

    @Test
    void missingSlipAndEmptySummaryArrayAreLegitimateAbsence() {
        server.expect(requestTo(ROOT + "/J-42")).andRespond(withStatus(HttpStatus.NOT_FOUND));
        expect("?startDate=2026-09-10&endDate=2026-09-10", "[]");
        assertThat(adapter.findBySlipNo("J-42")).isEmpty();
        assertThat(adapter.getJournalSummaries(DATE, DATE)).isEmpty();
        server.verify();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "null", "{}", "not-json"})
    void malformedSlipIsNotAbsence(String body) {
        expect("/J-42", body);
        assertThatThrownBy(() -> adapter.findBySlipNo("J-42")).isInstanceOf(RuntimeException.class);
        server.verify();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "null", "{}", "[null]", "[{}]"})
    void malformedSummaryCannotProduceEmptyReport(String body) {
        expect("?startDate=2026-09-10&endDate=2026-09-10", body);
        assertThatThrownBy(() -> adapter.getJournalSummaries(DATE, DATE)).isInstanceOf(RuntimeException.class);
        server.verify();
    }

    @Test
    void differentSlipCannotBeReturnedAsRequestedJournal() {
        expect("/J-42", VIEW.replace("J-42", "J-99"));
        assertThatThrownBy(() -> adapter.findBySlipNo("J-42")).isInstanceOf(RuntimeException.class);
        server.verify();
    }

    @ParameterizedTest
    @ValueSource(ints = {302, 500, 503})
    void providerErrorsDoNotBecomeMissingSlips(int status) {
        server.expect(requestTo(ROOT + "/J-42")).andRespond(withStatus(HttpStatus.valueOf(status)).body("sensitive-provider-detail"));
        assertThatThrownBy(() -> adapter.findBySlipNo("J-42"))
                .isInstanceOf(RuntimeException.class).hasMessageNotContaining("sensitive-provider-detail");
        server.verify();
    }

    @Test
    void unavailableDetailAndAggregateContractsFailWithoutFabricatingEndpoints() {
        assertThatThrownBy(() -> adapter.getJournalSummary(42L)).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> adapter.getJournalDetails(42L)).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> adapter.getJournalDetailsByAccountCodes(DATE, DATE, java.util.List.of("101"))).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> adapter.getJournalDetailAggregate(DATE, DATE, com.ho.account.contracts.journal.JournalSide.DEBIT)).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> adapter.getJournalDetailAggregateByAccount(DATE, DATE, com.ho.account.contracts.journal.JournalSide.DEBIT, "101")).isInstanceOf(UnsupportedOperationException.class);
        server.verify();
    }

    @ParameterizedTest
    @ValueSource(strings = {"id", "slipNo", "slipDate", "accountingDate", "status"})
    void incompleteJournalCannotBeReported(String field) throws Exception {
        var node = (com.fasterxml.jackson.databind.node.ObjectNode) new com.fasterxml.jackson.databind.ObjectMapper().readTree(VIEW);
        node.remove(field);
        expect("/J-42", node.toString());
        assertThatThrownBy(() -> adapter.findBySlipNo("J-42")).isInstanceOf(RuntimeException.class);
        server.verify();
    }

    @Test
    void invalidDatesAndBlankSlipDoNotCallProvider() {
        assertThatThrownBy(() -> adapter.getJournalSummaries(DATE.plusDays(1), DATE)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> adapter.getJournalSummaries(null, DATE)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> adapter.findBySlipNo(" ")).isInstanceOf(IllegalArgumentException.class);
        server.verify();
    }

    @Test
    void transportTimeoutIsNotMissingJournalAndDoesNotLeakCause() {
        server.expect(requestTo(ROOT + "/J-42")).andRespond(request -> {
            throw new java.net.SocketTimeoutException("sensitive-provider-detail");
        });
        assertThatThrownBy(() -> adapter.findBySlipNo("J-42"))
                .isInstanceOf(IllegalStateException.class).hasMessageNotContaining("sensitive-provider-detail").hasNoCause();
        server.verify();
    }

    private void expect(String suffix, String body) {
        server.expect(requestTo(ROOT + suffix)).andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));
    }
}
