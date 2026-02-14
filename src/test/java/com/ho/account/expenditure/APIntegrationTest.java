package com.ho.account.expenditure;

import com.ho.account.AccountApplication;
import com.ho.account.basic.domain.AccountSubject;
import com.ho.account.basic.domain.BusinessPartner;
import com.ho.account.basic.repository.AccountSubjectRepository;
import com.ho.account.basic.repository.BusinessPartnerRepository;
import com.ho.account.expenditure.domain.*;
import com.ho.account.expenditure.repository.AdvancePaymentRepository;
import com.ho.account.expenditure.repository.PayableRepository;
import com.ho.account.expenditure.repository.PaymentRepository;
import com.ho.account.expenditure.repository.PaymentRunRepository;
import com.ho.account.expenditure.repository.PurchaseInvoiceRepository;
import com.ho.account.expenditure.service.PaymentService;
import com.ho.account.expenditure.service.PurchaseService;
import com.ho.account.journal.domain.JournalDetail;
import com.ho.account.journal.domain.JournalEntry;
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
public class APIntegrationTest {

    @Autowired
    private PurchaseService purchaseService;
    @Autowired
    private PaymentService paymentService;
    @Autowired
    private PurchaseInvoiceRepository purchaseInvoiceRepository;
    @Autowired
    private PayableRepository payableRepository;
    @Autowired
    private PaymentRepository paymentRepository;
    @Autowired
    private PaymentRunRepository paymentRunRepository;
    @Autowired
    private AdvancePaymentRepository advancePaymentRepository;
    @Autowired
    private BusinessPartnerRepository businessPartnerRepository;
    @Autowired
    private AccountSubjectRepository accountSubjectRepository;
    @Autowired
    private JournalEntryRepository journalEntryRepository;

    private BusinessPartner testVendor;
    private AccountSubject apAccount; // 매입채무
    private AccountSubject expenseAccount; // 비용
    private AccountSubject vatReceivableAccount; // 부가세대급금
    private AccountSubject cashAccount; // 현금
    private AccountSubject advancePaymentAccount; // 선급금

    @BeforeEach
    void setUp() {
        // Clear all repositories for a clean test state
        journalEntryRepository.deleteAll();
        advancePaymentRepository.deleteAll();
        paymentRepository.deleteAll();
        paymentRunRepository.deleteAll();
        payableRepository.deleteAll();
        purchaseInvoiceRepository.deleteAll();
        businessPartnerRepository.deleteAll(); // Be careful with deleting shared master data
        accountSubjectRepository.deleteAll(); // Be careful with deleting shared master data

        // Setup common test data
        testVendor = new BusinessPartner();
        testVendor.setBusinessPartnerCode("VEND001");
        testVendor.setBusinessPartnerName("테스트 공급업체");
        testVendor.setUseYn(true);
        businessPartnerRepository.save(testVendor);

        apAccount = new AccountSubject();
        apAccount.setCode("21100"); // 매입채무
        apAccount.setName("매입채무");
        apAccount.setUseYn(true);
        apAccount.setUnsettled(true); // 미결제 계정
        accountSubjectRepository.save(apAccount);

        expenseAccount = new AccountSubject();
        expenseAccount.setCode("50100"); // 소모품비
        expenseAccount.setName("소모품비");
        expenseAccount.setUseYn(true);
        expenseAccount.setUnsettled(false);
        accountSubjectRepository.save(expenseAccount);

        vatReceivableAccount = new AccountSubject();
        vatReceivableAccount.setCode("13500"); // 부가세대급금
        vatReceivableAccount.setName("부가세대급금");
        vatReceivableAccount.setUseYn(true);
        vatReceivableAccount.setUnsettled(false);
        accountSubjectRepository.save(vatReceivableAccount);

        cashAccount = new AccountSubject();
        cashAccount.setCode("10100"); // 현금
        cashAccount.setName("현금및현금성자산");
        cashAccount.setUseYn(true);
        cashAccount.setUnsettled(false);
        accountSubjectRepository.save(cashAccount);

        advancePaymentAccount = new AccountSubject();
        advancePaymentAccount.setCode("13100"); // 선급금
        advancePaymentAccount.setName("선급금");
        advancePaymentAccount.setUseYn(true);
        advancePaymentAccount.setUnsettled(true); // 미결제 계정
        accountSubjectRepository.save(advancePaymentAccount);
    }

    @Test
    @DisplayName("매입 인보이스 생성 및 매입채무 인식, 전표 검증")
    void testCreatePurchaseInvoiceAndPayable() {
        // Given
        PurchaseInvoice newInvoice = createPurchaseInvoice(
                "P_INV001", testVendor.getBusinessPartnerCode(),
                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 31),
                new BigDecimal("110000"), new BigDecimal("10000"), new BigDecimal("100000"),
                "사무용품 구매"
        );

