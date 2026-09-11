package com.ho.account.loan.infrastructure.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ho.account.loan.application.port.out.LoanJournalPort.LoanJournalCommand;
import com.ho.account.loan.application.port.out.LoanJournalPort.LoanJournalLine;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import com.sun.net.httpserver.HttpServer;
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
    private static final String STORED_DRAFT = """
            {"id":42,"slipNo":"LN-42","status":"DRAFT","currencyCode":"KRW",
             "slipDate":"2026-09-10","accountingDate":"2026-09-10","description":"Loan disbursal",
             "entryType":"NORMAL","exchangeRate":1,"createdBy":"loan-operator","auditUser":"loan-operator",
             "lineageSourceType":"LOAN_DISBURSAL","lineageSourceId":"7",
             "lines":[
               {"id":81,"side":"DEBIT","accountCode":"131000","amount":123456789.12,
                "baseAmount":123456789.12,"departmentCode":null,"businessPartnerCode":null,"description":"Loan"},
               {"id":82,"side":"CREDIT","accountCode":"101000","amount":123456789.12,
                "baseAmount":123456789.12,"departmentCode":null,"businessPartnerCode":null,"description":"Cash"}]}
            """;
    private static final String POSTED = STORED_DRAFT.replace("DRAFT", "POSTED");
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
            ResponseActions request = switch (stage) {
                case 0 -> create();
                case 1, 4 -> lookup();
                case 2 -> approve();
                default -> post();
            };
            if (stage == failedStage) {
                request.andRespond(withStatus(stage == 2 ? HttpStatus.FORBIDDEN : HttpStatus.INTERNAL_SERVER_ERROR));
            } else {
                request.andRespond(withSuccess(stage == 0 ? DRAFT : stage == 1 ? STORED_DRAFT : "",
                        MediaType.APPLICATION_JSON));
            }
        }
        assertThatThrownBy(() -> adapter.post(command())).isInstanceOf(IllegalStateException.class);
        server.verify();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "null", "{}", "not-json",
            "{\"journalEntryId\":0,\"slipNo\":\"LN-42\",\"status\":\"DRAFT\"}",
            "{\"journalEntryId\":42,\"slipNo\":\" \",\"status\":\"DRAFT\"}",
            "{\"journalEntryId\":42,\"slipNo\":\"LN-42\",\"status\":\"CANCELLED\"}"})
    void malformedDraftCannotTriggerApproval(String response) {
        create().andRespond(withSuccess(response, MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> adapter.post(command())).isInstanceOf(IllegalStateException.class);
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
        assertThatThrownBy(() -> adapter.post(command())).isInstanceOf(IllegalStateException.class);
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
        assertThatThrownBy(() -> adapter.post(command())).isInstanceOf(IllegalStateException.class);
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
    @ValueSource(strings = {"id", "slipNo", "status", "currencyCode", "accountingDate", "slipDate",
            "description", "entryType", "exchangeRate", "createdBy", "auditUser", "lineageSourceType",
            "lineageSourceId", "missingLines", "missingLine", "duplicateLine", "nullLine",
            "line.side", "line.accountCode", "line.amount", "line.baseAmount", "line.description",
            "line.departmentCode", "line.businessPartnerCode", "line.missingAmount", "line.missingBaseAmount"})
    void mismatchedStoredDraftFailsBeforeApprovalOrPosting(String field) throws Exception {
        create().andRespond(withSuccess(DRAFT, MediaType.APPLICATION_JSON));
        lookup().andRespond(withSuccess(changed(STORED_DRAFT, field), MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> adapter.post(command())).isInstanceOf(IllegalStateException.class);
        // No approve/post expectations: any later write makes this regression fail.
        server.verify();
    }

    @ParameterizedTest
    @ValueSource(strings = {"slipDate", "description", "entryType", "exchangeRate", "createdBy", "auditUser",
            "missingLines", "missingLine", "duplicateLine", "nullLine", "line.side", "line.accountCode",
            "line.amount", "line.baseAmount", "line.description", "line.departmentCode",
            "line.businessPartnerCode", "line.missingAmount", "line.missingBaseAmount"})
    void finalPostedFinancialContentMustStillMatch(String field) throws Exception {
        create().andRespond(withSuccess(DRAFT, MediaType.APPLICATION_JSON));
        lookup().andRespond(withSuccess(STORED_DRAFT, MediaType.APPLICATION_JSON));
        approve().andRespond(withSuccess());
        post().andRespond(withSuccess());
        lookup().andRespond(withSuccess(changed(POSTED, field), MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> adapter.post(command())).isInstanceOf(IllegalStateException.class);
        server.verify();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "null", "{}", "not-json"})
    void malformedPreApprovalLookupCannotTriggerFurtherWrites(String response) {
        create().andRespond(withSuccess(DRAFT, MediaType.APPLICATION_JSON));
        lookup().andRespond(withSuccess(response, MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> adapter.post(command())).isInstanceOf(IllegalStateException.class);
        server.verify();
    }

    @Test
    void reusedHundredDraftCannotBeApprovedForTwoHundredRequest() {
        create().andRespond(withSuccess(DRAFT, MediaType.APPLICATION_JSON));
        lookup().andRespond(withSuccess(STORED_DRAFT.replace("123456789.12", "100.00"), MediaType.APPLICATION_JSON));
        var source = command();
        var twoHundred = new LoanJournalCommand(source.accountingDate(), source.description(), source.actor(),
                source.lineageSourceType(), source.lineageSourceId(), source.currencyCode(), source.lines().stream()
                    .map(line -> new LoanJournalLine(line.side(), line.accountCode(), new BigDecimal("200.00"),
                            line.description())).toList());
        assertThatThrownBy(() -> adapter.post(twoHundred)).hasMessageContaining("financial line confirmation");
        server.verify();
    }

    @Test
    void identicalCompletedRetryValidatesWithoutAdditionalApprovalOrPosting() {
        create().andRespond(withSuccess(DRAFT, MediaType.APPLICATION_JSON));
        lookup().andRespond(withSuccess(STORED_DRAFT, MediaType.APPLICATION_JSON));
        approve().andRespond(withSuccess());
        post().andRespond(withSuccess());
        lookup().andRespond(withSuccess(POSTED, MediaType.APPLICATION_JSON));
        create().andRespond(withSuccess(DRAFT.replace("DRAFT", "POSTED"), MediaType.APPLICATION_JSON));
        lookup().andRespond(withSuccess(POSTED, MediaType.APPLICATION_JSON));
        assertThat(adapter.post(command()).journalEntryId()).isEqualTo(42L);
        assertThat(adapter.post(command()).journalEntryId()).isEqualTo(42L);
        server.verify();
    }

    @Test
    void matchingApprovedRetryPostsWithoutDuplicateApproval() {
        create().andRespond(withSuccess(DRAFT.replace("DRAFT", "APPROVED"), MediaType.APPLICATION_JSON));
        lookup().andRespond(withSuccess(STORED_DRAFT.replace("DRAFT", "APPROVED"), MediaType.APPLICATION_JSON));
        post().andRespond(withSuccess());
        lookup().andRespond(withSuccess(POSTED, MediaType.APPLICATION_JSON));
        assertThat(adapter.post(command()).journalEntryId()).isEqualTo(42L);
        server.verify();
    }

    @Test
    void lineOrderAndDecimalScaleDoNotChangeFinancialIdentity() throws Exception {
        var stored = (com.fasterxml.jackson.databind.node.ObjectNode) new ObjectMapper()
                .enable(com.fasterxml.jackson.databind.DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
                .readTree(STORED_DRAFT.replace("123456789.12", "123456789.1200"));
        var lines = stored.withArray("lines");
        var first = lines.remove(0);
        lines.add(first);
        create().andRespond(withSuccess(DRAFT, MediaType.APPLICATION_JSON));
        lookup().andRespond(withSuccess(stored.toString(), MediaType.APPLICATION_JSON));
        approve().andRespond(withSuccess());
        post().andRespond(withSuccess());
        lookup().andRespond(withSuccess(stored.toString().replace("DRAFT", "POSTED"), MediaType.APPLICATION_JSON));
        assertThat(adapter.post(command()).journalEntryId()).isEqualTo(42L);
        server.verify();
    }

    @ParameterizedTest
    @org.junit.jupiter.params.provider.MethodSource("redirectStages")
    void everyRedirectStageFailsWithoutLaterRequestsOrRetry(int status, int failedStage) {
        for (int stage = 0; stage <= failedStage; stage++) {
            ResponseActions request = switch (stage) {
                case 0 -> create();
                case 1, 4 -> lookup();
                case 2 -> approve();
                default -> post();
            };
            if (stage == failedStage) {
                request.andRespond(withStatus(HttpStatus.valueOf(status)).location(java.net.URI.create(ROOT + "/unexpected")));
            } else {
                request.andRespond(withSuccess(stage == 0 ? DRAFT : stage == 1 ? STORED_DRAFT : "",
                        MediaType.APPLICATION_JSON));
            }
        }
        assertThatThrownBy(() -> adapter.post(command())).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("HTTP " + status);
        server.verify();
    }

    static java.util.stream.Stream<org.junit.jupiter.params.provider.Arguments> redirectStages() {
        return java.util.stream.IntStream.of(300, 301, 302, 303, 304, 307, 308).boxed().flatMap(status ->
                java.util.stream.IntStream.range(0, 5).mapToObj(stage ->
                        org.junit.jupiter.params.provider.Arguments.of(status, stage)));
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
                [{"side":"DEBIT","accountCode":"131000","amount":99999999999999999.9000,
                  "baseAmount":99999999999999999.90,"description":"Loan"},
                 {"side":"DEBIT","accountCode":"131000","amount":0.09,
                  "baseAmount":0.0900,"description":"Loan remainder"},
                 {"side":"CREDIT","accountCode":"101000","amount":99999999999999999.9900,
                  "baseAmount":99999999999999999.99,"description":"Cash"}]
                """;
        var source = command();
        var precise = new LoanJournalCommand(source.accountingDate(), source.description(), source.actor(),
                source.lineageSourceType(), source.lineageSourceId(), source.currencyCode(), List.of(
                        new LoanJournalLine("DEBIT", "131000", new BigDecimal("99999999999999999.90"), "Loan"),
                        new LoanJournalLine("DEBIT", "131000", new BigDecimal("0.09"), "Loan remainder"),
                        new LoanJournalLine("CREDIT", "101000", new BigDecimal("99999999999999999.99"), "Cash")));
        create().andRespond(withSuccess(DRAFT, MediaType.APPLICATION_JSON));
        lookup().andRespond(withSuccess(withLines(STORED_DRAFT, lines), MediaType.APPLICATION_JSON));
        approve().andRespond(withSuccess());
        post().andRespond(withSuccess());
        lookup().andRespond(withSuccess(withLines(POSTED, lines), MediaType.APPLICATION_JSON));
        assertThat(adapter.post(precise).journalEntryId()).isEqualTo(42L);
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

    private static String changed(String source, String field) throws Exception {
        var json = (com.fasterxml.jackson.databind.node.ObjectNode) new ObjectMapper().readTree(source);
        var lines = json.withArray("lines");
        var first = (com.fasterxml.jackson.databind.node.ObjectNode) lines.get(0);
        switch (field) {
            case "id" -> json.put("id", 43);
            case "slipDate", "accountingDate" -> json.put(field, "2026-09-11");
            case "exchangeRate" -> json.put(field, new BigDecimal("2"));
            case "missingLines" -> json.remove("lines");
            case "missingLine" -> lines.remove(1);
            case "duplicateLine" -> lines.set(1, first.deepCopy());
            case "nullLine" -> lines.set(1, com.fasterxml.jackson.databind.node.NullNode.instance);
            case "line.amount", "line.baseAmount" -> first.put(field.substring(5), new BigDecimal("100"));
            case "line.missingAmount" -> first.remove("amount");
            case "line.missingBaseAmount" -> first.remove("baseAmount");
            default -> {
                if (field.startsWith("line.")) first.put(field.substring(5), "OTHER");
                else json.put(field, "OTHER");
            }
        }
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
