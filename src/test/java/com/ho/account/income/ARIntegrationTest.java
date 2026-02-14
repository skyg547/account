package com.ho.account.income;

import com.ho.account.AccountApplication;
import com.ho.account.basic.domain.AccountSubject;
import com.ho.account.basic.domain.BusinessPartner;
import com.ho.account.basic.repository.AccountSubjectRepository;
import com.ho.account.basic.repository.BusinessPartnerRepository;
import com.ho.account.income.domain.*;
import com.ho.account.income.repository.CollectionRepository;
import com.ho.account.income.repository.MatchingRuleRepository;
import com.ho.account.income.repository.ReceivableRepository;
import com.ho.account.income.repository.SalesInvoiceRepository;
import com.ho.account.income.repository.UnmatchedCollectionRepository;
import com.ho.account.income.service.CollectionService;
import com.ho.account.income.service.SalesService;
import com.ho.account.journal.domain.JournalEntry;
import com.ho.account.journal.domain.JournalDetail;
import com.ho.account.journal.repository.JournalEntryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(classes = AccountApplication.class)
@Transactional
public class ARIntegrationTest {

    @Autowired
    private SalesService salesService;
    @Autowired
    private CollectionService collectionService;
    @Autowired
    private SalesInvoiceRepository salesInvoiceRepository;
    @Autowired
    private ReceivableRepository receivableRepository;
    @Autowired
    private CollectionRepository collectionRepository;
    @Autowired
    private MatchingRuleRepository matchingRuleRepository;
    @Autowired
    private UnmatchedCollectionRepository unmatchedCollectionRepository;
    @Autowired
    private BusinessPartnerRepository businessPartnerRepository;
    @Autowired
    private AccountSubjectRepository accountSubjectRepository;
    @Autowired
    private JournalEntryRepository journalEntryRepository;

    private BusinessPartner testCustomer;
    private AccountSubject arAccount;
    private AccountSubject salesRevenueAccount;
    private AccountSubject vatPayableAccount;
    private AccountSubject cashAccount;
    private AccountSubject arClearingAccount;

    @BeforeEach
    void setUp() {
        // Clear all repositories for a clean test state
        journalEntryRepository.deleteAll();
        unmatchedCollectionRepository.deleteAll();
        matchingRuleRepository.deleteAll();
        collectionRepository.deleteAll();
        receivableRepository.deleteAll();
        salesInvoiceRepository.deleteAll();
        businessPartnerRepository.deleteAll(); // Be careful with deleting shared master data
        accountSubjectRepository.deleteAll(); // Be careful with deleting shared master data

        // Setup common test data
        testCustomer = new BusinessPartner();
        testCustomer.setBusinessPartnerCode("CUST001");
        testCustomer.setBusinessPartnerName("테스트 고객");
        testCustomer.setUseYn(true);
        businessPartnerRepository.save(testCustomer);

        arAccount = new AccountSubject();
        arAccount.setCode("11100"); // 매출채권
        arAccount.setName("매출채권");
        arAccount.setUseYn(true);
        arAccount.setUnsettled(true); // 미결제 계정
        accountSubjectRepository.save(arAccount);

        salesRevenueAccount = new AccountSubject();
        salesRevenueAccount.setCode("40100"); // 상품매출
        salesRevenueAccount.setName("상품매출");
        salesRevenueAccount.setUseYn(true);
        salesRevenueAccount.setUnsettled(false);
        accountSubjectRepository.save(salesRevenueAccount);

        vatPayableAccount = new AccountSubject();
        vatPayableAccount.setCode("22100"); // 부가세예수금
        vatPayableAccount.setName("부가세예수금");
        vatPayableAccount.setUseYn(true);
        vatPayableAccount.setUnsettled(false);
        accountSubjectRepository.save(vatPayableAccount);

        cashAccount = new AccountSubject();
        cashAccount.setCode("10100"); // 현금
        cashAccount.setName("현금및현금성자산");
        cashAccount.setUseYn(true);
        cashAccount.setUnsettled(false);
        accountSubjectRepository.save(cashAccount);

        arClearingAccount = new AccountSubject();
        arClearingAccount.setCode("21100"); // AR Clearing (임시)
        arClearingAccount.setName("매출채권정리계정");
        arClearingAccount.setUseYn(true);
        arClearingAccount.setUnsettled(true); // 미결제 계정
        accountSubjectRepository.save(arClearingAccount);
    }

