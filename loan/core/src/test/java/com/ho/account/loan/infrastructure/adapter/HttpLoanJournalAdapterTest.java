package com.ho.account.loan.infrastructure.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ho.account.loan.application.port.out.LoanJournalPort.LoanJournalCommand;
import com.ho.account.loan.application.port.out.LoanJournalPort.LoanJournalLine;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
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
             "accountingDate":"2026-09-10","lineageSourceType":"LOAN_DISBURSAL","lineageSourceId":"7",
             "lines":[{"side":"DEBIT","amount":123456789.12},{"side":"CREDIT","amount":123456789.12}]}
            """;
    private static final String STORED_DRAFT = POSTED.replace("POSTED", "DRAFT");
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
        lookup().andRespond(withSuccess(STORED_DRAFT, MediaType.APPLICATION_JSON));
        approve().andRespond(withSuccess());
        post().andRespond(withSuccess());
        lookup().andRespond(withSuccess(POSTED, MediaType.APPLICATION_JSON));
        var result = adapter.post(command());
        assertThat(result.journalEntryId()).isEqualTo(42L);
        assertThat(result.slipNo()).isEqualTo("LN-42");
        server.verify();
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 3, 4})
    void failedStageStopsBeforeAnyLaterRequestOrRetry(int failedStage) {
        for (int stage = 0; stage <= failedStage; stage++) {
            ResponseActions action = switch (stage) {
                case 0 -> create();
                case 1, 4 -> lookup();
                case 2 -> approve();
                default -> post();
            };
            if (stage == failedStage) action.andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));
            else if (stage == 0) action.andRespond(withSuccess(DRAFT, MediaType.APPLICATION_JSON));
            else if (stage == 1) action.andRespond(withSuccess(STORED_DRAFT, MediaType.APPLICATION_JSON));
            else action.andRespond(withSuccess());
        }
        assertThatThrownBy(() -> adapter.post(command())).isInstanceOf(IllegalStateException.class);
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
        lookup().andRespond(withSuccess(STORED_DRAFT, MediaType.APPLICATION_JSON));
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
        lookup().andRespond(withSuccess(STORED_DRAFT, MediaType.APPLICATION_JSON));
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

    @ParameterizedTest
    @ValueSource(strings = {"id", "slipNo", "status", "currencyCode", "accountingDate", "lineageSourceType", "lineageSourceId"})
    void rejectsMismatchedDraftIdentityStateOrLineageBeforeApproval(String field) throws Exception {
        var response = (com.fasterxml.jackson.databind.node.ObjectNode) new ObjectMapper().readTree(STORED_DRAFT);
        response.put(field, field.equals("id") ? "43" : field.equals("accountingDate") ? "2026-09-11" : "OTHER");
        create().andRespond(withSuccess(DRAFT, MediaType.APPLICATION_JSON));
        lookup().andRespond(withSuccess(response.toString(), MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> adapter.post(command())).isInstanceOf(IllegalStateException.class);
        server.verify(); // Any approve/post or retry is an unexpected request and fails this test.
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "null", "{}", "not-json"})
    void emptyOrMalformedDraftLookupCannotTriggerApproval(String response) {
        create().andRespond(withSuccess(DRAFT, MediaType.APPLICATION_JSON));
        lookup().andRespond(withSuccess(response, MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> adapter.post(command())).isInstanceOf(IllegalStateException.class);
        server.verify();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "null", "[]", "[null]", "[{}]",
            "[{\"side\":\"DEBIT\",\"amount\":123456789.12}]",
            "[{\"side\":\"CREDIT\",\"amount\":123456789.12}]",
            "[{\"side\":null,\"amount\":123456789.12}]",
            "[{\"side\":\"OTHER\",\"amount\":123456789.12}]",
            "[{\"side\":\"DEBIT\",\"amount\":null}]",
            "[{\"side\":\"DEBIT\",\"amount\":-1}]",
            "[{\"side\":\"DEBIT\",\"amount\":0}]",
            "[{\"side\":\"DEBIT\",\"amount\":123456789.13},{\"side\":\"CREDIT\",\"amount\":123456789.12}]",
            "[{\"side\":\"DEBIT\",\"amount\":123456789.12},{\"side\":\"CREDIT\",\"amount\":123456789.11}]"})
    void invalidDraftLinesCannotTriggerApproval(String lines) throws Exception {
        create().andRespond(withSuccess(DRAFT, MediaType.APPLICATION_JSON));
        lookup().andRespond(withSuccess(withLines(STORED_DRAFT, lines), MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> adapter.post(command())).isInstanceOf(IllegalStateException.class);
        server.verify();
    }

    @Test
    void missingDraftLinesCannotTriggerApproval() throws Exception {
        var response = (com.fasterxml.jackson.databind.node.ObjectNode) new ObjectMapper().readTree(STORED_DRAFT);
        response.remove("lines");
        create().andRespond(withSuccess(DRAFT, MediaType.APPLICATION_JSON));
        lookup().andRespond(withSuccess(response.toString(), MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> adapter.post(command())).isInstanceOf(IllegalStateException.class);
        server.verify();
    }

    @Test
    void requestedTwoHundredButProviderOneHundredCannotTriggerApprovalOrPosting() {
        create().andRespond(withSuccess(DRAFT, MediaType.APPLICATION_JSON));
        lookup().andRespond(withSuccess(STORED_DRAFT.replace("123456789.12", "100"), MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> adapter.post(command("200"))).isInstanceOf(IllegalStateException.class);
        server.verify();
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1"})
    void balancedNegativeOrZeroExtraLinesAreStillRejected(String amount) throws Exception {
        // Keep totals equal to the requested amount so rejecting individual invalid lines is essential.
        String positiveAmount = new BigDecimal("123456789.12").subtract(new BigDecimal(amount)).toPlainString();
        String lines = "[{\"side\":\"DEBIT\",\"amount\":" + positiveAmount + "},"
                + "{\"side\":\"CREDIT\",\"amount\":" + positiveAmount + "},"
                + "{\"side\":\"DEBIT\",\"amount\":" + amount + "},{\"side\":\"CREDIT\",\"amount\":" + amount + "}]";
        create().andRespond(withSuccess(DRAFT, MediaType.APPLICATION_JSON));
        lookup().andRespond(withSuccess(withLines(STORED_DRAFT, lines), MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> adapter.post(command())).isInstanceOf(IllegalStateException.class);
        server.verify();
    }

    @Test
    void sumsMultipleLinesExactlyAndIgnoresScaleDifferences() throws Exception {
        String lines = """
                [{"side":"DEBIT","amount":99999999999999999.90},
                 {"side":"DEBIT","amount":0.09},
                 {"side":"CREDIT","amount":99999999999999999.9900}]
                """;
        create().andRespond(withSuccess(DRAFT, MediaType.APPLICATION_JSON));
        lookup().andRespond(withSuccess(withLines(STORED_DRAFT, lines), MediaType.APPLICATION_JSON));
        approve().andRespond(withSuccess());
        post().andRespond(withSuccess());
        lookup().andRespond(withSuccess(withLines(POSTED, lines), MediaType.APPLICATION_JSON));
        assertThat(adapter.post(command("99999999999999999.99")).journalEntryId()).isEqualTo(42L);
        server.verify();
    }

    @Test
    void amountChangedAfterPostingCannotReturnSuccess() {
        create().andRespond(withSuccess(DRAFT, MediaType.APPLICATION_JSON));
        lookup().andRespond(withSuccess(STORED_DRAFT, MediaType.APPLICATION_JSON));
        approve().andRespond(withSuccess());
        post().andRespond(withSuccess());
        lookup().andRespond(withSuccess(POSTED.replace("123456789.12", "100"), MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> adapter.post(command())).isInstanceOf(IllegalStateException.class);
        server.verify();
    }

    @ParameterizedTest
    @ValueSource(ints = {300, 301, 302, 303, 304, 307, 308})
    void productionTransportNeverFollowsDraftLookupRedirect(int status) throws Exception {
        HttpServer http = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        var creates = new AtomicInteger();
        var lookups = new AtomicInteger();
        var unexpected = new AtomicInteger();
        http.createContext("/", exchange -> {
            String path = exchange.getRequestURI().getPath();
            String body;
            int responseStatus;
            if (path.equals("/api/v1/journals/posting")) {
                creates.incrementAndGet();
                responseStatus = 200;
                body = DRAFT;
            } else if (path.equals("/api/journals/LN-42")) {
                lookups.incrementAndGet();
                responseStatus = status;
                body = STORED_DRAFT;
                exchange.getResponseHeaders().set("Location", "/redirect-target");
            } else {
                unexpected.incrementAndGet();
                responseStatus = 200;
                body = STORED_DRAFT;
            }
            exchange.getRequestBody().readAllBytes();
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(responseStatus, responseStatus == 304 ? -1 : bytes.length);
            if (responseStatus != 304) exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        http.start();
        try {
            var real = new HttpLoanJournalAdapter(RestClient.builder(),
                    "http://127.0.0.1:" + http.getAddress().getPort(), "2s", "2s");
            assertThatThrownBy(() -> real.post(command())).isInstanceOf(IllegalStateException.class);
            assertThat(creates.get()).isEqualTo(1);
            assertThat(lookups.get()).isEqualTo(1);
            assertThat(unexpected.get()).isZero();
        } finally {
            http.stop(0);
        }
    }

    private static String withLines(String response, String lines) throws Exception {
        var mapper = new ObjectMapper().enable(com.fasterxml.jackson.databind.DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);
        var json = (com.fasterxml.jackson.databind.node.ObjectNode) mapper.readTree(response);
        json.set("lines", mapper.readTree(lines));
        return json.toString();
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
        return command("123456789.12");
    }

    private LoanJournalCommand command(String amount) {
        return new LoanJournalCommand(LocalDate.of(2026, 9, 10), "Loan disbursal", "loan-operator",
                "LOAN_DISBURSAL", "7", "KRW", List.of(
                        new LoanJournalLine("DEBIT", "131000", new BigDecimal(amount), "Loan"),
                        new LoanJournalLine("CREDIT", "101000", new BigDecimal(amount), "Cash")));
    }
}
