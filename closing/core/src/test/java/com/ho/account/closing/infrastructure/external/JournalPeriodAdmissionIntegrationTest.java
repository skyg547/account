package com.ho.account.closing.infrastructure.external;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.ho.account.contracts.journal.JournalPeriodCheckpoint;
import com.ho.account.contracts.journal.JournalPeriodFenceCommand;
import com.ho.account.contracts.journal.JournalPeriodFencePort;
import com.ho.account.contracts.journal.JournalPeriodFenceResult;
import com.ho.account.contracts.journal.JournalPeriodScope;
import com.ho.account.journalledger.infrastructure.persistence.JdbcBalanceReaggregationControlAdapter;
import com.ho.account.journalledger.infrastructure.persistence.JdbcJournalPeriodAdmissionAdapter;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.RestClient;

/** Real Journal SQL and Closing HTTP client; only the HTTP dispatch harness and movement table are synthetic. */
class JournalPeriodAdmissionIntegrationTest {
    private static final LocalDate DATE = LocalDate.of(2026, 5, 10);
    private static final JournalPeriodScope SCOPE = new JournalPeriodScope(
            762L, DATE.withDayOfMonth(1), DATE.withDayOfMonth(31));

    @Test
    void localFenceDrainsTheOuterTransactionAndRejectsAnOmittedMovement() throws Exception {
        try (Fixture fixture = new Fixture()) {
            JournalPeriodCheckpoint before = fixture.provider.get().checkpoint(SCOPE);
            CountDownLatch written = new CountDownLatch(1);
            CountDownLatch permitCommit = new CountDownLatch(1);
            Future<?> posting = fixture.workers.submit(() -> fixture.transaction.executeWithoutResult(status -> {
                fixture.provider.get().admit(DATE);
                fixture.jdbc.update("INSERT INTO admission_test_movements VALUES (1)");
                written.countDown();
                await(permitCommit);
            }));
            try {
                await(written);
                Future<?> close = fixture.workers.submit(() -> fixture.provider.get().fence(command("stale", before)));
                // The gate must wait for the enclosing commit, even though the admission method returned.
                assertThatThrownBy(() -> close.get(150, TimeUnit.MILLISECONDS)).isInstanceOf(TimeoutException.class);
                permitCommit.countDown();
                posting.get(10, TimeUnit.SECONDS);
                assertThatThrownBy(() -> close.get(10, TimeUnit.SECONDS))
                        .hasRootCauseMessage("Journal changed after checkpoint; repeat validation from fresh sources");
                JournalPeriodCheckpoint accepted = fixture.provider.get().checkpoint(SCOPE);
                int snapshot = fixture.countMovements();
                assertThat(snapshot).isEqualTo(1);
                fixture.provider.get().fence(command("fresh", accepted));
                assertThatThrownBy(() -> fixture.move(2, DATE)).isInstanceOf(IllegalStateException.class);
                assertThat(fixture.countMovements()).isEqualTo(snapshot);
            } finally {
                permitCommit.countDown();
            }
        }
    }

    @Test
    void remoteLostFenceResponseSurvivesServerAndClientRecreation() throws Exception {
        try (Fixture fixture = new Fixture()) {
            fixture.startServer();
            JournalPeriodFencePort client = fixture.client(Duration.ofMillis(200));
            // Only the barrier-controlled fence response uses the short fault-injection deadline.
            JournalPeriodFenceCommand command = command("lost-response",
                    fixture.client(Duration.ofSeconds(3)).checkpoint(SCOPE));
            CountDownLatch committed = new CountDownLatch(1);
            CountDownLatch releaseResponse = new CountDownLatch(1);
            fixture.afterFence.set(() -> {
                committed.countDown();
                await(releaseResponse);
            });
            try {
                assertThatThrownBy(() -> client.fence(command)).isInstanceOf(IllegalStateException.class)
                        .hasMessageContaining("retry the same durable operation");
                await(committed);
                assertThatThrownBy(() -> fixture.move(1, DATE)).isInstanceOf(IllegalStateException.class);
            } finally {
                releaseResponse.countDown();
            }
            fixture.restartProviderAndServer();
            JournalPeriodFencePort restartedClient = fixture.client(Duration.ofSeconds(3));
            assertThat(restartedClient.fence(command).state()).isEqualTo(JournalPeriodFenceResult.State.ACTIVE);
            assertThat(restartedClient.release(command).state()).isEqualTo(JournalPeriodFenceResult.State.RELEASED);
            fixture.move(1, DATE);
            assertThat(fixture.countMovements()).isEqualTo(1);
            assertThat(restartedClient.checkpoint(SCOPE).revision()).isGreaterThan(command.checkpoint().revision());
        }
    }

