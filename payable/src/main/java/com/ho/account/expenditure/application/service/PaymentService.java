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
            payment.setVendor(payable.getVendor());
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

        // 채무 잔액 차감 (DDD: Payable 내부 로직 호출)
        Payable payable = payablePersistencePort.findByVendorCodeAndOutstandingAmountGreaterThan(
                        payment.getVendor().getBusinessPartnerCode(), BigDecimal.ZERO)
                .stream()
                .filter(candidate -> candidate.getOutstandingAmount().compareTo(payment.getAmount()) >= 0)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Matching payable not found for amount: " + payment.getAmount()));

        payable.applyPayment(payment.getAmount());
        payablePersistencePort.save(payable);

        postPaymentJournal(completedPayment);

        return completedPayment;
    }

    @Override
    public AdvancePayment recordAdvancePayment(AdvancePayment advancePayment) {
        String vendorCode = advancePayment.getVendor().getBusinessPartnerCode();
        validateVendor(vendorCode);
        
        BusinessPartner vendor = businessPartnerPersistencePort.findByBusinessPartnerCode(vendorCode)
                .orElseThrow(() -> new IllegalArgumentException("Vendor not found: " + vendorCode));
        advancePayment.setVendor(vendor);

        AdvancePayment savedAdvancePayment = advancePaymentPersistencePort.save(advancePayment);

        postAdvanceJournal(savedAdvancePayment, vendor);

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

        journalPostingPort.createDraftEntry(new JournalEntryCommand(
                payment.getPaymentDate(),
                payment.getPaymentDate(),
                "Payment: " + payment.getVendor().getBusinessPartnerName() + " - " + payment.getAmount(),
                "PAYMENT_EXECUTION",
                null, null, "SYSTEM", "SYSTEM",
                "PAYMENT", payment.getId().toString(),
                List.of(
                        new JournalLineCommand("DEBIT", "21100", payment.getAmount(), null, null,
                                payment.getVendor().getBusinessPartnerCode(), "AP Decrease"),
                        new JournalLineCommand("CREDIT", "10100", payment.getAmount(), null, null,
                                payment.getVendor().getBusinessPartnerCode(), "Cash/Bank Decrease"))));
    }

    private void postAdvanceJournal(AdvancePayment advance, BusinessPartner vendor) {
        requireAccount("13100", "Advance account missing");
        requireAccount("10100", "Cash account missing");

        journalPostingPort.createDraftEntry(new JournalEntryCommand(
                advance.getPaymentDate(),
                advance.getPaymentDate(),
                "Advance: " + vendor.getBusinessPartnerName() + " - " + advance.getAmount(),
                "ADVANCE_PAYMENT",
                null, null, "SYSTEM", "SYSTEM",
                "ADVANCE_PAYMENT", advance.getId().toString(),
                List.of(
                        new JournalLineCommand("DEBIT", "13100", advance.getAmount(), null, null,
                                vendor.getBusinessPartnerCode(), "Advance recognized"),
                        new JournalLineCommand("CREDIT", "10100", advance.getAmount(), null, null,
                                vendor.getBusinessPartnerCode(), "Cash Decrease"))));
    }

    private void postOffsetJournal(Payable payable, BigDecimal amount) {
        requireAccount("21100", "AP account missing");
        requireAccount("13100", "Advance account missing");

        journalPostingPort.createDraftEntry(new JournalEntryCommand(
                LocalDate.now(), LocalDate.now(),
                "Offset: " + payable.getVendor().getBusinessPartnerName() + " - " + amount,
                "AP_ADVANCE_OFFSET",
                null, null, "SYSTEM", "SYSTEM",
                "PAYABLE_OFFSET", payable.getId().toString(),
                List.of(
                        new JournalLineCommand("DEBIT", "21100", amount, null, null,
                                payable.getVendor().getBusinessPartnerCode(), "AP Offset"),
                        new JournalLineCommand("CREDIT", "13100", amount, null, null,
                                payable.getVendor().getBusinessPartnerCode(), "Advance Offset"))));
    }

    private void requireAccount(String accountCode, String message) {
        masterDataQueryPort.findAccountSubject(accountCode)
                .orElseThrow(() -> new IllegalStateException(message));
    }
}
