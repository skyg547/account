package com.ho.account.journalledger.adapter.in.kafka;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import com.ho.account.journalledger.application.port.in.JournalUseCase;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Reproduces the audited listener's missing-date fallback through its direct-call contract. */
class KafkaAccountingDateAuditedRegressionTest {

    @Test
    void missingAccountingDateFailsBeforeJournalGeneration() {
        JournalUseCase journals = mock(JournalUseCase.class);
        KafkaTransactionListener listener = new KafkaTransactionListener(journals);

        assertThatThrownBy(() -> listener.listenTransactionEvent(Map.of("ruleCode", "SOURCE_EVENT")))
                .isInstanceOf(RuntimeException.class)
                .hasRootCauseInstanceOf(IllegalArgumentException.class)
                .hasRootCauseMessage("Kafka event accountingDate must be an ISO-8601 date");
        verifyNoInteractions(journals);
    }
}
