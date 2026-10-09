package com.ho.account.journalledger.application.service.journal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ho.account.journalledger.application.port.out.JournalPersistencePort;
import com.ho.account.journalledger.application.port.out.JournalReversalPersistencePort;
import com.ho.account.journalledger.application.port.out.SlipNumberAllocationException;
import com.ho.account.journalledger.application.service.ledger.PostingService;
import com.ho.account.journalledger.application.service.journal.validator.JournalValidationEngine;
import com.ho.account.journalledger.domain.journal.domain.JournalEntry;
import com.ho.account.journalledger.domain.journal.domain.JournalReversalOperation;
import com.ho.account.journalledger.domain.journal.domain.ReversalOperationStatus;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;

@ExtendWith(MockitoExtension.class)
class JournalEntryServiceTest {

    @Mock
    private JournalPersistencePort journalPersistencePort;
    @Mock
    private JournalReversalPersistencePort journalReversalPersistencePort;
    @Mock
    private JournalRuleEngine journalRuleEngine;
    @Mock
    private PostingService postingService;
    @Mock
    private JournalValidationEngine journalValidationEngine;

    private JournalEntryService service;

    @BeforeEach
    void setUp() {
        lenient().when(journalPersistencePort.nextSlipNumber()).thenReturn(1L);
        service = new JournalEntryService(journalPersistencePort, journalReversalPersistencePort,
                journalRuleEngine, postingService, journalValidationEngine);
    }

    @Test
    @DisplayName("전표 생성 시 검증 엔진을 호출한다.")
    void validateCalledOnCreate() {
        JournalEntry entry = new JournalEntry();
        entry.setSlipDate(java.time.LocalDate.now());

        service.createJournalEntry(entry);

        verify(journalValidationEngine).validate(entry);
        verify(journalPersistencePort).save(entry);
        assertThat(entry.getSlipNo()).isEqualTo("JE-" + java.time.format.DateTimeFormatter.BASIC_ISO_DATE.format(entry.getSlipDate()) + "-00000001");
    }

    @Test
    void allocatorFailureDoesNotValidateOrSave() {
        JournalEntry entry = balancedDraft("maker");
        SlipNumberAllocationException unavailable = new SlipNumberAllocationException("database unavailable");
        when(journalPersistencePort.nextSlipNumber()).thenThrow(unavailable);

        assertThatThrownBy(() -> service.createJournalEntry(entry)).isSameAs(unavailable);
        verify(journalValidationEngine, never()).validate(any());
        verify(journalPersistencePort, never()).save(any());
        assertThat(entry.getSlipNo()).isNull();
    }

    @Test
    void exhaustedAllocatorDoesNotValidateOrSave() {
        JournalEntry entry = balancedDraft("maker");
        when(journalPersistencePort.nextSlipNumber()).thenReturn(2_821_109_907_456L);

        assertThatThrownBy(() -> service.createJournalEntry(entry))
                .isInstanceOf(SlipNumberAllocationException.class).hasMessageContaining("범위를 소진");
        verify(journalValidationEngine, never()).validate(any());
        verify(journalPersistencePort, never()).save(any());
    }

    @Test
    void manualSlipCannotOccupyFutureAutomaticNumber() {
        JournalEntry entry = balancedDraft("maker");
        entry.setSlipNo("JE-20260925-00000001");

        assertThatThrownBy(() -> service.createJournalEntry(entry))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("직접 지정할 수 없습니다");
        verify(journalPersistencePort, never()).nextSlipNumber();
        verify(journalValidationEngine, never()).validate(any());
        verify(journalPersistencePort, never()).save(any());
    }

    @Test
    void dateOutsideEightDigitContractFailsBeforeAllocation() {
        JournalEntry entry = balancedDraft("maker");
        entry.setSlipDate(LocalDate.of(10_000, 1, 1));

        assertThatThrownBy(() -> service.createJournalEntry(entry))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("0001~9999");
        verify(journalPersistencePort, never()).nextSlipNumber();
        verify(journalPersistencePort, never()).save(any());
    }

