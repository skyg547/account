package com.ho.account.expenditure.application.service;

import com.ho.account.contracts.journal.JournalEntryCommand;
import com.ho.account.contracts.journal.JournalLineCommand;
import com.ho.account.contracts.journal.JournalPostingPort;
import com.ho.account.contracts.masterdata.MasterDataQueryPort;
import com.ho.account.expenditure.application.port.in.PaymentUseCase;
import com.ho.account.expenditure.application.port.out.*;
import com.ho.account.expenditure.domain.*;
import com.ho.account.masterdata.core.application.port.out.BusinessPartnerPersistencePort;
import com.ho.account.masterdata.core.domain.model.BusinessPartner;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * [헥사고날 아키텍처 - 애플리케이션 서비스 (Application Service)]
 * 지급 실행, 선급금 관리 및 상계 처리를 담당하는 핵심 서비스입니다.
 * 
 * 🐣 [초보자를 위한 설명]
 * 이 서비스는 '회사의 지갑'을 관리하는 관리자입니다. 
 * "오늘 갚아야 할 돈이 있는 거래처들 다 모아봐!" (Payment Run), 
 * "A 거래처에 실제로 돈 보내고 장부에서 빚 지워줘!" (Execute Payment), 
 * "미리 준 돈(선급금)이 있으니 나중에 줄 돈이랑 퉁치자!" (Offset) 같은 복잡한 일들을 순서대로 처리합니다.
 * 
 * 타 모듈(Master Data, Journal Ledger)과는 ID/Code 기반으로 통신하여 결합도를 낮춥니다.
 */
@Service
@Transactional
public class PaymentService implements PaymentUseCase {

    private final PaymentPersistencePort paymentPersistencePort;
    private final PayablePersistencePort payablePersistencePort;
    private final PaymentRunPersistencePort paymentRunPersistencePort;
    private final AdvancePaymentPersistencePort advancePaymentPersistencePort;
    private final BusinessPartnerPersistencePort businessPartnerPersistencePort;
    private final MasterDataQueryPort masterDataQueryPort;
    private final JournalPostingPort journalPostingPort;

    public PaymentService(PaymentPersistencePort paymentPersistencePort,
                          PayablePersistencePort payablePersistencePort,
                          PaymentRunPersistencePort paymentRunPersistencePort,
                          AdvancePaymentPersistencePort advancePaymentPersistencePort,
                          BusinessPartnerPersistencePort businessPartnerPersistencePort,
                          MasterDataQueryPort masterDataQueryPort,
                          JournalPostingPort journalPostingPort) {
        this.paymentPersistencePort = paymentPersistencePort;
        this.payablePersistencePort = payablePersistencePort;
        this.paymentRunPersistencePort = paymentRunPersistencePort;
        this.advancePaymentPersistencePort = advancePaymentPersistencePort;
        this.businessPartnerPersistencePort = businessPartnerPersistencePort;
        this.masterDataQueryPort = masterDataQueryPort;
        this.journalPostingPort = journalPostingPort;
    }

    @Override
    public PaymentRun initiatePaymentRun(LocalDate runDate, String description, String createdBy) {
        PaymentRun paymentRun = new PaymentRun();
        paymentRun.setRunDate(runDate);
        paymentRun.setDescription(description);
        paymentRun.setCreatedBy(createdBy);
        paymentRun.setStatus(PaymentRunStatus.INITIATED);
        PaymentRun savedPaymentRun = paymentRunPersistencePort.save(paymentRun);

        List<Payable> duePayables = payablePersistencePort.findByDueDateBeforeAndStatusNot(
                runDate.plusDays(1), PayableStatus.PAID);

        for (Payable payable : duePayables) {
            Payment payment = new Payment();
            payment.setPaymentDate(runDate);
            payment.setVendorCode(payable.getVendorCode()); // ID 기반 참조로 변경
            payment.setAmount(payable.getOutstandingAmount());
            payment.setStatus(PaymentStatus.INITIATED);
            payment.setPaymentRun(savedPaymentRun);
            paymentPersistencePort.save(payment);
        }

        savedPaymentRun.setStatus(PaymentRunStatus.PROCESSING);
        return paymentRunPersistencePort.save(savedPaymentRun);
    }

    @Override
    public Payment executePayment(Long paymentId, String bankAccount) {
        Payment payment = paymentPersistencePort.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found: " + paymentId));

        if (!payment.canExecute()) {
            throw new IllegalStateException("Payment cannot be executed in current status: " + payment.getStatus());
        }

        // 실제 지급 실행 (Mock 성공)
        payment.markAsCompleted(bankAccount);
        Payment completedPayment = paymentPersistencePort.save(payment);

        // 채무 잔액 차감 (ID 기반 조회 및 DDD 로직 호출)
        Payable payable = payablePersistencePort.findByVendorCodeAndOutstandingAmountGreaterThan(
                        payment.getVendorCode(), BigDecimal.ZERO)
                .stream()
                .filter(candidate -> candidate.getOutstandingAmount().compareTo(payment.getAmount()) >= 0)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Matching payable not found for vendor: " + payment.getVendorCode()));

        payable.applyPayment(payment.getAmount());
        payablePersistencePort.save(payable);

        postPaymentJournal(completedPayment);

        return completedPayment;
    }

