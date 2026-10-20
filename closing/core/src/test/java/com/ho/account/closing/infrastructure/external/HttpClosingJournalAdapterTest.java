package com.ho.account.closing.infrastructure.external;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ho.account.closing.application.port.out.ClosingJournalEntryCommand;
import com.ho.account.closing.application.port.out.ClosingJournalLineCommand;
import com.ho.account.closing.application.port.out.ClosingJournalSide;
import com.ho.account.contracts.journal.JournalEntryCommand;
import com.ho.account.contracts.journal.JournalLineCommand;
import com.sun.net.httpserver.HttpServer;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.web.client.RestClient;

class HttpClosingJournalAdapterTest {
    private static final LocalDate DATE = LocalDate.of(2026, 9, 10);
    private static final String CREATE = "/api/v1/journals/posting";
    private static final String APPROVE = "/api/journals/42/approve";
    private static final String POST = "/api/journals/42/post";
    private static final String LOOKUP = "/api/journals/CL-42";
    private static final String DETAIL = "/api/journals/by-id/42";
    private static final String SUMMARIES = "/api/journals";
    private static final String DRAFT = "{\"journalEntryId\":42,\"slipNo\":\"CL-42\",\"status\":\"DRAFT\"}";
    private static final String VIEW = """
            {"id":42,"slipNo":"CL-42","slipDate":"2026-09-10","accountingDate":"2026-09-10",
             "status":"DRAFT","entryType":"NORMAL","currencyCode":"KRW",
             "lineageSourceType":"CLOSING","lineageSourceId":"20260910",
             "lines":[{"id":4201,"side":"CREDIT","accountCode":"41000",
                       "amount":1000.00,"baseAmount":1000.00,
                       "departmentCode":null,"businessPartnerCode":null,
                       "description":"Source revenue"}]}
            """;
    private HttpServer server;
    private HttpClosingJournalAdapter adapter;
    private final List<Received> requests = new CopyOnWriteArrayList<>();
    private volatile String failedPath;
    private volatile int failureStatus;
    private volatile String viewStatus = "DRAFT";
    private volatile String delayedPath;
    private volatile String createResponse = DRAFT;
    private volatile boolean persistCreatedDraft;
    private volatile String createdView;

    @BeforeEach
    void setUp() throws Exception {
        // A real loopback server exercises the production request factory, including automatic redirects.
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            String path = exchange.getRequestURI().getPath();
            String requestBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            requests.add(new Received(path, exchange.getRequestMethod(),
                    exchange.getRequestHeaders().getFirst("X-User-ID"),
                    exchange.getRequestHeaders().getFirst("X-Auth-User"),
                    exchange.getRequestHeaders().getFirst("X-Auth-Roles"),
                    requestBody));
            if (persistCreatedDraft && path.equals(CREATE)) {
                ObjectNode journal = (ObjectNode) new ObjectMapper().readTree(requestBody);
                journal.put("id", 42L);
                journal.put("status", "DRAFT");
                for (int index = 0; index < journal.withArray("lines").size(); index++) {
                    ObjectNode line = (ObjectNode) journal.withArray("lines").get(index);
                    line.put("id", 4201L + index);
                    line.set("side", line.remove("drcrType"));
                    line.set("description", line.remove("detailDescription"));
                }
                createdView = journal.toString();
            }
            if (path.equals(delayedPath)) {
                try {
                    Thread.sleep(300);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    exchange.close();
                    return;
                }
            }
            // Journal's write API requires trusted roles and a state transition through
            // request-approval; this test server must not pretend direct approve/post works.
            int status = persistCreatedDraft && path.startsWith("/api/journals/")
                    && !path.equals(DETAIL) && createdView == null ? 404
                    : path.equals(failedPath) ? failureStatus
                    : path.equals(APPROVE) || path.equals(POST) ? 403 : 200;
            String view = VIEW.replace("\"status\":\"DRAFT\"", "\"status\":\"" + viewStatus + "\"");
            if (persistCreatedDraft && createdView != null) view = createdView;
            String body = path.equals(CREATE) ? createResponse : path.equals(SUMMARIES) ? "[" + view + "]" : view;
            if (status >= 300 && status < 400) {
                exchange.getResponseHeaders().set("Location", "/redirect-target?private-provider-detail");
            } else if (status >= 400) body = "private-provider-detail";
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(status, status == 304 ? -1 : bytes.length);
            if (status != 304) exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();
        adapter = new HttpClosingJournalAdapter(RestClient.builder(),
                "http://127.0.0.1:" + server.getAddress().getPort(), "2s", "2s");
    }

