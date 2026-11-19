package com.ho.account.closing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ho.account.closing.application.port.in.ClosingUseCase;
import com.ho.account.closing.application.port.in.FinalCloseEvidenceUseCase;
import com.ho.account.closing.application.port.out.ClosingCalendarPersistencePort;
import com.ho.account.closing.application.service.ClosingPeriodTransitionService;
import com.ho.account.closing.application.service.ClosingTransitionPendingException;
import com.ho.account.closing.application.service.ClosingTransitionTransactions;
import com.ho.account.closing.domain.ClosingCalendar;
import com.ho.account.closing.domain.ClosingCalendar.ClosingCalendarStatus;
import com.ho.account.closing.domain.ClosingCalendar.TransitionStage;
import com.ho.account.closing.domain.ClosingTask.ClosingTaskStatus;
import com.ho.account.closing.domain.ReopenApproval.ReopenApprovalStatus;
import com.ho.account.closing.infrastructure.external.HttpJournalPeriodFenceAdapter;
import com.ho.account.contracts.journal.*;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestClient;

/** Closing transactions use real H2; response loss crosses actual HTTP and a recreated coordinator. */
@ActiveProfiles("local")
@SpringBootTest(classes = {ClosingApplication.class, ClosingAggregateConcurrencyIntegrationTest.Configuration.class},
        properties = {
                "spring.cloud.config.enabled=false", "spring.cloud.discovery.enabled=false",
                "spring.cloud.loadbalancer.enabled=false", "spring.cloud.vault.enabled=false",
                "eureka.client.enabled=false", "management.tracing.enabled=false",
                "spring.data.redis.repositories.enabled=false",
                "spring.datasource.url=jdbc:h2:mem:closing-journal-fence-recovery;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000",
                "spring.datasource.username=sa", "spring.datasource.password=",
                "spring.jpa.hibernate.ddl-auto=create-drop", "spring.flyway.enabled=false"
        })
class ClosingJournalFenceRecoveryIntegrationTest {
    @Autowired ClosingUseCase closing;
    @Autowired ClosingCalendarPersistencePort calendars;
    @Autowired ClosingTransitionTransactions transactions;
    @Autowired FinalCloseEvidenceUseCase evidence;
    @Autowired ClosingAggregateConcurrencyIntegrationTest.ControlledMaster master;
    @Autowired ControlledJournalPeriodFence journal;
    @Autowired ObjectMapper mapper;

    @Test
    void lostFenceResponseLeavesPreparedAndRestartReplaysExactlyOneCommand() throws Exception {
        ClosingCalendar prepared = prepared("2071", "01");
        try (Remote remote = new Remote("fence")) {
            assertThatThrownBy(() -> remote.coordinator().recoverTransition(
                    prepared.getId(), prepared.getTransitionId(), false, "operator"))
                    .isInstanceOf(ClosingTransitionPendingException.class);
            ClosingCalendar pending = calendars.findById(prepared.getId()).orElseThrow();
            assertThat(pending.getTransitionStage()).isEqualTo(TransitionStage.PREPARED);
            assertThat(journal.result(prepared.getTransitionId()).state()).isEqualTo(JournalPeriodFenceResult.State.ACTIVE);
            assertThat(master.findFiscalPeriodById(prepared.getTransitionFiscalPeriodId()).orElseThrow().closingStatus()).isEqualTo("OPEN");
            ClosingCalendar closed = remote.coordinator().recoverTransition(
                    prepared.getId(), prepared.getTransitionId(), false, "restarted-operator");
            assertThat(closed.getStatus()).isEqualTo(ClosingCalendarStatus.CLOSED);
            assertThat(closed.getJournalFenceCommand()).isEqualTo(prepared.getJournalFenceCommand());
            assertThat(remote.calls.get()).isGreaterThanOrEqualTo(2);
        }
    }

    @Test
    void lostCancelReleaseCommitsPendingBeforeRemoteCallAndRestartOnlyReleases() throws Exception {
        ClosingCalendar prepared = prepared("2071", "02");
        try (Remote remote = new Remote("release")) {
            assertThatThrownBy(() -> remote.coordinator().cancelPreparedClose(prepared.getId(),
                    prepared.getTransitionId(), "refresh evidence", "operator"))
                    .isInstanceOf(ClosingTransitionPendingException.class);
            ClosingCalendar pending = calendars.findById(prepared.getId()).orElseThrow();
            assertThat(pending.getTransitionStage()).isEqualTo(TransitionStage.CANCEL_RELEASE_PENDING);
            assertThatThrownBy(() -> transactions.requirePreparedFence(prepared.getId(), prepared.getTransitionId()))
                    .isInstanceOf(IllegalStateException.class);
            assertThatThrownBy(() -> journal.fence(prepared.getJournalFenceCommand())).isInstanceOf(IllegalStateException.class);
            ClosingCalendar released = remote.coordinator().recoverTransition(prepared.getId(),
                    prepared.getTransitionId(), false, "restarted-operator");
            assertThat(released.getStatus()).isEqualTo(ClosingCalendarStatus.IN_PROGRESS);
            assertThat(released.getTransitionId()).isNull();
            assertThat(released.getJournalFenceCommand()).isNull();
        }
    }

