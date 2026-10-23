package com.ho.account.closing.infrastructure.external;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.ho.account.contracts.journal.*;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class HttpJournalPeriodFenceAdapterTest {
    private static final JournalPeriodScope SCOPE = new JournalPeriodScope(7L,
            LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31));
    private static final JournalPeriodFenceCommand COMMAND = new JournalPeriodFenceCommand("operation",
            new JournalPeriodCheckpoint(SCOPE, 3), "closer");
    private static final String TOKEN = "synthetic-fence-adapter-token-76200";
    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Test
    void exactCheckpointAndCompleteReceiptRoundTrip() throws Exception {
        try (Server server = new Server(200, mapper.writeValueAsBytes(COMMAND.checkpoint()))) {
            assertThat(server.client().checkpoint(SCOPE)).isEqualTo(COMMAND.checkpoint());
        }
        for (var state : JournalPeriodFenceResult.State.values()) {
            try (Server server = new Server(200, mapper.writeValueAsBytes(new JournalPeriodFenceResult(COMMAND, state)))) {
                assertThat(state == JournalPeriodFenceResult.State.ACTIVE
                        ? server.client().fence(COMMAND) : server.client().release(COMMAND))
                        .isEqualTo(new JournalPeriodFenceResult(COMMAND, state));
            }
        }
    }

    @Test
    void mismatchedOperationActorRevisionScopeOrStateNeverAcknowledgesFence() throws Exception {
        var otherScope = new JournalPeriodScope(8L, SCOPE.startDate(), SCOPE.endDate());
        List<JournalPeriodFenceResult> responses = List.of(
                new JournalPeriodFenceResult(new JournalPeriodFenceCommand("other", COMMAND.checkpoint(), "closer"), JournalPeriodFenceResult.State.ACTIVE),
                new JournalPeriodFenceResult(new JournalPeriodFenceCommand("operation", COMMAND.checkpoint(), "other"), JournalPeriodFenceResult.State.ACTIVE),
                new JournalPeriodFenceResult(new JournalPeriodFenceCommand("operation", new JournalPeriodCheckpoint(SCOPE, 4), "closer"), JournalPeriodFenceResult.State.ACTIVE),
                new JournalPeriodFenceResult(new JournalPeriodFenceCommand("operation", new JournalPeriodCheckpoint(otherScope, 3), "closer"), JournalPeriodFenceResult.State.ACTIVE),
                new JournalPeriodFenceResult(COMMAND, JournalPeriodFenceResult.State.RELEASED));
        for (var response : responses) {
            try (Server server = new Server(200, mapper.writeValueAsBytes(response))) {
                assertThatThrownBy(() -> server.client().fence(COMMAND)).isInstanceOf(IllegalStateException.class)
                        .hasMessageContaining("exact command and state");
            }
        }
        try (Server server = new Server(200, mapper.writeValueAsBytes(new JournalPeriodCheckpoint(otherScope, 3)))) {
            assertThatThrownBy(() -> server.client().checkpoint(SCOPE)).isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("scope");
        }
    }

    @Test
    void redirectsAndMalformedBodiesFailWithoutForwardingCredential() throws Exception {
        for (int status : List.of(201, 202, 204, 301, 302, 307, 308, 401, 403, 409, 503)) {
            try (Server server = new Server(status, "private remote body".getBytes(java.nio.charset.StandardCharsets.UTF_8))) {
                assertThatThrownBy(() -> server.client().fence(COMMAND)).isInstanceOf(IllegalStateException.class)
                        .hasMessageNotContaining("private remote body").hasMessageNotContaining(TOKEN);
                assertThat(server.redirects.get()).isZero();
            }
        }
        String valid = mapper.writeValueAsString(new JournalPeriodFenceResult(COMMAND, JournalPeriodFenceResult.State.ACTIVE));
        for (String body : List.of("", "{}", "null", "not-json", valid + " {}",
                valid.replace("\"state\":\"ACTIVE\"", "\"state\":\"RELEASED\",\"state\":\"ACTIVE\""),
                valid.substring(0, valid.length() - 1) + ",\"extra\":true}",
                valid.replace("\"revision\":3", "\"revision\":3.5"),
                valid.replace("\"revision\":3", "\"revision\":\"3\""),
                valid.replace("\"revision\":3", "\"revision\":null"),
                valid.replace(",\"revision\":3", ""))) {
            try (Server server = new Server(200, body.getBytes(java.nio.charset.StandardCharsets.UTF_8))) {
                assertThatThrownBy(() -> server.client().fence(COMMAND)).isInstanceOf(IllegalStateException.class);
            }
        }
    }

    @Test
    void unboundedTimeoutsAndShortCredentialsAreRejectedAtConstruction() {
        for (Duration duration : List.of(Duration.ZERO, Duration.ofSeconds(61), Duration.ofMillis(-1))) {
            assertThatThrownBy(() -> new HttpJournalPeriodFenceAdapter(RestClient.builder(), "http://unused.test",
                    TOKEN, duration, Duration.ofSeconds(1))).isInstanceOf(IllegalArgumentException.class);
        }
        assertThatThrownBy(() -> new HttpJournalPeriodFenceAdapter(RestClient.builder(), "http://unused.test", "short",
                Duration.ofSeconds(1), Duration.ofSeconds(1))).isInstanceOf(IllegalArgumentException.class);
    }

    private static final class Server implements AutoCloseable {
        final HttpServer server;
        final AtomicInteger redirects = new AtomicInteger();
        Server(int status, byte[] response) throws Exception {
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/api/internal/journal-periods/", exchange -> {
                assertThat(exchange.getRequestHeaders().getFirst("Authorization")).isEqualTo("Bearer " + TOKEN);
                assertThat(exchange.getRequestHeaders().getFirst("X-Service-Identity")).isEqualTo("closing");
                exchange.getRequestBody().readAllBytes();
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.getResponseHeaders().set("Location", "/redirect");
                exchange.sendResponseHeaders(status, response.length);
                exchange.getResponseBody().write(response);
                exchange.close();
            });
            server.createContext("/redirect", exchange -> { redirects.incrementAndGet(); exchange.sendResponseHeaders(200, -1); exchange.close(); });
            server.start();
        }
        HttpJournalPeriodFenceAdapter client() {
            return new HttpJournalPeriodFenceAdapter(RestClient.builder(), "http://127.0.0.1:" + server.getAddress().getPort(),
                    TOKEN, Duration.ofSeconds(1), Duration.ofSeconds(1));
        }
        public void close() { server.stop(0); }
    }
}
