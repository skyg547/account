package com.ho.account.journalledger.application.service.ledger;

import com.ho.account.common.adapter.FiscalPeriodAccountingPeriodStatusAdapter;
import com.ho.account.contracts.closing.AccountingPeriodStatusPort;
import com.ho.account.contracts.masterdata.FiscalPeriodControlPort;
import com.ho.account.contracts.masterdata.FiscalPeriodRef;
import com.ho.account.journalledger.application.service.journal.validator.ClosingLockValidationFilter;
import com.ho.account.journalledger.domain.journal.domain.JournalDetail;
import com.ho.account.journalledger.domain.journal.domain.JournalEntry;
import com.ho.account.journalledger.domain.journal.domain.JournalEntryStatus;
import com.ho.account.journalledger.domain.journal.domain.JournalSide;
import com.ho.account.journalledger.application.port.out.JournalPersistencePort;
import com.ho.account.journalledger.application.port.out.JournalReversalPersistencePort;
import com.ho.account.journalledger.application.port.out.LedgerEntryPersistencePort;
import com.ho.account.journalledger.domain.journal.domain.JournalReversalOperation;
import com.ho.account.journalledger.domain.journal.domain.ReversalOperationStatus;
import com.ho.account.journalledger.domain.ledger.domain.GeneralLedger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PostingServiceTest {

    private static final LocalDate ACCOUNTING_DATE = LocalDate.of(2026, 5, 10);

    @Mock
    private JournalPersistencePort journalPersistencePort;
    @Mock
    private JournalReversalPersistencePort journalReversalPersistencePort;
    @Mock
    private LedgerEntryPersistencePort ledgerEntryPersistencePort;
    @Mock
    private LedgerService ledgerService;
    @Mock
    private AccountingPeriodStatusPort accountingPeriodStatusPort;
    @Mock
    private FiscalPeriodControlPort fiscalPeriodControlPort;

    private PostingService service;

    @BeforeEach
    void setUp() {
        service = postingService(accountingPeriodStatusPort);
    }

    @Test
    @DisplayName("승인된 전표를 POSTED로 전환하고 GL/SL 엔트리와 잔액을 생성한다.")
    void postsApprovedJournalEntry() {
        JournalEntry entry = approvedEntry();
        when(journalPersistencePort.findByIdWithDetails(1L)).thenReturn(Optional.of(entry));
        JournalState before = JournalState.capture(entry);
        when(accountingPeriodStatusPort.isClosed(ACCOUNTING_DATE)).thenAnswer(invocation -> {
            assertUnchangedWithoutWrites(entry, before);
            return false;
        });

        service.postJournalEntry(1L, "poster-1");

        GeneralLedger ledger = assertPostedOnce(entry, "poster-1");
        assertThat(JournalState.capture(entry).details()).isEqualTo(before.details());
        InOrder order = inOrder(accountingPeriodStatusPort, journalPersistencePort,
                ledgerEntryPersistencePort, ledgerService);
        order.verify(journalPersistencePort).findByIdWithDetails(1L);
        order.verify(accountingPeriodStatusPort).isClosed(ACCOUNTING_DATE);
        order.verify(journalPersistencePort).save(entry);
        order.verify(ledgerEntryPersistencePort).save(ledger);
        order.verify(ledgerService).updateLedgerBalancesBulk(entry.getDetails());
        verifyNoMoreInteractions(accountingPeriodStatusPort);
        assertThat(ledger.journalEntryId()).isEqualTo(1L);
        assertThat(ledger.currencyCode()).isEqualTo("KRW");
        assertThat(ledger.postings()).hasSize(2);
        assertThat(ledger.postings().get(0).debit().amount()).isEqualByComparingTo("100.00");
        assertThat(ledger.postings().get(0).credit().amount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(ledger.postings().get(1).debit().amount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(ledger.postings().get(1).credit().amount()).isEqualByComparingTo("100.00");
        assertThat(ledger.postings()).allSatisfy(posting -> {
            assertThat(posting.postingDate()).isEqualTo(LocalDate.of(2026, 5, 10));
            assertThat(posting.fiscalYear()).isEqualTo("2026");
            assertThat(posting.fiscalPeriod()).isEqualTo("05");
            assertThat(posting.lineageSourceType()).isEqualTo("UNIT_TEST");
            assertThat(posting.lineageSourceId()).isEqualTo("SRC-1");
            assertThat(posting.businessPartnerCode()).isEqualTo("BP-1");
            assertThat(posting.departmentCode()).isEqualTo("DEPT-1");
        });
        assertThat(ledger.postings()).extracting(GeneralLedger.Posting::journalDetailId)
                .containsExactly(11L, 12L);
        assertThat(ledger.postings()).extracting(GeneralLedger.Posting::accountCode)
                .containsExactly("10100", "40100");
        assertThat(ledger.postings().get(0).baseDebit().amount()).isEqualByComparingTo("100.00");
        assertThat(ledger.postings().get(1).baseCredit().amount()).isEqualByComparingTo("100.00");
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    @DisplayName("명시 처리자와 SYSTEM 경로 모두 마감된 회계기간이면 원본과 원장을 변경하지 않는다.")
    void rejectsClosedPeriod(boolean systemPoster) {
        JournalEntry entry = approvedEntry();
        JournalState before = JournalState.capture(entry);
        when(journalPersistencePort.findByIdWithDetails(1L)).thenReturn(Optional.of(entry));
        when(accountingPeriodStatusPort.isClosed(ACCOUNTING_DATE)).thenReturn(true);

        assertThatThrownBy(() -> post(systemPoster))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("이미 마감된 기간");

        assertUnchangedWithoutWrites(entry, before);
        verify(accountingPeriodStatusPort).isClosed(ACCOUNTING_DATE);
        verifyNoMoreInteractions(accountingPeriodStatusPort);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    @DisplayName("기간 조회 오류를 그대로 전파하고 승인 전표와 모든 쓰기를 보존한다.")
    void propagatesPeriodLookupFailure(boolean systemPoster) {
        JournalEntry entry = approvedEntry();
        JournalState before = JournalState.capture(entry);
        IllegalStateException lookupFailure = new IllegalStateException("synthetic period lookup failure");
        when(journalPersistencePort.findByIdWithDetails(1L)).thenReturn(Optional.of(entry));
        when(accountingPeriodStatusPort.isClosed(ACCOUNTING_DATE)).thenThrow(lookupFailure);

        assertThatThrownBy(() -> post(systemPoster)).isSameAs(lookupFailure);

        assertUnchangedWithoutWrites(entry, before);
        verify(accountingPeriodStatusPort).isClosed(ACCOUNTING_DATE);
        verifyNoMoreInteractions(accountingPeriodStatusPort);
    }

    @Test
    @DisplayName("기본 overload도 회계일자의 OPEN 확인 후 SYSTEM으로 한 번 전기한다.")
    void postsOpenPeriodWithSystemActor() {
        JournalEntry entry = approvedEntry();
        when(journalPersistencePort.findByIdWithDetails(1L)).thenReturn(Optional.of(entry));
        when(accountingPeriodStatusPort.isClosed(ACCOUNTING_DATE)).thenReturn(false);

        service.postJournalEntry(1L);

        assertPostedOnce(entry, "SYSTEM");
        verify(accountingPeriodStatusPort).isClosed(ACCOUNTING_DATE);
        verifyNoMoreInteractions(accountingPeriodStatusPort);
    }

    @ParameterizedTest
    @EnumSource(value = JournalEntryStatus.class, names = {"DRAFT", "POSTED"})
    @DisplayName("OPEN 응답이 가능해도 비승인·이미 전기된 전표를 기간 조회 전에 거부한다.")
    void rejectsInvalidStatusBeforePeriodLookup(JournalEntryStatus status) {
        JournalEntry entry = draftEntry();
        if (status == JournalEntryStatus.POSTED) {
            entry.requestApproval("maker-1");
            entry.approve("approver-1");
            entry.post("original-poster");
        }
        JournalState before = JournalState.capture(entry);
        when(journalPersistencePort.findByIdWithDetails(1L)).thenReturn(Optional.of(entry));

        // 기간 대역의 기본 응답은 OPEN(false)이어도 승인 검증을 우회할 수 없습니다.
        assertThatThrownBy(() -> service.postJournalEntry(1L, "poster-1"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("승인된 전표만 원장으로 전기할 수 있습니다.");

        assertUnchangedWithoutWrites(entry, before);
        verifyNoInteractions(accountingPeriodStatusPort);
    }

    @Test
    @DisplayName("operation이 연결되지 않은 REVERSAL 전표는 금융 검증과 모든 쓰기 전에 거부한다.")
    void rejectsReversalWithoutOperationBeforeFinancialWrites() {
        JournalEntry entry = approvedEntry();
        entry.setEntryType("REVERSAL");
        JournalState before = JournalState.capture(entry);
        when(journalPersistencePort.findByIdWithDetails(1L)).thenReturn(Optional.of(entry));
        when(journalReversalPersistencePort.findByReversalJournalEntryId(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.postJournalEntry(1L, "poster"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("역분개 작업이 연결되지 않은 역분개 전표는 전기할 수 없습니다.");

        assertUnchangedWithoutWrites(entry, before);
        verifyNoInteractions(accountingPeriodStatusPort);
        verify(journalReversalPersistencePort).findByReversalJournalEntryId(1L);
        verify(journalReversalPersistencePort, never()).save(any());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    @DisplayName("승인 후 거래금액 또는 기준통화 금액이 훼손된 전표는 기간 조회 전에 거부한다.")
    void rejectsUnbalancedJournalBeforePeriodLookup(boolean baseCurrency) {
        JournalEntry entry = approvedEntry();
        if (baseCurrency) {
            entry.getDetails().get(0).setBaseAmount(new BigDecimal("101.00"));
        } else {
            entry.getDetails().get(0).setAmount(new BigDecimal("101.00"));
            // KRW 환산 의미는 유지한 채 거래통화 차대 불일치 통제 자체를 검증합니다.
            entry.getDetails().get(0).setBaseAmount(new BigDecimal("101.00"));
        }
        JournalState before = JournalState.capture(entry);
        when(journalPersistencePort.findByIdWithDetails(1L)).thenReturn(Optional.of(entry));

        assertThatThrownBy(() -> service.postJournalEntry(1L, "poster-1"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(baseCurrency ? "기준통화" : "거래통화 차대변");

        assertUnchangedWithoutWrites(entry, before);
        verifyNoInteractions(accountingPeriodStatusPort);
    }

    @Test
    @DisplayName("승인 후 환율 의미가 훼손된 외화 전표는 원장 쓰기와 기간 조회 전에 거부한다.")
    void rejectsMalformedForeignJournalBeforeLedgerWrite() {
        JournalEntry entry = approvedEntry();
        entry.setCurrencyCode("USD");
        entry.setExchangeRate(new BigDecimal("1300"));
        JournalState before = JournalState.capture(entry);
        when(journalPersistencePort.findByIdWithDetails(1L)).thenReturn(Optional.of(entry));

        assertThatThrownBy(() -> service.postJournalEntry(1L, "poster-1"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("기준통화");

        assertUnchangedWithoutWrites(entry, before);
        verifyNoInteractions(accountingPeriodStatusPort);
    }

    @Test
    @DisplayName("저장된 상세 ID가 없는 전표는 기간 조회 전에 거부하여 lineage 검증을 유지한다.")
    void rejectsMissingDetailIdBeforePeriodLookup() {
        JournalEntry entry = approvedEntry();
        entry.getDetails().get(0).setId(null);
        JournalState before = JournalState.capture(entry);
        when(journalPersistencePort.findByIdWithDetails(1L)).thenReturn(Optional.of(entry));

        assertThatThrownBy(() -> service.postJournalEntry(1L, "poster-1"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("원장 전기 전에 전표 상세가 먼저 저장되어야 합니다.");

        assertUnchangedWithoutWrites(entry, before);
        verifyNoInteractions(accountingPeriodStatusPort);
    }

    @ParameterizedTest
    @ValueSource(strings = {"OPEN", "CLOSED", "PERMANENTLY_CLOSED"})
    @DisplayName("실제 Fiscal adapter와 필터를 거쳐 OPEN만 전기하고 두 마감 상태는 거부한다.")
    void respectsFiscalPeriodAdapterStatus(String closingStatus) {
        service = postingService(new FiscalPeriodAccountingPeriodStatusAdapter(fiscalPeriodControlPort));
        JournalEntry entry = approvedEntry();
        JournalState before = JournalState.capture(entry);
        when(journalPersistencePort.findByIdWithDetails(1L)).thenReturn(Optional.of(entry));
        when(fiscalPeriodControlPort.findFiscalPeriod("2026", "05")).thenReturn(Optional.of(
                new FiscalPeriodRef(5L, "2026", "05", LocalDate.of(2026, 5, 1),
                        LocalDate.of(2026, 5, 31), closingStatus)));

        if ("OPEN".equals(closingStatus)) {
            service.postJournalEntry(1L, "poster-1");
            GeneralLedger ledger = assertPostedOnce(entry, "poster-1");
            assertThat(ledger.accountingDate()).isEqualTo(ACCOUNTING_DATE);
            assertThat(JournalState.capture(entry).details()).isEqualTo(before.details());
        } else {
            assertThatThrownBy(() -> service.postJournalEntry(1L, "poster-1"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("이미 마감된 기간");
            assertUnchangedWithoutWrites(entry, before);
        }

        verify(fiscalPeriodControlPort).findFiscalPeriod("2026", "05");
        verifyNoMoreInteractions(fiscalPeriodControlPort);
        verifyNoInteractions(accountingPeriodStatusPort);
    }

    @Test
    @DisplayName("실제 Fiscal adapter에서 회계기간이 없으면 승인 전표와 원장을 변경하지 않는다.")
    void rejectsMissingFiscalPeriod() {
        service = postingService(new FiscalPeriodAccountingPeriodStatusAdapter(fiscalPeriodControlPort));
        JournalEntry entry = approvedEntry();
        JournalState before = JournalState.capture(entry);
        when(journalPersistencePort.findByIdWithDetails(1L)).thenReturn(Optional.of(entry));
        when(fiscalPeriodControlPort.findFiscalPeriod("2026", "05")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.postJournalEntry(1L, "poster-1"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Fiscal period is missing for accounting date " + ACCOUNTING_DATE);

        assertUnchangedWithoutWrites(entry, before);
        verify(fiscalPeriodControlPort).findFiscalPeriod("2026", "05");
        verifyNoMoreInteractions(fiscalPeriodControlPort);
    }

    @Test
    @DisplayName("PENDING 역분개 전표를 전기하면 원장 반영 뒤 작업도 POSTED로 저장한다.")
    void marksPendingReversalOperationPostedAfterFinancialWrites() {
        JournalEntry entry = approvedEntry();
        JournalReversalOperation operation = JournalReversalOperation.create(759L, entry.getId());
        when(journalPersistencePort.findByIdWithDetails(1L)).thenReturn(Optional.of(entry));
        when(journalReversalPersistencePort.findByReversalJournalEntryId(1L))
                .thenReturn(Optional.of(operation));
        when(accountingPeriodStatusPort.isClosed(ACCOUNTING_DATE)).thenReturn(false);

        service.postJournalEntry(1L, "reversal-poster");

        assertThat(operation.getStatus()).isEqualTo(ReversalOperationStatus.POSTED);
        assertThat(operation.getPostedAt()).isNotNull();
        verify(journalReversalPersistencePort).save(operation);
        InOrder order = inOrder(journalPersistencePort, ledgerEntryPersistencePort,
                ledgerService, journalReversalPersistencePort);
        order.verify(journalPersistencePort).save(entry);
        order.verify(ledgerEntryPersistencePort).save(any(GeneralLedger.class));
        order.verify(ledgerService).updateLedgerBalancesBulk(entry.getDetails());
        order.verify(journalReversalPersistencePort).save(operation);
    }

    @Test
    @DisplayName("역분개 원장 쓰기가 실패하면 작업은 PENDING이고 POSTED 작업 저장을 시작하지 않는다.")
    void financialWriteFailureLeavesReversalOperationPending() {
        JournalEntry entry = approvedEntry();
        JournalReversalOperation operation = JournalReversalOperation.create(759L, entry.getId());
        IllegalStateException failure = new IllegalStateException("synthetic balance failure");
        when(journalPersistencePort.findByIdWithDetails(1L)).thenReturn(Optional.of(entry));
        when(journalReversalPersistencePort.findByReversalJournalEntryId(1L))
                .thenReturn(Optional.of(operation));
        when(accountingPeriodStatusPort.isClosed(ACCOUNTING_DATE)).thenReturn(false);
        org.mockito.Mockito.doThrow(failure).when(ledgerService).updateLedgerBalancesBulk(entry.getDetails());

        assertThatThrownBy(() -> service.postJournalEntry(1L, "reversal-poster")).isSameAs(failure);

        assertThat(operation.getStatus()).isEqualTo(ReversalOperationStatus.PENDING);
        assertThat(operation.getPostedAt()).isNull();
        verify(journalReversalPersistencePort, never()).save(any());
    }

    private PostingService postingService(AccountingPeriodStatusPort periodStatusPort) {
        return new PostingService(journalPersistencePort, journalReversalPersistencePort,
                ledgerEntryPersistencePort, ledgerService,
                new ClosingLockValidationFilter(periodStatusPort));
    }

    private void post(boolean systemPoster) {
        if (systemPoster) {
            service.postJournalEntry(1L);
        } else {
            service.postJournalEntry(1L, "poster-1");
        }
    }

    private GeneralLedger assertPostedOnce(JournalEntry entry, String poster) {
        assertThat(entry.getStatus()).isEqualTo(JournalEntryStatus.POSTED);
        assertThat(entry.getApprovedBy()).isEqualTo("approver-1");
        assertThat(entry.getAuditUser()).isEqualTo(poster.toLowerCase(java.util.Locale.ROOT));
        verify(journalPersistencePort).findByIdWithDetails(entry.getId());
        verify(journalPersistencePort).save(entry);
        ArgumentCaptor<GeneralLedger> ledgerCaptor = ArgumentCaptor.forClass(GeneralLedger.class);
        verify(ledgerEntryPersistencePort).save(ledgerCaptor.capture());
        verify(ledgerService).updateLedgerBalancesBulk(entry.getDetails());
        verifyNoMoreInteractions(journalPersistencePort, ledgerEntryPersistencePort, ledgerService);
        return ledgerCaptor.getValue();
    }

    private void assertUnchangedWithoutWrites(JournalEntry entry, JournalState before) {
        assertThat(JournalState.capture(entry)).isEqualTo(before);
        verify(journalPersistencePort, never()).save(any());
        verifyNoInteractions(ledgerEntryPersistencePort, ledgerService);
    }

    private JournalEntry approvedEntry() {
        JournalEntry entry = draftEntry();
        entry.requestApproval("maker-1");
        entry.approve("approver-1");
        return entry;
    }

    private JournalEntry draftEntry() {
        JournalEntry entry = new JournalEntry();
        entry.setId(1L);
        entry.setSlipNo("JE-20260510-0001");
        // 작성일과 회계일을 다른 월로 두어 잘못된 기간 조회가 우연히 통과하지 않게 합니다.
        entry.setSlipDate(LocalDate.of(2026, 4, 30));
        entry.setAccountingDate(ACCOUNTING_DATE);
        entry.setCurrencyCode("KRW");
        entry.setLineageSourceType("UNIT_TEST");
        entry.setLineageSourceId("SRC-1");
        entry.setCreatedBy("maker-1");
        entry.addDetail(detail(11L, JournalSide.DEBIT, "10100"));
        entry.addDetail(detail(12L, JournalSide.CREDIT, "40100"));
        entry.initializeDraft();
        return entry;
    }

    private JournalDetail detail(Long id, JournalSide side, String accountCode) {
        JournalDetail detail = new JournalDetail();
        detail.setId(id);
        detail.setSide(side);
        detail.setAccountCode(accountCode);
        detail.setAmount(new BigDecimal("100.00"));
        detail.setBaseAmount(new BigDecimal("100.00"));
        detail.setDepartmentCode("DEPT-1");
        detail.setBusinessPartnerCode("BP-1");
        detail.setDetailDescription("synthetic line " + id);
        detail.setAuditUser("detail-author");
        return detail;
    }

    private record JournalState(
            JournalEntryStatus status, String approvedBy, String auditUser, List<DetailState> details) {
        private static JournalState capture(JournalEntry entry) {
            return new JournalState(entry.getStatus(), entry.getApprovedBy(), entry.getAuditUser(),
                    entry.getDetails().stream().map(DetailState::capture).toList());
        }
    }

    // 가변 상세 객체의 참조만 보관하면 사후 변조를 놓치므로 원래 값도 독립적으로 고정합니다.
    private record DetailState(JournalDetail original, Long id, JournalEntry owner, JournalSide side,
                               String accountCode, BigDecimal amount, BigDecimal baseAmount,
                               String departmentCode, String businessPartnerCode, String description,
                               LocalDateTime createdAt, LocalDateTime updatedAt, String auditUser) {
        private static DetailState capture(JournalDetail detail) {
            return new DetailState(detail, detail.getId(), detail.getJournalEntry(), detail.getSide(),
                    detail.getAccountCode(), detail.getAmount(), detail.getBaseAmount(),
                    detail.getDepartmentCode(), detail.getBusinessPartnerCode(), detail.getDetailDescription(),
                    detail.getCreatedAt(), detail.getUpdatedAt(), detail.getAuditUser());
        }
    }
}
