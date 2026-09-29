package com.ho.account.closing.application.service;

import com.ho.account.closing.application.port.out.ClosingCalendarPersistencePort;
import com.ho.account.closing.application.port.out.PeriodLockPersistencePort;
import com.ho.account.closing.domain.ClosingCalendar;
import com.ho.account.closing.domain.ClosingCalendar.ClosingCalendarStatus;
import com.ho.account.closing.domain.PeriodLock;
import com.ho.account.closing.domain.PeriodLock.PeriodLockType;
import com.ho.account.closing.infrastructure.external.ClosingStatusAdapter;
import com.ho.account.contracts.masterdata.FiscalPeriodControlPort;
import com.ho.account.contracts.masterdata.FiscalPeriodRef;
import com.ho.account.journalledger.application.port.out.JournalPersistencePort;
import com.ho.account.journalledger.application.port.out.LedgerEntryPersistencePort;
import com.ho.account.journalledger.application.service.journal.validator.ClosingLockValidationFilter;
import com.ho.account.journalledger.application.service.ledger.LedgerService;
import com.ho.account.journalledger.application.service.ledger.PostingService;
import com.ho.account.journalledger.domain.journal.domain.JournalDetail;
import com.ho.account.journalledger.domain.journal.domain.JournalEntry;
import com.ho.account.journalledger.domain.journal.domain.JournalEntryStatus;
import com.ho.account.journalledger.domain.journal.domain.JournalSide;
import com.ho.account.journalledger.domain.ledger.domain.GeneralLedger;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Assembles the real Closing query/adapter and Journal consumers against synthetic port state.
 * This proves their shared-port behavior, not standalone Journal bean selection or a commit fence.
 */
class ClosingJournalAdmissionIntegrationTest {

    private static final LocalDate ACCOUNTING_DATE = LocalDate.of(2026, 5, 10);
    private final FiscalPeriodControlPort fiscalPeriods = mock(FiscalPeriodControlPort.class);
    private final ClosingCalendarPersistencePort calendars = mock(ClosingCalendarPersistencePort.class);
    private final PeriodLockPersistencePort locks = mock(PeriodLockPersistencePort.class);
    private final JournalPersistencePort journals = mock(JournalPersistencePort.class);
    private final LedgerEntryPersistencePort ledgers = mock(LedgerEntryPersistencePort.class);
    private final LedgerService balances = mock(LedgerService.class);

    private FiscalPeriodRef fiscalPeriod;
    private ClosingCalendar calendar;
    private PeriodLock activeLock;
    private ClosingLockValidationFilter filter;
    private PostingService postingService;