    @Override
    public AdvancePayment recordAdvancePayment(AdvancePayment advancePayment) {
        String vendorCode = advancePayment.getVendorCode();
        validateVendor(vendorCode);
        
        BusinessPartner vendor = businessPartnerPersistencePort.findByBusinessPartnerCode(vendorCode)
                .orElseThrow(() -> new IllegalArgumentException("Vendor not found: " + vendorCode));

        AdvancePayment savedAdvancePayment = advancePaymentPersistencePort.save(advancePayment);

        postAdvanceJournal(savedAdvancePayment, vendor.getBusinessPartnerName());

        return savedAdvancePayment;
    }

    @Override
    public Payable offsetPayableWithAdvancePayment(Long payableId, Long advancePaymentId, BigDecimal offsetAmount) {
        Payable payable = payablePersistencePort.findById(payableId)
                .orElseThrow(() -> new IllegalArgumentException("Payable not found: " + payableId));
        AdvancePayment advancePayment = advancePaymentPersistencePort.findById(advancePaymentId)
                .orElseThrow(() -> new IllegalArgumentException("Advance payment not found: " + advancePaymentId));

        // DDD: 비즈니스 로직을 엔티티로 이관
        payable.applyOffset(offsetAmount);
        advancePayment.applyOffset(offsetAmount);

        payablePersistencePort.save(payable);
        advancePaymentPersistencePort.save(advancePayment);

        postOffsetJournal(payable, offsetAmount);

        return payable;
    }

    private void validateVendor(String vendorCode) {
        masterDataQueryPort.findBusinessPartner(vendorCode)
                .orElseThrow(() -> new IllegalArgumentException("Vendor info missing: " + vendorCode));
    }

    private void postPaymentJournal(Payment payment) {
        requireAccount("21100", "AP account missing");
        requireAccount("10100", "Cash account missing");

        String vendorName = businessPartnerPersistencePort.findByBusinessPartnerCode(payment.getVendorCode())
                .map(BusinessPartner::getBusinessPartnerName)
                .orElse(payment.getVendorCode());

        journalPostingPort.createDraftEntry(new JournalEntryCommand(
                payment.getPaymentDate(),
                payment.getPaymentDate(),
                "Payment: " + vendorName + " - " + payment.getAmount(),
                "PAYMENT_EXECUTION",
                null, null, "SYSTEM", "SYSTEM",
                "PAYMENT", payment.getId().toString(),
                List.of(
                        new JournalLineCommand("DEBIT", "21100", payment.getAmount(), null, null,
                                payment.getVendorCode(), "AP Decrease"),
                        new JournalLineCommand("CREDIT", "10100", payment.getAmount(), null, null,
                                payment.getVendorCode(), "Cash/Bank Decrease"))));
    }

    private void postAdvanceJournal(AdvancePayment advance, String vendorName) {
        requireAccount("13100", "Advance account missing");
        requireAccount("10100", "Cash account missing");

        journalPostingPort.createDraftEntry(new JournalEntryCommand(
                advance.getPaymentDate(),
                advance.getPaymentDate(),
                "Advance: " + vendorName + " - " + advance.getAmount(),
                "ADVANCE_PAYMENT",
                null, null, "SYSTEM", "SYSTEM",
                "ADVANCE_PAYMENT", advance.getId().toString(),
                List.of(
                        new JournalLineCommand("DEBIT", "13100", advance.getAmount(), null, null,
                                advance.getVendorCode(), "Advance recognized"),
                        new JournalLineCommand("CREDIT", "10100", advance.getAmount(), null, null,
                                advance.getVendorCode(), "Cash Decrease"))));
    }

    private void postOffsetJournal(Payable payable, BigDecimal amount) {
        requireAccount("21100", "AP account missing");
        requireAccount("13100", "Advance account missing");

        String vendorName = businessPartnerPersistencePort.findByBusinessPartnerCode(payable.getVendorCode())
                .map(BusinessPartner::getBusinessPartnerName)
                .orElse(payable.getVendorCode());

        journalPostingPort.createDraftEntry(new JournalEntryCommand(
                LocalDate.now(), LocalDate.now(),
                "Offset: " + vendorName + " - " + amount,
                "AP_ADVANCE_OFFSET",
                null, null, "SYSTEM", "SYSTEM",
                "PAYABLE_OFFSET", payable.getId().toString(),
                List.of(
                        new JournalLineCommand("DEBIT", "21100", amount, null, null,
                                payable.getVendorCode(), "AP Offset"),
                        new JournalLineCommand("CREDIT", "13100", amount, null, null,
                                payable.getVendorCode(), "Advance Offset"))));
    }

    private void requireAccount(String accountCode, String message) {
        masterDataQueryPort.findAccountSubject(accountCode)
                .orElseThrow(() -> new IllegalStateException(message));
    }
}
