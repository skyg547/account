package com.ho.account.deposit.infrastructure.adapter.out.external;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ho.account.contracts.journal.JournalEntryCommand;
import com.ho.account.contracts.journal.JournalLineCommand;
import com.sun.net.httpserver.HttpServer;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicInteger;
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

class HttpDepositJournalPostingAdapterTest {
    private static final String ROOT = "http://journal.invalid";
    private static final String RESULT = "{\"journalEntryId\":42,\"slipNo\":\"DEP-42\",\"status\":\"DRAFT\"}";
    private static final String VIEW = """
            {"id":42,"slipNo":"DEP-42","status":"DRAFT","slipDate":"2026-09-10",
             "accountingDate":"2026-09-10","currencyCode":"KRW","exchangeRate":1,
             "description":"Deposit opening","entryType":"NORMAL","createdBy":"deposit-operator",
             "auditUser":"deposit-operator","lineageSourceType":"DEPOSIT_OPENING","lineageSourceId":"7",
             "lines":[{"side":"DEBIT","accountCode":"10100","amount":123456789.1234,
             "baseAmount":123456789.1234,"departmentCode":"D1","businessPartnerCode":"C1","description":"Cash"},
             {"side":"CREDIT","accountCode":"20200","amount":123456789.1234,
             "baseAmount":123456789.1234,"departmentCode":"D1","businessPartnerCode":"C1","description":"Deposit"}]}
            """;
    private MockRestServiceServer server;
    private HttpDepositJournalPostingAdapter adapter;