    @BeforeEach
    void setUp() {
        fiscalPeriod = fiscalPeriod("OPEN");
        calendar = new ClosingCalendar();
        calendar.setId(20L);
        calendar.setFiscalYear("2026");
        calendar.setFiscalPeriod("05");
        calendar.setStatus(ClosingCalendarStatus.OPEN);
        // Mutable port state makes a second admission read observe changes after approval.
        when(fiscalPeriods.findFiscalPeriod("2026", "05"))
                .thenAnswer(invocation -> Optional.ofNullable(fiscalPeriod));
        when(calendars.findByFiscalYearAndFiscalPeriod("2026", "05"))
                .thenAnswer(invocation -> Optional.ofNullable(calendar));
        when(locks.findByFiscalPeriodId(5L))
                .thenAnswer(invocation -> Optional.ofNullable(activeLock));

        ClosingAdmissionService admission = new ClosingAdmissionService(fiscalPeriods, calendars, locks);
        filter = new ClosingLockValidationFilter(new ClosingStatusAdapter(admission));
        postingService = new PostingService(journals, ledgers, balances, filter);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void openPeriodPostsApprovedBalancedJournalExactlyOnce(boolean systemPoster) {
        JournalEntry entry = approvedEntry();
        JournalState before = JournalState.capture(entry);
        when(journals.findByIdWithDetails(1L)).thenReturn(Optional.of(entry));

        post(systemPoster);

        assertThat(entry.getStatus()).isEqualTo(JournalEntryStatus.POSTED);
        assertThat(entry.getAuditUser()).isEqualTo(systemPoster ? "system" : "poster-1");
        assertThat(JournalState.capture(entry).details()).isEqualTo(before.details());
        verify(journals).findByIdWithDetails(1L);
        verify(journals).save(entry);
        ArgumentCaptor<GeneralLedger> ledger = ArgumentCaptor.forClass(GeneralLedger.class);
        verify(ledgers).save(ledger.capture());
        verify(balances).updateLedgerBalancesBulk(entry.getDetails());
        verifyNoMoreInteractions(journals, ledgers, balances);
        assertThat(ledger.getValue().accountingDate()).isEqualTo(ACCOUNTING_DATE);
        assertThat(ledger.getValue().postings()).hasSize(2);
        assertThat(ledger.getValue().postings().get(0).debit().amount()).isEqualByComparingTo("100.00");
        assertThat(ledger.getValue().postings().get(1).credit().amount()).isEqualByComparingTo("100.00");
        verify(fiscalPeriods).findFiscalPeriod("2026", "05");
        verify(calendars).findByFiscalYearAndFiscalPeriod("2026", "05");
        verify(locks).findByFiscalPeriodId(5L);
    }

    @ParameterizedTest
    @EnumSource(PeriodLockType.class)
    void everyActiveLockRejectsValidationAndPostingWithoutWrites(PeriodLockType lockType) {
        activeLock = lock(lockType);

        assertRejectedWithoutWrites(approvedEntry(), false);
    }

    @ParameterizedTest
    @EnumSource(value = ClosingCalendarStatus.class, names = "OPEN", mode = EnumSource.Mode.EXCLUDE)
    void everyNonOpenCalendarRejectsValidationAndPostingWithoutWrites(ClosingCalendarStatus status) {
        calendar.setStatus(status);

        assertRejectedWithoutWrites(approvedEntry(), false);
    }

    @Test
    void calendarWithUnknownStatusRejectsWithoutWrites() {
        calendar.setStatus(null);

        assertRejectedWithoutWrites(approvedEntry(), false);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"CLOSED", "PERMANENTLY_CLOSED", "UNKNOWN", "open", " OPEN "})
    void onlyExplicitMasterOpenCanAdmitJournal(String status) {
        fiscalPeriod = fiscalPeriod(status);

        assertRejectedWithoutWrites(approvedEntry(), false);
    }

    @Test
    void missingFiscalPeriodRejectsWithoutWrites() {
        fiscalPeriod = null;

        assertRejectedWithoutWrites(approvedEntry(), false);
    }

    @Test
    void missingClosingCalendarRejectsWithoutWrites() {
        calendar = null;

        assertRejectedWithoutWrites(approvedEntry(), false);
    }

    @ParameterizedTest
    @ValueSource(strings = {"fiscal", "calendar", "lock"})
    void eachLookupFailureReachesPostingBeforeAnyMutation(String failingLookup) {
        IllegalStateException failure = new IllegalStateException("synthetic " + failingLookup + " failure");
        switch (failingLookup) {
            case "fiscal" -> when(fiscalPeriods.findFiscalPeriod("2026", "05")).thenThrow(failure);
            case "calendar" -> when(calendars.findByFiscalYearAndFiscalPeriod("2026", "05")).thenThrow(failure);
            case "lock" -> when(locks.findByFiscalPeriodId(5L)).thenThrow(failure);
            default -> throw new AssertionError("Unexpected lookup " + failingLookup);
        }
        JournalEntry entry = approvedEntry();
        JournalState before = JournalState.capture(entry);
        when(journals.findByIdWithDetails(1L)).thenReturn(Optional.of(entry));

        assertThatThrownBy(() -> postingService.postJournalEntry(1L, "poster-1")).isSameAs(failure);

        assertUnchangedWithoutWrites(entry, before);
    }

