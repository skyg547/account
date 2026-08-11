package com.ho.account.expenditure.application.service;

import com.ho.account.contracts.closing.AccountingPeriodStatusPort;
import com.ho.account.contracts.journal.JournalEntryCommand;
import com.ho.account.contracts.journal.JournalLineCommand;
import com.ho.account.contracts.journal.JournalPostingPort;
import com.ho.account.contracts.masterdata.BusinessPartnerRef;
import com.ho.account.contracts.masterdata.MasterDataQueryPort;
import com.ho.account.expenditure.application.port.in.PurchaseInvoiceCommand;
import com.ho.account.expenditure.application.port.in.PurchaseUseCase;
import com.ho.account.expenditure.application.port.out.PayableAccountMappingPort;
import com.ho.account.expenditure.application.port.out.PayablePersistencePort;
import com.ho.account.expenditure.application.port.out.PurchaseInvoicePersistencePort;
import com.ho.account.expenditure.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * [PurchaseService]
 * 매입 인보이스 관리 및 채무(Payable) 생성을 담당합니다.
 * 타 모듈(Master Data, Journal Ledger, Closing)과는 ID/Code/Port 기반으로 참조하여 결합도를 최소화합니다.
 */
@Service
@Transactional
public class PurchaseService implements PurchaseUseCase {

    private final PurchaseInvoicePersistencePort purchaseInvoicePersistencePort;
    private final PayablePersistencePort payablePersistencePort;
    private final MasterDataQueryPort masterDataQueryPort;
    private final JournalPostingPort journalPostingPort;
    private final PayableAccountMappingPort payableAccountMappingPort;
    private final AccountingPeriodStatusPort accountingPeriodStatusPort;

    public PurchaseService(PurchaseInvoicePersistencePort purchaseInvoicePersistencePort,
                           PayablePersistencePort payablePersistencePort,
                           MasterDataQueryPort masterDataQueryPort,
                           JournalPostingPort journalPostingPort,
                           PayableAccountMappingPort payableAccountMappingPort,
                           AccountingPeriodStatusPort accountingPeriodStatusPort) {
        this.purchaseInvoicePersistencePort = purchaseInvoicePersistencePort;
        this.payablePersistencePort = payablePersistencePort;
        this.masterDataQueryPort = masterDataQueryPort;
        this.journalPostingPort = journalPostingPort;
        this.payableAccountMappingPort = payableAccountMappingPort;
        this.accountingPeriodStatusPort = accountingPeriodStatusPort;
    }

    @Override
    public PurchaseInvoice createPurchaseInvoice(PurchaseInvoiceCommand command) {
        validateAccountingPeriodOpen(command.issueDate());

        PurchaseInvoice invoice = toInvoice(command);
        String vendorCode = invoice.getVendorCode();
        BusinessPartnerRef vendor = validateVendor(vendorCode);

        if (purchaseInvoicePersistencePort.findByInvoiceNoAndVendorCode(
                invoice.getInvoiceNo(), vendor.code()).isPresent()) {
            throw new IllegalArgumentException("Duplicate invoice number for vendor: " + vendor.name());
        }

        if (invoice.getStatus() == null) {
            invoice.setStatus(PurchaseInvoiceStatus.RECEIVED);
        }
        invoice.setCreatedBy(requireActor(invoice.getCreatedBy()));

        PurchaseInvoice savedInvoice = purchaseInvoicePersistencePort.save(invoice);

        // 1. Payable 생성 (ID 기반 참조 적용)
        Payable payable = new Payable();
        payable.setPurchaseInvoiceNo(savedInvoice.getInvoiceNo());
        payable.setPurchaseInvoiceVendorCode(savedInvoice.getVendorCode());
        payable.setVendorCode(savedInvoice.getVendorCode());
        payable.setOriginalAmount(savedInvoice.getTotalAmount());
        payable.setOutstandingAmount(savedInvoice.getTotalAmount());
        payable.setDueDate(savedInvoice.getDueDate());
        payable.setStatus(PayableStatus.OPEN);
        payablePersistencePort.save(payable);

        // 2. 전표 발행
        postPurchaseJournal(savedInvoice, vendor.name());

        return savedInvoice;
    }

    @Override
    public void updatePayableStatus(LocalDate asOfDate) {
        List<Payable> overduePayables = payablePersistencePort.findByDueDateBeforeAndStatusNot(asOfDate, PayableStatus.PAID);
        for (Payable payable : overduePayables) {
            payable.markAsOverdue();
            payablePersistencePort.save(payable);
            
            // 연관 인보이스 상태 업데이트 (ID 기반 조회 후 처리)
            purchaseInvoicePersistencePort.findByInvoiceNoAndVendorCode(
                    payable.getPurchaseInvoiceNo(), payable.getPurchaseInvoiceVendorCode())
                .ifPresent(pi -> {
                    if (pi.getStatus() != PurchaseInvoiceStatus.PARTIAL_PAID) {
                        pi.setStatus(PurchaseInvoiceStatus.OVERDUE);
                        purchaseInvoicePersistencePort.save(pi);
                    }
                });
        }
    }

