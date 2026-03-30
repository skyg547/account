package com.ho.account.ledger;

import com.ho.account.AccountApplication;
import com.ho.account.basic.domain.AccountSubject;
import com.ho.account.basic.domain.BusinessPartner;
import com.ho.account.basic.domain.Currency;
import com.ho.account.basic.domain.Department;
import com.ho.account.basic.repository.AccountSubjectRepository;
import com.ho.account.basic.repository.BusinessPartnerRepository;
import com.ho.account.basic.repository.CurrencyRepository;
import com.ho.account.basic.repository.DepartmentRepository;
import com.ho.account.journal.domain.JournalDetail;
import com.ho.account.journal.domain.JournalEntry;
import com.ho.account.journal.domain.JournalEntryStatus;
import com.ho.account.journal.service.JournalService;
import com.ho.account.ledger.domain.GlBalance;
import com.ho.account.ledger.domain.SlBalance;
import com.ho.account.ledger.service.LedgerService;
import com.ho.account.ledger.web.DrilldownController;
import com.ho.account.ledger.web.LedgerController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = AccountApplication.class)
@AutoConfigureMockMvc
@Transactional
public class LedgerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JournalService journalService;

    @Autowired
    private LedgerService ledgerService;

    @Autowired
    private AccountSubjectRepository accountSubjectRepository;

    @Autowired
    private BusinessPartnerRepository businessPartnerRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private CurrencyRepository currencyRepository;

    @Autowired
    private LedgerController ledgerController; // Direct controller for testing

    @Autowired
    private DrilldownController drilldownController; // Direct controller for testing

    private AccountSubject cashAccount;
    private AccountSubject salesRevenueAccount;
    private BusinessPartner customerA;
    private Department salesDept;
    private Currency usdCurrency;

    @BeforeEach
    void setUp() {
        // Clear all balances before each test (for re-aggregation tests)
        ledgerService.reaggregateLedgerBalancesForPeriod(LocalDate.MIN, LocalDate.MAX);

        // Setup common test data
        usdCurrency = new Currency();
        usdCurrency.setCode("USD");
        usdCurrency.setName("US Dollar");
        currencyRepository.save(usdCurrency);

        cashAccount = new AccountSubject();
        cashAccount.setCode("10100");
        cashAccount.setName("현금");
        accountSubjectRepository.save(cashAccount);

        salesRevenueAccount = new AccountSubject();
        salesRevenueAccount.setCode("40100");
        salesRevenueAccount.setName("상품매출");
        accountSubjectRepository.save(salesRevenueAccount);

        customerA = new BusinessPartner();
        customerA.setBusinessPartnerCode("CUST001");
        customerA.setBusinessPartnerName("Customer A");
        customerA.setUseYn(true);
        businessPartnerRepository.save(customerA);

        salesDept = new Department();
        salesDept.setCode("SALES01");
        salesDept.setName("Sales Department");
        departmentRepository.save(salesDept);
    }

    private JournalEntry createAndPostJournalEntry(LocalDate accountingDate, BigDecimal amount, String description, String lineageSourceType, String lineageSourceId) {
        JournalEntry journalEntry = new JournalEntry();
        journalEntry.setSlipDate(accountingDate);
        journalEntry.setAccountingDate(accountingDate);
        journalEntry.setDescription(description);
        journalEntry.setCreatedBy("testuser");
        journalEntry.setAuditUser("testuser");
        journalEntry.setCurrency(usdCurrency);
        journalEntry.setLineageSourceType(lineageSourceType);
        journalEntry.setLineageSourceId(lineageSourceId);

        JournalDetail debitDetail = new JournalDetail();
        debitDetail.setDrcrType("DEBIT");
        debitDetail.setAccountSubject(cashAccount);
        debitDetail.setAmount(amount);
        debitDetail.setBaseAmount(amount);
        debitDetail.setBusinessPartner(customerA);
        debitDetail.setDepartment(salesDept);
        debitDetail.setDetailDescription("현금 증가");
        journalEntry.addDetail(debitDetail);

        JournalDetail creditDetail = new JournalDetail();
        creditDetail.setDrcrType("CREDIT");
        creditDetail.setAccountSubject(salesRevenueAccount);
        creditDetail.setAmount(amount);
        creditDetail.setBaseAmount(amount);
        creditDetail.setBusinessPartner(customerA);
        creditDetail.setDepartment(salesDept);
        creditDetail.setDetailDescription("매출 증가");
        journalEntry.addDetail(creditDetail);

        JournalEntry savedEntry = journalService.createJournalEntry(journalEntry);
        // Assuming the status flow goes DRAFT -> APPROVED -> POSTED
        savedEntry.setStatus(JournalEntryStatus.APPROVED);
        journalService.postJournalEntry(savedEntry.getId());
        return savedEntry;
    }

    @Test
    @DisplayName("전표 전기 후 GL/SL 잔액이 올바르게 업데이트되는지 확인")
    void testGlSlBalanceUpdateAfterPosting() throws Exception {
        // Given
        LocalDate today = LocalDate.now();
        BigDecimal amount = new BigDecimal("1000.00");
        createAndPostJournalEntry(today, amount, "Test Sale 1", "SALES", "INV001");

        // When & Then - Verify GL Balance
        mockMvc.perform(get("/api/ledger/gl-balances")
                        .param("startDate", today.toString())
                        .param("endDate", today.toString())
                        .param("accountCode", cashAccount.getCode())
                        .param("currencyCode", usdCurrency.getCode()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].accountSubject.code").value(cashAccount.getCode()))
                .andExpect(jsonPath("$[0].debitAmount").value(amount.doubleValue()))
                .andExpect(jsonPath("$[0].endingBalance").value(amount.doubleValue()));

        mockMvc.perform(get("/api/ledger/gl-balances")
                        .param("startDate", today.toString())
                        .param("endDate", today.toString())
                        .param("accountCode", salesRevenueAccount.getCode())
                        .param("currencyCode", usdCurrency.getCode()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].accountSubject.code").value(salesRevenueAccount.getCode()))
                .andExpect(jsonPath("$[0].creditAmount").value(amount.doubleValue()))
                .andExpect(jsonPath("$[0].endingBalance").value(amount.doubleValue()));

        // When & Then - Verify SL Balance
        mockMvc.perform(get("/api/ledger/sl-balances")
                        .param("startDate", today.toString())
                        .param("endDate", today.toString())
                        .param("accountCode", cashAccount.getCode())
                        .param("businessPartnerCode", customerA.getBusinessPartnerCode())
                        .param("deptCode", salesDept.getCode())
                        .param("currencyCode", usdCurrency.getCode()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].accountSubject.code").value(cashAccount.getCode()))
                .andExpect(jsonPath("$[0].businessPartner.businessPartnerCode").value(customerA.getBusinessPartnerCode()))
                .andExpect(jsonPath("$[0].department.code").value(salesDept.getCode()))
                .andExpect(jsonPath("$[0].debitAmount").value(amount.doubleValue()))
                .andExpect(jsonPath("$[0].endingBalance").value(amount.doubleValue()));
    }

    @Test
    @DisplayName("전표 전기 후 GL/SL 잔액이 여러 건에 대해 누적되는지 확인")
    void testGlSlBalanceAccumulation() throws Exception {
        // Given
        LocalDate today = LocalDate.now();
        BigDecimal amount1 = new BigDecimal("1000.00");
        BigDecimal amount2 = new BigDecimal("500.00");
        createAndPostJournalEntry(today, amount1, "Test Sale 1", "SALES", "INV001");
        createAndPostJournalEntry(today, amount2, "Test Sale 2", "INV", "INV002"); // Another entry

        // When & Then - Verify GL Balance (Cash account)
        mockMvc.perform(get("/api/ledger/gl-balances")
                        .param("startDate", today.toString())
                        .param("endDate", today.toString())
                        .param("accountCode", cashAccount.getCode())
                        .param("currencyCode", usdCurrency.getCode()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].accountSubject.code").value(cashAccount.getCode()))
                .andExpect(jsonPath("$[0].debitAmount").value(amount1.add(amount2).doubleValue()))
                .andExpect(jsonPath("$[0].endingBalance").value(amount1.add(amount2).doubleValue()));
    }

    @Test
    @DisplayName("드릴다운: 전표 ID로 전표 상세 정보 조회")
    void testDrilldownGetJournalEntryDetails() throws Exception {
        // Given
        LocalDate today = LocalDate.now();
        JournalEntry postedEntry = createAndPostJournalEntry(today, new BigDecimal("2000.00"), "Drilldown Test Sale", "DRILL", "DRL001");

        // When & Then
        mockMvc.perform(get("/api/drilldown/journal-entry/{journalEntryId}", postedEntry.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(postedEntry.getId()))
                .andExpect(jsonPath("$.description").value("Drilldown Test Sale"))
                .andExpect(jsonPath("$.details").isArray())
                .andExpect(jsonPath("$.details.length()").value(2)); // Debit and Credit detail
    }

    @Test
    @DisplayName("드릴다운: 전표 ID로 원천 문서 조회")
    void testDrilldownGetSourceDocument() throws Exception {
        // Given
        LocalDate today = LocalDate.now();
        String lineageSourceType = "SALES"; // Assuming SALES maps to SalesInvoice (which SourceDocumentService handles)
        String lineageSourceId = "TEST_SI_001"; // Needs to be a valid ID for a mock SalesInvoice if we fully integrate

        // To properly test this, we would need to mock/create a SalesInvoice and its repository
        // For now, let's create a dummy entry and expect the service to try and find it.
        JournalEntry postedEntry = createAndPostJournalEntry(today, new BigDecimal("1500.00"), "Source Document Test", lineageSourceType, lineageSourceId);

        // When & Then - For a real test, ensure SalesInvoice (or appropriate entity) exists for lineageSourceId
        // Currently, SourceDocumentService would return empty optional if actual SalesInvoice not found
        // We will assert that the call is successful, but for full verification, a mock SalesInvoice might be needed.
        mockMvc.perform(get("/api/drilldown/journal-entry/{journalEntryId}/source-document", postedEntry.getId()))
                .andExpect(status().isOk()) // Assuming success even if data is just type/empty for now
                .andExpect(jsonPath("$.type").value("SalesInvoice")) // As defined in SourceDocumentService for SALES
                .andExpect(jsonPath("$.data").doesNotExist()); // For this mock test, actual data might not exist

        // This requires actual SalesInvoice (or FixedAsset, LeaseContract etc.) to be present in DB
        // to return the full data.
        // For current mock setup, we assume SourceDocumentService is correctly called and identifies the type.
    }

    @Test
    @DisplayName("잔액 재집계 기능 확인 (마감 배치)")
    void testReaggregateBalancesForPeriod() throws Exception {
        // Given
        LocalDate date1 = LocalDate.now().minusDays(5);
        LocalDate date2 = LocalDate.now().minusDays(2);
        LocalDate date3 = LocalDate.now().minusDays(1);

        createAndPostJournalEntry(date1, new BigDecimal("100.00"), "Pre-reagg 1", "PREAGG", "PA001");
        createAndPostJournalEntry(date2, new BigDecimal("200.00"), "Pre-reagg 2", "PREAGG", "PA002");

        // Verify initial state
        mockMvc.perform(get("/api/ledger/gl-balances")
                        .param("startDate", date1.toString())
                        .param("endDate", date2.toString())
                        .param("accountCode", cashAccount.getCode())
                        .param("currencyCode", usdCurrency.getCode()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].debitAmount").value(300.00));

        // When - Reaggregate for a period that includes the entries
        mockMvc.perform(post("/api/ledger/reaggregate-balances")
                        .param("startDate", date1.toString())
                        .param("endDate", date2.toString()))
                .andExpect(status().isOk());

        // Then - Balances should still be correct (re-calculated from scratch)
        mockMvc.perform(get("/api/ledger/gl-balances")
                        .param("startDate", date1.toString())
                        .param("endDate", date2.toString())
                        .param("accountCode", cashAccount.getCode())
                        .param("currencyCode", usdCurrency.getCode()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].debitAmount").value(300.00));

        // Create a new entry after re-aggregation, ensure it's incrementally added
        createAndPostJournalEntry(date3, new BigDecimal("50.00"), "Post-reagg", "POSTAGG", "PAG001");

        mockMvc.perform(get("/api/ledger/gl-balances")
                        .param("startDate", date1.toString())
                        .param("endDate", date3.toString()) // Check cumulative balance
                        .param("accountCode", cashAccount.getCode())
                        .param("currencyCode", usdCurrency.getCode()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].debitAmount").value(350.00));
    }
}