    @ParameterizedTest
    @ValueSource(strings = {"lock", "calendar", "fiscal"})
    void stateChangedAfterApprovalIsReadAgainAtPosting(String changedState) {
        JournalEntry entry = draftEntry();
        assertThatCode(() -> filter.validate(entry)).doesNotThrowAnyException();
        entry.requestApproval("journal-author");
        entry.approve("approver-1");
        switch (changedState) {
            case "lock" -> activeLock = lock(PeriodLockType.ALL_TRANSACTIONS);
            case "calendar" -> calendar.start("closing-operator");
            case "fiscal" -> fiscalPeriod = fiscalPeriod("CLOSED");
            default -> throw new AssertionError("Unexpected state " + changedState);
        }

        assertRejectedWithoutWrites(entry, false);
    }

    @ParameterizedTest
    @EnumSource(PeriodLockType.class)
    void systemActorAndAdjustmentTextDoNotBypassAnyLock(PeriodLockType lockType) {
        activeLock = lock(lockType);
        JournalEntry entry = approvedEntry();
        entry.setEntryType("ADJUSTMENT");
        entry.setLineageSourceType("CLOSING_ADJUSTMENT");
        entry.setDescription("SYSTEM ADJUSTMENT");

        assertRejectedWithoutWrites(entry, true);
    }

    private void assertRejectedWithoutWrites(JournalEntry entry, boolean systemPoster) {
        JournalState before = JournalState.capture(entry);
        when(journals.findByIdWithDetails(1L)).thenReturn(Optional.of(entry));

        assertThatThrownBy(() -> filter.validate(entry)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> post(systemPoster)).isInstanceOf(IllegalStateException.class);

        assertUnchangedWithoutWrites(entry, before);
    }

    private void assertUnchangedWithoutWrites(JournalEntry entry, JournalState before) {
        assertThat(JournalState.capture(entry)).isEqualTo(before);
        verify(journals).findByIdWithDetails(1L);
        verify(journals, never()).save(any());
        verifyNoMoreInteractions(journals);
        verifyNoInteractions(ledgers, balances);
        verify(calendars, never()).save(any());
        verify(locks, never()).save(any());
        verify(locks, never()).delete(any());
    }

    private void post(boolean systemPoster) {
        if (systemPoster) {
            postingService.postJournalEntry(1L);
        } else {
            postingService.postJournalEntry(1L, "poster-1");
        }
    }

    private FiscalPeriodRef fiscalPeriod(String status) {
        return new FiscalPeriodRef(5L, "2026", "05", LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 31), status);
    }

    private PeriodLock lock(PeriodLockType type) {
        PeriodLock lock = new PeriodLock();
        lock.assignFiscalPeriod(5L, "2026", "05");
        lock.setLockType(type);
        return lock;
    }

    private JournalEntry approvedEntry() {
        JournalEntry entry = draftEntry();
        entry.requestApproval("journal-author");
        entry.approve("approver-1");
        return entry;
    }

    private JournalEntry draftEntry() {
        JournalEntry entry = new JournalEntry();
        entry.setId(1L);
        entry.setSlipNo("JE-20260510-0001");
        // Different months expose accidental use of the document date for admission.
        entry.setSlipDate(LocalDate.of(2026, 4, 30));
        entry.setAccountingDate(ACCOUNTING_DATE);
        entry.setEntryType("NORMAL");
        entry.setCurrencyCode("KRW");
        entry.setCreatedBy("journal-author");
        entry.setLineageSourceType("SYNTHETIC_TEST");
        entry.setLineageSourceId("SOURCE-1");
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
        detail.setDepartmentCode("SYNTHETIC-DEPT");
        detail.setBusinessPartnerCode("SYNTHETIC-BP");
        detail.setDetailDescription("synthetic line " + id);
        detail.setAuditUser("detail-author");
        return detail;
    }

    private record JournalState(JournalEntryStatus status, String auditUser, LocalDateTime createdAt,
                                LocalDateTime updatedAt, List<DetailState> details) {
        private static JournalState capture(JournalEntry entry) {
            return new JournalState(entry.getStatus(), entry.getAuditUser(), entry.getCreatedAt(),
                    entry.getUpdatedAt(), entry.getDetails().stream().map(DetailState::capture).toList());
        }
    }

    // Keep immutable values as well as object identity so a mutated detail cannot hide in a snapshot.
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
