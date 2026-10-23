package com.ho.account.journalledger.adapter.in.event;

import com.ho.account.contracts.journal.JournalEntryCommand;
import com.ho.account.contracts.journal.JournalLineCommand;
import com.ho.account.contracts.journal.JournalPostingPort;
import com.ho.account.contracts.journal.JournalPostingResult;
import com.ho.account.journalledger.application.service.journal.JournalCreationConflictException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class JournalPostingEventListenerTest {

    @Test
    @DisplayName("비동기 전표 이벤트의 위조 identity를 고정 service maker로 교체한다")
    void handleJournalPostingEvent_replacesPayloadIdentityWithTrustedServiceMaker() {
        // given
        JournalPostingPort mockPort = mock(JournalPostingPort.class);
        JournalPostingEventListener listener = new JournalPostingEventListener(mockPort);

        JournalLineCommand debit = new JournalLineCommand("DEBIT", "10100", BigDecimal.valueOf(5000), BigDecimal.valueOf(5000), null, null, "현금 차변");
        JournalLineCommand credit = new JournalLineCommand("CREDIT", "20100", BigDecimal.valueOf(5000), BigDecimal.valueOf(5000), null, null, "대출금 대변");

        JournalEntryCommand command = new JournalEntryCommand(
                LocalDate.of(2026, 8, 12),
                LocalDate.of(2026, 8, 12),
                "비동기 이벤트 전표 전기 테스트",
                "GENERAL",
                "KRW",
                BigDecimal.ONE,
                "spoofed-maker",
                "spoofed-auditor",
                "LOAN",
                "LOAN-999",
                null,
                "LOAN-999:MAIN:v1",
                List.of(debit, credit)
        );

        JournalPostingResult expectedResult = new JournalPostingResult(200L, "SLIP-20260812-0002", "DRAFT");
        when(mockPort.createDraftEntry(any(JournalEntryCommand.class))).thenReturn(expectedResult);

        // when
        JournalPostingResult actualResult = listener.handleJournalPostingEvent(command);

        // then
        assertThat(actualResult).isNotNull();
        assertThat(actualResult.journalEntryId()).isEqualTo(200L);
        assertThat(actualResult.slipNo()).isEqualTo("SLIP-20260812-0002");
        var commandCaptor = org.mockito.ArgumentCaptor.forClass(JournalEntryCommand.class);
        verify(mockPort, times(1)).createDraftEntry(commandCaptor.capture());
        JournalEntryCommand trustedCommand = commandCaptor.getValue();
        assertThat(trustedCommand.createdBy()).isEqualTo("service:journal-spring-event-maker");
        assertThat(trustedCommand.auditUser()).isEqualTo("service:journal-spring-event-maker");
        assertThat(trustedCommand.lineageSourceId()).isEqualTo(command.lineageSourceId());
        assertThat(trustedCommand.operationKey()).isEqualTo(command.operationKey());
        assertThat(trustedCommand.lines()).isEqualTo(command.lines());
    }

    @Test
    void equivalentReplayReturnsSameResultAndChangedReplayConflictPropagates() {
        JournalPostingPort port = mock(JournalPostingPort.class);
        JournalPostingEventListener listener = new JournalPostingEventListener(port);
        JournalEntryCommand command = command(new BigDecimal("100.00"));
        JournalPostingResult existing = new JournalPostingResult(765L, "SPRING-765", "DRAFT");
        when(port.createDraftEntry(any(JournalEntryCommand.class)))
                .thenReturn(existing, existing)
                .thenThrow(new JournalCreationConflictException());

        assertThat(listener.handleJournalPostingEvent(command)).isEqualTo(existing);
        assertThat(listener.handleJournalPostingEvent(command(new BigDecimal("100.0")))).isEqualTo(existing);
        assertThatThrownBy(() -> listener.handleJournalPostingEvent(command(new BigDecimal("101.00"))))
                .isInstanceOf(JournalCreationConflictException.class)
                .hasMessage("Journal creation identity is already linked to different request content.");
        verify(port, times(3)).createDraftEntry(any(JournalEntryCommand.class));
    }

    private static JournalEntryCommand command(BigDecimal amount) {
        return new JournalEntryCommand(
                LocalDate.of(2026, 9, 25), LocalDate.of(2026, 9, 25),
                "spring event retry", "NORMAL", "KRW", BigDecimal.ONE,
                "payload-maker", "payload-maker", "PAYABLE", "INV-765", null, "INV-765:MAIN:v1", List.of(
                        new JournalLineCommand("DEBIT", "10100", amount, amount, null, null, "debit"),
                        new JournalLineCommand("CREDIT", "20100", amount, amount, null, null, "credit")));
    }
}