    @AfterEach
    void tearDown() {
        if (server != null) server.stop(0);
    }

    static Stream<Arguments> redirectStages() {
        return IntStream.of(300, 301, 302, 303, 304, 307, 308).boxed()
                .flatMap(status -> Stream.of(CREATE, LOOKUP, DETAIL, SUMMARIES)
                        .map(path -> Arguments.of(status, path)));
    }

    @ParameterizedTest
    @MethodSource("redirectStages")
    void rejectsRedirectWithoutFollowingLocationOrCallingLaterStages(int status, String path) {
        failedPath = path;
        failureStatus = status;
        assertThatThrownBy(() -> invoke(path))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("HTTP " + status)
                .hasMessageNotContaining("private-provider-detail")
                .hasNoCause();
        assertThat(requests).extracting(Received::path).containsExactly(path);
        // CREATE failure must never reach an approval or posting endpoint.
        assertThat(requests).noneMatch(request -> request.path().startsWith("/redirect-target"));
    }

    @Test
    void healthyDraftCreationAndQueriesPreservePrecisionActorAndLineage() throws Exception {
        var result = adapter.createDraftEntry(command());
        assertThat(result.journalEntryId()).isEqualTo(42L);
        assertThat(result.slipNo()).isEqualTo("CL-42");
        assertThat(result.status()).isEqualTo("DRAFT");
        var summary = adapter.findBySlipNo(result.slipNo()).orElseThrow();
        assertThat(summary.getStatus()).isEqualTo("DRAFT");
        assertThat(summary.getLineageSourceType()).isEqualTo("CLOSING");
        assertThat(summary.getLineageSourceId()).isEqualTo("20260910");
        assertThat(adapter.getJournalSummaries(DATE, DATE)).hasSize(1);
        var details = adapter.getJournalDetails(42L);
        assertThat(details).singleElement().satisfies(detail -> {
            assertThat(detail.getAccountCode()).isEqualTo("41000");
            assertThat(detail.getAccountCategory()).isNull();
            assertThat(detail.getAmount()).isEqualByComparingTo("1000.00");
            assertThat(detail.getBaseAmount()).isEqualByComparingTo("1000.00");
        });
        assertThat(requests).extracting(Received::path)
                .containsExactly(CREATE, LOOKUP, SUMMARIES, DETAIL);
        assertThat(requests).extracting(Received::method)
                .containsExactly("POST", "GET", "GET", "GET");
        assertThat(requests.get(0).authUser()).isEqualTo("closing-operator");
        assertThat(requests.get(0).authRoles()).isEqualTo("ROLE_JOURNAL_MAKER");
        JsonNode json = new ObjectMapper()
                .enable(com.fasterxml.jackson.databind.DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
                .readTree(requests.get(0).body());
        assertThat(json.path("createdBy").asText()).isEqualTo("closing-operator");
        assertThat(json.path("auditUser").asText()).isEqualTo("closing-operator");
        assertThat(json.path("lineageSourceType").asText()).isEqualTo("CLOSING");
        assertThat(json.path("lineageSourceId").asText()).isEqualTo("20260910");
        assertThat(json.path("lines").size()).isEqualTo(2);
        for (JsonNode line : json.path("lines")) {
            assertThat(line.path("amount").decimalValue()).isEqualByComparingTo("99999999999999999.99");
            assertThat(line.path("baseAmount").decimalValue()).isEqualByComparingTo("99999999999999999.99");
        }
    }

    @Test
    void rejectsMismatchedPostingSlipWithoutExposingProviderContentOrRetrying() {
        createResponse = "{\"journalEntryId\":42,\"slipNo\":\"OTHER-42\",\"status\":\"DRAFT\"}";
        assertThatThrownBy(() -> adapter.createDraftEntry(command()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Journal Ledger posting returned an invalid response")
                .hasMessageNotContaining("OTHER-42")
                .hasNoCause();
        assertThat(requests).extracting(Received::path).containsExactly(CREATE);
    }

    @Test
    void acceptsAllocatedSlipWhenCommandDoesNotSpecifyOne() {
        var result = adapter.createDraftEntry(command(null));
        assertThat(result.slipNo()).isEqualTo("CL-42");
        assertThat(result.status()).isEqualTo("DRAFT");
        assertThat(requests).extracting(Received::path).containsExactly(CREATE);
    }

    @ParameterizedTest
    @ValueSource(strings = {CREATE, LOOKUP, DETAIL, SUMMARIES})
    void serverFailureIsSanitizedAndNeverRetried(String path) {
        failedPath = path;
        failureStatus = 503;
        assertThatThrownBy(() -> invoke(path)).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("HTTP 503").hasMessageNotContaining("private-provider-detail").hasNoCause();
        assertThat(requests).extracting(Received::path).containsExactly(path);
    }

    @Test
    void autoPostConfigurationFailsBeforeCreatingRemoteDraft() {
        adapter = new HttpClosingJournalAdapter(RestClient.builder(),
                "http://127.0.0.1:" + server.getAddress().getPort(), "2s", "2s", true);
        assertThatThrownBy(() -> adapter.createDraftEntry(command()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("trusted maker, checker, and poster")
                .hasMessageContaining("auto-post-adjustments")
                .hasNoCause();
        assertThat(requests).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"DRAFT", "REQUESTED", "APPROVED", "POSTED"})
    void existingRemoteEntryCannotBeAdvancedBySingleActor(String status) {
        viewStatus = status;
        var existing = adapter.findBySlipNo("CL-42").orElseThrow();
        assertThat(existing.getStatus()).isEqualTo(status);
        assertThatThrownBy(() -> adapter.approveAndPost(existing.getId(), "closing-operator"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("trusted maker, checker, and poster")
                .hasNoCause();
        assertThat(requests).extracting(Received::path).containsExactly(LOOKUP);
    }

    @Test
    void uncertainDraftCreationResponseIsNeverRetried() {
        delayedPath = CREATE;
        adapter = new HttpClosingJournalAdapter(RestClient.builder(),
                "http://127.0.0.1:" + server.getAddress().getPort(), "2s", "50ms");
        assertThatThrownBy(() -> adapter.createDraftEntry(command()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("posting failed")
                .hasNoCause();
        assertThat(requests).extracting(Received::path).containsExactly(CREATE);
    }

    static Stream<Arguments> closingAdjustments() {
        return Stream.of(
                Arguments.of("FX", fxDraft()),
                Arguments.of("ECL", eclDraft()));
    }

    // These are service-shaped commands at the adapter create phase. Approval/posting is
    // deliberately outside this test: the HTTP adapter rejects auto-post without trusted actors.
    @ParameterizedTest(name = "{0} adapter create response loss reconciles the exact persisted draft")
    @MethodSource("closingAdjustments")
    void lostCreateResponseReconcilesOnRestartWithoutSecondCreate(
            String adjustmentType, ClosingJournalEntryCommand command) throws Exception {
        persistCreatedDraft = true;
        delayedPath = CREATE;
        String baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
        var firstHttp = new HttpClosingJournalAdapter(RestClient.builder(), baseUrl, "2s", "50ms");
        var firstRun = new JournalLedgerClosingJournalEntryAdapter(firstHttp, firstHttp);

        assertThatThrownBy(() -> firstRun.createDraftAdjustment(command))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("posting failed")
                .hasNoCause();
        assertThat(createdView).as(adjustmentType + " draft was persisted before timeout").isNotNull();
        JsonNode persisted = new ObjectMapper().readTree(createdView);
        assertThat(persisted.path("lines").size()).isEqualTo(2);
        assertThat(persisted.path("lines").get(0).path("id").asLong()).isPositive();
        assertThat(persisted.path("lines").get(0).path("side").asText()).isEqualTo("DEBIT");

        // A new adapter models a restarted Closing process; its only durable key is the slip number.
        var restartedHttp = new HttpClosingJournalAdapter(RestClient.builder(), baseUrl, "2s", "2s");
        var restartedRun = new JournalLedgerClosingJournalEntryAdapter(restartedHttp, restartedHttp);
        var recovered = restartedRun.createDraftAdjustment(command);
        assertThat(recovered.journalEntryId()).isEqualTo(42L);
        assertThat(recovered.slipNo()).isEqualTo(command.slipNo());
        assertThat(requests).extracting(Received::path)
                .containsExactly("/api/journals/" + command.slipNo(), CREATE,
                        "/api/journals/" + command.slipNo(), DETAIL);
        assertThat(requests.stream().filter(request -> request.path().equals(CREATE))).hasSize(1);
        assertThat(requests).noneMatch(request -> request.path().endsWith("/approve")
                || request.path().endsWith("/post"));

        // Recovery must still reject a collided slip with different financial content.
        String exactView = createdView;
        createdView = exactView.replace(command.lines().get(0).amount().toPlainString(), "0.01");
        assertThatThrownBy(() -> restartedRun.createDraftAdjustment(command))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("different business content");
        createdView = exactView.replace("\"status\":\"DRAFT\"", "\"status\":\"CANCELLED\"");
        assertThatThrownBy(() -> restartedRun.createDraftAdjustment(command))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("non-reusable status CANCELLED");
        assertThat(requests.stream().filter(request -> request.path().equals(CREATE))).hasSize(1);
    }

    private static ClosingJournalEntryCommand fxDraft() {
        BigDecimal amount = new BigDecimal("1234.56");
        return new ClosingJournalEntryCommand(DATE, DATE, "Month-end FX Valuation",
                "CLOSING_ADJUSTMENT", "BATCH", "SYSTEM", "FX_VALUATION", "42|131000|USD",
                "KRW", "FXV202609107A9F0D77A",
                List.of(new ClosingJournalLineCommand(ClosingJournalSide.DEBIT, "131000",
                                amount, amount, "FX Revaluation adjustment"),
                        new ClosingJournalLineCommand(ClosingJournalSide.CREDIT, "41000",
                                amount, amount, "FX Translation Gain/Loss")));
    }

    private static ClosingJournalEntryCommand eclDraft() {
        BigDecimal amount = new BigDecimal("789.12");
        BigDecimal base = new BigDecimal("1025856.00"); // 789.12 USD at 1300 KRW per USD.
        return new ClosingJournalEntryCommand(DATE, DATE,
                "Month-end ECL Provision (Impairment) [USD/KRW @ 1300]",
                "CLOSING_ADJUSTMENT", "BATCH", "SYSTEM", "ECL_PROVISION", "42|139000|USD",
                "USD", new BigDecimal("1300.000000"), "ECL20260910D362162C6",
                List.of(new ClosingJournalLineCommand(ClosingJournalSide.DEBIT, "51000",
                                amount, base, "Bad Debt Expense (ECL Addition)"),
                        new ClosingJournalLineCommand(ClosingJournalSide.CREDIT, "139000",
                                amount, base, "Allowance for Doubtful Accounts (ECL Addition)")));
    }

    @Test
    void missingSlipReturnsEmpty() {
        failedPath = LOOKUP;
        failureStatus = 404;
        assertThat(adapter.findBySlipNo("CL-42")).isEmpty();
        assertThat(requests).hasSize(1);
    }

    @ParameterizedTest
    @ValueSource(strings = {"0ms", "-1s", "2147483648ms", "invalid"})
    void rejectsInvalidTransportTimeouts(String timeout) {
        assertThatThrownBy(() -> new HttpClosingJournalAdapter(RestClient.builder(), "http://provider.invalid", timeout, "2s"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new HttpClosingJournalAdapter(RestClient.builder(), "http://provider.invalid", "2s", timeout))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private void invoke(String path) {
        switch (path) {
            case CREATE -> adapter.createDraftEntry(command());
            case LOOKUP -> adapter.findBySlipNo("CL-42");
            case DETAIL -> adapter.getJournalDetails(42L);
            case SUMMARIES -> adapter.getJournalSummaries(DATE, DATE);
            default -> throw new AssertionError("Unknown test stage");
        }
    }

    private static JournalEntryCommand command() {
        return command("CL-42");
    }

    private static JournalEntryCommand command(String slipNo) {
        BigDecimal amount = new BigDecimal("99999999999999999.99");
        return new JournalEntryCommand(DATE, DATE, "Closing", "NORMAL", "KRW", BigDecimal.ONE,
                "closing-operator", "closing-operator", "CLOSING", "20260910", slipNo, List.of(
                new JournalLineCommand("DEBIT", "131000", amount, amount, null, null, "Closing debit"),
                new JournalLineCommand("CREDIT", "101000", amount, amount, null, null, "Closing credit")));
    }

    private record Received(
            String path,
            String method,
            String actor,
            String authUser,
            String authRoles,
            String body) { }
}
