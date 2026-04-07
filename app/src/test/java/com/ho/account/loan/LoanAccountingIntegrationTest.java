package com.ho.account.loan;

import com.ho.account.basic.domain.AccountSubject;
import com.ho.account.basic.domain.BusinessPartner;
import com.ho.account.basic.domain.Currency;
import com.ho.account.basic.repository.AccountSubjectRepository;
import com.ho.account.basic.repository.BusinessPartnerRepository;
import com.ho.account.basic.repository.CurrencyRepository;
import com.ho.account.loan.domain.*;
import com.ho.account.loan.service.LoanService;
import com.ho.account.loan.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("dev")
@Transactional
public class LoanAccountingIntegrationTest {

    @Autowired
    private LoanService loanService;

    @Autowired
    private LoanRepository loanRepository;

    @Autowired
    private BusinessPartnerRepository businessPartnerRepository;

    @Autowired
    private CurrencyRepository currencyRepository;

    @Autowired
    private AccountSubjectRepository accountSubjectRepository;

    @Autowired
    private EIRAmortizationScheduleRepository eirAmortizationScheduleRepository;

    @Autowired
    private RecalculationRunRepository recalculationRunRepository;

    private Loan testLoan;

    @BeforeEach
    void setUp() {
        // 1. 기초 데이터 설정 (계정과목)
        setupAccountSubject("101000", "보통예금");
        setupAccountSubject("131000", "대출채권");
        setupAccountSubject("401000", "이자수익");
        setupAccountSubject("171000", "이연대출부대손익");

        // 2. 통화 설정
        Currency krw = new Currency();
        krw.setCurrencyCode("KRW");
        krw.setCurrencyName("Korean Won");
        currencyRepository.save(krw);

        // 3. 거래처 설정
        BusinessPartner bp = new BusinessPartner();
        bp.setBusinessPartnerCode("CUST001");
        bp.setBusinessPartnerName("테스트 차입자");
        businessPartnerRepository.save(bp);

        // 4. 대출 생성
        Loan loan = new Loan();
        loan.setLoanNumber("LN-2026-001");
        loan.setBusinessPartner(bp);
        loan.setCurrency(krw);
        loan.setLoanType(Loan.LoanType.TERM_LOAN);
        loan.setPrincipalAmount(BigDecimal.valueOf(1000000));
        loan.setInterestRate(BigDecimal.valueOf(0.05)); // 5% 명목이자율
        loan.setDisbursalDate(LocalDate.of(2026, 1, 1));
        loan.setMaturityDate(LocalDate.of(2027, 1, 1));
        loan.setPaymentFrequency(Loan.PaymentFrequency.MONTHLY);
        
        testLoan = loanService.createLoan(loan);
    }

    private void setupAccountSubject(String code, String name) {
        if (!accountSubjectRepository.existsById(code)) {
            AccountSubject account = new AccountSubject();
            account.setCode(code);
            account.setName(name);
            accountSubjectRepository.save(account);
        }
    }

    @Test
    void testLoanAccountingDoDScenario() {
        // DoD 시나리오: 실행 -> 이연 -> 3개월 상각 -> 중도상환 재계산
        
        // 1. 대출 실행
        LoanDisbursal disbursal = loanService.disburseLoan(testLoan.getId(), testLoan.getDisbursalDate(), testLoan.getPrincipalAmount(), "USER1");
        assertNotNull(disbursal.getJournalEntry());
        assertEquals(0, testLoan.getPrincipalAmount().compareTo(disbursal.getDisbursedAmount()));

        // 2. DoD 시나리오 실행 (이연 -> 상각 -> 중도상환 재계산)
        RecalculationRun run = loanService.reproduceDoDScenario(testLoan.getId(), "USER1");
        
        assertNotNull(run);
        assertEquals(RecalculationRun.RecalculationReason.EARLY_REPAYMENT, run.getReason());
        assertTrue(run.getNewEIR().compareTo(run.getOldEIR()) != 0, "EIR should be changed after early repayment");

        // 3. 상각 스케줄 확인
        List<EIRAmortizationSchedule> schedules = eirAmortizationScheduleRepository.findByLoan(testLoan);
        assertFalse(schedules.isEmpty());
        
        // 재계산 전/후 스케줄이 섞여 있을 것이므로 일자별로 확인 가능
        long activeSchedules = schedules.stream().filter(s -> !s.isRecalculated()).count();
        assertTrue(activeSchedules > 0);

        // 4. 분개 전표 생성 확인
        assertNotNull(run.getAdjustmentJournalEntry());
        assertEquals("EARLY_REPAYMENT", run.getAdjustmentJournalEntry().getLineageSourceType());
        
        System.out.println("DoD Scenario Success!");
        System.out.println("Initial EIR: " + run.getOldEIR());
        System.out.println("Recalculated EIR: " + run.getNewEIR());
    }
}
