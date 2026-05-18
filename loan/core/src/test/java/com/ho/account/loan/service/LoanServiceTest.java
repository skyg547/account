package com.ho.account.loan.service;

import com.ho.account.journalledger.application.port.in.JournalUseCase;
import com.ho.account.journalledger.domain.journal.domain.JournalDetail;
import com.ho.account.journalledger.domain.journal.domain.JournalEntry;
import com.ho.account.journalledger.domain.journal.domain.JournalEntryStatus;
import com.ho.account.journalledger.domain.journal.domain.JournalSide;
import com.ho.account.loan.domain.Loan;
import com.ho.account.loan.domain.LoanDisbursal;
import com.ho.account.loan.dto.LoanRequestDto;
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
import com.ho.account.masterdata.core.domain.model.BusinessPartner;
import com.ho.account.masterdata.core.domain.model.Currency;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.InOrder;

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
    private LoanAccountingProperties accountingProperties;

    @BeforeEach
    void setUp() {
        accountingProperties = new LoanAccountingProperties();
        accountingProperties.setCashAccountCode("101999");
        accountingProperties.setLoanReceivableAccountCode("131999");
        accountingProperties.setDeferredAssetAccountCode("171999");
        accountingProperties.setRecognizedIncomeAccountCode("401999");

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
                accountingProperties,
                journalUseCase);
    }

    @Test
    @DisplayName("대출 생성 요청 DTO는 거래처/통화 참조를 서비스 검증 경로로 전달한다.")
    void createLoanFromRequestDtoResolvesMasterReferences() {
        LoanRequestDto request = new LoanRequestDto();
        request.setLoanNumber("LN-2026-DTO");
        request.setBusinessPartnerId(100L);
        request.setCurrencyCode("KRW");
        request.setLoanType(Loan.LoanType.TERM_LOAN);
        request.setPrincipalAmount(new BigDecimal("5000000.00"));
        request.setInterestRate(new BigDecimal("0.0450"));
        request.setDisbursalDate(LocalDate.of(2026, 5, 10));
        request.setMaturityDate(LocalDate.of(2027, 5, 10));
        request.setPaymentFrequency(Loan.PaymentFrequency.MONTHLY);

        BusinessPartner partner = new BusinessPartner();
        partner.setId(100L);
        partner.setBusinessPartnerName("Loan Customer");
        Currency currency = new Currency();
        currency.setCurrencyCode("KRW");

        when(businessPartnerPersistencePort.findById(100L)).thenReturn(Optional.of(partner));
        when(currencyPersistencePort.findByCode("KRW")).thenReturn(Optional.of(currency));
        when(loanRepository.save(any(Loan.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Loan created = service.createLoan(request.toEntity());

        assertThat(created.getBusinessPartner()).isSameAs(partner);
        assertThat(created.getCurrency()).isSameAs(currency);
        assertThat(created.getInitialEIR()).isEqualByComparingTo("0.0450");
        assertThat(created.getCurrentEIR()).isEqualByComparingTo("0.0450");
        assertThat(created.getStatus()).isEqualTo(Loan.LoanStatus.ACTIVE);
    }

    @Test
    @DisplayName("대출 실행은 JournalUseCase로 균형 전표를 생성하고 실행 이력에 연결한다.")
    void disburseLoanCreatesJournalThroughUseCase() {
        Loan loan = new Loan();
        loan.setId(1L);
        loan.setLoanNumber("LN-2026-001");
        loan.setCurrency(currency("KRW"));

        AccountSubject cashAccount = account("101999");
        AccountSubject loanReceivableAccount = account("131999");
        JournalEntry savedJournal = new JournalEntry();
        savedJournal.setId(77L);
        savedJournal.setSlipNo("JE-LOAN-1");
        JournalEntry postedJournal = new JournalEntry();
        postedJournal.setId(77L);
        postedJournal.setSlipNo("JE-LOAN-1");
        postedJournal.setStatus(JournalEntryStatus.POSTED);

        when(loanRepository.findById(1L)).thenReturn(Optional.of(loan));
        when(accountSubjectPersistencePort.findByCode("101999")).thenReturn(Optional.of(cashAccount));
        when(accountSubjectPersistencePort.findByCode("131999")).thenReturn(Optional.of(loanReceivableAccount));
        when(journalUseCase.createJournalEntry(any(JournalEntry.class))).thenReturn(savedJournal);
        when(journalUseCase.getJournalEntryWithDetails(77L)).thenReturn(Optional.of(postedJournal));
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
        assertThat(journal.getCurrencyCode()).isEqualTo("KRW");
        assertThat(journal.getDetails()).hasSize(2);

        JournalDetail debit = journal.getDetails().get(0);
        JournalDetail credit = journal.getDetails().get(1);
        assertThat(debit.getSide()).isEqualTo(JournalSide.DEBIT);
        assertThat(debit.getAccountCode()).isEqualTo("131999");
        assertThat(debit.getAmount()).isEqualByComparingTo("1000000.00");
        assertThat(credit.getSide()).isEqualTo(JournalSide.CREDIT);
        assertThat(credit.getAccountCode()).isEqualTo("101999");
        assertThat(credit.getAmount()).isEqualByComparingTo("1000000.00");

        InOrder journalOrder = inOrder(journalUseCase);
        journalOrder.verify(journalUseCase).createJournalEntry(any(JournalEntry.class));
        journalOrder.verify(journalUseCase).approveJournalEntry(77L, "loan-user");
        journalOrder.verify(journalUseCase).postJournalEntry(77L, "loan-user");

        assertThat(disbursal.getJournalEntry()).isSameAs(postedJournal);
        assertThat(disbursal.getJournalEntry().getStatus()).isEqualTo(JournalEntryStatus.POSTED);
        assertThat(disbursal.getAuditUser()).isEqualTo("loan-user");
    }

    @Test
    @DisplayName("대출 자동전표 계정 설정이 없으면 숨은 기본값 없이 실패한다.")
    void disburseLoanRejectsMissingAccountingConfiguration() {
        accountingProperties.setCashAccountCode(null);
        Loan loan = new Loan();
        loan.setId(1L);
        loan.setLoanNumber("LN-2026-001");
        loan.setCurrency(currency("KRW"));

        when(loanRepository.findById(1L)).thenReturn(Optional.of(loan));

        assertThatThrownBy(() -> service.disburseLoan(
                1L,
                LocalDate.of(2026, 5, 10),
                new BigDecimal("1000000.00"),
                "loan-user"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("account.loan.accounting.cash-account-code");
        verify(journalUseCase, never()).createJournalEntry(any());
    }

    private AccountSubject account(String code) {
        AccountSubject account = new AccountSubject();
        account.setCode(code);
        account.setName("ACCOUNT-" + code);
        return account;
    }

    private Currency currency(String code) {
        Currency currency = new Currency();
        currency.setCurrencyCode(code);
        return currency;
    }
}
