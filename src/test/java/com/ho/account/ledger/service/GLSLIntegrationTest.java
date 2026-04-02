package com.ho.account.ledger.service;

import com.ho.account.basic.domain.AccountSubject;
import com.ho.account.basic.domain.BusinessPartner;
import com.ho.account.basic.domain.Currency;
import com.ho.account.basic.repository.AccountSubjectRepository;
import com.ho.account.basic.repository.BusinessPartnerRepository;
import com.ho.account.basic.repository.CurrencyRepository;
import com.ho.account.journal.domain.JournalDetail;
import com.ho.account.journal.domain.JournalEntry;
import com.ho.account.journal.domain.JournalEntryStatus;
import com.ho.account.journal.repository.JournalEntryRepository;
import com.ho.account.ledger.domain.GlBalance;
import com.ho.account.ledger.domain.SlBalance;
import com.ho.account.ledger.domain.GlEntry;
import com.ho.account.ledger.repository.GlBalanceRepository;
import com.ho.account.ledger.repository.SlBalanceRepository;
import com.ho.account.ledger.repository.GlEntryRepository;
import com.ho.account.ledger.repository.SlEntryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class GLSLIntegrationTest {

    @Autowired
    private PostingService postingService;

    @Autowired
    private LedgerService ledgerService;

    @Autowired
    private JournalEntryRepository journalEntryRepository;

    @Autowired
    private GlBalanceRepository glBalanceRepository;

    @Autowired
    private SlBalanceRepository slBalanceRepository;

    @Autowired
    private GlEntryRepository glEntryRepository;

    @Autowired
    private SlEntryRepository slEntryRepository;

    @Autowired
    private AccountSubjectRepository accountSubjectRepository;

    @Autowired
    private BusinessPartnerRepository businessPartnerRepository;

    @Autowired
    private CurrencyRepository currencyRepository;

    private AccountSubject reqAccount;
    private BusinessPartner bpData;
    private Currency currency;
    private JournalEntry testJournal;

    @BeforeEach
    void setUp() {
        reqAccount = new AccountSubject();
        reqAccount.setCode("111000");
        reqAccount.setName("Test Account");
        reqAccount.setAccountType("ASSET");
        accountSubjectRepository.save(reqAccount);

        bpData = new BusinessPartner();
        bpData.setBusinessPartnerCode("BP001");
        bpData.setBusinessPartnerName("Test BP");
        businessPartnerRepository.save(bpData);

        currency = new Currency();
        currency.setCode("KRW");
        currency.setName("South Korean Won");
        currencyRepository.save(currency);

        LocalDate today = LocalDate.now();

        testJournal = new JournalEntry();
        testJournal.setSlipNo(today.toString().replace("-", "") + "-0001");
        testJournal.setSlipDate(today);
        testJournal.setAccountingDate(today);
        testJournal.setStatus(JournalEntryStatus.APPROVED);
        testJournal.setCurrency(currency);
        testJournal.setExchangeRate(BigDecimal.ONE);
        testJournal.setLineageSourceType("ERP_AP");
        testJournal.setLineageSourceId("INV-2023-001");

        JournalDetail detail1 = new JournalDetail();
        detail1.setDrcrType("DEBIT");
        detail1.setAccountSubject(reqAccount);
        detail1.setAmount(BigDecimal.valueOf(1000));
        detail1.setBaseAmount(BigDecimal.valueOf(1000));
        detail1.setBusinessPartner(bpData);
        testJournal.addDetail(detail1);

        JournalDetail detail2 = new JournalDetail();
        detail2.setDrcrType("CREDIT");
        detail2.setAccountSubject(reqAccount);
        detail2.setAmount(BigDecimal.valueOf(1000));
        detail2.setBaseAmount(BigDecimal.valueOf(1000));
        detail2.setBusinessPartner(bpData);
        testJournal.addDetail(detail2);

        journalEntryRepository.save(testJournal);
    }

    @Test
    @DisplayName("GL/SL Posting Integration Test - Increment Balance & Lineage Drill-down")
    void testPostingAndDrillDown() {
        // 1. 전표 전기
        postingService.postJournalEntry(testJournal.getId());

        // 2. 전표 상태가 POSTED로 변경되었는지 검증
        JournalEntry postedJournal = journalEntryRepository.findById(testJournal.getId()).orElseThrow();
        assertThat(postedJournal.getStatus()).isEqualTo(JournalEntryStatus.POSTED);

        // 3. GL 잔액(계정 x 기간)이 증분 반영되었는지 검증
        LocalDate today = LocalDate.now();
        List<GlBalance> glBalances = ledgerService.getGlBalances(today, today, reqAccount, currency);
        assertThat(glBalances).isNotEmpty();
        GlBalance glb = glBalances.get(0);
        assertThat(glb.getDebitAmount()).isEqualByComparingTo("1000");
        assertThat(glb.getCreditAmount()).isEqualByComparingTo("1000");
        assertThat(glb.getEndingBalance()).isEqualByComparingTo("0");

        // 4. SL 잔액(거래처 x 기간)이 증분 반영되었는지 검증
        List<SlBalance> slBalances = ledgerService.getSlBalances(today, today, reqAccount, bpData, null, currency);
        assertThat(slBalances).isNotEmpty();
        SlBalance slb = slBalances.get(0);
        assertThat(slb.getDebitAmount()).isEqualByComparingTo("1000");
        assertThat(slb.getCreditAmount()).isEqualByComparingTo("1000");
        assertThat(slb.getEndingBalance()).isEqualByComparingTo("0");

        // 5. GL 엔트리에 계보 데이터가 존재하는지 검증(드릴다운 원천 -> 전표 -> 계정)
        List<GlEntry> lineageEntries = glEntryRepository.findByLineageSourceTypeAndLineageSourceId("ERP_AP", "INV-2023-001");
        assertThat(lineageEntries).hasSize(2);
        assertThat(lineageEntries.get(0).getPostingDate()).isEqualTo(today);
        assertThat(lineageEntries.get(0).getJournalDetail().getJournalEntry().getSlipNo()).isEqualTo(testJournal.getSlipNo());
    }
}
