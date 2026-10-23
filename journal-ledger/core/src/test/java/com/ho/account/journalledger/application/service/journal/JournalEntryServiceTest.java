package com.ho.account.journalledger.application.service.journal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ho.account.journalledger.application.port.in.JournalCreationIdentity;
import com.ho.account.journalledger.application.port.out.JournalCreationOperationPersistencePort;
import com.ho.account.contracts.closing.AccountingPeriodStatusPort;
import com.ho.account.journalledger.application.port.out.JournalPersistencePort;
import com.ho.account.journalledger.application.port.out.JournalReversalPersistencePort;
import com.ho.account.journalledger.application.port.out.SlipNumberAllocationException;
import com.ho.account.journalledger.application.service.ledger.PostingService;
import com.ho.account.journalledger.application.service.journal.validator.JournalValidationEngine;
import com.ho.account.journalledger.application.service.journal.validator.ClosingLockValidationFilter;
import com.ho.account.journalledger.domain.journal.domain.JournalEntry;
import com.ho.account.journalledger.domain.journal.domain.JournalCreationOperation;
import com.ho.account.journalledger.domain.journal.domain.JournalReversalOperation;
import com.ho.account.journalledger.domain.journal.domain.ReversalOperationStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;
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
    private JournalCreationOperationPersistencePort journalCreationOperationPersistencePort;
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
        service = new JournalEntryService(journalPersistencePort, journalCreationOperationPersistencePort,
                journalReversalPersistencePort,
                journalRuleEngine, postingService, journalValidationEngine);
    }

    @Test
    @DisplayName("같은 생성 identity와 fingerprint 재시도는 현재 검증을 다시 실행하지 않고 기존 전표를 반환한다.")
    void equivalentCreationRetryReturnsExistingJournalWithoutRevalidation() {
        JournalCreationIdentity identity = JournalCreationIdentity.primary("ISSUE_765", "RETRY-1");
        JournalEntry retryRequest = balancedDraft("maker");
        retryRequest.setLineageSourceType(identity.sourceType());
        retryRequest.setLineageSourceId(identity.sourceId());
        String fingerprint = JournalCreationFingerprint.calculate(retryRequest);
        JournalCreationOperation operation = JournalCreationOperation.create(
                identity.sourceType(), identity.sourceId(), identity.operationKey(),
                JournalCreationFingerprint.CURRENT_VERSION, fingerprint, 765L);
        JournalEntry existing = balancedDraft("maker");
        existing.setId(765L);
        existing.setSlipNo("JE-20260925-0765");
        when(journalCreationOperationPersistencePort.findByIdentity(identity))
                .thenReturn(Optional.of(operation));
        when(journalPersistencePort.findByIdWithDetailsWithoutLock(765L)).thenReturn(Optional.of(existing));

        JournalEntry result = service.createJournalEntry(identity, retryRequest);

        assertThat(result).isSameAs(existing);
        assertThat(result.getDetails()).hasSize(2);
        verify(journalCreationOperationPersistencePort).acquireOperationLock(identity);
        verify(journalCreationOperationPersistencePort).findByIdentity(identity);
        verify(journalPersistencePort).findByIdWithDetailsWithoutLock(765L);
        verify(journalValidationEngine, never()).validate(any());
        verify(journalPersistencePort, never()).save(any());
        verify(journalCreationOperationPersistencePort, never()).save(any());
    }

    @Test
    @DisplayName("같은 생성 identity의 금융 내용 변경은 명시적 conflict이며 검증과 쓰기를 수행하지 않는다.")
    void changedCreationRetryThrowsClearConflictBeforeRevalidationOrWrite() {
        JournalCreationIdentity identity = JournalCreationIdentity.primary("ISSUE_765", "RETRY-2");
        JournalEntry firstRequest = balancedDraft("maker");
        firstRequest.setLineageSourceType(identity.sourceType());
        firstRequest.setLineageSourceId(identity.sourceId());
        JournalCreationOperation operation = JournalCreationOperation.create(
                identity.sourceType(), identity.sourceId(), identity.operationKey(),
                JournalCreationFingerprint.CURRENT_VERSION,
                JournalCreationFingerprint.calculate(firstRequest), 766L);
        when(journalCreationOperationPersistencePort.findByIdentity(identity))
                .thenReturn(Optional.of(operation));
        JournalEntry changedRetry = balancedDraft("maker");
        changedRetry.setLineageSourceType(identity.sourceType());
        changedRetry.setLineageSourceId(identity.sourceId());
        changedRetry.setDescription("changed financial request");

        assertThatThrownBy(() -> service.createJournalEntry(identity, changedRetry))
                .isInstanceOf(JournalCreationConflictException.class)
                .hasMessage("Journal creation identity is already linked to different request content.");

        verify(journalCreationOperationPersistencePort).acquireOperationLock(identity);
        verify(journalCreationOperationPersistencePort).findByIdentity(identity);
        verify(journalValidationEngine, never()).validate(any());
        verify(journalPersistencePort, never()).save(any());
        verify(journalPersistencePort, never()).findById(any());
        verify(journalCreationOperationPersistencePort, never()).save(any());
    }

    @Test
    void committedEventRetryReturnsJournalBeforeRuleReevaluation() {
        LocalDate date = LocalDate.of(2026, 9, 25);
        Map<String, Object> event = Map.of(
                "ruleCode", "PAYABLE", "lineageSourceType", "PAYABLE",
                "lineageSourceId", "INV-765", "journalOperationKey", "INV-765:v1",
                "amount", new java.math.BigDecimal("10.00"));
        JournalCreationIdentity identity = JournalCreationIdentity.fromEvent(event);
        JournalEntry generated = balancedDraft("service:journal-kafka-maker");
        generated.setLineageSourceType(identity.sourceType());
        generated.setLineageSourceId(identity.sourceId());
        generated.setId(765L);
        JournalCreationOperation receipt = JournalCreationOperation.create(
                identity.sourceType(), identity.sourceId(), identity.operationKey(),
                JournalEventRequestFingerprint.VERSION,
                JournalEventRequestFingerprint.calculate(event, date), 765L);
        when(journalCreationOperationPersistencePort.findByIdentity(identity))
                .thenReturn(Optional.empty(), Optional.of(receipt));
        AtomicBoolean ruleActive = new AtomicBoolean(true);
        when(journalRuleEngine.generateJournalEntry(event, date)).thenAnswer(
                ignored -> ruleActive.get() ? Optional.of(generated) : Optional.empty());
        when(journalPersistencePort.save(generated)).thenReturn(generated);
        when(journalPersistencePort.findByIdWithDetailsWithoutLock(765L)).thenReturn(Optional.of(generated));

        assertThat(service.createJournalEntryFromEvent(identity, event, date).orElseThrow()).isSameAs(generated);
        // The rule is now inactive. A second lookup would return empty and falsely acknowledge it.
        ruleActive.set(false);
        Map<String, Object> reorderedRetry = new HashMap<>(event);
        reorderedRetry.put("amount", 10);
        assertThat(service.createJournalEntryFromEvent(identity, reorderedRetry, date).orElseThrow()).isSameAs(generated);

        verify(journalRuleEngine, times(1)).generateJournalEntry(any(), any());
        verify(journalPersistencePort, times(1)).save(any());
        verify(journalCreationOperationPersistencePort, times(1)).save(any());
        verify(journalCreationOperationPersistencePort, times(2)).acquireOperationLock(identity);
    }

    @Test
    void changedEventAmountConflictsBeforeRuleEvaluationEvenWhenRuleInactive() {
        LocalDate date = LocalDate.of(2026, 9, 25);
        Map<String, Object> original = Map.of(
                "ruleCode", "PAYABLE", "lineageSourceType", "PAYABLE",
                "lineageSourceId", "INV-765", "journalOperationKey", "INV-765:v1",
                "amount", new java.math.BigDecimal("10.00"));
        JournalCreationIdentity identity = JournalCreationIdentity.fromEvent(original);
        JournalCreationOperation receipt = JournalCreationOperation.create(
                identity.sourceType(), identity.sourceId(), identity.operationKey(),
                JournalEventRequestFingerprint.VERSION,
                JournalEventRequestFingerprint.calculate(original, date), 765L);
        when(journalCreationOperationPersistencePort.findByIdentity(identity)).thenReturn(Optional.of(receipt));
        Map<String, Object> changed = new HashMap<>(original);
        changed.put("amount", new java.math.BigDecimal("11.00"));

        assertThatThrownBy(() -> service.createJournalEntryFromEvent(identity, changed, date))
                .isInstanceOf(JournalCreationConflictException.class);
        verify(journalRuleEngine, never()).generateJournalEntry(any(), any());
        verify(journalPersistencePort, never()).save(any());
        verify(journalCreationOperationPersistencePort, never()).save(any());
    }

    @Test
    @DisplayName("전표 생성 시 검증 엔진을 호출한다.")
    void validateCalledOnCreate() {
        JournalEntry entry = new JournalEntry();
        entry.setSlipDate(java.time.LocalDate.now());
        JournalEntryService compatibilityService = new JournalEntryService(
                journalPersistencePort, journalRuleEngine, postingService, journalValidationEngine);

        compatibilityService.createJournalEntry(entry);

        verify(journalValidationEngine).validate(entry);
        verify(journalPersistencePort).save(entry);
        assertThat(entry.getSlipNo()).isEqualTo("JE-" + java.time.format.DateTimeFormatter.BASIC_ISO_DATE.format(entry.getSlipDate()) + "-00000001");
    }

    @Test
    void closedSourceEventDateFailsBeforeJournalPersistence() {
        LocalDate sourceDate = LocalDate.of(2026, 8, 31);
        Map<String, Object> event = Map.of("ruleCode", "SOURCE_EVENT");
        JournalEntry generated = balancedDraft("service:journal-kafka-maker");
        generated.setAccountingDate(sourceDate);
        when(journalRuleEngine.generateJournalEntry(event, sourceDate)).thenReturn(Optional.of(generated));
        AccountingPeriodStatusPort periods = org.mockito.Mockito.mock(AccountingPeriodStatusPort.class);
        when(periods.isClosed(sourceDate)).thenReturn(true);
        JournalEntryService realPeriodService = new JournalEntryService(
                journalPersistencePort, journalReversalPersistencePort, journalRuleEngine,
                postingService, new JournalValidationEngine(List.of(new ClosingLockValidationFilter(periods))));

        assertThatThrownBy(() -> realPeriodService.createJournalEntryFromEvent(event, sourceDate))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("이미 마감된 기간");

        verify(journalRuleEngine).generateJournalEntry(event, sourceDate);
        verify(periods).isClosed(sourceDate);
        verify(journalPersistencePort, never()).save(any());
    }

    @Test
    void explicitOpenAdjustmentDateCreatesDraftWithoutReplacingItWithSourceDate() {
        LocalDate sourceDate = LocalDate.of(2026, 8, 31);
        LocalDate chosenDate = LocalDate.of(2026, 10, 1);
        Map<String, Object> event = Map.of(
                "ruleCode", "ADJUSTMENT", "sourceEventDate", sourceDate.toString());
        JournalEntry generated = balancedDraft("accounting-admin");
        generated.setSlipDate(chosenDate);
        generated.setAccountingDate(chosenDate);
        when(journalRuleEngine.generateJournalEntry(event, chosenDate)).thenReturn(Optional.of(generated));
        AccountingPeriodStatusPort periods = org.mockito.Mockito.mock(AccountingPeriodStatusPort.class);
        when(periods.isClosed(chosenDate)).thenReturn(false);
        when(journalPersistencePort.save(generated)).thenReturn(generated);
        JournalEntryService realPeriodService = new JournalEntryService(
                journalPersistencePort, journalReversalPersistencePort, journalRuleEngine,
                postingService, new JournalValidationEngine(List.of(new ClosingLockValidationFilter(periods))));

        JournalEntry saved = realPeriodService.createJournalEntryFromEvent(event, chosenDate).orElseThrow();

        assertThat(saved).isSameAs(generated);
        assertThat(saved.getStatus().name()).isEqualTo("DRAFT");
        assertThat(saved.getAccountingDate()).isEqualTo(chosenDate).isNotEqualTo(sourceDate);
        verify(journalRuleEngine).generateJournalEntry(event, chosenDate);
        verify(periods).isClosed(chosenDate);
        verify(periods, never()).isClosed(sourceDate);
        verify(journalPersistencePort).save(generated);
    }

    @Test
    void allocatorFailureDoesNotValidateOrSave() {
        JournalEntry entry = balancedDraft("maker");
        SlipNumberAllocationException unavailable = new SlipNumberAllocationException("database unavailable");
        when(journalPersistencePort.nextSlipNumber()).thenThrow(unavailable);

        assertThatThrownBy(() -> directCompatibilityService().createJournalEntry(entry)).isSameAs(unavailable);
        verify(journalValidationEngine, never()).validate(any());
        verify(journalPersistencePort, never()).save(any());
        assertThat(entry.getSlipNo()).isNull();
    }

    @Test
    void exhaustedAllocatorDoesNotValidateOrSave() {
        JournalEntry entry = balancedDraft("maker");
        when(journalPersistencePort.nextSlipNumber()).thenReturn(2_821_109_907_456L);

        assertThatThrownBy(() -> directCompatibilityService().createJournalEntry(entry))
                .isInstanceOf(SlipNumberAllocationException.class).hasMessageContaining("범위를 소진");
        verify(journalValidationEngine, never()).validate(any());
        verify(journalPersistencePort, never()).save(any());
    }

    @Test
    void manualSlipCannotOccupyFutureAutomaticNumber() {
        JournalEntry entry = balancedDraft("maker");
        entry.setSlipNo("JE-20260925-00000001");

        assertThatThrownBy(() -> directCompatibilityService().createJournalEntry(entry))
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

        assertThatThrownBy(() -> directCompatibilityService().createJournalEntry(entry))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("0001~9999");
        verify(journalPersistencePort, never()).nextSlipNumber();
        verify(journalPersistencePort, never()).save(any());
    }

    @Test
    void legacyManualSlipAndAutomaticSlipCanCoexist() {
        JournalEntry manual = balancedDraft("maker");
        manual.setSlipNo("JE-20260925-0001");
        directCompatibilityService().createJournalEntry(manual);

        JournalEntry automatic = balancedDraft("maker");
        directCompatibilityService().createJournalEntry(automatic);

        assertThat(manual.getSlipNo()).isEqualTo("JE-20260925-0001");
        assertThat(automatic.getSlipNo()).isEqualTo("JE-20260925-00000001");
        verify(journalPersistencePort).nextSlipNumber();
        verify(journalPersistencePort).save(manual);
        verify(journalPersistencePort).save(automatic);
    }

    @Test
    void productionDirectCreateRequiresExplicitOperationIdentityEvenWithSlipNo() {
        JournalEntry entry = balancedDraft("maker");
        entry.setSlipNo("LEGACY-765");

        assertThatThrownBy(() -> service.createJournalEntry(entry))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Explicit journal creation operation identity");
        verify(journalValidationEngine, never()).validate(any());
        verify(journalPersistencePort, never()).save(any());
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

    private JournalEntryService directCompatibilityService() {
        // Exercises the shared slip allocator without bypassing the production identity guard.
        return new JournalEntryService(journalPersistencePort, journalRuleEngine,
                postingService, journalValidationEngine);
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