    @Test
    void legacyManualSlipAndAutomaticSlipCanCoexist() {
        JournalEntry manual = balancedDraft("maker");
        manual.setSlipNo("JE-20260925-0001");
        service.createJournalEntry(manual);

        JournalEntry automatic = balancedDraft("maker");
        service.createJournalEntry(automatic);

        assertThat(manual.getSlipNo()).isEqualTo("JE-20260925-0001");
        assertThat(automatic.getSlipNo()).isEqualTo("JE-20260925-00000001");
        verify(journalPersistencePort).nextSlipNumber();
        verify(journalPersistencePort).save(manual);
        verify(journalPersistencePort).save(automatic);
    }

    @Test
    @DisplayName("공개 생성 경로는 caller가 지정한 REVERSAL을 검증·저장 전에 거부한다.")
    void publicCreateRejectsCallerSuppliedReversalBeforeValidationOrSave() {
        JournalEntry entry = balancedDraft("maker");
        entry.setEntryType(" reversal ");

        assertThatThrownBy(() -> service.createJournalEntry(entry))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("역분개 전표는 reverseJournalEntry 유스케이스로만 생성할 수 있습니다.");

        verify(journalValidationEngine, never()).validate(any());
        verify(journalPersistencePort, never()).save(any());
        verifyNoMoreInteractions(journalReversalPersistencePort);
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
        when(journalPersistencePort.findByIdWithDetails(10L)).thenReturn(java.util.Optional.of(entry));

        service.requestJournalEntryApproval(10L, " MAKER-1 ");

        assertThat(entry.getStatus()).isEqualTo(
                com.ho.account.journalledger.domain.journal.domain.JournalEntryStatus.REQUESTED);
        verify(journalPersistencePort).findByIdWithDetails(10L);
        verify(journalPersistencePort).save(entry);
    }