    @Test
    void aReleaseThatOvertakesAnUnknownFenceCannotBeUndoneByItsDelayedArrival() throws Exception {
        try (Fixture fixture = new Fixture()) {
            fixture.startServer();
            JournalPeriodFencePort shortClient = fixture.client(Duration.ofMillis(200));
            JournalPeriodFenceCommand abandoned = command("abandoned",
                    fixture.client(Duration.ofSeconds(3)).checkpoint(SCOPE));
            CountDownLatch received = new CountDownLatch(1);
            CountDownLatch allowLateFence = new CountDownLatch(1);
            CountDownLatch lateFenceFinished = new CountDownLatch(1);
            fixture.beforeFence.set(() -> {
                received.countDown();
                await(allowLateFence);
            });
            fixture.afterFence.set(lateFenceFinished::countDown);
            try {
                assertThatThrownBy(() -> shortClient.fence(abandoned)).isInstanceOf(IllegalStateException.class);
                await(received);
                JournalPeriodFencePort client = fixture.client(Duration.ofSeconds(3));
                client.release(abandoned);
                JournalPeriodFenceCommand replacement = command("replacement", client.checkpoint(SCOPE));
                client.fence(replacement);
                client.release(abandoned);
                allowLateFence.countDown();
                await(lateFenceFinished);
                assertThat(client.fence(replacement).state()).isEqualTo(JournalPeriodFenceResult.State.ACTIVE);
                assertThatThrownBy(() -> client.fence(abandoned)).isInstanceOf(IllegalStateException.class);
                assertThatThrownBy(() -> fixture.move(1, DATE)).isInstanceOf(IllegalStateException.class);
                assertThat(fixture.jdbc.queryForObject(
                        "SELECT COUNT(*) FROM journal_period_fences WHERE state = 'ACTIVE'", Integer.class)).isEqualTo(1);
            } finally {
                allowLateFence.countDown();
            }
        }
    }

