package com.ho.account.closing.infrastructure.external;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.web.client.RestClient;

class HttpClosingMasterDataQueryAdapterTest {

    private static final LocalDate EFFECTIVE_DATE = LocalDate.of(2026, 6, 30);
    private static final String ACCOUNT = "41000";
    private static final String VALID_ACCOUNT = """
            {"code":"41000","name":"Revenue","unsettled":false,"fixedAsset":false,
             "normalBalanceSide":"CREDIT","accountCategory":"REVENUE"}
            """;

    private final List<String> requests = new CopyOnWriteArrayList<>();
    private HttpServer server;
    private HttpClosingMasterDataQueryAdapter adapter;
    private volatile int responseStatus;
    private volatile String responseBody;
    private volatile int forwardedRequests;

    @BeforeEach
    void setUp() throws Exception {
        responseStatus = 200;
        responseBody = VALID_ACCOUNT;
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/basic/references/account-subjects", exchange -> {
            requests.add(exchange.getRequestMethod() + " " + exchange.getRequestURI());
            if (responseStatus >= 300 && responseStatus < 400) {
                exchange.getResponseHeaders().set("Location", "/forwarded?private-provider-detail");
            }
            byte[] body = responseBody == null
                    ? new byte[0]
                    : responseBody.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(responseStatus, body.length == 0 ? -1 : body.length);
            if (body.length > 0) {
                exchange.getResponseBody().write(body);
            }
            exchange.close();
        });
        server.createContext("/forwarded", exchange -> {
            forwardedRequests++;
            byte[] body = VALID_ACCOUNT.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        adapter = new HttpClosingMasterDataQueryAdapter(
                RestClient.builder(), baseUrl(), Duration.ofSeconds(2), Duration.ofSeconds(2));
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void datedAccountLookupUsesExactRouteAndPreservesClassification() {
        var result = adapter.findAccountSubjectAt(" 41000 ", EFFECTIVE_DATE).orElseThrow();

        assertThat(result.code()).isEqualTo(ACCOUNT);
        assertThat(result.name()).isEqualTo("Revenue");
        assertThat(result.normalBalanceSide()).isEqualTo("CREDIT");
        assertThat(result.accountCategory()).isEqualTo("REVENUE");
        assertThat(requests).containsExactly(
                "GET /api/basic/references/account-subjects/41000?effectiveDate=2026-06-30");
    }

    @Test
    void notFoundReturnsEmptyWithoutRetry() {
        responseStatus = 404;
        responseBody = null;

        assertThat(adapter.findAccountSubjectAt(ACCOUNT, EFFECTIVE_DATE)).isEmpty();
        assertThat(requests).hasSize(1);
    }

    static Stream<Arguments> invalidAccountResponses() {
        return Stream.of(
                Arguments.of("mismatched code", VALID_ACCOUNT.replace("41000", "42000")),
                Arguments.of("blank name", VALID_ACCOUNT.replace("Revenue", " ")),
                Arguments.of("missing category", VALID_ACCOUNT.replace(
                        "\"accountCategory\":\"REVENUE\"", "\"accountCategory\":null")),
                Arguments.of("blank category", VALID_ACCOUNT.replace(
                        "\"accountCategory\":\"REVENUE\"", "\"accountCategory\":\" \"")));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidAccountResponses")
    void invalidSuccessfulResponseFailsClosedWithoutRetry(String reason, String body) {
        responseBody = body;

        assertThatThrownBy(() -> adapter.findAccountSubjectAt(ACCOUNT, EFFECTIVE_DATE))
                .isInstanceOf(IllegalStateException.class);
        assertThat(requests).hasSize(1);
    }

    @ParameterizedTest
    @ValueSource(ints = {301, 302, 303, 307, 308, 503})
    void redirectOrServerFailureIsSanitizedAndNeverRetriedOrForwarded(int status) {
        responseStatus = status;
        responseBody = "private-provider-detail";

        assertThatThrownBy(() -> adapter.findAccountSubjectAt(ACCOUNT, EFFECTIVE_DATE))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("HTTP " + status)
                .hasMessageNotContaining("private-provider-detail")
                .hasNoCause();
        assertThat(requests).hasSize(1);
        assertThat(forwardedRequests).isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {"0ms", "-1s", "2147483648ms", "invalid"})
    void rejectsInvalidOrUnboundedTimeouts(String timeout) {
        assertThatThrownBy(() -> new HttpClosingMasterDataQueryAdapter(
                RestClient.builder(), baseUrl(), timeout, "2s"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new HttpClosingMasterDataQueryAdapter(
                RestClient.builder(), baseUrl(), "2s", timeout))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void undatedOrUnsupportedReferenceLookupsFailExplicitly() {
        assertThatThrownBy(() -> adapter.findAccountSubject(ACCOUNT))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> adapter.findBusinessPartner("BP-1"))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> adapter.findDepartment("D-1"))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThat(requests).isEmpty();
    }

    private String baseUrl() {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }
}
