package com.ho.account.closing;

import com.ho.account.contracts.journal.*;
import java.util.HashMap;
import java.util.Map;

/** Test provider for Closing saga semantics; actual Journal database fencing is tested in Journal/core. */
final class ControlledJournalPeriodFence implements JournalPeriodFencePort {
    private long revision;
    private final Map<String, JournalPeriodFenceResult> operations = new HashMap<>();

    @Override public synchronized JournalPeriodCheckpoint checkpoint(JournalPeriodScope scope) {
        return new JournalPeriodCheckpoint(scope, revision);
    }
    @Override public synchronized JournalPeriodFenceResult fence(JournalPeriodFenceCommand command) {
        var previous = operations.get(command.operationId());
        if (previous != null) {
            if (!previous.command().equals(command) || previous.state() != JournalPeriodFenceResult.State.ACTIVE) {
                throw new IllegalStateException("Command collision or released operation");
            }
            return previous;
        }
        if (command.checkpoint().revision() != revision) throw new IllegalStateException("Stale Journal checkpoint");
        var result = new JournalPeriodFenceResult(command, JournalPeriodFenceResult.State.ACTIVE);
        operations.put(command.operationId(), result);
        return result;
    }
    @Override public synchronized JournalPeriodFenceResult release(JournalPeriodFenceCommand command) {
        var previous = operations.get(command.operationId());
        if (previous != null && !previous.command().equals(command)) throw new IllegalStateException("Command collision");
        if (previous != null && previous.state() == JournalPeriodFenceResult.State.RELEASED) return previous;
        revision++;
        var result = new JournalPeriodFenceResult(command, JournalPeriodFenceResult.State.RELEASED);
        operations.put(command.operationId(), result);
        return result;
    }
    synchronized JournalPeriodFenceResult result(String operationId) { return operations.get(operationId); }
}
