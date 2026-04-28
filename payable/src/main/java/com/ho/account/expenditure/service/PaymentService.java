package com.ho.account.expenditure.service;

import com.ho.account.masterdata.core.domain.model.BusinessPartner;
import com.ho.account.masterdata.core.application.port.out.BusinessPartnerPersistencePort;
import com.ho.account.contracts.journal.JournalEntryCommand;
import com.ho.account.contracts.journal.JournalLineCommand;
import com.ho.account.contracts.journal.JournalPostingPort;
import com.ho.account.contracts.masterdata.MasterDataQueryPort;
import com.ho.account.expenditure.domain.AdvancePayment;
import com.ho.account.expenditure.domain.AdvancePaymentStatus;
import com.ho.account.expenditure.domain.Payable;
import com.ho.account.expenditure.domain.PayableStatus;
import com.ho.account.expenditure.domain.Payment;
import com.ho.account.expenditure.domain.PaymentRun;
import com.ho.account.expenditure.domain.PaymentRunStatus;
import com.ho.account.expenditure.domain.PaymentStatus;
import com.ho.account.expenditure.repository.AdvancePaymentRepository;
import com.ho.account.expenditure.repository.PayableRepository;
import com.ho.account.expenditure.repository.PaymentRepository;
import com.ho.account.expenditure.repository.PaymentRunRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PayableRepository payableRepository;
    private final PaymentRunRepository paymentRunRepository;
    private final AdvancePaymentRepository advancePaymentRepository;
    private final BusinessPartnerPersistencePort businessPartnerPersistencePort;
    private final MasterDataQueryPort masterDataQueryPort;
    private final JournalPostingPort journalPostingPort;

    public PaymentService(PaymentRepository paymentRepository,
                          PayableRepository payableRepository,
                          PaymentRunRepository paymentRunRepository,
                          AdvancePaymentRepository advancePaymentRepository,
                          BusinessPartnerPersistencePort businessPartnerPersistencePort,
                          MasterDataQueryPort masterDataQueryPort,
                          JournalPostingPort journalPostingPort) {
        this.paymentRepository = paymentRepository;
        this.payableRepository = payableRepository;
        this.paymentRunRepository = paymentRunRepository;
        this.advancePaymentRepository = advancePaymentRepository;
        this.businessPartnerPersistencePort = businessPartnerPersistencePort;
        this.masterDataQueryPort = masterDataQueryPort;
        this.journalPostingPort = journalPostingPort;
    }

    public PaymentRun initiatePaymentRun(LocalDate runDate, String description, String createdBy) {
        PaymentRun paymentRun = new PaymentRun();
        paymentRun.setRunDate(runDate);
        paymentRun.setDescription(description);
        paymentRun.setCreatedBy(createdBy);
        paymentRun.setStatus(PaymentRunStatus.INITIATED);
        PaymentRun savedPaymentRun = paymentRunRepository.save(paymentRun);

        List<Payable> duePayables = payableRepository.findByDueDateBeforeAndStatusNot(
                runDate.plusDays(1), PayableStatus.PAID);

        for (Payable payable : duePayables) {
            Payment payment = new Payment();
            payment.setPaymentDate(runDate);
            payment.setVendor(payable.getVendor());
            payment.setAmount(payable.getOutstandingAmount());
            payment.setStatus(PaymentStatus.INITIATED);
            payment.setPaymentRun(savedPaymentRun);
            paymentRepository.save(payment);
        }

        savedPaymentRun.setStatus(PaymentRunStatus.PROCESSING);
        return paymentRunRepository.save(savedPaymentRun);
    }

    public Payment executePayment(Long paymentId, String bankAccount) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found: " + paymentId));

        if (payment.getStatus() != PaymentStatus.INITIATED && payment.getStatus() != PaymentStatus.APPROVED) {
            throw new IllegalStateException("Payment cannot be executed in status: " + payment.getStatus());
        }

        boolean paymentSuccess = true;
        if (!paymentSuccess) {
            payment.setStatus(PaymentStatus.FAILED);
            paymentRepository.save(payment);
            throw new IllegalStateException("Payment execution failed.");
        }

        payment.setStatus(PaymentStatus.COMPLETED);
        payment.setBankAccount(bankAccount);
        Payment completedPayment = paymentRepository.save(payment);

        Payable payable = payableRepository.findByVendorBusinessPartnerCodeAndOutstandingAmountGreaterThan(
                        payment.getVendor().getBusinessPartnerCode(), BigDecimal.ZERO)
                .stream()
                .filter(candidate -> candidate.getOutstandingAmount().compareTo(payment.getAmount()) >= 0)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Payable to pay was not found."));

        payable.setOutstandingAmount(payable.getOutstandingAmount().subtract(payment.getAmount()));
        if (payable.getOutstandingAmount().compareTo(BigDecimal.ZERO) <= 0) {
            payable.setStatus(PayableStatus.PAID);
            payable.setOutstandingAmount(BigDecimal.ZERO);
        } else {
            payable.setStatus(PayableStatus.PARTIAL_PAID);
        }
        payableRepository.save(payable);

        requireAccount("21100", "AccountSubject for Accounts Payable not found");
        requireAccount("10100", "AccountSubject for Cash/Bank not found");

        journalPostingPort.createDraftEntry(new JournalEntryCommand(
                payment.getPaymentDate(),
                payment.getPaymentDate(),
                "Payment executed: " + payment.getVendor().getBusinessPartnerName() + " - " + payment.getAmount(),
                "PAYMENT_EXECUTION",
                null,
                null,
                "PAYMENT_MAKER",
                "PAYMENT_CHECKER",
                "PAYMENT",
                completedPayment.getId().toString(),
                List.of(
                        new JournalLineCommand("DEBIT", "21100", payment.getAmount(), null, null,
                                payment.getVendor().getBusinessPartnerCode(), "Accounts payable decrease"),
                        new JournalLineCommand("CREDIT", "10100", payment.getAmount(), null, null,
                                payment.getVendor().getBusinessPartnerCode(), "Cash/Bank decrease"))));

        return completedPayment;
    }

    public AdvancePayment recordAdvancePayment(AdvancePayment advancePayment) {
        String vendorCode = advancePayment.getVendor().getBusinessPartnerCode();
        masterDataQueryPort.findBusinessPartner(vendorCode)
                .orElseThrow(() -> new IllegalArgumentException("Vendor not found: " + vendorCode));
        BusinessPartner vendor = businessPartnerPersistencePort.findByBusinessPartnerCode(vendorCode)
                .orElseThrow(() -> new IllegalArgumentException("Vendor not found: " + vendorCode));
        advancePayment.setVendor(vendor);

        AdvancePayment savedAdvancePayment = advancePaymentRepository.save(advancePayment);

        requireAccount("13100", "AccountSubject for Advance Payment not found");
        requireAccount("10100", "AccountSubject for Cash/Bank not found");

        journalPostingPort.createDraftEntry(new JournalEntryCommand(
                advancePayment.getPaymentDate(),
                advancePayment.getPaymentDate(),
                "Advance payment: " + vendor.getBusinessPartnerName() + " - " + advancePayment.getAmount(),
                "ADVANCE_PAYMENT",
                null,
                null,
                "PAYMENT_MAKER",
                "PAYMENT_CHECKER",
                "ADVANCE_PAYMENT",
                savedAdvancePayment.getId().toString(),
                List.of(
                        new JournalLineCommand("DEBIT", "13100", advancePayment.getAmount(), null, null,
                                vendor.getBusinessPartnerCode(), "Advance payment recognized"),
                        new JournalLineCommand("CREDIT", "10100", advancePayment.getAmount(), null, null,
                                vendor.getBusinessPartnerCode(), "Cash/Bank decrease"))));

        return savedAdvancePayment;
    }

    public Payable offsetPayableWithAdvancePayment(Long payableId, Long advancePaymentId, BigDecimal offsetAmount) {
        Payable payable = payableRepository.findById(payableId)
                .orElseThrow(() -> new IllegalArgumentException("Payable not found: " + payableId));
        AdvancePayment advancePayment = advancePaymentRepository.findById(advancePaymentId)
                .orElseThrow(() -> new IllegalArgumentException("Advance payment not found: " + advancePaymentId));

        if (offsetAmount.compareTo(BigDecimal.ZERO) <= 0
                || offsetAmount.compareTo(payable.getOutstandingAmount()) > 0
                || offsetAmount.compareTo(advancePayment.getOutstandingAmount()) > 0) {
            throw new IllegalArgumentException("Invalid offset amount.");
        }

        payable.setOutstandingAmount(payable.getOutstandingAmount().subtract(offsetAmount));
        if (payable.getOutstandingAmount().compareTo(BigDecimal.ZERO) <= 0) {
            payable.setStatus(PayableStatus.PAID);
            payable.setOutstandingAmount(BigDecimal.ZERO);
        } else {
            payable.setStatus(PayableStatus.PARTIAL_PAID);
        }
        payableRepository.save(payable);

        advancePayment.setOutstandingAmount(advancePayment.getOutstandingAmount().subtract(offsetAmount));
        if (advancePayment.getOutstandingAmount().compareTo(BigDecimal.ZERO) <= 0) {
            advancePayment.setStatus(AdvancePaymentStatus.OFFSET);
            advancePayment.setOutstandingAmount(BigDecimal.ZERO);
        } else {
            advancePayment.setStatus(AdvancePaymentStatus.ACTIVE);
        }
        advancePaymentRepository.save(advancePayment);

        requireAccount("21100", "AccountSubject for Accounts Payable not found");
        requireAccount("13100", "AccountSubject for Advance Payment not found");

        journalPostingPort.createDraftEntry(new JournalEntryCommand(
                LocalDate.now(),
                LocalDate.now(),
                "Offset payable with advance payment: " + payable.getVendor().getBusinessPartnerName() + " - "
                        + offsetAmount,
                "AP_ADVANCE_OFFSET",
                null,
                null,
                "PAYMENT_MAKER",
                "PAYMENT_CHECKER",
                "PAYABLE_OFFSET",
                payable.getId().toString(),
                List.of(
                        new JournalLineCommand("DEBIT", "21100", offsetAmount, null, null,
                                payable.getVendor().getBusinessPartnerCode(), "Accounts payable offset"),
                        new JournalLineCommand("CREDIT", "13100", offsetAmount, null, null,
                                payable.getVendor().getBusinessPartnerCode(), "Advance payment offset"))));

        return payable;
    }

    private void requireAccount(String accountCode, String message) {
        masterDataQueryPort.findAccountSubject(accountCode)
                .orElseThrow(() -> new IllegalStateException(message));
    }
}
