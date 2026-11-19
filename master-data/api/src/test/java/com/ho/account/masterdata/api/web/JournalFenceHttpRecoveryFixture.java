package com.ho.account.masterdata.api.web;

import com.ho.account.contracts.journal.JournalPeriodCheckpoint;
import com.ho.account.contracts.journal.JournalPeriodFenceCommand;
import com.ho.account.contracts.journal.JournalPeriodFencePort;
import com.ho.account.contracts.journal.JournalPeriodFenceResult;
import com.ho.account.contracts.journal.JournalPeriodFenceResult.State;
import com.ho.account.contracts.journal.JournalPeriodScope;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.support.TransactionTemplate;

/** Independent committed Journal receipt fixture; ACK loss occurs only after the release commits. */
final class JournalFenceHttpRecoveryFixture implements JournalPeriodFencePort {
    private final JdbcTemplate jdbc;
    private final TransactionTemplate transactions;
    private final List<JournalPeriodFenceCommand> releaseAttempts = new ArrayList<>();
    private boolean loseReleaseAck;

    JournalFenceHttpRecoveryFixture() {
        var source = new DriverManagerDataSource("jdbc:h2:mem:journal-http-" + UUID.randomUUID()
                + ";DB_CLOSE_DELAY=-1", "sa", "");
        jdbc = new JdbcTemplate(source);
        transactions = new TransactionTemplate(new DataSourceTransactionManager(source));
        jdbc.execute("CREATE TABLE journal_fixture_receipts (operation_id VARCHAR(100) PRIMARY KEY, "
                + "period_id BIGINT NOT NULL, start_date DATE NOT NULL, end_date DATE NOT NULL, "
                + "revision BIGINT NOT NULL, actor VARCHAR(50) NOT NULL, state VARCHAR(10) NOT NULL, "
                + "release_effects INTEGER NOT NULL)");
    }

    @Override public JournalPeriodCheckpoint checkpoint(JournalPeriodScope scope) {
        throw new UnsupportedOperationException("This recovery fixture starts with an existing close checkpoint");
    }

    @Override public JournalPeriodFenceResult fence(JournalPeriodFenceCommand command) {
        return transactions.execute(status -> {
            var existing = find(command.operationId());
            if (existing != null) {
                requireExact(command, existing);
                if (existing.state() == State.RELEASED) throw new IllegalStateException("Released operation cannot fence again");
                return existing;
            }
            insert(command, State.ACTIVE, 0);
            return new JournalPeriodFenceResult(command, State.ACTIVE);
        });
    }

    @Override public JournalPeriodFenceResult release(JournalPeriodFenceCommand command) {
        releaseAttempts.add(command);
        JournalPeriodFenceResult released = transactions.execute(status -> {
            var existing = find(command.operationId());
            if (existing == null) {
                insert(command, State.RELEASED, 1);
            } else {
                requireExact(command, existing);
                jdbc.update("UPDATE journal_fixture_receipts SET state='RELEASED', release_effects=release_effects+1 "
                        + "WHERE operation_id=? AND state='ACTIVE'", command.operationId());
            }
            return new JournalPeriodFenceResult(command, State.RELEASED);
        });
        if (loseReleaseAck) {
            loseReleaseAck = false;
            throw new IllegalStateException("Synthetic Journal release committed but acknowledgement lost");
        }
        return released;
    }

    void loseNextReleaseAck() { loseReleaseAck = true; }
    List<JournalPeriodFenceCommand> releaseAttempts() { return List.copyOf(releaseAttempts); }

    State state(JournalPeriodFenceCommand command) {
        var receipt = find(command.operationId());
        requireExact(command, receipt);
        return receipt.state();
    }

    int releaseEffects(JournalPeriodFenceCommand command) {
        requireExact(command, find(command.operationId()));
        return jdbc.queryForObject("SELECT release_effects FROM journal_fixture_receipts WHERE operation_id=?",
                Integer.class, command.operationId());
    }

    private JournalPeriodFenceResult find(String operationId) {
        return jdbc.query("SELECT * FROM journal_fixture_receipts WHERE operation_id=?", (row, index) ->
                new JournalPeriodFenceResult(new JournalPeriodFenceCommand(row.getString("operation_id"),
                        new JournalPeriodCheckpoint(new JournalPeriodScope(row.getLong("period_id"),
                                row.getDate("start_date").toLocalDate(), row.getDate("end_date").toLocalDate()),
                                row.getLong("revision")), row.getString("actor")), State.valueOf(row.getString("state"))),
                operationId).stream().findFirst().orElse(null);
    }

    private void insert(JournalPeriodFenceCommand command, State state, int releaseEffects) {
        var scope = command.checkpoint().scope();
        jdbc.update("INSERT INTO journal_fixture_receipts VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                command.operationId(), scope.fiscalPeriodId(), scope.startDate(), scope.endDate(),
                command.checkpoint().revision(), command.actor(), state.name(), releaseEffects);
    }

    private static void requireExact(JournalPeriodFenceCommand command, JournalPeriodFenceResult receipt) {
        if (receipt == null || !command.equals(receipt.command())) {
            throw new IllegalStateException("Journal receipt identity mismatch");
        }
    }
}
