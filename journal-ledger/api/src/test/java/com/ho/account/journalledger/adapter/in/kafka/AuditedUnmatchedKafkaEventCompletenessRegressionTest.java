package com.ho.account.journalledger.adapter.in.kafka;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.ho.account.journalledger.application.port.in.JournalUseCase;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * Byte-copyable regression using only APIs present at audited source a97d10ab.
 * The audited listener returns normally; the fixed compatibility path fails closed.
 */
class AuditedUnmatchedKafkaEventCompletenessRegressionTest {

    @Test
    void unmatchedEventCannotReturnSuccessfullyWithoutDurableBrokerIdentity() {
        JournalUseCase journalUseCase = Mockito.mock(JournalUseCase.class);
        when(journalUseCase.createJournalEntryFromEvent(any(), any())).thenReturn(Optional.empty());
        KafkaTransactionListener listener = new KafkaTransactionListener(journalUseCase);

        assertThatThrownBy(() -> listener.listenTransactionEvent(Map.of(
                        "ruleCode", "MISSING_GL18_RULE",
                        "accountingDate", "2026-09-28",
                        "amount", "100.00")))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("retry/DLQ");
    }
}
