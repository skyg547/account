package com.ho.account.journalledger;

import com.ho.account.expenditure.application.port.in.PaymentUseCase;
import com.ho.account.expenditure.application.port.in.PurchaseUseCase;
import com.ho.account.expenditure.application.port.out.PayablePersistencePort;
import com.ho.account.expenditure.domain.Payable;
import com.ho.account.expenditure.domain.PayableStatus;
import com.ho.account.expenditure.domain.PurchaseInvoice;
import com.ho.account.masterdata.core.application.port.out.BusinessPartnerPersistencePort;
import com.ho.account.masterdata.core.application.port.out.AccountSubjectPersistencePort;
import com.ho.account.masterdata.core.domain.model.AccountSubject;
import com.ho.account.masterdata.core.domain.model.BusinessPartner;
import com.ho.account.tax.application.port.in.TaxInvoiceUseCase;
import com.ho.account.tax.domain.TaxInvoice;
import com.ho.account.tax.application.port.in.TaxInvoiceCommand;
import com.ho.account.expenditure.application.port.in.PurchaseInvoiceCommand;
import com.ho.account.expenditure.application.port.in.PaymentRunCommand;
import com.ho.account.expenditure.application.port.in.ExecutePaymentCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * [IntegratedBusinessProcessTest]
 * 전사 통합 비즈니스 프로세스(세금계산서 -> 매입 -> 채무 -> 지급)를 검증합니다.
 * 리팩토링된 ID 기반 참조 체계에서도 데이터 정합성이 유지되는지 확인합니다.
 */
@SpringBootTest(properties = "spring.main.allow-bean-definition-overriding=true")
@Transactional
public class IntegratedBusinessProcessTest {

    @Autowired
    private TaxInvoiceUseCase taxInvoiceUseCase;

    @Autowired
    private PurchaseUseCase purchaseUseCase;

    @Autowired
    private PaymentUseCase paymentUseCase;

    @Autowired
    private BusinessPartnerPersistencePort businessPartnerPersistencePort;

    @Autowired
    private AccountSubjectPersistencePort accountSubjectPersistencePort;

    @Autowired
    private PayablePersistencePort payablePersistencePort;

    @Autowired
    private com.ho.account.expenditure.application.port.out.PaymentPersistencePort paymentPersistencePort;

    @Autowired
    private com.ho.account.masterdata.core.application.port.out.CurrencyPersistencePort currencyPersistencePort;

    @Autowired
    private com.ho.account.masterdata.core.application.port.out.FiscalPeriodPersistencePort fiscalPeriodPersistencePort;

    private BusinessPartner vendor;

    @BeforeEach
    void setUp() {
        // 1. 마스터 데이터 설정
        vendor = new BusinessPartner();
        vendor.setBusinessPartnerCode("VEND-001");
        vendor.setBusinessPartnerName("Test Vendor");
        vendor.setPartnerType(BusinessPartner.PartnerType.VENDOR);
        businessPartnerPersistencePort.save(vendor);

        createAccount("21100", "Accounts Payable", AccountSubject.AccountCategory.LIABILITIES, AccountSubject.BalanceType.CREDIT);
        createAccount("50100", "Purchase Expense", AccountSubject.AccountCategory.EXPENSES, AccountSubject.BalanceType.DEBIT);
        createAccount("13500", "Input VAT", AccountSubject.AccountCategory.ASSETS, AccountSubject.BalanceType.DEBIT);
        createAccount("10100", "Cash", AccountSubject.AccountCategory.ASSETS, AccountSubject.BalanceType.DEBIT);

        com.ho.account.masterdata.core.domain.model.Currency krw = new com.ho.account.masterdata.core.domain.model.Currency();
        krw.setCurrencyCode("KRW");
        krw.setCurrencyName("Korean Won");
        krw.setSymbol("₩");
        currencyPersistencePort.save(krw);

        LocalDate now = LocalDate.now();
        com.ho.account.masterdata.core.domain.model.FiscalPeriod period = new com.ho.account.masterdata.core.domain.model.FiscalPeriod();
        period.setFiscalYear(String.valueOf(now.getYear()));
        period.setFiscalPeriod(String.format("%02d", now.getMonthValue()));
        period.setStartDate(now.withDayOfMonth(1));
        period.setEndDate(now.withDayOfMonth(now.lengthOfMonth()));
        // Note: ClosingStatus is set to OPEN by default in @PrePersist
        fiscalPeriodPersistencePort.save(period);
    }

