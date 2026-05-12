package com.ho.account.loan.service;

import com.ho.account.journalledger.application.port.in.JournalUseCase;
import com.ho.account.journalledger.domain.journal.domain.JournalDetail;
import com.ho.account.journalledger.domain.journal.domain.JournalEntry;
import com.ho.account.journalledger.domain.journal.domain.JournalSide;
import com.ho.account.loan.domain.Loan;
import com.ho.account.loan.domain.LoanDisbursal;
import com.ho.account.loan.infrastructure.persistence.DeferredItemRepository;
import com.ho.account.loan.infrastructure.persistence.DeferredItemTypeRepository;
import com.ho.account.loan.infrastructure.persistence.EIRAmortizationScheduleRepository;
import com.ho.account.loan.infrastructure.persistence.LoanDisbursalRepository;
import com.ho.account.loan.infrastructure.persistence.LoanEventRepository;
import com.ho.account.loan.infrastructure.persistence.LoanRepository;
import com.ho.account.loan.infrastructure.persistence.RecalculationRunRepository;
import com.ho.account.masterdata.core.application.port.out.AccountSubjectPersistencePort;
import com.ho.account.masterdata.core.application.port.out.BusinessPartnerPersistencePort;
import com.ho.account.masterdata.core.application.port.out.CurrencyPersistencePort;
import com.ho.account.masterdata.core.domain.model.AccountSubject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoanServiceTest {

    @Mock
    private LoanRepository loanRepository;
    @Mock
    private LoanDisbursalRepository loanDisbursalRepository;
    @Mock
    private LoanEventRepository loanEventRepository;
    @Mock
    private DeferredItemTypeRepository deferredItemTypeRepository;
    @Mock
    private DeferredItemRepository deferredItemRepository;
    @Mock
    private EIRAmortizationScheduleRepository eirAmortizationScheduleRepository;
    @Mock
    private RecalculationRunRepository recalculationRunRepository;
    @Mock
    private EIRCalculator eirCalculator;
    @Mock
    private BusinessPartnerPersistencePort businessPartnerPersistencePort;
    @Mock
    private CurrencyPersistencePort currencyPersistencePort;
    @Mock
    private AccountSubjectPersistencePort accountSubjectPersistencePort;
    @Mock
    private JournalUseCase journalUseCase;

    private LoanService service;

    @BeforeEach
    void setUp() {
        service = new LoanService(
                loanRepository,
                loanDisbursalRepository,
                loanEventRepository,
                deferredItemTypeRepository,
                deferredItemRepository,
                eirAmortizationScheduleRepository,
                recalculationRunRepository,
                eirCalculator,
                businessPartnerPersistencePort,
                currencyPersistencePort,
                accountSubjectPersistencePort,
                journalUseCase);
    }

    @Test
    @DisplayName("대출 실행은 JournalUseCase로 균형 전표를 생성하고 실행 이력에 연결한다.")
    void disburseLoanCreatesJournalThroughUseCase() {
        Loan loan = new Loan();
        loan.setId(1L);
        loan.setLoanNumber("LN-2026-001");

        AccountSubject cashAccount = account("101000");
        AccountSubject loanReceivableAccount = account("131000");
        JournalEntry savedJournal = new JournalEntry();
        savedJournal.setSlipNo("JE-LOAN-1");

        when(loanRepository.findById(1L)).thenReturn(Optional.of(loan));
        when(accountSubjectPersistencePort.findByCode("101000")).thenReturn(Optional.of(cashAccount));
        when(accountSubjectPersistencePort.findByCode("131000")).thenReturn(Optional.of(loanReceivableAccount));
        when(journalUseCase.createJournalEntry(any(JournalEntry.class))).thenReturn(savedJournal);
        when(loanDisbursalRepository.save(any(LoanDisbursal.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LoanDisbursal disbursal = service.disburseLoan(
                1L,
                LocalDate.of(2026, 5, 10),
                new BigDecimal("1000000.00"),
                "loan-user");

        ArgumentCaptor<JournalEntry> journalCaptor = ArgumentCaptor.forClass(JournalEntry.class);
        verify(journalUseCase).createJournalEntry(journalCaptor.capture());

        JournalEntry journal = journalCaptor.getValue();
        assertThat(journal.getAccountingDate()).isEqualTo(LocalDate.of(2026, 5, 10));
        assertThat(journal.getDescription()).isEqualTo("LN-2026-001 loan disbursal");
        assertThat(journal.getCreatedBy()).isEqualTo("loan-user");
        assertThat(journal.getLineageSourceType()).isEqualTo("LOAN_DISBURSAL");
        assertThat(journal.getLineageSourceId()).isEqualTo("1");
        assertThat(journal.getDetails()).hasSize(2);

        JournalDetail debit = journal.getDetails().get(0);
        JournalDetail credit = journal.getDetails().get(1);
        assertThat(debit.getSide()).isEqualTo(JournalSide.DEBIT);
        assertThat(debit.getAccountSubject().getCode()).isEqualTo("131000");
        assertThat(debit.getAmount()).isEqualByComparingTo("1000000.00");
        assertThat(credit.getSide()).isEqualTo(JournalSide.CREDIT);
        assertThat(credit.getAccountSubject().getCode()).isEqualTo("101000");
        assertThat(credit.getAmount()).isEqualByComparingTo("1000000.00");

        assertThat(disbursal.getJournalEntry()).isSameAs(savedJournal);
        assertThat(disbursal.getAuditUser()).isEqualTo("loan-user");
    }

    private AccountSubject account(String code) {
        AccountSubject account = new AccountSubject();
        account.setCode(code);
        account.setName("ACCOUNT-" + code);
        return account;
    }
}
