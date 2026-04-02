package com.ho.account.report;

import com.ho.account.basic.domain.AccountSubject;
import com.ho.account.basic.domain.AccountSubject.AccountCategory;
import com.ho.account.basic.repository.AccountSubjectRepository;
import com.ho.account.journal.domain.JournalDetail;
import com.ho.account.journal.domain.JournalEntry;
import com.ho.account.journal.domain.JournalEntryStatus;
import com.ho.account.journal.repository.JournalEntryRepository;
import com.ho.account.report.domain.ReportLineMapping;
import com.ho.account.report.dto.FinancialStatementDTO;
import com.ho.account.report.repository.ReportLineMappingRepository;
import com.ho.account.report.service.FinancialStatementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
public class FinancialReportingIntegrationTest {

    @Autowired
    private FinancialStatementService financialStatementService;

    @Autowired
    private ReportLineMappingRepository mappingRepository;

    @Autowired
    private AccountSubjectRepository accountSubjectRepository;

    @Autowired
    private JournalEntryRepository journalEntryRepository;

    private AccountSubject cashAccount;
    private AccountSubject interestIncomeAccount;

    @BeforeEach
    void setUp() {
        // 1. 계정과목 설정
        cashAccount = createAccount("101000", "보통예금", AccountCategory.ASSETS);
        interestIncomeAccount = createAccount("401000", "이자수익", AccountCategory.REVENUE);

        // 2. 보고 라인 매핑 설정
        createMapping("BS", "BS_CASH", "현금 및 현금성자산", "101000", 1);
        createMapping("IS", "IS_INT_INC", "이자수익", "401000", 1);
    }

    @Test
    @DisplayName("재무보고 생성 및 Drill-through 검증 (DoD)")
    void testFinancialReportingAndDrillThrough() {
        // 3. 전표 생성 및 전기 (이자 수익 1,000원 발생)
        JournalEntry entry = new JournalEntry();
        entry.setSlipNo("JE-2026-REPORT-01");
        entry.setAccountingDate(LocalDate.of(2026, 4, 1));
        entry.setSlipDate(LocalDate.of(2026, 4, 1));
        entry.setStatus(JournalEntryStatus.POSTED); // 전기 완료 상태
        entry.setCreatedBy("USER1");
        entry.setAuditUser("USER1");

        // 차변: 현금 1,000
        JournalDetail debit = new JournalDetail();
        debit.setAccountSubject(cashAccount);
        debit.setDrcrType("DEBIT");
        debit.setAmount(BigDecimal.valueOf(1000));
        debit.setBaseAmount(BigDecimal.valueOf(1000));
        entry.addDetail(debit);

        // 대변: 이자수익 1,000
        JournalDetail credit = new JournalDetail();
        credit.setAccountSubject(interestIncomeAccount);
        credit.setDrcrType("CREDIT");
        credit.setAmount(BigDecimal.valueOf(1000));
        credit.setBaseAmount(BigDecimal.valueOf(1000));
        entry.addDetail(credit);

        journalEntryRepository.save(entry);

        // 4. 재무상태표(BS) 조회
        List<FinancialStatementDTO> bs = financialStatementService.generateBalanceSheet(LocalDate.of(2026, 4, 30));
        
        FinancialStatementDTO cashLine = bs.stream()
                .filter(l -> l.getAccountCode().equals("BS_CASH"))
                .findFirst().orElseThrow();
        
        assertEquals(0, BigDecimal.valueOf(1000).compareTo(cashLine.getAmount()), "BS 현금 라인 금액이 1,000이어야 함");

        // 5. [Drill-through] 보고 라인에서 원천 전표 추적
        List<JournalDetail> sourceDetails = financialStatementService.getJournalDetailsByReportLine(
                "BS_CASH", 
                LocalDate.of(2026, 1, 1), 
                LocalDate.of(2026, 12, 31)
        );

        assertFalse(sourceDetails.isEmpty(), "원천 전표 내역이 조회되어야 함");
        assertEquals(1, sourceDetails.size());
        assertEquals("JE-2026-REPORT-01", sourceDetails.get(0).getJournalEntry().getSlipNo());
        assertEquals(0, BigDecimal.valueOf(1000).compareTo(sourceDetails.get(0).getAmount()));
        
        System.out.println("Drill-through Success: 보고 라인 'BS_CASH'에서 원천 전표 '" 
                + sourceDetails.get(0).getJournalEntry().getSlipNo() + "'를 즉시 추적 완료.");
    }

    private AccountSubject createAccount(String code, String name, AccountCategory category) {
        AccountSubject account = new AccountSubject();
        account.setCode(code);
        account.setName(name);
        account.setCategory(category);
        account.setActive(true);
        return accountSubjectRepository.save(account);
    }

    private void createMapping(String type, String lineCode, String lineName, String accountCode, int order) {
        ReportLineMapping mapping = new ReportLineMapping();
        mapping.setReportType(type);
        mapping.setLineCode(lineCode);
        mapping.setLineName(lineName);
        mapping.setAccountCode(accountCode);
        mapping.setAggregationType("SUM");
        mapping.setDisplayOrder(order);
        mapping.setValidFromDate(LocalDate.of(2026, 1, 1));
        mappingRepository.save(mapping);
    }
}
