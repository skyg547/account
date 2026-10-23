package com.ho.account.journalledger.infrastructure.persistence;

import com.ho.account.contracts.journal.JournalPeriodCheckpoint;
import com.ho.account.contracts.journal.JournalPeriodFenceCommand;
import com.ho.account.contracts.journal.JournalPeriodFencePort;
import com.ho.account.contracts.journal.JournalPeriodFenceResult;
import com.ho.account.contracts.journal.JournalPeriodFenceResult.State;
import com.ho.account.contracts.journal.JournalPeriodScope;
import com.ho.account.journalledger.application.port.out.BalanceReaggregationControlPort;
import com.ho.account.journalledger.application.port.out.JournalMonetaryAdmissionPort;
import java.sql.Connection;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

/** Database serialization shared by local contract calls, HTTP controls and every monetary writer. */
@Component
public class JdbcJournalPeriodAdmissionAdapter implements JournalMonetaryAdmissionPort, JournalPeriodFencePort {
    private final JdbcTemplate jdbc;
    private final BalanceReaggregationControlPort reaggregation;
    private final TransactionTemplate controlTransaction;

    public JdbcJournalPeriodAdmissionAdapter(JdbcTemplate jdbc, BalanceReaggregationControlPort reaggregation,
                                             PlatformTransactionManager transactionManager) {
        this.jdbc = jdbc;
        this.reaggregation = reaggregation;
        this.controlTransaction = new TransactionTemplate(transactionManager);
        // Fence acknowledgement is sent only after its own commit, also for local monolith callers.
        this.controlTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.controlTransaction.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
    }

    @Override
    public void admit(LocalDate accountingDate) {
        Objects.requireNonNull(accountingDate, "Accounting date is required");
        lockRevision();
        reaggregation.assertOpen();
        requireUnfenced(accountingDate);
        incrementRevision();
    }

    @Override
    public void beginReaggregation(LocalDate startDate) {
        Objects.requireNonNull(startDate, "Reaggregation start is required");
        lockRevision();
        requireUnfenced(startDate);
        incrementRevision();
    }

    @Override
    public void completeReaggregation() {
        lockRevision();
        incrementRevision();
    }

    @Override
    public JournalPeriodCheckpoint checkpoint(JournalPeriodScope scope) {
        Objects.requireNonNull(scope, "Period scope is required");
        return controlTransaction.execute(status -> {
            long revision = lockRevision();
            reaggregation.assertOpen();
            return new JournalPeriodCheckpoint(scope, revision);
        });
    }

    @Override
    public JournalPeriodFenceResult fence(JournalPeriodFenceCommand command) {
        Objects.requireNonNull(command, "Fence command is required");
        return controlTransaction.execute(status -> {
            long revision = lockRevision();
            reaggregation.assertOpen();
            JournalPeriodFenceResult previous = existing(command);
            if (previous != null) {
                // Full payload identity wins over revision changes; a tombstone never becomes ACTIVE again.
                if (previous.state() == State.RELEASED) {
                    throw new IllegalStateException("Journal fence operation was released; use a fresh command");
                }
                return previous;
            }
            if (revision != command.checkpoint().revision()) {
                throw new IllegalStateException("Journal changed after checkpoint; repeat validation from fresh sources");
            }
            insert(command, State.ACTIVE);
            return new JournalPeriodFenceResult(command, State.ACTIVE);
        });
    }

    @Override
    public JournalPeriodFenceResult release(JournalPeriodFenceCommand command) {
        Objects.requireNonNull(command, "Fence command is required");
        return controlTransaction.execute(status -> {
            lockRevision();
            JournalPeriodFenceResult previous = existing(command);
            if (previous == null) {
                // Release can overtake an uncertain HTTP fence. Persist the tombstone before replying.
                insert(command, State.RELEASED);
            } else if (previous.state() == State.RELEASED) {
                return previous;
            } else {
                jdbc.update("UPDATE journal_period_fences SET state = 'RELEASED', released_at = CURRENT_TIMESTAMP"
                        + " WHERE operation_id = ? AND state = 'ACTIVE'", command.operationId());
            }
            incrementRevision();
            return new JournalPeriodFenceResult(command, State.RELEASED);
        });
    }

    private long lockRevision() {
        if (!TransactionSynchronizationManager.isActualTransactionActive()
                || TransactionSynchronizationManager.isCurrentTransactionReadOnly()) {
            throw new IllegalStateException("Journal admission requires an active write transaction");
        }
        Integer isolation = jdbc.execute((ConnectionCallback<Integer>) Connection::getTransactionIsolation);
        if (!Integer.valueOf(Connection.TRANSACTION_READ_COMMITTED).equals(isolation)) {
            throw new IllegalStateException("Journal admission requires READ_COMMITTED transaction isolation");
        }
        List<Long> revisions = jdbc.query("SELECT revision FROM journal_monetary_revision"
                + " WHERE control_id = 1 FOR UPDATE", (rs, row) -> rs.getLong(1));
        if (revisions.size() != 1) {
            throw new IllegalStateException("Journal monetary control is missing; apply migration V20");
        }
        return revisions.get(0);
    }

    private void requireUnfenced(LocalDate accountingDate) {
        // Backdated movement changes successor beginning/ending balances, even before scope.startDate.
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM journal_period_fences"
                + " WHERE state = 'ACTIVE' AND range_end >= ?", Integer.class, accountingDate);
        if (count == null || count > 0) {
            throw new IllegalStateException("Journal monetary writes are fenced for the affected period");
        }
    }

    private void incrementRevision() {
        if (jdbc.update("UPDATE journal_monetary_revision SET revision = revision + 1 WHERE control_id = 1") != 1) {
            throw new IllegalStateException("Journal monetary control is missing");
        }
    }

    private JournalPeriodFenceResult existing(JournalPeriodFenceCommand command) {
        List<JournalPeriodFenceResult> results = jdbc.query("SELECT fiscal_period_id, range_start, range_end,"
                        + " expected_revision, actor, state FROM journal_period_fences WHERE operation_id = ?",
                (rs, row) -> new JournalPeriodFenceResult(new JournalPeriodFenceCommand(command.operationId(),
                        new JournalPeriodCheckpoint(new JournalPeriodScope(rs.getLong(1),
                                rs.getObject(2, LocalDate.class), rs.getObject(3, LocalDate.class)), rs.getLong(4)),
                        rs.getString(5)), State.valueOf(rs.getString(6))), command.operationId());
        if (results.isEmpty()) return null;
        JournalPeriodFenceResult previous = results.get(0);
        if (!previous.command().equals(command)) {
            throw new IllegalStateException("Fence operation payload does not match its immutable command");
        }
        return previous;
    }

    private void insert(JournalPeriodFenceCommand command, State state) {
        JournalPeriodScope scope = command.checkpoint().scope();
        jdbc.update("INSERT INTO journal_period_fences (operation_id, fiscal_period_id, range_start, range_end,"
                        + " expected_revision, actor, state, released_at) VALUES (?, ?, ?, ?, ?, ?, ?, "
                        + (state == State.RELEASED ? "CURRENT_TIMESTAMP" : "NULL") + ")",
                command.operationId(), scope.fiscalPeriodId(), scope.startDate(), scope.endDate(),
                command.checkpoint().revision(), command.actor(), state.name());
    }
}
