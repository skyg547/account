package com.ho.account.contracts.journal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class JournalPeriodFenceContractTest {
    private final JournalPeriodScope scope = new JournalPeriodScope(
            7L, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31));

    @Test
    void immutableReceiptPreservesTheEntireRetryIdentityAcrossHttpJson() throws Exception {
        ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
        JournalPeriodFenceCommand command = new JournalPeriodFenceCommand(
                "close-operation-7", new JournalPeriodCheckpoint(scope, 42), "closing-actor");
        for (JournalPeriodFenceResult.State state : JournalPeriodFenceResult.State.values()) {
            JournalPeriodFenceResult result = new JournalPeriodFenceResult(command, state);
            assertThat(mapper.readValue(mapper.writeValueAsBytes(result), JournalPeriodFenceResult.class))
                    .isEqualTo(result);
        }
    }

    @Test
    void rejectsInvalidScopeAndRevisionRatherThanCreatingAnUnboundedGate() {
        assertThatIllegalArgumentException().isThrownBy(() -> new JournalPeriodScope(0L, scope.startDate(), scope.endDate()));
        assertThatIllegalArgumentException().isThrownBy(() -> new JournalPeriodScope(7L, scope.endDate(), scope.startDate()));
        assertThatNullPointerException().isThrownBy(() -> new JournalPeriodScope(7L, null, scope.endDate()));
        assertThatNullPointerException().isThrownBy(() -> new JournalPeriodCheckpoint(null, 0));
        assertThatIllegalArgumentException().isThrownBy(() -> new JournalPeriodCheckpoint(scope, -1));
        assertThat(new JournalPeriodCheckpoint(scope, Long.MAX_VALUE).revision()).isEqualTo(Long.MAX_VALUE);
    }

    @Test
    void rejectsAmbiguousOrUnboundedOperationAndActorIdentities() {
        JournalPeriodCheckpoint checkpoint = new JournalPeriodCheckpoint(scope, 0);
        for (String operation : new String[] {"", " ", " op", "op ", "x".repeat(101)}) {
            assertThatIllegalArgumentException().isThrownBy(() -> new JournalPeriodFenceCommand(operation, checkpoint, "actor"));
        }
        for (String actor : new String[] {"", " ", " actor", "actor ", "x".repeat(51)}) {
            assertThatIllegalArgumentException().isThrownBy(() -> new JournalPeriodFenceCommand("operation", checkpoint, actor));
        }
        assertThatNullPointerException().isThrownBy(() -> new JournalPeriodFenceCommand("operation", null, "actor"));
    }

    @Test
    void receiptCannotOmitItsIdentityOrState() {
        JournalPeriodFenceCommand command = new JournalPeriodFenceCommand(
                "operation", new JournalPeriodCheckpoint(scope, 0), "actor");
        assertThatNullPointerException().isThrownBy(() -> new JournalPeriodFenceResult(null, JournalPeriodFenceResult.State.ACTIVE));
        assertThatNullPointerException().isThrownBy(() -> new JournalPeriodFenceResult(command, null));
    }
}