    @BeforeEach
    void setUp() {
        new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(JacksonAutoConfiguration.class,
                HttpMessageConvertersAutoConfiguration.class, RestClientAutoConfiguration.class)).run(context -> {
            var builder = context.getBean(RestClient.Builder.class).baseUrl(ROOT);
            server = MockRestServiceServer.bindTo(builder).build();
            adapter = new HttpDepositJournalPostingAdapter(builder.build());
        });
    }

    @ParameterizedTest
    @ValueSource(ints = {302, 303, 307, 308})
    void productionTransportRejectsLookupRedirectBeforeAcknowledgingDraft(int status) throws Exception {
        HttpServer provider = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        AtomicInteger requests = new AtomicInteger();
        AtomicInteger followed = new AtomicInteger();
        provider.createContext("/", exchange -> {
            requests.incrementAndGet();
            String path = exchange.getRequestURI().getPath();
            if (path.equals("/api/v1/journals/posting") || path.equals("/redirect-target")) {
                if (path.equals("/redirect-target")) followed.incrementAndGet();
                byte[] body = (path.equals("/redirect-target") ? VIEW : RESULT).getBytes(StandardCharsets.UTF_8);
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
            var remote = new HttpDepositJournalPostingAdapter(RestClient.builder(),
                    "http://127.0.0.1:" + provider.getAddress().getPort(),
                    Duration.ofSeconds(2), Duration.ofSeconds(2));
            // The POST succeeds, but a redirected verification must leave the outbox unacknowledged.
            assertThatThrownBy(() -> remote.createDraftEntry(command())).isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("HTTP " + status).hasMessageContaining("verify remote state").hasNoCause();
            assertThat(requests.get()).isEqualTo(2);
            assertThat(followed.get()).isZero();
        } finally {
            provider.stop(0);
        }
    }

    @Test
    void createsVerifiedDraftWithExactDateActorLineageAndDecimalWireContract() {
        create().andExpect(request -> {
            var json = new ObjectMapper().enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
                    .readTree(((MockClientHttpRequest) request).getBodyAsString());
            assertThat(json.path("slipDate").asText()).isEqualTo("2026-09-10");
            assertThat(json.path("accountingDate").asText()).isEqualTo("2026-09-10");
            assertThat(json.path("createdBy").asText()).isEqualTo("deposit-operator");
            assertThat(json.path("auditUser").asText()).isEqualTo("deposit-operator");
            assertThat(json.path("lineageSourceType").asText()).isEqualTo("DEPOSIT_OPENING");
            assertThat(json.path("lineageSourceId").asText()).isEqualTo("7");
            assertThat(json.path("lines").get(0).path("drcrType").asText()).isEqualTo("DEBIT");
            assertThat(json.path("lines").get(1).path("drcrType").asText()).isEqualTo("CREDIT");
            for (var line : json.path("lines")) {
                assertThat(line.path("amount").decimalValue()).isEqualByComparingTo("123456789.1234");
                assertThat(line.path("baseAmount").decimalValue()).isEqualByComparingTo("123456789.1234");
                assertThat(line.path("detailDescription").asText()).isNotBlank();
                assertThat(line.has("side")).isFalse();
                assertThat(line.has("description")).isFalse();
            }
        }).andRespond(withSuccess(RESULT, MediaType.APPLICATION_JSON));
        lookup().andRespond(withSuccess(VIEW, MediaType.APPLICATION_JSON));
        var result = adapter.createDraftEntry(command());
        assertThat(result.journalEntryId()).isEqualTo(42L);
        assertThat(result.slipNo()).isEqualTo("DEP-42");
        assertThat(result.status()).isEqualTo("DRAFT");
        server.verify();
    }

    @ParameterizedTest
    @ValueSource(strings = {"REQUESTED", "APPROVED", "POSTED"})
    void retryAcknowledgesExistingJournalOnlyAfterVerifyingItsContents(String status) {
        create().andRespond(withSuccess(RESULT.replace("DRAFT", status), MediaType.APPLICATION_JSON));
        lookup().andRespond(withSuccess(VIEW.replace("DRAFT", status).replace("\"auditUser\":\"deposit-operator\"", "\"auditUser\":\"approving-operator\""), MediaType.APPLICATION_JSON));
        assertThat(adapter.createDraftEntry(command()).status()).isEqualTo(status);
        server.verify();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "null", "{}", "not-json",
            "{\"journalEntryId\":0,\"slipNo\":\"DEP-42\",\"status\":\"DRAFT\"}",
            "{\"journalEntryId\":42,\"slipNo\":\" \",\"status\":\"DRAFT\"}",
            "{\"journalEntryId\":42,\"slipNo\":\"DEP-42\",\"status\":\"REJECTED\"}"})
    void malformedDraftResponseNeverReportsSuccess(String body) {
        create().andRespond(withSuccess(body, MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> adapter.createDraftEntry(command())).isInstanceOf(IllegalStateException.class);
        server.verify();
    }

    @ParameterizedTest
    @ValueSource(strings = {"id", "slipNo", "status", "slipDate", "accountingDate", "currencyCode",
            "lineageSourceType", "lineageSourceId", "createdBy", "entryType"})
    void rejectsDifferentStoredIdentityStateOrLineage(String field) throws Exception {
        ObjectNode json = (ObjectNode) new ObjectMapper().readTree(VIEW);
        json.put(field, field.equals("id") ? "43" : field.endsWith("Date") ? "2026-09-11" : "OTHER");
        create().andRespond(withSuccess(RESULT, MediaType.APPLICATION_JSON));
        lookup().andRespond(withSuccess(json.toString(), MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> adapter.createDraftEntry(command())).isInstanceOf(IllegalStateException.class);
        server.verify();
    }

    @ParameterizedTest
    @ValueSource(strings = {"amount", "baseAmount", "side", "accountCode", "departmentCode", "businessPartnerCode", "description"})
    void lineageDuplicateWithDifferentFinancialLineCannotBeAcknowledged(String field) throws Exception {
        ObjectNode json = (ObjectNode) new ObjectMapper().enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS).readTree(VIEW);
        ((ObjectNode) json.path("lines").get(0)).put(field,
                field.equals("amount") || field.equals("baseAmount") ? "123456789.1235" : "OTHER");
        create().andRespond(withSuccess(RESULT, MediaType.APPLICATION_JSON));
        lookup().andRespond(withSuccess(json.toString(), MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> adapter.createDraftEntry(command())).isInstanceOf(IllegalStateException.class);
        server.verify();
    }

    @Test
    void providerLineOrderAndDecimalScaleDoNotChangeJournalIdentity() throws Exception {
        ObjectNode json = (ObjectNode) new ObjectMapper().enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS).readTree(VIEW);
        var lines = json.putArray("lines");
        var original = new ObjectMapper().enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS).readTree(VIEW).path("lines");
        lines.add(original.get(1));
        lines.add(original.get(0));
        ((ObjectNode) lines.get(0)).put("amount", new BigDecimal("123456789.123400"));
        create().andRespond(withSuccess(RESULT, MediaType.APPLICATION_JSON));
        lookup().andRespond(withSuccess(json.toString(), MediaType.APPLICATION_JSON));
        assertThat(adapter.createDraftEntry(command()).journalEntryId()).isEqualTo(42L);
        server.verify();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "null", "{}", "not-json"})
    void emptyOrMalformedReadbackNeverAcknowledgesRemoteWrite(String body) {
        create().andRespond(withSuccess(RESULT, MediaType.APPLICATION_JSON));
        lookup().andRespond(withSuccess(body, MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> adapter.createDraftEntry(command())).isInstanceOf(IllegalStateException.class);
        server.verify();
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1})
    void failedWriteOrVerificationStopsWithoutRetryAndSanitizesException(int stage) {
        if (stage == 1) create().andRespond(withSuccess(RESULT, MediaType.APPLICATION_JSON));
        (stage == 0 ? create() : lookup()).andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("sensitive-provider-detail").contentType(MediaType.TEXT_PLAIN));
        assertThatThrownBy(() -> adapter.createDraftEntry(command())).isInstanceOf(IllegalStateException.class)
                .hasMessageNotContaining("sensitive-provider-detail").hasNoCause();
        server.verify();
    }

    @Test
    void unsupportedApprovalFailsWithoutPretendingToPost() {
        assertThatThrownBy(() -> adapter.approveAndPost(42L, "deposit-operator"))
                .isInstanceOf(UnsupportedOperationException.class);
        server.verify();
    }

    @ParameterizedTest
    @ValueSource(strings = {"0ms", "-1s", "2147483648ms", "invalid"})
    void rejectsUnboundedTimeouts(String timeout) {
        assertThatThrownBy(() -> new HttpDepositJournalPostingAdapter(RestClient.builder(), ROOT, timeout, "5s"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new HttpDepositJournalPostingAdapter(RestClient.builder(), ROOT, "2s", timeout))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private ResponseActions create() {
        return server.expect(requestTo(ROOT + "/api/v1/journals/posting")).andExpect(method(HttpMethod.POST)).andExpect(header("X-User-ID", "deposit-operator"));
    }
    private ResponseActions lookup() {
        return server.expect(requestTo(ROOT + "/api/journals/DEP-42")).andExpect(method(HttpMethod.GET));
    }
    private JournalEntryCommand command() {
        BigDecimal amount = new BigDecimal("123456789.1234");
        return new JournalEntryCommand(LocalDate.of(2026, 9, 10), LocalDate.of(2026, 9, 10),
                "Deposit opening", "NORMAL", "KRW", BigDecimal.ONE, "deposit-operator", "deposit-operator",
                "DEPOSIT_OPENING", "7", List.of(
                new JournalLineCommand("DEBIT", "10100", amount, amount, "D1", "C1", "Cash"),
                new JournalLineCommand("CREDIT", "20200", amount, amount, "D1", "C1", "Deposit")));
    }
}
