package com.ho.account.journalledger.adapter.in.web.journal;

import com.ho.account.contracts.journal.JournalEntryCommand;
import com.ho.account.contracts.journal.JournalLineCommand;
import com.ho.account.contracts.journal.JournalPostingPort;
import com.ho.account.contracts.journal.JournalPostingResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class JournalPostingRestControllerTest {

    @Test
    @DisplayName("REST API 통해 전표 전기 요청 시 JournalPostingPort를 성공적으로 호출하고 200 OK 응답을 반환한다")
    void createPosting_success() {
        // given
        JournalPostingPort mockPort = mock(JournalPostingPort.class);
        JournalPostingRestController controller = new JournalPostingRestController(mockPort);

        JournalLineCommand debit = new JournalLineCommand("DEBIT", "10100", BigDecimal.valueOf(1000), BigDecimal.valueOf(1000), null, null, "현금 차변");
        JournalLineCommand credit = new JournalLineCommand("CREDIT", "20100", BigDecimal.valueOf(1000), BigDecimal.valueOf(1000), null, null, "외상매입금 대변");

        JournalEntryCommand command = new JournalEntryCommand(
                LocalDate.of(2026, 8, 12),
                LocalDate.of(2026, 8, 12),
                "REST 전표 전기 테스트",
                "GENERAL",
                "KRW",
                BigDecimal.ONE,
                "USER1",
                "USER1",
                "PAYABLE",
                "INV-100",
                List.of(debit, credit)
        );

        JournalPostingResult expectedResult = new JournalPostingResult(100L, "SLIP-20260812-0001", "DRAFT");
        when(mockPort.createDraftEntry(any(JournalEntryCommand.class))).thenReturn(expectedResult);

        // when
        var response = controller.createPosting(
                " Trusted-Maker ", "ROLE_JOURNAL_MAKER", command);

        // then
        assertThat(response.getStatusCodeValue()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().journalEntryId()).isEqualTo(100L);
        assertThat(response.getBody().slipNo()).isEqualTo("SLIP-20260812-0001");
        ArgumentCaptor<JournalEntryCommand> captor = ArgumentCaptor.forClass(JournalEntryCommand.class);
        verify(mockPort, times(1)).createDraftEntry(captor.capture());
        assertThat(captor.getValue().createdBy()).isEqualTo("trusted-maker");
        assertThat(captor.getValue().auditUser()).isEqualTo("trusted-maker");
        assertThat(captor.getValue().lineageSourceId()).isEqualTo(command.lineageSourceId());
    }
}