    @Test
    @DisplayName("매출 인보이스 생성 및 매출채권 인식, 전표 검증")
    void testCreateSalesInvoiceAndReceivable() {
        // Given
        SalesInvoice newInvoice = createSalesInvoice(
                "INV001", testCustomer.getBusinessPartnerCode(),
                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 31),
                new BigDecimal("110000"), new BigDecimal("10000"), new BigDecimal("100000"),
                "상품 판매"
        );

        // When
        SalesInvoice createdInvoice = salesService.createSalesInvoice(newInvoice);

        // Then
        assertThat(createdInvoice).isNotNull();
        assertThat(createdInvoice.getId()).isNotNull();
        assertThat(createdInvoice.getStatus()).isEqualTo(SalesInvoiceStatus.ISSUED);

        Optional<Receivable> receivableOpt = receivableRepository.findBySalesInvoice(createdInvoice);
        assertThat(receivableOpt).isPresent();
        Receivable receivable = receivableOpt.get();
        assertThat(receivable.getOriginalAmount()).isEqualByComparingTo(createdInvoice.getTotalAmount());
        assertThat(receivable.getOutstandingAmount()).isEqualByComparingTo(createdInvoice.getTotalAmount());
        assertThat(receivable.getStatus()).isEqualTo(ReceivableStatus.OPEN);

        // Verify journal entry for sales recognition
        List<JournalEntry> entries = journalEntryRepository.findByLineageSourceTypeAndLineageSourceId(
                "SALES_INVOICE", createdInvoice.getId().toString());
        assertThat(entries).hasSize(1);
        JournalEntry salesEntry = entries.get(0);
        assertThat(salesEntry.getEntryType()).isEqualTo("SALES_RECOGNITION");
        assertThat(salesEntry.getDetails()).hasSize(3);

        // Debit: AR Account
        JournalDetail debitAr = salesEntry.getDetails().stream()
                .filter(d -> d.getDrcrType().equals("DEBIT") && d.getAccountSubject().getCode().equals(arAccount.getCode()))
                .findFirst().get();
        assertThat(debitAr.getAmount()).isEqualByComparingTo(createdInvoice.getTotalAmount());

        // Credit: Sales Revenue
        JournalDetail creditSales = salesEntry.getDetails().stream()
                .filter(d -> d.getDrcrType().equals("CREDIT") && d.getAccountSubject().getCode().equals(salesRevenueAccount.getCode()))
                .findFirst().get();
        assertThat(creditSales.getAmount()).isEqualByComparingTo(createdInvoice.getNetAmount());