    @Test
    void lostReopenReleaseKeepsCommittedNewCycleBlockedUntilRestartAcknowledgesOriginalFence() throws Exception {
        ClosingCalendar prepared = prepared("2071", "03");
        try (Remote remote = new Remote("release")) {
            remote.coordinator().recoverTransition(prepared.getId(), prepared.getTransitionId(), false, "operator");
            var approval = closing.requestPeriodReopen(prepared.getTransitionFiscalPeriodId(), "maker", "correction");
            assertThatThrownBy(() -> remote.coordinator().decide(approval.getId(), ReopenApprovalStatus.APPROVED, "checker"))
                    .isInstanceOf(ClosingTransitionPendingException.class);
            ClosingCalendar pending = calendars.findById(prepared.getId()).orElseThrow();
            assertThat(pending.getStatus()).isEqualTo(ClosingCalendarStatus.OPEN);
            assertThat(pending.getCycleNumber()).isEqualTo(2);
            assertThat(pending.getTransitionStage()).isEqualTo(TransitionStage.RELEASE_PENDING);
            assertThat(master.findFiscalPeriodById(prepared.getTransitionFiscalPeriodId()).orElseThrow().closingStatus()).isEqualTo("OPEN");
            assertThat(pending.getJournalFenceCommand()).isEqualTo(prepared.getJournalFenceCommand());
            assertThatThrownBy(() -> transactions.start(pending.getId(), "early-start"))
                    .isInstanceOf(IllegalStateException.class);
            assertThatThrownBy(() -> evidence.checkpoint(pending.getId())).isInstanceOf(IllegalStateException.class);
            ClosingCalendar open = remote.coordinator().recoverTransition(pending.getId(), pending.getTransitionId(), false, "operator");
            assertThat(open.getTransitionId()).isNull();
            assertThat(open.getJournalFenceCommand()).isNull();
            assertThat(open.getCycleNumber()).isEqualTo(2);
        }
    }

    private ClosingCalendar prepared(String year, String month) {
        long periodId = master.create(year, month, "OPEN");
        ClosingCalendar calendar = new ClosingCalendar();
        calendar.setFiscalYear(year); calendar.setFiscalPeriod(month);
        calendar.setStatus(ClosingCalendarStatus.IN_PROGRESS);
        calendar.setCloseInitiatedAt(LocalDateTime.now(ZoneOffset.UTC).minusSeconds(1));
        calendar.setAuditUser("fixture");
        calendar = calendars.save(calendar);
        var task = closing.createClosingTask(ClosingAggregateConcurrencyIntegrationTest.newTask(calendar, true));
        closing.updateClosingTaskStatus(task.getId(), ClosingTaskStatus.IN_PROGRESS, "worker");
        closing.updateClosingTaskStatus(task.getId(), ClosingTaskStatus.COMPLETED, "worker");
        var gate = closing.createClosingGate(ClosingAggregateConcurrencyIntegrationTest.newGate(calendar));
        closing.checkAndPassClosingGate(gate.getId(), "reviewer");
        FinalCloseEvidenceFixtures.recordValid(evidence, calendar, periodId,
                master.findFiscalPeriodById(periodId).orElseThrow().endDate());
        return transactions.prepareClose(calendar.getId(), "closer");
    }

    private final class Remote implements AutoCloseable {
        private static final String TOKEN = "synthetic-test-period-fence-token-762";
        private final HttpServer server;
        private final AtomicBoolean drop = new AtomicBoolean(true);
        private final AtomicInteger calls = new AtomicInteger();
        Remote(String loseOperation) throws Exception {
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/api/internal/journal-periods/", exchange -> {
                calls.incrementAndGet();
                assertThat(exchange.getRequestHeaders().getFirst("Authorization")).isEqualTo("Bearer " + TOKEN);
                assertThat(exchange.getRequestHeaders().getFirst("X-Service-Identity")).isEqualTo("closing");
                String operation = exchange.getRequestURI().getPath().substring(exchange.getRequestURI().getPath().lastIndexOf('/') + 1);
                JournalPeriodFenceCommand command = mapper.readValue(exchange.getRequestBody(), JournalPeriodFenceCommand.class);
                JournalPeriodFenceResult result = operation.equals("fence") ? journal.fence(command) : journal.release(command);
                if (operation.equals(loseOperation) && drop.compareAndSet(true, false)) {
                    // A truncated successful response is ambiguous after the provider has applied it.
                    // Sending headers prevents HttpURLConnection from transparently retrying a dropped POST.
                    exchange.getResponseHeaders().set("Content-Type", "application/json");
                    exchange.sendResponseHeaders(200, 100);
                    exchange.getResponseBody().write('{');
                    exchange.close();
                    return;
                }
                byte[] body = mapper.writeValueAsBytes(result);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, body.length);
                exchange.getResponseBody().write(body);
                exchange.close();
            });
            server.start();
        }
        ClosingPeriodTransitionService coordinator() {
            return new ClosingPeriodTransitionService(transactions, calendars,
                    new HttpJournalPeriodFenceAdapter(RestClient.builder(),
                            "http://127.0.0.1:" + server.getAddress().getPort(), TOKEN,
                            Duration.ofSeconds(1), Duration.ofSeconds(1)));
        }
        public void close() { server.stop(0); }
    }
}
