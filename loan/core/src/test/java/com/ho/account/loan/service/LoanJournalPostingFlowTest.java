package com.ho.account.loan.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import com.ho.account.contracts.closing.AccountingPeriodStatusPort;
import com.ho.account.journalledger.application.port.out.LedgerEntryPersistencePort;
import com.ho.account.journalledger.application.service.journal.JournalCreationConflictException;
import com.ho.account.journalledger.application.service.journal.JournalEntryService;
import com.ho.account.journalledger.application.service.ledger.LedgerService;
import com.ho.account.loan.application.port.out.*;
import com.ho.account.loan.domain.*;
import com.ho.account.loan.infrastructure.persistence.LoanPersistenceAdapter;
import com.ho.account.loan.support.DurableLoanJournalTestConfiguration;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** Actual source JPA transaction rollback and independently committed Journal receipt/posting. */
@SpringBootTest(classes = DurableLoanJournalTestConfiguration.class, webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {"spring.config.name=loan-durable-test", "spring.jpa.hibernate.ddl-auto=validate",
                "spring.jpa.mapping-resources=META-INF/journal-ledger-orm.xml", "spring.jpa.open-in-view=false",
                "spring.flyway.enabled=false", "spring.sql.init.mode=never", "spring.cloud.config.enabled=false",
                "spring.cloud.discovery.enabled=false", "spring.cloud.vault.enabled=false", "eureka.client.enabled=false"})
class LoanJournalPostingFlowTest {
    private static final LocalDate DATE = LocalDate.of(2026, 5, 18);
    @Autowired LoanService service;
    @Autowired JournalEntryService journals;
    @Autowired JdbcTemplate jdbc;
    @Autowired PlatformTransactionManager transactionManager;
    @Autowired LoanReferenceDataPort references;
    @Autowired EIRCalculator calculator;
    @Autowired AccountingPeriodStatusPort periods;
    @Autowired LedgerEntryPersistencePort ledgerEntries;
    @Autowired LedgerService ledger;
    @SpyBean LoanPersistenceAdapter source;
    private TransactionTemplate tx;
    private Long loanId;
    private Long typeId;

    @BeforeEach void setup() {
        reset(source, references, calculator, periods, ledgerEntries, ledger);
        tx = new TransactionTemplate(transactionManager);
        for (String table : List.of("loan_events", "recalculation_runs", "eir_amortization_schedules",
                "deferred_items", "loan_disbursals", "loan_accrual_log", "loans", "deferred_item_types",
                "journal_creation_operations", "journal_details", "journal_entries")) {
            jdbc.update("DELETE FROM " + table);
        }
        when(references.requireAccount(anyString(), any())).thenAnswer(call ->
                new LoanReferenceDataPort.AccountReference(call.getArgument(0), "Synthetic account"));
        when(calculator.calculateEIR(any(), any(BigDecimal.class), any(LocalDate.class), anyList()))
                .thenReturn(new BigDecimal("0.0450"));
        loanId = tx.execute(status -> source.saveLoan(Loan.create("LN-SYNTHETIC", 100L, "KRW",
                Loan.LoanType.TERM_LOAN, new BigDecimal("1000"), new BigDecimal("0.0450"), DATE,
                DATE.plusYears(1), Loan.PaymentFrequency.MONTHLY, "loan-maker")).getId());
        typeId = tx.execute(status -> source.saveDeferredItemType(DeferredItemType.create("FEE", "Fee", null,
                DeferredItemType.DeferralMethod.EIR_METHOD,
                DeferredItemType.EirCashFlowTreatment.CUSTOMER_FEE_INFLOW,
                "171900", "401900", true, "loan-maker")).getId());
    }

    @Test void closedPeriodDoesNotSaveLoanDisbursalOrJournalLineage() {
        when(periods.isClosed(DATE)).thenReturn(true);
        assertThatThrownBy(this::disburse).hasMessage("해당 회계 반영일(" + DATE + ")은 이미 마감된 기간입니다.");
        verify(periods).isClosed(DATE);
        verify(ledgerEntries, never()).save(any());
        assertThat(count("loan_disbursals")).isZero(); assertThat(count("journal_creation_operations")).isZero();
        assertThat(count("journal_entries")).isZero();
        assertThat(loan().getStatus()).isEqualTo(Loan.LoanStatus.PENDING_DISBURSEMENT);
    }

    @Test void ledgerWriteFailureDoesNotSaveLoanDisbursalOrJournalLineage() {
        doThrow(new IllegalStateException("ledger write failed")).when(ledgerEntries).save(any());
        assertThatThrownBy(this::disburse).hasMessage("ledger write failed");
        verify(ledgerEntries).save(any()); verify(ledger, never()).updateLedgerBalancesBulk(any());
        assertThat(count("loan_disbursals")).isZero(); assertThat(count("journal_creation_operations")).isZero();
        assertThat(loan().getStatus()).isEqualTo(Loan.LoanStatus.PENDING_DISBURSEMENT);
    }