        // Credit: VAT Payable
        JournalDetail creditVat = salesEntry.getDetails().stream()
                .filter(d -> d.getDrcrType().equals("CREDIT") && d.getAccountSubject().getCode().equals(vatPayableAccount.getCode()))
                .findFirst().get();
        assertThat(creditVat.getAmount()).isEqualByComparingTo(createdInvoice.getTaxAmount());
    }

    @Test
    @DisplayName("수금 수신 및 초기 전표 검증")
    void testReceiveCollection() {
        // Given
        Collection newCollection = createCollection(
                LocalDate.of(2024, 1, 15), testCustomer.getBusinessPartnerCode(),
                new BigDecimal("110000"), "은행A", "REF001"
        );

        // When
        Collection receivedCollection = collectionService.receivePayment(newCollection);

        // Then
        assertThat(receivedCollection).isNotNull();
        assertThat(receivedCollection.getId()).isNotNull();
        // Initial status will be RECEIVED, then attemptAutoMatching changes it to UNMATCHED since no rules/receivables yet
        assertThat(receivedCollection.getStatus()).isEqualTo(CollectionStatus.UNMATCHED);

        // Verify journal entry for collection recognition
        List<JournalEntry> entries = journalEntryRepository.findByLineageSourceTypeAndLineageSourceId(
                "COLLECTION", receivedCollection.getId().toString());
        assertThat(entries).hasSize(1);
        JournalEntry collectionEntry = entries.get(0);
        assertThat(collectionEntry.getEntryType()).isEqualTo("COLLECTION_RECOGNITION");
        assertThat(collectionEntry.getDetails()).hasSize(2);

        // Debit: CashAccount
        JournalDetail debitCash = collectionEntry.getDetails().stream()
                .filter(d -> d.getDrcrType().equals("DEBIT") && d.getAccountSubject().getCode().equals(cashAccount.getCode()))
                .findFirst().get();
        assertThat(debitCash.getAmount()).isEqualByComparingTo(receivedCollection.getAmount());

        // Credit: AR Clearing
        JournalDetail creditArClearing = collectionEntry.getDetails().stream()
                .filter(d -> d.getDrcrType().equals("CREDIT") && d.getAccountSubject().getCode().equals(arClearingAccount.getCode()))
                .findFirst().get();
        assertThat(creditArClearing.getAmount()).isEqualByComparingTo(receivedCollection.getAmount());

        // Verify UnmatchedCollection
        List<UnmatchedCollection> unmatched = unmatchedCollectionRepository.findByCollection(receivedCollection);
        assertThat(unmatched).hasSize(1);
        assertThat(unmatched.get(0).getStatus()).isEqualTo(UnmatchedCollectionStatus.PENDING);
    }

    @Test
    @DisplayName("자동 매칭 (참조 번호 일치) 및 전표 검증")
    void testAutoMatchingByReferenceNo() {
        // Given - 매출 인보이스 및 수금 (참조 번호 일치)
        SalesInvoice invoice = createSalesInvoice(
                "INV002", testCustomer.getBusinessPartnerCode(),
                LocalDate.of(2024, 2, 1), LocalDate.of(2024, 2, 28),
                new BigDecimal("220000"), new BigDecimal("20000"), new BigDecimal("200000"),
                "서비스 제공"
        );
        SalesInvoice createdInvoice = salesService.createSalesInvoice(invoice);
        Receivable receivable = receivableRepository.findBySalesInvoice(createdInvoice).orElseThrow();

        Collection collection = createCollection(
                LocalDate.of(2024, 2, 15), testCustomer.getBusinessPartnerCode(),
                new BigDecimal("220000"), "은행B", "INV002" // INV002로 참조 번호 일치
        );
        // Matching rule: Reference No Exact
        MatchingRule refNoRule = new MatchingRule();
        refNoRule.setRuleName("RefNoExact");
        refNoRule.setPriority(1);
        refNoRule.setMatchCriteria(MatchingCriteria.REFERENCE_NO_EXACT);
        refNoRule.setActive(true);
        matchingRuleRepository.save(refNoRule);

        // When
        Collection receivedCollection = collectionService.receivePayment(collection);

        // Then
        assertThat(receivedCollection.getStatus()).isEqualTo(CollectionStatus.MATCHED);
        Receivable updatedReceivable = receivableRepository.findById(receivable.getId()).orElseThrow();
        assertThat(updatedReceivable.getOutstandingAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(updatedReceivable.getStatus()).isEqualTo(ReceivableStatus.PAID);
        assertThat(createdInvoice.getStatus()).isEqualTo(SalesInvoiceStatus.PAID);

        // Verify UnmatchedCollection is resolved
        List<UnmatchedCollection> unmatched = unmatchedCollectionRepository.findByCollection(receivedCollection);
        assertThat(unmatched).isEmpty(); // Successfully matched, so no unmatched record

        // Verify matching journal entry
        List<JournalEntry> matchingEntries = journalEntryRepository.findAll().stream()
                .filter(e -> e.getEntryType().equals("COLLECTION_MATCHING") && e.getLineageSourceId().equals(receivedCollection.getId().toString()))
                .collect(Collectors.toList());
        assertThat(matchingEntries).hasSize(1);
        JournalEntry matchingEntry = matchingEntries.get(0);
        assertThat(matchingEntry.getDetails()).hasSize(2);

        // Debit: AR Clearing
        JournalDetail debitArClearing = matchingEntry.getDetails().stream()
                .filter(d -> d.getDrcrType().equals("DEBIT") && d.getAccountSubject().getCode().equals(arClearingAccount.getCode()))
                .findFirst().get();
        assertThat(debitArClearing.getAmount()).isEqualByComparingTo(receivedCollection.getAmount());

        // Credit: AR Account
        JournalDetail creditAr = matchingEntry.getDetails().stream()
                .filter(d -> d.getDrcrType().equals("CREDIT") && d.getAccountSubject().getCode().equals(arAccount.getCode()))
                .findFirst().get();
        assertThat(creditAr.getAmount()).isEqualByComparingTo(receivedCollection.getAmount());
    }

    @Test
    @DisplayName("자동 매칭 실패 시 미매칭 큐에 추가 및 수동 매칭")
    void testAutoMatchingFailureAndManualMatching() {
        // Given - 매출 인보이스 (INV003), 수금 (REF003 - 매칭 규칙 없음)
        SalesInvoice invoice = createSalesInvoice(
                "INV003", testCustomer.getBusinessPartnerCode(),
                LocalDate.of(2024, 3, 1), LocalDate.of(2024, 3, 31),
                new BigDecimal("330000"), new BigDecimal("30000"), new BigDecimal("300000"),
                "컨설팅"
        );
        SalesInvoice createdInvoice = salesService.createSalesInvoice(invoice);
        Receivable receivable = receivableRepository.findBySalesInvoice(createdInvoice).orElseThrow();

        Collection collection = createCollection(
                LocalDate.of(2024, 3, 15), testCustomer.getBusinessPartnerCode(),
                new BigDecimal("330000"), "은행C", "REF003" // INV003과 다른 참조 번호
        );

        // When - 수금 수신 (자동 매칭 실패 예상)
        Collection receivedCollection = collectionService.receivePayment(collection);

        // Then - UnmatchedCollection 확인
        assertThat(receivedCollection.getStatus()).isEqualTo(CollectionStatus.UNMATCHED);
        List<UnmatchedCollection> unmatchedCollectionsInQueue = collectionService.getUnmatchedCollections();
        assertThat(unmatchedCollectionsInQueue).hasSize(1);
        UnmatchedCollection unmatched = unmatchedCollectionsInQueue.get(0);
        assertThat(unmatched.getCollection().getId()).isEqualTo(receivedCollection.getId());
        assertThat(unmatched.getStatus()).isEqualTo(UnmatchedCollectionStatus.PENDING);

        // When - 수동 매칭 수행
        collectionService.manualMatchCollection(receivedCollection.getId(), receivable.getId(), new BigDecimal("330000"));

        // Then - 상태 및 전표 검증
        Collection manuallyMatchedCollection = collectionRepository.findById(receivedCollection.getId()).orElseThrow();
        assertThat(manuallyMatchedCollection.getStatus()).isEqualTo(CollectionStatus.MATCHED);
        Receivable updatedReceivable = receivableRepository.findById(receivable.getId()).orElseThrow();
        assertThat(updatedReceivable.getOutstandingAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(updatedReceivable.getStatus()).isEqualTo(ReceivableStatus.PAID);

        // UnmatchedCollection이 해결됨
        List<UnmatchedCollection> updatedUnmatched = unmatchedCollectionRepository.findByCollection(manuallyMatchedCollection);
        assertThat(updatedUnmatched).hasSize(1);
        assertThat(updatedUnmatched.get(0).getStatus()).isEqualTo(UnmatchedCollectionStatus.RESOLVED);

        // Verify matching journal entry from manual match
        List<JournalEntry> matchingEntries = journalEntryRepository.findAll().stream()
                .filter(e -> e.getEntryType().equals("COLLECTION_MATCHING") && e.getLineageSourceId().equals(manuallyMatchedCollection.getId().toString()))
                .collect(Collectors.toList());
        assertThat(matchingEntries).hasSize(1);
    }


    private SalesInvoice createSalesInvoice(String invoiceNo, String customerCode, LocalDate issueDate, LocalDate dueDate,
                                            BigDecimal totalAmount, BigDecimal taxAmount, BigDecimal netAmount, String description) {
        SalesInvoice invoice = new SalesInvoice();
        invoice.setInvoiceNo(invoiceNo);
        BusinessPartner customer = new BusinessPartner();
        customer.setBusinessPartnerCode(customerCode); // Only code needed for mapping
        invoice.setCustomer(customer);
        invoice.setIssueDate(issueDate);
        invoice.setDueDate(dueDate);
        invoice.setTotalAmount(totalAmount);
        invoice.setTaxAmount(taxAmount);
        invoice.setNetAmount(netAmount);
        invoice.setDescription(description);
        invoice.setCreatedBy("SYSTEM_TEST");
        return invoice;
    }

    private Collection createCollection(LocalDate collectionDate, String customerCode, BigDecimal amount, String bankAccount, String referenceNo) {
        Collection collection = new Collection();
        collection.setCollectionDate(collectionDate);
        BusinessPartner customer = new BusinessPartner();
        customer.setBusinessPartnerCode(customerCode); // Only code needed for mapping
        collection.setCustomer(customer);
        collection.setAmount(amount);
        collection.setBankAccount(bankAccount);
        collection.setReferenceNo(referenceNo);
        return collection;
    }
}