        // When
        PurchaseInvoice createdInvoice = purchaseService.createPurchaseInvoice(newInvoice);

        // Then
        assertThat(createdInvoice).isNotNull();
        assertThat(createdInvoice.getInvoiceNo()).isEqualTo("P_INV001");
        assertThat(createdInvoice.getStatus()).isEqualTo(PurchaseInvoiceStatus.RECEIVED);

        Optional<Payable> payableOpt = payableRepository.findByPurchaseInvoiceNoAndPurchaseInvoiceVendorCode(createdInvoice.getInvoiceNo(), createdInvoice.getVendor().getBusinessPartnerCode());
        assertThat(payableOpt).isPresent();
        Payable payable = payableOpt.get();
        assertThat(payable.getOriginalAmount()).isEqualByComparingTo(createdInvoice.getTotalAmount());
        assertThat(payable.getOutstandingAmount()).isEqualByComparingTo(createdInvoice.getTotalAmount());
        assertThat(payable.getStatus()).isEqualTo(PayableStatus.OPEN);

        // Verify journal entry for purchase recognition
        List<JournalEntry> entries = journalEntryRepository.findByLineageSourceTypeAndLineageSourceId(
                "PURCHASE_INVOICE", createdInvoice.getInvoiceNo() + "_" + createdInvoice.getVendor().getBusinessPartnerCode());
        assertThat(entries).hasSize(1);
        JournalEntry purchaseEntry = entries.get(0);
        assertThat(purchaseEntry.getEntryType()).isEqualTo("PURCHASE_RECOGNITION");
        assertThat(purchaseEntry.getDetails()).hasSize(3);

        // Debit: Expense
        JournalDetail debitExpense = purchaseEntry.getDetails().stream()
                .filter(d -> d.getDrcrType().equals("DEBIT") && d.getAccountSubject().getCode().equals(expenseAccount.getCode()))
                .findFirst().get();
        assertThat(debitExpense.getAmount()).isEqualByComparingTo(createdInvoice.getNetAmount());

        // Debit: VAT Receivable
        JournalDetail debitVat = purchaseEntry.getDetails().stream()
                .filter(d -> d.getDrcrType().equals("DEBIT") && d.getAccountSubject().getCode().equals(vatReceivableAccount.getCode()))
                .findFirst().get();
        assertThat(debitVat.getAmount()).isEqualByComparingTo(createdInvoice.getTaxAmount());