    private PurchaseInvoice toInvoice(PurchaseInvoiceCommand command) {
        PurchaseInvoice invoice = new PurchaseInvoice();
        invoice.setInvoiceNo(command.invoiceNo());
        invoice.setVendorCode(command.vendorCode());
        invoice.setIssueDate(command.issueDate());
        invoice.setDueDate(command.dueDate());
        invoice.setTotalAmount(command.totalAmount());
        invoice.setTaxAmount(command.taxAmount());
        invoice.setNetAmount(command.netAmount());
        invoice.setCreatedBy(command.createdBy());
        invoice.setDescription(command.description());
        return invoice;
    }

    private BusinessPartnerRef validateVendor(String vendorCode) {
        return masterDataQueryPort.findBusinessPartner(vendorCode)
                .orElseThrow(() -> new IllegalArgumentException("Vendor info missing in master data: " + vendorCode));
    }

    private void postPurchaseJournal(PurchaseInvoice invoice, String vendorName) {
        PayableAccountMappingPort.PurchaseRecognitionAccounts accounts =
                payableAccountMappingPort.resolvePurchaseRecognitionAccounts(invoice);
        requireAccounts(accounts.requiredAccountCodes());

        journalPostingPort.createDraftEntry(new JournalEntryCommand(
                invoice.getIssueDate(),
                invoice.getIssueDate(),
                "Purchase: " + invoice.getInvoiceNo() + " - " + vendorName,
                "PURCHASE_RECOGNITION",
                null,
                null,
                invoice.getCreatedBy(),
                invoice.getCreatedBy(),
                "PURCHASE_INVOICE",
                invoice.getInvoiceNo() + "_" + invoice.getVendorCode(),
                List.of(
                        new JournalLineCommand("DEBIT", accounts.expenseAccountCode(), invoice.getNetAmount(), null, null,
                                invoice.getVendorCode(), "Purchase Expense"),
                        new JournalLineCommand("DEBIT", accounts.inputVatAccountCode(), invoice.getTaxAmount(), null, null,
                                invoice.getVendorCode(), "Input VAT"),
                        new JournalLineCommand("CREDIT", accounts.accountsPayableAccountCode(), invoice.getTotalAmount(), null, null,
                                invoice.getVendorCode(), "Accounts Payable"))));
    }

    private void requireAccounts(List<String> accountCodes) {
        for (String accountCode : accountCodes) {
            masterDataQueryPort.findAccountSubject(accountCode)
                    .orElseThrow(() -> new IllegalStateException("Account missing: " + accountCode));
        }
    }

    private String requireActor(String actor) {
        if (actor == null || actor.isBlank()) {
            throw new IllegalArgumentException("createdBy is required for purchase invoice creation");
        }
        return actor.trim();
    }

    /**
     * 회계기간 마감 여부를 사전에 검증합니다.
     *
     * 🎓 [금융 회계 내부 통제 및 마감 정합성 - Accounting Period Controls]
     * 회계 시스템에서 마감(CLOSED) 처리된 회계기간에 매입 인보이스 및 매입 전표가 작성되는 것을 사전에 차단합니다.
     *
     * 1. 소급 마감 차단 (Anti-Backdating):
     *    이미 월마감이 완료된 회계기간에 매입 인보이스를 소급 입력하면 매입채무와 매입비용이 소급 증가하여
     *    이미 확정된 매입부가세 및 당기순손익이 변경되는 회계적 오류가 발생합니다.
     * 2. 회계 내부 통제 이점 (Internal Control Benefits):
     *    인보이스 생성 및 매입 인식 전표 발행 전 회계기간 상태를 사전 검증(Fail-Closed)하여
     *    마감 기간에 대한 무단 거래 입력을 차단하고 내부 통제 준수성을 확보합니다.
     *
     * @param date 검증할 인보이스 발행일자(Issue Date)
     * @throws IllegalStateException 해당 회계기간이 이미 마감(CLOSED)된 경우
     */
    private void validateAccountingPeriodOpen(LocalDate date) {
        if (accountingPeriodStatusPort.isClosed(date)) {
            throw new IllegalStateException("해당 회계 반영일(" + date + ")은 이미 마감된 기간입니다.");
        }
    }
}
