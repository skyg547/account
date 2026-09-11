package com.ho.account.closing.infrastructure.external;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    private static final String SUMMARIES = "/api/journals";
    private static final String DRAFT = "{\"journalEntryId\":42,\"slipNo\":\"CL-42\",\"status\":\"DRAFT\"}";
    private static final String VIEW = """
            {"id":42,"slipNo":"CL-42","slipDate":"2026-09-10","accountingDate":"2026-09-10",
             "status":"POSTED","entryType":"NORMAL","currencyCode":"KRW",
             "lineageSourceType":"CLOSING","lineageSourceId":"20260910"}
            """;
    private HttpServer server;
    private HttpClosingJournalAdapter adapter;
    private final List<Received> requests = new CopyOnWriteArrayList<>();
    private volatile String failedPath;
    private volatile int failureStatus;

    @BeforeEach
    void setUp() throws Exception {
        // A real loopback server exercises the production request factory, including automatic redirects.
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            String path = exchange.getRequestURI().getPath();
            requests.add(new Received(path, exchange.getRequestMethod(),
                    exchange.getRequestHeaders().getFirst("X-User-ID"),
                    new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8)));
            int status = path.equals(failedPath) ? failureStatus : 200;
            String body = path.equals(CREATE) ? DRAFT : path.equals(SUMMARIES) ? "[" + VIEW + "]" : VIEW;
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
                .flatMap(status -> Stream.of(CREATE, APPROVE, POST, LOOKUP, SUMMARIES)
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
        List<String> expected = path.equals(POST) ? List.of(APPROVE, POST) : List.of(path);
        assertThat(requests).extracting(Received::path).containsExactlyElementsOf(expected);
        // CREATE failure must never reach approval/post; approval failure must never reach post.
        assertThat(requests).noneMatch(request -> request.path().startsWith("/redirect-target"));
    }

    @Test
    void healthyCreateApprovePostAndQueriesPreservePrecisionActorAndLineage() throws Exception {
        var result = adapter.createDraftEntry(command());
        assertThat(result.journalEntryId()).isEqualTo(42L);
        assertThat(result.slipNo()).isEqualTo("CL-42");
        adapter.approveAndPost(result.journalEntryId(), " closing-operator ");
        var summary = adapter.findBySlipNo(result.slipNo()).orElseThrow();
        assertThat(summary.getLineageSourceType()).isEqualTo("CLOSING");
        assertThat(summary.getLineageSourceId()).isEqualTo("20260910");
        assertThat(adapter.getJournalSummaries(DATE, DATE)).hasSize(1);
        assertThat(requests).extracting(Received::path).containsExactly(CREATE, APPROVE, POST, LOOKUP, SUMMARIES);
        assertThat(requests).extracting(Received::method).containsExactly("POST", "POST", "POST", "GET", "GET");
        assertThat(requests.get(1).actor()).isEqualTo("closing-operator");
        assertThat(requests.get(2).actor()).isEqualTo("closing-operator");
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

    @ParameterizedTest
    @ValueSource(strings = {CREATE, APPROVE, POST, LOOKUP, SUMMARIES})
    void serverFailureIsSanitizedAndNeverRetried(String path) {
        failedPath = path;
        failureStatus = 503;
        assertThatThrownBy(() -> invoke(path)).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("HTTP 503").hasMessageNotContaining("private-provider-detail").hasNoCause();
        assertThat(requests).extracting(Received::path)
                .containsExactlyElementsOf(path.equals(POST) ? List.of(APPROVE, POST) : List.of(path));
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
            case CREATE -> {
                var result = adapter.createDraftEntry(command());
                adapter.approveAndPost(result.journalEntryId(), "closing-operator");
            }
            case APPROVE, POST -> adapter.approveAndPost(42L, "closing-operator");
            case LOOKUP -> adapter.findBySlipNo("CL-42");
            case SUMMARIES -> adapter.getJournalSummaries(DATE, DATE);
            default -> throw new AssertionError("Unknown test stage");
        }
    }

    private static JournalEntryCommand command() {
        BigDecimal amount = new BigDecimal("99999999999999999.99");
        return new JournalEntryCommand(DATE, DATE, "Closing", "NORMAL", "KRW", BigDecimal.ONE,
                "closing-operator", "closing-operator", "CLOSING", "20260910", List.of(
                new JournalLineCommand("DEBIT", "131000", amount, amount, null, null, "Closing debit"),
                new JournalLineCommand("CREDIT", "101000", amount, amount, null, null, "Closing credit")));
    }

    private record Received(String path, String method, String actor, String body) { }
}