        // Credit: AP Account
        JournalDetail creditAp = purchaseEntry.getDetails().stream()
                .filter(d -> d.getDrcrType().equals("CREDIT") && d.getAccountSubject().getCode().equals(apAccount.getCode()))
                .findFirst().get();
        assertThat(creditAp.getAmount()).isEqualByComparingTo(createdInvoice.getTotalAmount());
    }

    @Test
    @DisplayName("지급 실행 및 전표 검증")
    void testExecutePaymentRunAndPayment() {
        // Given - 매입 인보이스 생성
        PurchaseInvoice invoice = createAndSavePurchaseInvoice(
                "P_INV002", testVendor.getBusinessPartnerCode(),
                LocalDate.of(2024, 2, 1), LocalDate.of(2024, 2, 15),
                new BigDecimal("220000"), new BigDecimal("20000"), new BigDecimal("200000"),
                "컨설팅 비용"
        );
        Payable payable = payableRepository.findByPurchaseInvoiceNoAndPurchaseInvoiceVendorCode(invoice.getInvoiceNo(), invoice.getVendor().getBusinessPartnerCode()).orElseThrow();

        // When - 지급 실행 시작
        PaymentRun paymentRun = paymentService.initiatePaymentRun(LocalDate.of(2024, 2, 15), "2월 지급 실행", "SYSTEM");
        List<Payment> paymentsInRun = paymentRepository.findByPaymentRunId(paymentRun.getId());
        assertThat(paymentsInRun).hasSize(1);
        Payment payment = paymentsInRun.get(0);

        // Then - 지급 실행 (executePayment)
        Payment completedPayment = paymentService.executePayment(payment.getId(), "우리은행 계좌");

        assertThat(completedPayment.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
        Payable updatedPayable = payableRepository.findById(payable.getId()).orElseThrow();
        assertThat(updatedPayable.getOutstandingAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(updatedPayable.getStatus()).isEqualTo(PayableStatus.PAID);

        // Verify payment journal entry
        List<JournalEntry> entries = journalEntryRepository.findByLineageSourceTypeAndLineageSourceId(
                "PAYMENT", completedPayment.getId().toString());
        assertThat(entries).hasSize(1);
        JournalEntry paymentEntry = entries.get(0);
        assertThat(paymentEntry.getEntryType()).isEqualTo("PAYMENT_EXECUTION");
        assertThat(paymentEntry.getDetails()).hasSize(2);

        // Debit: AP Account
        JournalDetail debitAp = paymentEntry.getDetails().stream()
                .filter(d -> d.getDrcrType().equals("DEBIT") && d.getAccountSubject().getCode().equals(apAccount.getCode()))
                .findFirst().get();
        assertThat(debitAp.getAmount()).isEqualByComparingTo(completedPayment.getAmount());

        // Credit: Cash Account
        JournalDetail creditCash = paymentEntry.getDetails().stream()
                .filter(d -> d.getDrcrType().equals("CREDIT") && d.getAccountSubject().getCode().equals(cashAccount.getCode()))
                .findFirst().get();
        assertThat(creditCash.getAmount()).isEqualByComparingTo(completedPayment.getAmount());
    }

    @Test
    @DisplayName("선급금 기록 및 상계 처리 전표 검증")
    void testAdvancePaymentAndOffset() {
        // Given - 선급금 기록
        AdvancePayment newAdvancePayment = createAdvancePayment(
                testVendor.getBusinessPartnerCode(), LocalDate.of(2024, 3, 1),
                new BigDecimal("50000"), "사전 구매 계약금"
        );
        AdvancePayment recordedAdvancePayment = paymentService.recordAdvancePayment(newAdvancePayment);
        
        // Given - 매입 인보이스 생성 (선급금 상계 대상)
        PurchaseInvoice invoice = createAndSavePurchaseInvoice(
                "P_INV003", testVendor.getBusinessPartnerCode(),
                LocalDate.of(2024, 3, 10), LocalDate.of(2024, 3, 31),
                new BigDecimal("110000"), new BigDecimal("10000"), new BigDecimal("100000"),
                "물품 구매"
        );
        Payable payable = payableRepository.findByPurchaseInvoiceNoAndPurchaseInvoiceVendorCode(invoice.getInvoiceNo(), invoice.getVendor().getBusinessPartnerCode()).orElseThrow();

        // When - 선급금으로 매입채무 상계
        BigDecimal offsetAmount = new BigDecimal("50000");
        Payable updatedPayable = paymentService.offsetPayableWithAdvancePayment(payable.getId(), recordedAdvancePayment.getId(), offsetAmount);

        // Then
        assertThat(updatedPayable.getOutstandingAmount()).isEqualByComparingTo(new BigDecimal("60000")); // 110,000 - 50,000
        assertThat(updatedPayable.getStatus()).isEqualTo(PayableStatus.PARTIAL_PAID);

        AdvancePayment updatedAdvancePayment = advancePaymentRepository.findById(recordedAdvancePayment.getId()).orElseThrow();
        assertThat(updatedAdvancePayment.getOutstandingAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(updatedAdvancePayment.getStatus()).isEqualTo(AdvancePaymentStatus.OFFSET);

        // Verify offset journal entry
        List<JournalEntry> entries = journalEntryRepository.findByLineageSourceTypeAndLineageSourceId(
                "PAYABLE_OFFSET", updatedPayable.getId().toString());
        assertThat(entries).hasSize(1);
        JournalEntry offsetEntry = entries.get(0);
        assertThat(offsetEntry.getEntryType()).isEqualTo("AP_ADVANCE_OFFSET");
        assertThat(offsetEntry.getDetails()).hasSize(2);

        // Debit: AP Account
        JournalDetail debitAp = offsetEntry.getDetails().stream()
                .filter(d -> d.getDrcrType().equals("DEBIT") && d.getAccountSubject().getCode().equals(apAccount.getCode()))
                .findFirst().get();
        assertThat(debitAp.getAmount()).isEqualByComparingTo(offsetAmount);

        // Credit: Advance Payment Account
        JournalDetail creditAdvancePayment = offsetEntry.getDetails().stream()
                .filter(d -> d.getDrcrType().equals("CREDIT") && d.getAccountSubject().getCode().equals(advancePaymentAccount.getCode()))
                .findFirst().get();
        assertThat(creditAdvancePayment.getAmount()).isEqualByComparingTo(offsetAmount);
    }

    @Test
    @DisplayName("부분 지급 처리 전표 검증")
    void testPartialPayment() {
        // Given - 매입 인보이스 생성
        PurchaseInvoice invoice = createAndSavePurchaseInvoice(
                "P_INV004", testVendor.getBusinessPartnerCode(),
                LocalDate.of(2024, 4, 1), LocalDate.of(2024, 4, 30),
                new BigDecimal("100000"), new BigDecimal("10000"), new BigDecimal("90000"),
                "서비스 이용료"
        );
        Payable payable = payableRepository.findByPurchaseInvoiceNoAndPurchaseInvoiceVendorCode(invoice.getInvoiceNo(), invoice.getVendor().getBusinessPartnerCode()).orElseThrow();

        // When - 부분 지급 실행
        PaymentRun paymentRun = paymentService.initiatePaymentRun(LocalDate.of(2024, 4, 15), "4월 부분 지급 실행", "SYSTEM");
        // Payable의 금액을 부분적으로 지급하도록 Payment 객체를 수정하거나 생성해야 합니다.
        // 현재 initiatePaymentRun은 전액 지급을 가정하므로, 여기서는 수동으로 payment를 생성
        Payment partialPayment = new Payment();
        partialPayment.setPaymentDate(LocalDate.of(2024, 4, 15));
        partialPayment.setVendor(testVendor);
        BigDecimal partialAmount = new BigDecimal("50000");
        partialPayment.setAmount(partialAmount);
        partialPayment.setStatus(PaymentStatus.INITIATED);
        partialPayment.setPaymentRun(paymentRun);
        paymentRepository.save(partialPayment);


        Payment completedPayment = paymentService.executePayment(partialPayment.getId(), "국민은행 계좌");

        // Then
        assertThat(completedPayment.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
        Payable updatedPayable = payableRepository.findById(payable.getId()).orElseThrow();
        assertThat(updatedPayable.getOutstandingAmount()).isEqualByComparingTo(new BigDecimal("50000")); // 100,000 - 50,000
        assertThat(updatedPayable.getStatus()).isEqualTo(PayableStatus.PARTIAL_PAID);

        // Verify payment journal entry
        List<JournalEntry> entries = journalEntryRepository.findByLineageSourceTypeAndLineageSourceId(
                "PAYMENT", completedPayment.getId().toString());
        assertThat(entries).hasSize(1);
        JournalEntry paymentEntry = entries.get(0);
        assertThat(paymentEntry.getEntryType()).isEqualTo("PAYMENT_EXECUTION");
        assertThat(paymentEntry.getDetails()).hasSize(2);

        // Debit: AP Account
        JournalDetail debitAp = paymentEntry.getDetails().stream()
                .filter(d -> d.getDrcrType().equals("DEBIT") && d.getAccountSubject().getCode().equals(apAccount.getCode()))
                .findFirst().get();
        assertThat(debitAp.getAmount()).isEqualByComparingTo(partialAmount);

        // Credit: Cash Account
        JournalDetail creditCash = paymentEntry.getDetails().stream()
                .filter(d -> d.getDrcrType().equals("CREDIT") && d.getAccountSubject().getCode().equals(cashAccount.getCode()))
                .findFirst().get();
        assertThat(creditCash.getAmount()).isEqualByComparingTo(partialAmount);
    }


    private PurchaseInvoice createPurchaseInvoice(String invoiceNo, String vendorCode, LocalDate issueDate, LocalDate dueDate,
                                                  BigDecimal totalAmount, BigDecimal taxAmount, BigDecimal netAmount, String description) {
        PurchaseInvoice invoice = new PurchaseInvoice();
        invoice.setInvoiceNo(invoiceNo);
        BusinessPartner vendor = new BusinessPartner();
        vendor.setBusinessPartnerCode(vendorCode); // Only code needed for mapping
        invoice.setVendor(vendor);
        invoice.setIssueDate(issueDate);
        invoice.setDueDate(dueDate);
        invoice.setTotalAmount(totalAmount);
        invoice.setTaxAmount(taxAmount);
        invoice.setNetAmount(netAmount);
        invoice.setDescription(description);
        invoice.setCreatedBy("SYSTEM_TEST");
        return invoice;
    }

    private PurchaseInvoice createAndSavePurchaseInvoice(String invoiceNo, String vendorCode, LocalDate issueDate, LocalDate dueDate,
                                                         BigDecimal totalAmount, BigDecimal taxAmount, BigDecimal netAmount, String description) {
        PurchaseInvoice invoice = createPurchaseInvoice(invoiceNo, vendorCode, issueDate, dueDate, totalAmount, taxAmount, netAmount, description);
        return purchaseService.createPurchaseInvoice(invoice);
    }

    private AdvancePayment createAdvancePayment(String vendorCode, LocalDate paymentDate, BigDecimal amount, String description) {
        AdvancePayment advancePayment = new AdvancePayment();
        BusinessPartner vendor = new BusinessPartner();
        vendor.setBusinessPartnerCode(vendorCode);
        advancePayment.setVendor(vendor);
        advancePayment.setPaymentDate(paymentDate);
        advancePayment.setAmount(amount);
        advancePayment.setOutstandingAmount(amount);
        advancePayment.setDescription(description);
        return advancePayment;
    }
}