    private void createAccount(String code, String name, AccountSubject.AccountCategory category, AccountSubject.BalanceType balanceType) {
        AccountSubject account = new AccountSubject();
        account.setCode(code);
        account.setName(name);
        account.setCategory(category);
        account.setBalanceType(balanceType);
        account.setValidFrom(LocalDate.of(2020, 1, 1));
        account.setValidTo(LocalDate.of(9999, 12, 31));
        accountSubjectPersistencePort.save(account);
    }

    @Test
    void testFullBusinessProcess() {
        // Step 1: 세금계산서 수취 (AP Tax Invoice)
        TaxInvoiceCommand taxCommand = new TaxInvoiceCommand(
                "TAX-2026-001",
                "PURCHASE",
                LocalDate.now(),
                "VEND-001",
                new BigDecimal("1000000"),
                new BigDecimal("100000"),
                new BigDecimal("1100000")
        );

        TaxInvoice taxInvoice = taxInvoiceUseCase.createAPInvoice(taxCommand);
        assertThat(taxInvoice.getId()).isNotNull();

        // Step 2: 매입 인식 (Purchase Invoice & Payable 생성)
        PurchaseInvoiceCommand purchaseCommand = new PurchaseInvoiceCommand(
                taxInvoice.getIssueId(),
                "VEND-001",
                taxInvoice.getIssueDate(),
                taxInvoice.getIssueDate(),
                taxInvoice.getTotalAmount(),
                taxInvoice.getTaxAmount(),
                taxInvoice.getSupplyAmount(),
                "integration-user",
                "Integration Test Purchase"
        );
        
        PurchaseInvoice savedPurchase = purchaseUseCase.createPurchaseInvoice(purchaseCommand);
        assertThat(savedPurchase.getId()).isNotNull();

        // Step 3: Payable 검증
        List<Payable> payables = payablePersistencePort.findByDueDateBeforeAndStatusNot(
                LocalDate.now().plusDays(1), PayableStatus.PAID);
        assertThat(payables).isNotEmpty();
        
        Payable payable = payables.stream()
                .filter(p -> p.getPurchaseInvoiceNo().equals("TAX-2026-001")) // ID 기반 필드 사용
                .findFirst()
                .orElseThrow();
        assertThat(payable.getOutstandingAmount()).isEqualByComparingTo(new BigDecimal("1100000"));
        assertThat(payable.getVendorCode()).isEqualTo("VEND-001");

        // Step 4: 지급 실행 (Payment)
        PaymentRunCommand paymentRunCommand = new PaymentRunCommand(LocalDate.now(), "Monthly Payment", "ADMIN");
        var paymentRun = paymentUseCase.initiatePaymentRun(paymentRunCommand);
        assertThat(paymentRun.getStatus().name()).isEqualTo("PROCESSING");

        // 생성된 Payment 찾기
        var payments = paymentPersistencePort.findByPaymentRunId(paymentRun.getId());
        assertThat(payments).isNotEmpty();
        var payment = payments.get(0);
        
        ExecutePaymentCommand executeCommand = new ExecutePaymentCommand(payment.getId(), "KOOKMIN-123-456");
        paymentUseCase.executePayment(executeCommand);

        // Step 5: 최종 검증 (Payable 상태 및 잔액)
        Payable updatedPayable = payablePersistencePort.findById(payable.getId()).orElseThrow();
        assertThat(updatedPayable.getStatus()).isEqualTo(PayableStatus.PAID);
        assertThat(updatedPayable.getOutstandingAmount()).isEqualByComparingTo(BigDecimal.ZERO);
    }
}