    @Test void loanDisbursementFlowsThroughLoanJournalAdapterToLedgerPosting() {
        LoanDisbursal result = disburse();
        var posted = journals.getJournalEntryWithDetails(result.getJournalEntryId()).orElseThrow();
        assertThat(posted.getStatus().name()).isEqualTo("POSTED");
        assertThat(posted.getCreatedBy()).isEqualTo("loan-maker");
        assertThat(posted.getApprovedBy()).isEqualTo("service:loan-checker");
        assertThat(posted.getDetails()).hasSize(2);
        assertThat(jdbc.queryForObject("SELECT operation_key FROM journal_creation_operations", String.class))
                .isEqualTo("DISBURSAL_V1");
        assertThat(loan().getStatus()).isEqualTo(Loan.LoanStatus.ACTIVE);
        verify(ledgerEntries).save(any()); verify(ledger).updateLedgerBalancesBulk(any());
    }

    @Test void sourceRollbackAfterDisbursalRemoteCommitReusesOriginalPostedReceipt() {
        doThrow(new IllegalStateException("source commit failure")).when(source).saveDisbursal(any());
        assertThatThrownBy(this::disburse).hasMessage("source commit failure");
        assertThat(count("loan_disbursals")).isZero(); assertThat(count("journal_creation_operations")).isOne();
        assertThat(loan().getStatus()).isEqualTo(Loan.LoanStatus.PENDING_DISBURSEMENT);
        doCallRealMethod().when(source).saveDisbursal(any());
        Long original = jdbc.queryForObject("SELECT journal_entry_id FROM journal_creation_operations", Long.class);
        assertThat(disburse().getJournalEntryId()).isEqualTo(original);
        assertThat(count("journal_entries")).isOne(); verify(ledgerEntries, times(1)).save(any());
    }

    @Test void deferredRollbackRetryRejectsChangedRawContentAndPreservesMultipleSameDayOperations() {
        activate();
        doThrow(new IllegalStateException("source commit failure")).when(source).saveDeferredItem(any());
        assertThatThrownBy(() -> deferred("fee-1", "100.1", DATE.plusMonths(3))).hasMessage("source commit failure");
        assertThat(count("deferred_items")).isZero(); assertThat(count("journal_creation_operations")).isOne();
        doCallRealMethod().when(source).saveDeferredItem(any());
        // Both amounts round to KRW 100; the raw request digest must still detect the change.
        assertThatThrownBy(() -> deferred("fee-1", "100.2", DATE.plusMonths(3)))
                .isInstanceOf(JournalCreationConflictException.class);
        assertThatThrownBy(() -> deferred("fee-1", "100.1", DATE.plusMonths(4)))
                .isInstanceOf(JournalCreationConflictException.class);
        assertThat(count("deferred_items")).isZero();
        var first = deferred("fee-1", "100.10", DATE.plusMonths(3));
        assertThat(deferred("fee-1", "100.1", DATE.plusMonths(3)).getId()).isEqualTo(first.getId());
        assertThatThrownBy(() -> deferred("fee-1", "101", DATE.plusMonths(3)))
                .isInstanceOf(LoanOperationConflictException.class);
        assertThat(deferred("fee-2", "100.1", DATE.plusMonths(3)).getInitialJournalEntryId())
                .isNotEqualTo(first.getInitialJournalEntryId());
        assertThat(count("deferred_items")).isEqualTo(2); assertThat(count("journal_creation_operations")).isEqualTo(2);
        verify(ledgerEntries, times(2)).save(any());
    }

    @Test void recalculationRollbackRejectsChangedMaturityAndCompletedReplayDoesNotApplyPrincipalTwice() {
        activate();
        doThrow(new IllegalStateException("source commit failure")).when(source).saveRecalculationRun(any());
        assertThatThrownBy(() -> recalculate("repay-1", "900", Optional.empty())).hasMessage("source commit failure");
        assertThat(loan().getOutstandingPrincipal()).isEqualByComparingTo("1000");
        assertThat(count("loan_events")).isZero(); assertThat(count("eir_amortization_schedules")).isZero();
        assertThat(count("journal_creation_operations")).isOne();
        doCallRealMethod().when(source).saveRecalculationRun(any());
        assertThatThrownBy(() -> recalculate("repay-1", "900", Optional.of(DATE.plusMonths(10))))
                .isInstanceOf(JournalCreationConflictException.class);
        assertThat(loan().getOutstandingPrincipal()).isEqualByComparingTo("1000");
        assertThat(count("loan_events")).isZero();
        var first = recalculate("repay-1", "900", Optional.empty());
        assertThat(recalculate("repay-1", "900.0", Optional.empty()).getId()).isEqualTo(first.getId());
        assertThatThrownBy(() -> recalculate("repay-1", "800", Optional.empty()))
                .isInstanceOf(LoanOperationConflictException.class);
        assertThat(loan().getOutstandingPrincipal()).isEqualByComparingTo("900");
        assertThat(count("loan_events")).isOne(); assertThat(count("journal_creation_operations")).isOne();
        verify(ledgerEntries, times(1)).save(any());
    }

