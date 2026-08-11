package com.ho.account.expenditure.application.service;

import com.ho.account.contracts.closing.AccountingPeriodStatusPort;
import com.ho.account.contracts.journal.JournalEntryCommand;
import com.ho.account.contracts.journal.JournalLineCommand;
import com.ho.account.contracts.journal.JournalPostingPort;
import com.ho.account.contracts.masterdata.BusinessPartnerRef;
import com.ho.account.contracts.masterdata.MasterDataQueryPort;
import com.ho.account.expenditure.application.port.in.*;
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
 * 타 모듈(Master Data, Journal Ledger, Closing)과는 ID/Code/Port 기반으로 통신하여 결합도를 낮춥니다.
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
    private final AccountingPeriodStatusPort accountingPeriodStatusPort;

    public PaymentService(PaymentPersistencePort paymentPersistencePort,
                          PayablePersistencePort payablePersistencePort,
                          PaymentRunPersistencePort paymentRunPersistencePort,
                          AdvancePaymentPersistencePort advancePaymentPersistencePort,
                          MasterDataQueryPort masterDataQueryPort,
                          JournalPostingPort journalPostingPort,
                          PayableAccountMappingPort payableAccountMappingPort,
                          PaymentExecutionPort paymentExecutionPort,
                          AccountingPeriodStatusPort accountingPeriodStatusPort) {
        this.paymentPersistencePort = paymentPersistencePort;
        this.payablePersistencePort = payablePersistencePort;
        this.paymentRunPersistencePort = paymentRunPersistencePort;
        this.advancePaymentPersistencePort = advancePaymentPersistencePort;
        this.masterDataQueryPort = masterDataQueryPort;
        this.journalPostingPort = journalPostingPort;
        this.payableAccountMappingPort = payableAccountMappingPort;
        this.paymentExecutionPort = paymentExecutionPort;
        this.accountingPeriodStatusPort = accountingPeriodStatusPort;
    }

    @Override
    public PaymentRun initiatePaymentRun(PaymentRunCommand command) {
        LocalDate runDate = command.runDate();
        validateAccountingPeriodOpen(runDate);

        String description = command.description();
        String createdBy = command.createdBy();
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
    public Payment executePayment(ExecutePaymentCommand command) {
        Long paymentId = command.paymentId();
        String bankAccount = command.bankAccount();
        Payment payment = paymentPersistencePort.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found: " + paymentId));

        validateAccountingPeriodOpen(payment.getPaymentDate());

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
    public AdvancePayment recordAdvancePayment(AdvancePaymentCommand command) {
        validateAccountingPeriodOpen(command.paymentDate());

        AdvancePayment advancePayment = toAdvancePayment(command);
        String vendorCode = advancePayment.getVendorCode();
        BusinessPartnerRef vendor = validateVendor(vendorCode);

        AdvancePayment savedAdvancePayment = advancePaymentPersistencePort.save(advancePayment);

        postAdvanceJournal(savedAdvancePayment, vendor.name());

        return savedAdvancePayment;
    }

    @Override
    public Payable offsetPayableWithAdvancePayment(OffsetPayableCommand command) {
        validateAccountingPeriodOpen(LocalDate.now());

        Long payableId = command.payableId();
        Long advancePaymentId = command.advancePaymentId();
        BigDecimal offsetAmount = command.offsetAmount();
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

    private AdvancePayment toAdvancePayment(AdvancePaymentCommand command) {
        AdvancePayment advancePayment = new AdvancePayment();
        advancePayment.setVendorCode(command.vendorCode());
        advancePayment.setPaymentDate(command.paymentDate());
        advancePayment.setAmount(command.amount());
        advancePayment.setDescription(command.description());
        return advancePayment;
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

    /**
     * 회계기간 마감 여부를 사전에 검증합니다.
     *
     * 🎓 [금융 회계 내부 통제 및 마감 정합성 - Accounting Period Controls]
     * 회계 시스템에서 마감(CLOSED) 처리된 회계기간에 새로운 지급/선급금/상계 거래가 발생하거나 전표가 발행되는 것을
     * 사전에 차단하는 것은 재무제표의 신뢰성과 내부 통제(Internal Control)의 핵심 요구사항입니다.
     *
     * 1. 소급 마감 차단 (Anti-Backdating):
     *    마감된 과거 회계기간으로 지급/선급금 전표가 작성되면 이미 확정된 당기순이익, 현금/예금 잔액, 채무 잔액이
     *    변경되어 재무제표의 왜곡을 초래합니다.
     * 2. 회계 내부 통제 이점 (Internal Control Benefits):
     *    지급 및 전표 발행 전 회계기간 마감 여부를 사전 검증(Fail-Closed)함으로써 무단/부정 지급 및
     *    마감 후 전표 삽입을 원천 차단하고 감사 추적성(Audit Trail)을 보장합니다.
     *
     * @param date 검증할 지급일자 또는 거래일자
     * @throws IllegalStateException 해당 회계기간이 이미 마감(CLOSED)된 경우
     */
    private void validateAccountingPeriodOpen(LocalDate date) {
        if (accountingPeriodStatusPort.isClosed(date)) {
            throw new IllegalStateException("해당 회계 반영일(" + date + ")은 이미 마감된 기간입니다.");
        }
    }
}
