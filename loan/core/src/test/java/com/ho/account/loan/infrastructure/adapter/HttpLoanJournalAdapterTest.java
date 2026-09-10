package com.ho.account.loan.infrastructure.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ho.account.loan.application.port.out.LoanJournalPort.LoanJournalCommand;
import com.ho.account.loan.application.port.out.LoanJournalPort.LoanJournalLine;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.autoconfigure.web.client.RestClientAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.ResponseActions;
import org.springframework.web.client.RestClient;

class HttpLoanJournalAdapterTest {
    private static final String ROOT = "http://journal.invalid";
    private static final String DRAFT = "{\"journalEntryId\":42,\"slipNo\":\"LN-42\",\"status\":\"DRAFT\"}";
    private static final String POSTED = """
            {"id":42,"slipNo":"LN-42","status":"POSTED","currencyCode":"KRW",
             "accountingDate":"2026-09-10","lineageSourceType":"LOAN_DISBURSAL","lineageSourceId":"7"}
            """;
    private MockRestServiceServer server;
    private HttpLoanJournalAdapter adapter;

    @BeforeEach
    void setUp() {
        // Production injects Boot's builder, whose message converters use the configured
        // Jackson mapper. A bare RestClient.builder() instead serializes LocalDate as arrays.
        new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(
                JacksonAutoConfiguration.class, HttpMessageConvertersAutoConfiguration.class,
                RestClientAutoConfiguration.class)).run(context -> {
                    assertThat(context).hasNotFailed();
                    var builder = context.getBean(RestClient.Builder.class).baseUrl(ROOT);
                    server = MockRestServiceServer.bindTo(builder).build();
                    adapter = new HttpLoanJournalAdapter(builder.build());
                });
    }

    @Test
    void createsApprovesPostsThenChecksStoredJournalPreservingActorLineageAndPrecision() {
        create().andExpect(request -> {
            var json = new ObjectMapper()
                    .enable(com.fasterxml.jackson.databind.DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
                    .readTree(((MockClientHttpRequest) request).getBodyAsString());
            assertThat(json.path("slipDate").asText()).isEqualTo("2026-09-10");
            assertThat(json.path("accountingDate").asText()).isEqualTo("2026-09-10");
            assertThat(json.path("currencyCode").asText()).isEqualTo("KRW");
            assertThat(json.path("description").asText()).isEqualTo("Loan disbursal");
            assertThat(json.path("entryType").asText()).isEqualTo("NORMAL");
            assertThat(json.path("createdBy").asText()).isEqualTo("loan-operator");
            assertThat(json.path("auditUser").asText()).isEqualTo("loan-operator");
            assertThat(json.path("lineageSourceType").asText()).isEqualTo("LOAN_DISBURSAL");
            assertThat(json.path("lineageSourceId").asText()).isEqualTo("7");
            assertThat(json.path("lines").size()).isEqualTo(2);
            assertThat(json.path("lines").get(0).path("drcrType").asText()).isEqualTo("DEBIT");
            assertThat(json.path("lines").get(0).path("accountCode").asText()).isEqualTo("131000");
            assertThat(json.path("lines").get(1).path("drcrType").asText()).isEqualTo("CREDIT");
            assertThat(json.path("lines").get(1).path("accountCode").asText()).isEqualTo("101000");
            assertThat(json.path("lines").get(0).path("detailDescription").asText()).isEqualTo("Loan");
            assertThat(json.path("lines").get(1).path("detailDescription").asText()).isEqualTo("Cash");
            for (var line : json.path("lines")) {
                assertThat(line.has("side")).isFalse();
                assertThat(line.has("description")).isFalse();
                assertThat(line.path("amount").decimalValue()).isEqualByComparingTo("123456789.12");
                assertThat(line.path("baseAmount").decimalValue()).isEqualByComparingTo("123456789.12");
            }
        }).andRespond(withSuccess(DRAFT, MediaType.APPLICATION_JSON));
        approve().andRespond(withSuccess());
        post().andRespond(withSuccess());
        lookup().andRespond(withSuccess(POSTED, MediaType.APPLICATION_JSON));
        var result = adapter.post(command());
        assertThat(result.journalEntryId()).isEqualTo(42L);
        assertThat(result.slipNo()).isEqualTo("LN-42");
        server.verify();
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 3})
    void failedStageStopsBeforeAnyLaterRequestOrRetry(int failedStage) {
        var create = create();
        if (failedStage == 0) create.andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));
        else {
            create.andRespond(withSuccess(DRAFT, MediaType.APPLICATION_JSON));
            var approve = approve();
            if (failedStage == 1) approve.andRespond(withStatus(HttpStatus.BAD_REQUEST));
            else {
                approve.andRespond(withSuccess());
                var post = post();
                if (failedStage == 2) post.andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));
                else {
                    post.andRespond(withSuccess());
                    lookup().andRespond(withStatus(HttpStatus.NOT_FOUND));
                }
            }
        }
        assertThatThrownBy(() -> adapter.post(command())).isInstanceOf(RuntimeException.class);
        server.verify();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "null", "{}", "not-json",
            "{\"journalEntryId\":0,\"slipNo\":\"LN-42\",\"status\":\"DRAFT\"}",
            "{\"journalEntryId\":42,\"slipNo\":\" \",\"status\":\"DRAFT\"}",
            "{\"journalEntryId\":42,\"slipNo\":\"LN-42\",\"status\":\"POSTED\"}"})
    void malformedDraftCannotTriggerApproval(String response) {
        create().andRespond(withSuccess(response, MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> adapter.post(command())).isInstanceOf(RuntimeException.class);
        server.verify();
    }

    @ParameterizedTest
    @ValueSource(strings = {"id", "slipNo", "status", "currencyCode", "accountingDate", "lineageSourceType", "lineageSourceId"})
    void rejectsPostedJournalWithMismatchedIdentityStateOrLineage(String field) throws Exception {
        var response = new ObjectMapper().readTree(POSTED);
        ((com.fasterxml.jackson.databind.node.ObjectNode) response).put(field,
                field.equals("id") ? "43" : field.equals("accountingDate") ? "2026-09-11" : "OTHER");
        create().andRespond(withSuccess(DRAFT, MediaType.APPLICATION_JSON));
        approve().andRespond(withSuccess());
        post().andRespond(withSuccess());
        lookup().andRespond(withSuccess(response.toString(), MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> adapter.post(command())).isInstanceOf(RuntimeException.class);
        server.verify();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "null", "{}", "not-json"})
    void emptyOrMalformedPostedLookupCannotReturnSuccess(String response) {
        create().andRespond(withSuccess(DRAFT, MediaType.APPLICATION_JSON));
        approve().andRespond(withSuccess());
        post().andRespond(withSuccess());
        lookup().andRespond(withSuccess(response, MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> adapter.post(command())).isInstanceOf(RuntimeException.class);
        server.verify();
    }

    @ParameterizedTest
    @ValueSource(strings = {"0ms", "-1s", "2147483648ms", "invalid"})
    void rejectsUnboundedOrInvalidTransportTimeouts(String timeout) {
        assertThatThrownBy(() -> new HttpLoanJournalAdapter(RestClient.builder(), "http://provider.invalid", timeout, "5s"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new HttpLoanJournalAdapter(RestClient.builder(), "http://provider.invalid", "2s", timeout))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void transportFailureDoesNotExposeProviderResponse() {
        create().andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("sensitive-provider-detail").contentType(MediaType.TEXT_PLAIN));
        assertThatThrownBy(() -> adapter.post(command()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageNotContaining("sensitive-provider-detail")
                .hasNoCause();
        server.verify();
    }

    private ResponseActions create() {
        return server.expect(requestTo(ROOT + "/api/v1/journals/posting")).andExpect(method(HttpMethod.POST));
    }

    private ResponseActions approve() {
        return server.expect(requestTo(ROOT + "/api/journals/42/approve"))
                .andExpect(method(HttpMethod.POST)).andExpect(header("X-User-ID", "loan-operator"));
    }

    private ResponseActions post() {
        return server.expect(requestTo(ROOT + "/api/journals/42/post"))
                .andExpect(method(HttpMethod.POST)).andExpect(header("X-User-ID", "loan-operator"));
    }

    private ResponseActions lookup() {
        return server.expect(requestTo(ROOT + "/api/journals/LN-42")).andExpect(method(HttpMethod.GET));
    }

    private LoanJournalCommand command() {
        return new LoanJournalCommand(LocalDate.of(2026, 9, 10), "Loan disbursal", "loan-operator",
                "LOAN_DISBURSAL", "7", "KRW", List.of(
                        new LoanJournalLine("DEBIT", "131000", new BigDecimal("123456789.12"), "Loan"),
                        new LoanJournalLine("CREDIT", "101000", new BigDecimal("123456789.12"), "Cash")));
    }
}
