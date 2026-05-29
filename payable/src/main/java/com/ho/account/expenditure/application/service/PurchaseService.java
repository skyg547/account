package com.ho.account.expenditure.application.service;

import com.ho.account.contracts.journal.JournalEntryCommand;
import com.ho.account.contracts.journal.JournalLineCommand;
import com.ho.account.contracts.journal.JournalPostingPort;
import com.ho.account.contracts.masterdata.MasterDataQueryPort;
import com.ho.account.expenditure.application.port.in.PurchaseUseCase;
import com.ho.account.expenditure.application.port.out.PayableAccountMappingPort;
import com.ho.account.expenditure.application.port.out.PayablePersistencePort;
import com.ho.account.expenditure.application.port.out.PurchaseInvoicePersistencePort;
import com.ho.account.expenditure.domain.*;
import com.ho.account.masterdata.core.application.port.out.BusinessPartnerPersistencePort;
import com.ho.account.masterdata.core.domain.model.BusinessPartner;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * [PurchaseService]
 * 매입 인보이스 관리 및 채무(Payable) 생성을 담당합니다.
 * 타 모듈(Master Data, Journal Ledger)과는 ID/Code 기반으로 참조하여 결합도를 최소화합니다.
 */
@Service
@Transactional
public class PurchaseService implements PurchaseUseCase {

    private final PurchaseInvoicePersistencePort purchaseInvoicePersistencePort;
    private final PayablePersistencePort payablePersistencePort;
    private final BusinessPartnerPersistencePort businessPartnerPersistencePort;
    private final MasterDataQueryPort masterDataQueryPort;
    private final JournalPostingPort journalPostingPort;
    private final PayableAccountMappingPort payableAccountMappingPort;

    public PurchaseService(PurchaseInvoicePersistencePort purchaseInvoicePersistencePort,
                           PayablePersistencePort payablePersistencePort,
                           BusinessPartnerPersistencePort businessPartnerPersistencePort,
                           MasterDataQueryPort masterDataQueryPort,
                           JournalPostingPort journalPostingPort,
                           PayableAccountMappingPort payableAccountMappingPort) {
        this.purchaseInvoicePersistencePort = purchaseInvoicePersistencePort;
        this.payablePersistencePort = payablePersistencePort;
        this.businessPartnerPersistencePort = businessPartnerPersistencePort;
        this.masterDataQueryPort = masterDataQueryPort;
        this.journalPostingPort = journalPostingPort;
        this.payableAccountMappingPort = payableAccountMappingPort;
    }

    @Override
    public PurchaseInvoice createPurchaseInvoice(PurchaseInvoice invoice) {
        String vendorCode = invoice.getVendorCode();
        validateVendor(vendorCode);
        
        BusinessPartner vendor = businessPartnerPersistencePort.findByBusinessPartnerCode(vendorCode)
                .orElseThrow(() -> new IllegalArgumentException("Vendor not found: " + vendorCode));

        if (purchaseInvoicePersistencePort.findByInvoiceNoAndVendorCode(
                invoice.getInvoiceNo(), vendor.getBusinessPartnerCode()).isPresent()) {
            throw new IllegalArgumentException("Duplicate invoice number for vendor: " + vendor.getBusinessPartnerName());
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
        postPurchaseJournal(savedInvoice, vendor.getBusinessPartnerName());

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

    private void validateVendor(String vendorCode) {
        masterDataQueryPort.findBusinessPartner(vendorCode)
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
}
