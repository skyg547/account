package com.ho.account.expenditure.application.service;

import com.ho.account.contracts.journal.JournalEntryCommand;
import com.ho.account.contracts.journal.JournalLineCommand;
import com.ho.account.contracts.journal.JournalPostingPort;
import com.ho.account.contracts.masterdata.BusinessPartnerRef;
import com.ho.account.contracts.masterdata.MasterDataQueryPort;
import com.ho.account.expenditure.application.port.in.PaymentUseCase;
import com.ho.account.expenditure.application.port.out.*;
import com.ho.account.expenditure.domain.*;
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
    private final MasterDataQueryPort masterDataQueryPort;
    private final JournalPostingPort journalPostingPort;
    private final PayableAccountMappingPort payableAccountMappingPort;
    private final PaymentExecutionPort paymentExecutionPort;

    public PaymentService(PaymentPersistencePort paymentPersistencePort,
                          PayablePersistencePort payablePersistencePort,
                          PaymentRunPersistencePort paymentRunPersistencePort,
                          AdvancePaymentPersistencePort advancePaymentPersistencePort,
                          MasterDataQueryPort masterDataQueryPort,
                          JournalPostingPort journalPostingPort,
                          PayableAccountMappingPort payableAccountMappingPort,
                          PaymentExecutionPort paymentExecutionPort) {
        this.paymentPersistencePort = paymentPersistencePort;
        this.payablePersistencePort = payablePersistencePort;
        this.paymentRunPersistencePort = paymentRunPersistencePort;
        this.advancePaymentPersistencePort = advancePaymentPersistencePort;
        this.masterDataQueryPort = masterDataQueryPort;
        this.journalPostingPort = journalPostingPort;
        this.payableAccountMappingPort = payableAccountMappingPort;
        this.paymentExecutionPort = paymentExecutionPort;
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
            if (payable.getId() == null) {
                throw new IllegalStateException("Persisted payable must have an ID before payment run creation");
            }
            Payment payment = new Payment();
            payment.setPaymentDate(runDate);
            payment.setVendorCode(payable.getVendorCode()); // ID 기반 참조로 변경
            payment.setPayableId(payable.getId());
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

        // 완료된 지급을 다시 호출하면 외부 송금과 채무 차감을 반복하지 않고 기존 결과를 반환합니다.
        if (payment.getStatus() == PaymentStatus.COMPLETED) {
            return payment;
        }
        if (!payment.canExecute()) {
            throw new IllegalStateException("Payment cannot be executed in current status: " + payment.getStatus());
        }
        if (payment.getPayableId() == null) {
            throw new IllegalStateException("Payment is not linked to a payable: " + paymentId);
        }

        // 같은 Payment ID는 항상 같은 멱등 키를 사용하므로, 장애 후 재시도해도 중복 송금을 방지할 수 있습니다.
        payment.beginExecutionAttempt();
        PaymentExecutionPort.PaymentExecutionResult executionResult = paymentExecutionPort.execute(
                new PaymentExecutionPort.PaymentExecutionCommand(
                        "PAYMENT:" + payment.getId(),
                        payment.getId(),
                        payment.getPayableId(),
                        payment.getVendorCode(),
                        payment.getAmount(),
                        bankAccount));
        if (!executionResult.successful()) {
            payment.markAsFailed(executionResult.failureReason());
            return paymentPersistencePort.save(payment);
        }

        // 채무 잔액 차감 (ID 기반 조회 및 DDD 로직 호출)
        Payable payable = payablePersistencePort.findById(payment.getPayableId())
                .orElseThrow(() -> new IllegalStateException("Linked payable not found: " + payment.getPayableId()));

        payable.applyPayment(payment.getAmount());
        payablePersistencePort.save(payable);

        payment.markAsCompleted(bankAccount, executionResult.referenceNo());
        Payment completedPayment = paymentPersistencePort.save(payment);
        postPaymentJournal(completedPayment);

        return completedPayment;
    }

    @Override
    public AdvancePayment recordAdvancePayment(AdvancePayment advancePayment) {
        String vendorCode = advancePayment.getVendorCode();
        BusinessPartnerRef vendor = validateVendor(vendorCode);

        AdvancePayment savedAdvancePayment = advancePaymentPersistencePort.save(advancePayment);

        postAdvanceJournal(savedAdvancePayment, vendor.name());

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

    private BusinessPartnerRef validateVendor(String vendorCode) {
        return masterDataQueryPort.findBusinessPartner(vendorCode)
                .orElseThrow(() -> new IllegalArgumentException("Vendor info missing: " + vendorCode));
    }

    private void postPaymentJournal(Payment payment) {
        PayableAccountMappingPort.PaymentExecutionAccounts accounts =
                payableAccountMappingPort.resolvePaymentExecutionAccounts(payment);
        requireAccounts(accounts.requiredAccountCodes());

        String vendorName = masterDataQueryPort.findBusinessPartner(payment.getVendorCode())
                .map(BusinessPartnerRef::name)
                .orElse(payment.getVendorCode());
        String actor = resolveActor(payment);

        journalPostingPort.createDraftEntry(new JournalEntryCommand(
                payment.getPaymentDate(),
                payment.getPaymentDate(),
                "Payment: " + vendorName + " - " + payment.getAmount(),
                "PAYMENT_EXECUTION",
                null, null, actor, actor,
                "PAYMENT", payment.getId().toString(),
                List.of(
                        new JournalLineCommand("DEBIT", accounts.accountsPayableAccountCode(), payment.getAmount(), null, null,
                                payment.getVendorCode(), "AP Decrease"),
                        new JournalLineCommand("CREDIT", accounts.cashAccountCode(), payment.getAmount(), null, null,
                                payment.getVendorCode(), "Cash/Bank Decrease"))));
    }

    private void postAdvanceJournal(AdvancePayment advance, String vendorName) {
        PayableAccountMappingPort.AdvancePaymentAccounts accounts =
                payableAccountMappingPort.resolveAdvancePaymentAccounts(advance);
        requireAccounts(accounts.requiredAccountCodes());

        journalPostingPort.createDraftEntry(new JournalEntryCommand(
                advance.getPaymentDate(),
                advance.getPaymentDate(),
                "Advance: " + vendorName + " - " + advance.getAmount(),
                "ADVANCE_PAYMENT",
                null, null, "SYSTEM", "SYSTEM",
                "ADVANCE_PAYMENT", advance.getId().toString(),
                List.of(
                        new JournalLineCommand("DEBIT", accounts.advanceAccountCode(), advance.getAmount(), null, null,
                                advance.getVendorCode(), "Advance recognized"),
                        new JournalLineCommand("CREDIT", accounts.cashAccountCode(), advance.getAmount(), null, null,
                                advance.getVendorCode(), "Cash Decrease"))));
    }

    private void postOffsetJournal(Payable payable, BigDecimal amount) {
        PayableAccountMappingPort.AdvanceOffsetAccounts accounts =
                payableAccountMappingPort.resolveAdvanceOffsetAccounts(payable);
        requireAccounts(accounts.requiredAccountCodes());

        String vendorName = masterDataQueryPort.findBusinessPartner(payable.getVendorCode())
                .map(BusinessPartnerRef::name)
                .orElse(payable.getVendorCode());

        journalPostingPort.createDraftEntry(new JournalEntryCommand(
                LocalDate.now(), LocalDate.now(),
                "Offset: " + vendorName + " - " + amount,
                "AP_ADVANCE_OFFSET",
                null, null, "SYSTEM", "SYSTEM",
                "PAYABLE_OFFSET", payable.getId().toString(),
                List.of(
                        new JournalLineCommand("DEBIT", accounts.accountsPayableAccountCode(), amount, null, null,
                                payable.getVendorCode(), "AP Offset"),
                        new JournalLineCommand("CREDIT", accounts.advanceAccountCode(), amount, null, null,
                                payable.getVendorCode(), "Advance Offset"))));
    }

    private void requireAccounts(List<String> accountCodes) {
        for (String accountCode : accountCodes) {
            masterDataQueryPort.findAccountSubject(accountCode)
                    .orElseThrow(() -> new IllegalStateException("Account missing: " + accountCode));
        }
    }

    private String resolveActor(Payment payment) {
        if (payment.getPaymentRun() != null
                && payment.getPaymentRun().getCreatedBy() != null
                && !payment.getPaymentRun().getCreatedBy().isBlank()) {
            return payment.getPaymentRun().getCreatedBy().trim();
        }
        return "SYSTEM";
    }
}