    @Test
    @DisplayName("관리된 reverse 유스케이스는 역분개를 저장하고 순차 중복에는 같은 전표를 반환한다.")
    void sequentialDuplicateReversalsReturnTheSamePersistedJournal() {
        JournalEntry original = postedOriginal();
        when(journalPersistencePort.findByIdWithDetails(759L)).thenReturn(Optional.of(original));
        when(journalReversalPersistencePort.findByOriginalJournalEntryId(759L))
                .thenReturn(Optional.empty());
        when(journalPersistencePort.save(any(JournalEntry.class))).thenAnswer(invocation -> {
            JournalEntry reversal = invocation.getArgument(0);
            reversal.setId(760L);
            return reversal;
        });
        when(journalReversalPersistencePort.save(any(JournalReversalOperation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        JournalEntry first = service.reverseJournalEntry(
                759L, LocalDate.of(2026, 9, 30), "maker", "correction");
        ArgumentCaptor<JournalReversalOperation> operationCaptor =
                ArgumentCaptor.forClass(JournalReversalOperation.class);
        verify(journalReversalPersistencePort).save(operationCaptor.capture());
        JournalReversalOperation operation = operationCaptor.getValue();
        when(journalReversalPersistencePort.findByOriginalJournalEntryId(759L))
                .thenReturn(Optional.of(operation));
        when(journalPersistencePort.findByIdWithDetailsWithoutLock(760L)).thenReturn(Optional.of(first));

        JournalEntry duplicate = service.reverseJournalEntry(
                759L, LocalDate.of(2026, 10, 1), "other-maker", "different retry payload");

        assertThat(duplicate).isSameAs(first);
        assertThat(operation.getStatus()).isEqualTo(ReversalOperationStatus.PENDING);
        assertThat(operation.getOriginalJournalEntryId()).isEqualTo(759L);
        assertThat(operation.getReversalJournalEntryId()).isEqualTo(760L);
        verify(journalPersistencePort, times(1)).save(any(JournalEntry.class));
        verify(journalReversalPersistencePort, times(1)).save(same(operation));
        verify(journalValidationEngine, times(1)).validate(first);
    }

    @Test
    @DisplayName("역분개 회계일 마감 검증 실패는 전표와 작업을 저장하기 전에 전파된다.")
    void closedReversalAccountingDateFailsBeforeAnyPersistence() {
        JournalEntry original = postedOriginal();
        IllegalStateException closed = new IllegalStateException("이미 마감된 기간입니다.");
        when(journalPersistencePort.findByIdWithDetails(759L)).thenReturn(Optional.of(original));
        when(journalReversalPersistencePort.findByOriginalJournalEntryId(759L)).thenReturn(Optional.empty());
        org.mockito.Mockito.doThrow(closed).when(journalValidationEngine).validate(any(JournalEntry.class));

        assertThatThrownBy(() -> service.reverseJournalEntry(
                759L, LocalDate.of(2026, 8, 31), "maker", "closed date"))
                .isSameAs(closed);

        verify(journalPersistencePort, never()).save(any());
        verify(journalReversalPersistencePort, never()).save(any());
    }

    @Test
    @DisplayName("PENDING 역분개 취소는 전표를 비전기 상태로 확정하고 다음 요청에 새 전표 하나를 연결한다.")
    void cancellingPendingReversalAllowsExactlyOneReplacement() {
        JournalEntry original = postedOriginal();
        JournalEntry pending = original.createReversal(
                "maker", LocalDate.of(2026, 9, 30), "correction");
        pending.setId(760L);
        pending.initializeDraft();
        JournalReversalOperation operation = JournalReversalOperation.create(759L, 760L);
        when(journalReversalPersistencePort.findByOriginalJournalEntryId(759L))
                .thenReturn(Optional.of(operation));
        when(journalPersistencePort.findByIdWithDetails(760L)).thenReturn(Optional.of(pending));

        JournalEntry cancelled = service.cancelReversal(759L, " Cancel.Operator ", " abandoned ");

        assertThat(cancelled.getStatus()).isEqualTo(
                com.ho.account.journalledger.domain.journal.domain.JournalEntryStatus.REJECTED);
        assertThat(operation.getStatus()).isEqualTo(ReversalOperationStatus.CANCELLED);

        when(journalPersistencePort.findByIdWithDetails(759L)).thenReturn(Optional.of(original));
        when(journalPersistencePort.save(any(JournalEntry.class))).thenAnswer(invocation -> {
            JournalEntry replacement = invocation.getArgument(0);
            if (replacement != pending) replacement.setId(761L);
            return replacement;
        });
        JournalEntry replacement = service.reverseJournalEntry(
                759L, LocalDate.of(2026, 10, 1), "maker", "replacement");
        when(journalPersistencePort.findByIdWithDetailsWithoutLock(761L)).thenReturn(Optional.of(replacement));
        JournalEntry duplicate = service.reverseJournalEntry(
                759L, LocalDate.of(2026, 10, 2), "maker", "duplicate replacement");

        assertThat(replacement.getId()).isEqualTo(761L);
        assertThat(duplicate).isSameAs(replacement);
        assertThat(operation.getStatus()).isEqualTo(ReversalOperationStatus.PENDING);
        assertThat(operation.getReversalJournalEntryId()).isEqualTo(761L);
        verify(journalPersistencePort, times(2)).save(any(JournalEntry.class));
        verify(journalReversalPersistencePort, times(2)).save(same(operation));
    }

    @Test
    @DisplayName("POSTED 역분개 작업은 취소 시 전표나 작업을 다시 저장하지 않는다.")
    void postedReversalOperationCannotBeCancelled() {
        JournalReversalOperation operation = JournalReversalOperation.create(759L, 760L);
        operation.markPosted(760L);
        when(journalReversalPersistencePort.findByOriginalJournalEntryId(759L))
                .thenReturn(Optional.of(operation));

        assertThatThrownBy(() -> service.cancelReversal(759L, "operator", "too late"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("전기 완료된 역분개 작업");

        verify(journalPersistencePort, never()).findByIdWithDetails(760L);
        verify(journalPersistencePort, never()).save(any());
        verify(journalReversalPersistencePort, never()).save(any());
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

    private JournalEntry postedOriginal() {
        JournalEntry entry = balancedDraft("maker");
        entry.setId(759L);
        entry.setSlipNo("GL759-ORIGINAL");
        entry.setAccountingDate(LocalDate.of(2026, 9, 25));
        entry.setDescription("posted original");
        entry.setCurrencyCode("KRW");
        entry.requestApproval("maker");
        entry.approve("checker");
        entry.post("poster");
        return entry;
    }
}