    private static JournalPeriodFenceCommand command(String id, JournalPeriodCheckpoint checkpoint) {
        return new JournalPeriodFenceCommand(id, checkpoint, "synthetic-reviewer");
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("Test barrier timed out");
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Test barrier interrupted", interrupted);
        }
    }

    private static final class Fixture implements AutoCloseable {
        final ExecutorService workers = Executors.newCachedThreadPool();
        final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
        final JdbcTemplate jdbc;
        final DataSourceTransactionManager manager;
        final TransactionTemplate transaction;
        final AtomicReference<JdbcJournalPeriodAdmissionAdapter> provider = new AtomicReference<>();
        final AtomicReference<Runnable> beforeFence = new AtomicReference<>();
        final AtomicReference<Runnable> afterFence = new AtomicReference<>();
        final String token = UUID.randomUUID().toString() + UUID.randomUUID();
        HttpServer server;

        Fixture() {
            String schema = "admission_" + UUID.randomUUID().toString().replace("-", "");
            String postgres = System.getenv("JOURNAL_POSTING_TEST_POSTGRES_URL");
            DriverManagerDataSource source;
            if (postgres == null || postgres.isBlank()) {
                source = new DriverManagerDataSource("jdbc:h2:mem:" + schema
                        + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000", "sa", "");
            } else {
                new JdbcTemplate(new DriverManagerDataSource(postgres, "postgres", ""))
                        .execute("CREATE SCHEMA " + schema);
                source = new DriverManagerDataSource(postgres + (postgres.contains("?") ? "&" : "?")
                        + "currentSchema=" + schema, "postgres", "");
            }
            new ResourceDatabasePopulator(
                    new ClassPathResource("db/migration/V15__ledger_reaggregation_control.sql"),
                    new ClassPathResource("db/migration/V20__journal_period_admission.sql")).execute(source);
            jdbc = new JdbcTemplate(source);
            jdbc.execute("CREATE TABLE admission_test_movements (id INTEGER PRIMARY KEY)");
            manager = new DataSourceTransactionManager(source);
            transaction = new TransactionTemplate(manager);
            recreateProvider();
        }

        void recreateProvider() {
            provider.set(new JdbcJournalPeriodAdmissionAdapter(jdbc,
                    new JdbcBalanceReaggregationControlAdapter(jdbc), manager));
        }

        void move(int id, LocalDate date) {
            transaction.executeWithoutResult(status -> {
                provider.get().admit(date);
                jdbc.update("INSERT INTO admission_test_movements VALUES (?)", id);
            });
        }

        int countMovements() {
            return jdbc.queryForObject("SELECT COUNT(*) FROM admission_test_movements", Integer.class);
        }

        void startServer() throws IOException {
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.setExecutor(workers);
            server.createContext("/api/internal/journal-periods/", this::handle);
            server.start();
        }

        void restartProviderAndServer() throws IOException {
            server.stop(0);
            beforeFence.set(null);
            afterFence.set(null);
            recreateProvider();
            startServer();
        }

        JournalPeriodFencePort client(Duration timeout) {
            return new HttpJournalPeriodFenceAdapter(RestClient.builder(),
                    "http://127.0.0.1:" + server.getAddress().getPort(), token, Duration.ofSeconds(3), timeout);
        }

        void handle(HttpExchange exchange) throws IOException {
            try (exchange) {
                if (!"POST".equals(exchange.getRequestMethod())
                        || !"closing".equals(exchange.getRequestHeaders().getFirst("X-Service-Identity"))
                        || !("Bearer " + token).equals(exchange.getRequestHeaders().getFirst("Authorization"))) {
                    exchange.sendResponseHeaders(403, -1);
                    return;
                }
                Object response;
                String path = exchange.getRequestURI().getPath();
                if (path.endsWith("/checkpoint")) {
                    response = provider.get().checkpoint(mapper.readValue(exchange.getRequestBody(), JournalPeriodScope.class));
                } else {
                    JournalPeriodFenceCommand command = mapper.readValue(exchange.getRequestBody(), JournalPeriodFenceCommand.class);
                    if (path.endsWith("/fence")) {
                        Runnable before = beforeFence.getAndSet(null);
                        Runnable after = afterFence.getAndSet(null);
                        if (before != null) before.run();
                        try {
                            response = provider.get().fence(command);
                        } finally {
                            if (after != null) after.run();
                        }
                    } else if (path.endsWith("/release")) {
                        response = provider.get().release(command);
                    } else {
                        exchange.sendResponseHeaders(404, -1);
                        return;
                    }
                }
                byte[] body = mapper.writeValueAsBytes(response);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, body.length);
                exchange.getResponseBody().write(body);
            } catch (RuntimeException failure) {
                // Fault injection intentionally terminates requests; never echo payloads/credentials.
                exchange.close();
            }
        }

        @Override
        public void close() throws InterruptedException {
            if (server != null) server.stop(0);
            workers.shutdownNow();
            workers.awaitTermination(10, TimeUnit.SECONDS);
        }
    }
}
