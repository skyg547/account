package com.ho.account.loan.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ho.account.basic.domain.BusinessPartner;
import com.ho.account.basic.domain.Currency;
import com.ho.account.loan.domain.DeferredItemType;
import com.ho.account.loan.domain.EIRAmortizationSchedule;
import com.ho.account.loan.domain.Loan;
import com.ho.account.loan.domain.LoanDisbursal;
import com.ho.account.loan.domain.RecalculationRun;
import com.ho.account.loan.dto.LoanRequestDto;
import com.ho.account.loan.service.LoanService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LoanController.class)
class LoanControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LoanService loanService;

    @Autowired
    private ObjectMapper objectMapper;

    private Loan testLoan;
    private LoanDisbursal testLoanDisbursal;
    private DeferredItemType testDeferredItemType;
    private EIRAmortizationSchedule testScheduleEntry;
    private RecalculationRun testRecalculationRun;

    @BeforeEach
    void setUp() {
        BusinessPartner testBp = new BusinessPartner();
        testBp.setId(1L);
        testBp.setBusinessPartnerName("Test Borrower");

        Currency testCurrency = new Currency();
        testCurrency.setCurrencyCode("KRW");

        testLoan = new Loan();
        testLoan.setId(1L);
        testLoan.setLoanNumber("LN001");
        testLoan.setBusinessPartner(testBp);
        testLoan.setCurrency(testCurrency);
        testLoan.setPrincipalAmount(BigDecimal.valueOf(1000000));
        testLoan.setInterestRate(BigDecimal.valueOf(0.05));
        testLoan.setDisbursalDate(LocalDate.of(2023, 1, 1));
        testLoan.setMaturityDate(LocalDate.of(2024, 1, 1));
        testLoan.setPaymentFrequency(Loan.PaymentFrequency.MONTHLY);
        testLoan.setInitialEIR(BigDecimal.valueOf(0.05));
        testLoan.setCurrentEIR(BigDecimal.valueOf(0.05));
        testLoan.setStatus(Loan.LoanStatus.ACTIVE);
        testLoan.setCreatedAt(LocalDateTime.now());
        testLoan.setUpdatedAt(LocalDateTime.now());
        testLoan.setAuditUser("SYSTEM");

        testLoanDisbursal = new LoanDisbursal();
        testLoanDisbursal.setId(1L);
        testLoanDisbursal.setLoan(testLoan);
        testLoanDisbursal.setDisbursalDate(LocalDate.of(2023, 1, 1));
        testLoanDisbursal.setDisbursedAmount(BigDecimal.valueOf(1000000));
        testLoanDisbursal.setCreatedAt(LocalDateTime.now());
        testLoanDisbursal.setUpdatedAt(LocalDateTime.now());
        testLoanDisbursal.setAuditUser("SYSTEM");

        testDeferredItemType = new DeferredItemType();
        testDeferredItemType.setId(100L);
        testDeferredItemType.setCode("LOAN_ORIGINATION_FEE");
        testDeferredItemType.setName("Loan Origination Fee");
        testDeferredItemType.setDeferralMethod(DeferredItemType.DeferralMethod.EIR_METHOD);
        testDeferredItemType.setActive(true);
        testDeferredItemType.setCreatedAt(LocalDateTime.now());
        testDeferredItemType.setUpdatedAt(LocalDateTime.now());
        testDeferredItemType.setAuditUser("SYSTEM");

        testScheduleEntry = new EIRAmortizationSchedule();
        testScheduleEntry.setId(200L);
        testScheduleEntry.setLoan(testLoan);
        testScheduleEntry.setScheduleDate(LocalDate.of(2023, 1, 31));
        testScheduleEntry.setBeginningBalance(BigDecimal.valueOf(1000000));
        testScheduleEntry.setInterestIncome(BigDecimal.valueOf(4166.67));
        testScheduleEntry.setPrincipalRepayment(BigDecimal.valueOf(80000));
        testScheduleEntry.setEndingBalance(BigDecimal.valueOf(915833.33));
        testScheduleEntry.setCreatedAt(LocalDateTime.now());
        testScheduleEntry.setUpdatedAt(LocalDateTime.now());
        testScheduleEntry.setAuditUser("SYSTEM");

        testRecalculationRun = new RecalculationRun();
        testRecalculationRun.setId(300L);
        testRecalculationRun.setLoan(testLoan);
        testRecalculationRun.setRecalculationDate(LocalDate.of(2023, 4, 1));
        testRecalculationRun.setReason(RecalculationRun.RecalculationReason.EARLY_REPAYMENT);
        testRecalculationRun.setOldEIR(BigDecimal.valueOf(0.05));
        testRecalculationRun.setNewEIR(BigDecimal.valueOf(0.051));
        testRecalculationRun.setCreatedAt(LocalDateTime.now());
        testRecalculationRun.setUpdatedAt(LocalDateTime.now());
        testRecalculationRun.setAuditUser("SYSTEM");
    }

    @Test
    void testCreateLoan() throws Exception {
        LoanRequestDto requestDto = new LoanRequestDto();
        requestDto.setLoanNumber("LN001");
        requestDto.setBusinessPartnerId(1L);
        requestDto.setCurrencyCode("KRW");
        requestDto.setLoanType(Loan.LoanType.TERM_LOAN);
        requestDto.setPrincipalAmount(BigDecimal.valueOf(1000000));
        requestDto.setInterestRate(BigDecimal.valueOf(0.05));
        requestDto.setDisbursalDate(LocalDate.of(2023, 1, 1));
        requestDto.setMaturityDate(LocalDate.of(2024, 1, 1));
        requestDto.setPaymentFrequency(Loan.PaymentFrequency.MONTHLY);

        when(loanService.createLoan(any(Loan.class))).thenReturn(testLoan);

        mockMvc.perform(post("/api/loan/loans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.loanNumber").value("LN001"));
    }

    @Test
    void testGetLoanById() throws Exception {
        when(loanService.findLoanById(anyLong())).thenReturn(testLoan);

        mockMvc.perform(get("/api/loan/loans/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.loanNumber").value("LN001"));
    }

    @Test
    void testDisburseLoan() throws Exception {
        when(loanService.disburseLoan(anyLong(), any(LocalDate.class), any(BigDecimal.class), anyString())).thenReturn(testLoanDisbursal);

        mockMvc.perform(post("/api/loan/disbursals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"loanId\": 1, \"disbursalDate\": \"2023-01-01\", \"disbursedAmount\": 1000000, \"user\": \"user1\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.loanNumber").value("LN001"));
    }

    @Test
    void testGenerateAmortizationSchedule() throws Exception {
        when(loanService.generateAmortizationSchedule(anyLong(), any(LocalDate.class), any(BigDecimal.class), anyString()))
                .thenReturn(Arrays.asList(testScheduleEntry));

        mockMvc.perform(post("/api/loan/amortization-schedules/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"loanId\": 1, \"recalculationDate\": \"2023-01-01\", \"newEIR\": 0.05, \"user\": \"user1\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$[0].loanNumber").value("LN001"))
                .andExpect(jsonPath("$[0].interestIncome").value(4166.67));
    }

    @Test
    void testProcessLoanEvent() throws Exception {
        when(loanService.recalculateLoan(anyLong(), any(LocalDate.class), any(RecalculationRun.RecalculationReason.class), anyString(), any(Optional.class), any(Optional.class)))
                .thenReturn(testRecalculationRun);

        mockMvc.perform(post("/api/loan/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"loanId\": 1, \"eventType\": \"EARLY_REPAYMENT\", \"eventDate\": \"2023-04-01\", \"user\": \"user1\", \"newPrincipal\": 950000}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.loanNumber").value("LN001"))
                .andExpect(jsonPath("$.reason").value("EARLY_REPAYMENT"));
    }

    @Test
    void testReproduceDoDScenario() throws Exception {
        when(loanService.reproduceDoDScenario(anyLong(), anyString())).thenReturn(testRecalculationRun);

        mockMvc.perform(post("/api/loan/dod-scenario")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"loanId\": 1, \"user\": \"dod_tester\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.loanNumber").value("LN001"))
                .andExpect(jsonPath("$.reason").value("EARLY_REPAYMENT"));
    }
}