    @Test void maximumLengthScenarioIdSeparatesChildrenAndCompletedReplayDoesNotRecalculate() {
        activate();
        String key = "s".repeat(64);
        var first = service.reproduceDoDScenario(loanId, "loan-maker", key);
        var replay = service.reproduceDoDScenario(loanId, "loan-maker", key);
        assertThat(replay.getId()).isEqualTo(first.getId());
        assertThat(loan().getOutstandingPrincipal()).isEqualByComparingTo("700");
        assertThat(count("deferred_items")).isOne(); assertThat(count("loan_events")).isOne();
        assertThat(jdbc.queryForList("SELECT operation_key FROM journal_creation_operations", String.class))
                .containsExactlyInAnyOrder("DOD_DEFERRED_V1:" + key, "DOD_RECALCULATION_V1:" + key);
        assertThatThrownBy(() -> service.reproduceDoDScenario(loanId, "different-maker", key))
                .isInstanceOf(LoanOperationConflictException.class);
        verify(ledgerEntries, times(2)).save(any());
    }

    @Test void scenarioSourceRollbackDoesNotRekeyWhenGeneratedDeferredTypeIdChanges() {
        activate();
        doThrow(new IllegalStateException("source commit failure")).when(source).saveDeferredItem(any());
        assertThatThrownBy(() -> service.reproduceDoDScenario(loanId, "loan-maker", "scenario-rollback"))
                .hasMessage("source commit failure");
        assertThat(count("deferred_items")).isZero(); assertThat(count("loan_events")).isZero();
        assertThat(count("journal_creation_operations")).isOne();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM deferred_item_types WHERE code='DOD_DEFERRED_FEE'", Integer.class))
                .isZero();
        doCallRealMethod().when(source).saveDeferredItem(any());
        service.reproduceDoDScenario(loanId, "loan-maker", "scenario-rollback");
        assertThat(count("deferred_items")).isOne(); assertThat(count("loan_events")).isOne();
        assertThat(count("journal_creation_operations")).isEqualTo(2);
        assertThat(loan().getOutstandingPrincipal()).isEqualByComparingTo("700");
        var typeCaptures = org.mockito.ArgumentCaptor.forClass(DeferredItemType.class);
        verify(source, atLeast(2)).saveDeferredItemType(typeCaptures.capture());
        assertThat(typeCaptures.getAllValues().stream().filter(type -> "DOD_DEFERRED_FEE".equals(type.getCode()))
                .map(DeferredItemType::getId).distinct()).hasSize(2);
        verify(ledgerEntries, times(2)).save(any());
    }

    @Test void missingOperationIdFailsBeforeSourceOrJournalWrites() {
        clearInvocations(source);
        assertThatThrownBy(() -> deferred(null, "100", DATE.plusMonths(3)))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("operationId");
        verifyNoInteractions(source); assertThat(count("journal_entries")).isZero();
    }

    private LoanDisbursal disburse() { return service.disburseLoan(loanId, DATE, new BigDecimal("1000"), "loan-maker"); }
    private DeferredItem deferred(String key, String amount, LocalDate end) {
        return service.createDeferredItem(loanId, typeId, new BigDecimal(amount), DATE, end, "loan-maker", key);
    }
    private RecalculationRun recalculate(String key, String principal, Optional<LocalDate> maturity) {
        return service.recalculateLoan(loanId, DATE.plusMonths(3), RecalculationRun.RecalculationReason.EARLY_REPAYMENT,
                "loan-maker", Optional.of(new BigDecimal(principal)), maturity, key);
    }
    private Loan loan() { return tx.execute(status -> source.findLoan(loanId).orElseThrow()); }
    private void activate() { tx.executeWithoutResult(status -> {
        var loan = source.findLoanForUpdate(loanId).orElseThrow();
        loan.activateAfterDisbursal(DATE, loan.getPrincipalAmount(), "loan-maker"); source.saveLoan(loan);
    }); }
    private int count(String table) { return jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class); }
}
