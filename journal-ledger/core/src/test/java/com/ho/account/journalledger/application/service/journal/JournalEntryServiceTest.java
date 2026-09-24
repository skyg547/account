package com.ho.account.journalledger.application.service.journal;

import static org.assertj.core.api.Assertions.assertThat;

import com.ho.account.journalledger.application.port.out.JournalPersistencePort;
import com.ho.account.journalledger.application.service.ledger.PostingService;
import com.ho.account.journalledger.application.service.journal.validator.JournalValidationEngine;
import com.ho.account.journalledger.domain.journal.domain.JournalEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class JournalEntryServiceTest {

    @Mock
    private JournalPersistencePort journalPersistencePort;
    @Mock
    private JournalRuleEngine journalRuleEngine;
    @Mock
    private PostingService postingService;
    @Mock
    private JournalValidationEngine journalValidationEngine;

    private JournalEntryService service;

    @BeforeEach
    void setUp() {
        service = new JournalEntryService(journalPersistencePort, journalRuleEngine, postingService, journalValidationEngine);
    }

    @Test
    @DisplayName("전표 생성 시 검증 엔진을 호출한다.")
    void validateCalledOnCreate() {
        JournalEntry entry = new JournalEntry();
        entry.setSlipDate(java.time.LocalDate.now());

        service.createJournalEntry(entry);

        verify(journalValidationEngine).validate(entry);
        verify(journalPersistencePort).save(entry);
    }

    @Test
    @DisplayName("전기는 PostingService 단일 경로로 위임한다.")
    void delegatesPostingToPostingService() {
        service.postJournalEntry(10L, "poster-1");

        verify(postingService).postJournalEntry(10L, "poster-1");
        verify(journalPersistencePort, never()).findById(10L);
        verify(journalPersistencePort, never()).save(org.mockito.ArgumentMatchers.any());
        verifyNoMoreInteractions(postingService);
    }

    @Test
    @DisplayName("승인 요청은 도메인 전이를 거쳐 저장한다.")
    void requestsApprovalThroughDomain() {
        JournalEntry entry = balancedDraft("maker-1");
        when(journalPersistencePort.findById(10L)).thenReturn(java.util.Optional.of(entry));

        service.requestJournalEntryApproval(10L, " MAKER-1 ");

        assertThat(entry.getStatus()).isEqualTo(
                com.ho.account.journalledger.domain.journal.domain.JournalEntryStatus.REQUESTED);
        verify(journalPersistencePort).save(entry);
    }

    private JournalEntry balancedDraft(String maker) {
        JournalEntry entry = new JournalEntry();
        entry.setSlipDate(java.time.LocalDate.of(2026, 9, 25));
        entry.setCreatedBy(maker);
        for (var side : com.ho.account.journalledger.domain.journal.domain.JournalSide.values()) {
            var detail = new com.ho.account.journalledger.domain.journal.domain.JournalDetail();
            detail.setSide(side);
            detail.setAccountCode(side.name());
            detail.setAmount(new java.math.BigDecimal("10.00"));
            detail.setBaseAmount(new java.math.BigDecimal("10.00"));
            entry.addDetail(detail);
        }
        entry.initializeDraft();
        return entry;
    }
}
